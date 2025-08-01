package ceui.lisa.download;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;

import com.qmuiteam.qmui.skin.QMUISkinManager;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ceui.lisa.R;
import ceui.lisa.activities.BaseActivity;
import ceui.lisa.activities.Shaft;
import ceui.lisa.cache.Cache;
import ceui.lisa.core.DownloadItem;
import ceui.lisa.core.Manager;
import ceui.lisa.feature.HostManager;
import ceui.lisa.file.LegacyFile;
import ceui.lisa.file.OutPut;
import ceui.lisa.file.SAFile;
import ceui.lisa.http.ErrorCtrl;
import ceui.lisa.interfaces.Callback;
import ceui.lisa.interfaces.FeedBack;
import ceui.lisa.models.GifResponse;
import ceui.lisa.models.IllustsBean;
import ceui.lisa.models.ImageUrlsBean;
import ceui.lisa.models.NovelBean;
import ceui.lisa.models.NovelDetail;
import ceui.lisa.models.NovelSeriesItem;
import ceui.lisa.utils.Common;
import ceui.lisa.utils.Params;
import ceui.lisa.utils.PixivOperate;

import static android.provider.DocumentsContract.EXTRA_INITIAL_URI;

public class IllustDownload {

    private static DownloadItem buildDownloadItem(IllustsBean illust, int index) {
        return buildDownloadItem(illust, index, Params.IMAGE_RESOLUTION_ORIGINAL);
    }

    private static DownloadItem buildDownloadItem(IllustsBean illust, int index, String imageResolution) {
        if (illust.isGif()) {
            return null;
        } else if (illust.getPage_count() == 1) {
            DownloadItem item = new DownloadItem(illust, 0);
            item.setUrl(getUrl(illust, 0, imageResolution));
            item.setShowUrl(getShowUrl(illust, 0));
            return item;
        } else {
            DownloadItem item = new DownloadItem(illust, index);
            item.setUrl(getUrl(illust, index, imageResolution));
            item.setShowUrl(getShowUrl(illust, index));
            return item;
        }
    }

    public static void downloadIllustFirstPage(IllustsBean illust, BaseActivity<?> activity) {
        check(activity, () -> downloadIllustFirstPage(illust));
    }

    public static void downloadIllustFirstPageWithResolution(IllustsBean illust, String imageResolution, BaseActivity<?> activity) {
        check(activity, () -> {
            if (illust.getPage_count() == 1) {
                DownloadItem item = buildDownloadItem(illust, 0, imageResolution);
                Common.showToast('1' + Shaft.getContext().getString(R.string.has_been_added));
            }
        });
    }

    public static void downloadIllustFirstPage(IllustsBean illust) {
        downloadIllustFirstPageWithResolution(illust, Params.IMAGE_RESOLUTION_ORIGINAL);
    }

    public static void downloadIllustFirstPageWithResolution(IllustsBean illust, String imageResolution) {
        if (illust.getPage_count() == 1) {
            DownloadItem item = buildDownloadItem(illust, 0, imageResolution);
            Common.showToast('1' + Shaft.getContext().getString(R.string.has_been_added));
        }
    }

    public static void downloadIllustCertainPage(IllustsBean illust, int index, BaseActivity<?> activity) {
        check(activity, () -> {
            if (illust.getPage_count() == 1) {
                // index!=0 时不合理
                downloadIllustFirstPage(illust);
            } else {
                DownloadItem item = buildDownloadItem(illust, index);
                Common.showToast('1' + Shaft.getContext().getString(R.string.has_been_added));
            }
        });
    }

    public static void downloadIllustAllPages(IllustsBean illust, BaseActivity<?> activity) {
        check(activity, () -> downloadIllustAllPages(illust));
    }

    public static void downloadIllustAllPagesWithResolution(IllustsBean illust, String imageResolution, BaseActivity<?> activity) {
        check(activity, () -> {
            if (illust.getPage_count() == 1) {
                downloadIllustFirstPage(illust, activity);
            } else {
                List<DownloadItem> tempList = new ArrayList<>();
                for (int i = 0; i < illust.getPage_count(); i++) {
                    DownloadItem item = buildDownloadItem(illust, i, imageResolution);
                    tempList.add(item);
                }
                Common.showToast(tempList.size() + Shaft.getContext().getString(R.string.has_been_added));
            }
        });
    }

    public static void downloadIllustAllPages(IllustsBean illust) {
        if (illust.isGif()){
            downloadGif(illust);
        } else if (illust.getPage_count() == 1) {
            downloadIllustFirstPage(illust);
        } else {
            List<DownloadItem> tempList = new ArrayList<>();
            for (int i = 0; i < illust.getPage_count(); i++) {
                DownloadItem item = buildDownloadItem(illust, i);
                tempList.add(item);
            }
            Common.showToast(tempList.size() + Shaft.getContext().getString(R.string.has_been_added));
        }
    }


    public static void downloadCheckedIllustAllPages(List<IllustsBean> beans, BaseActivity<?> activity) {
        check(activity, () -> {
            List<DownloadItem> tempList = new ArrayList<>();
            int taskCount = 0;
            for (int i = 0; i < beans.size(); i++) {
                if (beans.get(i).isChecked()) {
                    final IllustsBean illust = beans.get(i);

                    if(illust.isGif()){
                        downloadGif(illust);
                        taskCount++;
                    } else if (illust.getPage_count() == 1) {
                        DownloadItem item = new DownloadItem(illust, 0);
                        item.setUrl(getUrl(illust, 0));
                        item.setShowUrl(getShowUrl(illust, 0));
                        tempList.add(item);
                        taskCount++;
                    } else {
                        for (int j = 0; j < illust.getPage_count(); j++) {
                            DownloadItem item = new DownloadItem(illust, j);
                            item.setUrl(getUrl(illust, j));
                            item.setShowUrl(getShowUrl(illust, j));
                            tempList.add(item);
                            taskCount++;
                        }
                    }
                }
            }
            Common.showToast(taskCount + Shaft.getContext().getString(R.string.has_been_added));
        });
    }

    public static DownloadItem downloadGif(GifResponse response, IllustsBean illust) {
        return downloadGif(response, illust, false);
    }

    public static DownloadItem downloadGif(GifResponse response, IllustsBean illust, boolean autoSave) {
        DownloadItem item = new DownloadItem(illust, 0);
        item.setAutoSave(autoSave);
        item.setUrl(HostManager.get().replaceUrl(response.getUgoira_metadata().getZip_urls().getMedium()));
        item.setShowUrl(HostManager.get().replaceUrl(illust.getImage_urls().getMedium()));
        return item;
    }

    public static void downloadGif(IllustsBean illustsBean){
        if(!illustsBean.isGif()){
            return;
        }
        PixivOperate.getGifInfo(illustsBean, new ErrorCtrl<GifResponse>() {
            @Override
            public void next(GifResponse gifResponse) {
                Cache.get().saveModel(Params.ILLUST_ID + "_" + illustsBean.getId(), gifResponse);
                downloadGif(gifResponse, illustsBean, true);
            }
        });
    }

    public static void downloadNovel(BaseActivity<?> activity, NovelSeriesItem novelSeriesItem, String content, Callback<Uri> targetCallback) {
        String displayName = FileCreator.deleteSpecialWords("NovelSeries_" + novelSeriesItem.getId() + "_Chapter_1~" + novelSeriesItem.getContent_count() + "_" + novelSeriesItem.getTitle() + ".txt");
        downloadNovel(activity, displayName, content, targetCallback);
    }

    public static String truncateTitle(String title, int maxLength) {
        if (title == null)  return " ";
        if (title.length() <= maxLength)  return title;
        if (maxLength < 3) return title.substring(0, maxLength);

        int available = maxLength - 3;
        int front = available / 2;
        int rear = available - front;

        return title.substring(0, front) + "..." + title.substring(title.length() - rear);
    }


    public static String getNovelText( String title , NovelBean novelBean, NovelDetail novelDetail) {
        String content = title +"\n\n"+
                "RawTitle:"+novelBean.getTitle().replaceAll("([第（(章卷篇幕回节册季话集])", "$1'")+"\n"+
                "Date:"+novelBean.getCreate_date().substring(0, 10)+" "+ "Length:"+novelBean.getText_length()+"\n"+
                "Name:"+novelBean.getUser().getName()+"(https://www.pixiv.net/users/"+novelBean.getUser().getId()+ ")\n" +
                "Source:"+"https://www.pixiv.net/novel/show.php?id="+novelBean.getId()+"\n"+
                "Tags:"+Arrays.toString(novelBean.getTagNames())+"\n"+
                "Caption:\n"+novelBean.getCaption().replaceAll("<br />", "\n")+
                "\n>---------------------<\n";
        content=content+ novelDetail.getNovel_text()+"\n\n";
        return content;
    }


    public static void downloadNovel(BaseActivity<?> activity, NovelBean novelBean, NovelDetail novelDetail, Callback<Uri> targetCallback) {

        String title = novelBean.getTitle();
        if (novelBean.getSeries()!= null && novelBean.getSeries().getTitle() != null){
            title=novelBean.getSeries().getTitle()+"_"+title;
        }
        String newTitle = truncateTitle(title, 58);
        String displayName = FileCreator.deleteSpecialWords("Novel_" + novelBean.getId() + "_" + newTitle + ".txt");

        String content = getNovelText(title, novelBean, novelDetail);
        downloadNovel(activity, displayName, content, targetCallback);
    }

    public static void downloadNovel(BaseActivity<?> activity, String displayName, String content, Callback<Uri> targetCallback) {
        check(activity, new FeedBack() {
            @Override
            public void doSomething() {
                File textFile = LegacyFile.textFile(activity, displayName);
                try {
                    OutputStream outStream = new FileOutputStream(textFile);
                    outStream.write(content.getBytes());
                    outStream.close();
                    Common.showLog("downloadNovel displayName " + textFile.getName());
                    OutPut.outPutNovel(activity, textFile, textFile.getName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Uri fileURI = FileProvider.getUriForFile(activity,
                        activity.getApplicationContext().getPackageName() + ".provider", textFile);
                if (targetCallback != null) {
                    targetCallback.doSomething(fileURI);
                }
            }
        });
    }

    public static void downloadFile(BaseActivity<?> activity, String displayName, String content, Callback<Uri> targetCallback) {
        check(activity, new FeedBack() {
            @Override
            public void doSomething() {
                File textFile = LegacyFile.textFile(activity, displayName);
                try {
                    OutputStream outStream = new FileOutputStream(textFile);
                    outStream.write(content.getBytes());
                    outStream.close();
                    Common.showLog("downloadFile displayName " + textFile.getName());
                    OutPut.outPutFile(activity, textFile, textFile.getName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Uri fileURI = FileProvider.getUriForFile(activity,
                        activity.getApplicationContext().getPackageName() + ".provider", textFile);
                if (targetCallback != null) {
                    targetCallback.doSomething(fileURI);
                }
            }
        });
    }

    public static void downloadBackupFile(BaseActivity<?> activity, String displayName, String content, Callback<Uri> targetCallback){
        check(activity, new FeedBack() {
            @Override
            public void doSomething() {
                File textFile = LegacyFile.textFile(activity, displayName);
                try {
                    OutputStream outStream = new FileOutputStream(textFile);
                    outStream.write(content.getBytes());
                    outStream.close();
                    Common.showLog("downloadBackupFile displayName " + textFile.getName());
                    OutPut.outPutBackupFile(activity, textFile, textFile.getName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Uri fileURI = FileProvider.getUriForFile(activity,
                        activity.getApplicationContext().getPackageName() + ".provider", textFile);
                if (targetCallback != null) {
                    targetCallback.doSomething(fileURI);
                }
            }
        });
    }

    public static String getUrl(IllustsBean illust, int index) {
        return getUrl(illust, index, Params.IMAGE_RESOLUTION_ORIGINAL);
    }

    public static String getUrl(IllustsBean illust, int index, String imageResolution) {
        return HostManager.get().replaceUrl(getImageUrlByResolution(illust, index, imageResolution));
    }

    private static String getImageUrlByResolution(IllustsBean illust, int index, String imageResolution) {
        ImageUrlsBean imageUrlsBean = getImageUrlsBean(illust, index, imageResolution);
        switch (imageResolution) {
            case Params.IMAGE_RESOLUTION_ORIGINAL:
                return imageUrlsBean.getOriginal();
            case Params.IMAGE_RESOLUTION_LARGE:
                return imageUrlsBean.getLarge();
            case Params.IMAGE_RESOLUTION_MEDIUM:
                return imageUrlsBean.getMedium();
            case Params.IMAGE_RESOLUTION_SQUARE_MEDIUM:
                return imageUrlsBean.getSquare_medium();
            default:
                return imageUrlsBean.getMaxImage();
        }
    }

    private static ImageUrlsBean getImageUrlsBean(IllustsBean illust, int index, String imageResolution) {
        if (illust.getPage_count() == 1) {
            return imageResolution.equals(Params.IMAGE_RESOLUTION_ORIGINAL) ? illust.getMeta_single_page() : illust.getImage_urls();
        } else {
            return illust.getMeta_pages().get(index).getImage_urls();
        }
    }

    public static String getShowUrl(IllustsBean illust, int index) {
        if (illust.getPage_count() == 1) {
            return illust.getImage_urls().getMedium();
        } else {
            return illust.getMeta_pages().get(index).getImage_urls().getMedium();
        }
    }

    public static void check(BaseActivity<?> activity, FeedBack feedBack) {
        if (Shaft.sSettings.getDownloadWay() == 1) {
            if (TextUtils.isEmpty(Shaft.sSettings.getRootPathUri())) {
                activity.setFeedBack(feedBack);
                new QMUIDialog.MessageDialogBuilder(activity)
                        .setTitle(activity.getResources().getString(R.string.string_143))
                        .setMessage(activity.getResources().getString(R.string.string_313))
                        .setSkinManager(QMUISkinManager.defaultInstance(activity))
                        .addAction(0, activity.getResources().getString(R.string.string_142),
                                QMUIDialogAction.ACTION_PROP_NEGATIVE,
                                (dialog, index) -> dialog.dismiss())
                        .addAction(0, activity.getResources().getString(R.string.string_312),
                                (dialog, index) -> {
                                    try {
                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                                                if (!TextUtils.isEmpty(Shaft.sSettings.getRootPathUri()) &&
                                                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                    Uri start = Uri.parse(Shaft.sSettings.getRootPathUri());
                                                    intent.putExtra(EXTRA_INITIAL_URI, start);
                                                }
                                                activity.startActivityForResult(intent, BaseActivity.ASK_URI);
                                            }
                                        }).start();
                                    } catch (Exception e) {
                                        Common.showToast(e.toString());
                                        e.printStackTrace();
                                    }
                                    dialog.dismiss();
                                })
                        .show();
            } else {
                DocumentFile root = SAFile.rootFolder(activity);
                if (root == null || !root.exists() || !root.isDirectory()) {
                    activity.setFeedBack(feedBack);
                    new QMUIDialog.MessageDialogBuilder(activity)
                            .setTitle(activity.getResources().getString(R.string.string_143))
                            .setMessage(activity.getResources().getString(R.string.string_365))
                            .setSkinManager(QMUISkinManager.defaultInstance(activity))
                            .addAction(0, activity.getResources().getString(R.string.string_142),
                                    QMUIDialogAction.ACTION_PROP_NEGATIVE,
                                    (dialog, index) -> dialog.dismiss())
                            .addAction(0, activity.getResources().getString(R.string.string_366),
                                    (dialog, index) -> {
                                        try {
                                            new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                                                    if (!TextUtils.isEmpty(Shaft.sSettings.getRootPathUri()) &&
                                                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                        Uri start = Uri.parse(Shaft.sSettings.getRootPathUri());
                                                        intent.putExtra(EXTRA_INITIAL_URI, start);
                                                    }
                                                    activity.startActivityForResult(intent, BaseActivity.ASK_URI);
                                                }
                                            }).start();
                                        } catch (Exception e) {
                                            Common.showToast(e.toString());
                                            e.printStackTrace();
                                        }
                                        dialog.dismiss();
                                    })
                            .show();
                } else {
                    if (feedBack != null) {
                        try {
                            feedBack.doSomething();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } else {
            if (feedBack != null) {
                try {
                    feedBack.doSomething();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
