package ceui.pixiv.ui.task

import android.os.Parcelable
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import ceui.lisa.R
import ceui.lisa.database.AppDatabase
import ceui.lisa.models.ModelObject
import ceui.lisa.models.ObjectSpec
import ceui.lisa.utils.Common
import ceui.loxia.Client
import ceui.loxia.Illust
import ceui.loxia.KListShow
import ceui.loxia.Novel
import ceui.loxia.pushFragment
import ceui.pixiv.db.GeneralEntity
import ceui.pixiv.db.RecordType
import ceui.pixiv.ui.common.findCurrentFragmentOrNull
import ceui.pixiv.ui.common.getFileSize
import ceui.pixiv.utils.TokenGenerator
import com.blankj.utilcode.util.PathUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hjq.toast.ToastUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object PixivTaskType {
    const val DownloadAll = 1
    const val BookmarkAll = 2
    const val DownloadSeriesNovels = 3
}

@Parcelize
data class HumanReadableTask(
    val taskUUID: String,
    val taskFullName: String,
    val taskType: Int,
    val createdTime: Long,
) : ModelObject, Parcelable {
    override val objectUniqueId: Long
        get() = taskUUID.hashCode().toLong()
    override val objectType: Int
        get() = ObjectSpec.HUMAN_READABLE_TASK
}

open class FetchAllTask<Item, ResponseT : KListShow<Item>>(
    private val activity: FragmentActivity,
    taskFullName: String,
    taskType: Int,
    initialLoader: suspend () -> KListShow<Item>
) {

    private val results = mutableListOf<Item>()
    private val gson = Gson()

    init {
        activity.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    if (results.isNotEmpty()) {
                        results.clear()
                    }
                    var nextPageUrl: String? = null

                    Common.showLog("FetchAllTask initialLoader start ${results.size}")

                    // Load initial page
                    val resp = initialLoader()
                    if (resp.displayList.isNotEmpty()) {
                        results.addAll(resp.displayList)
                        Common.showLog("FetchAllTask initialLoader end ${results.size}")
                        nextPageUrl = resp.nextPageUrl
                    }

                    // Fetch subsequent pages
                    while (!nextPageUrl.isNullOrEmpty()) {
                        val responseClass = resp::class.java
                        Common.showLog("FetchAllTask subsequent start ${results.size}")
                        delay(1500L)
                        val responseBody = Client.appApi.generalGet(nextPageUrl)
                        val responseJson = responseBody.string()
                        val response = gson.fromJson(responseJson, responseClass) as ResponseT

                        if (response.displayList.isNotEmpty()) {
                            results.addAll(response.displayList)
                        }
                        Common.showLog("FetchAllTask subsequent end ${results.size}")
                        nextPageUrl = response.nextPageUrl
                    }

                    val taskUUID = TokenGenerator.generateToken()
                    // Serialize results to JSON and write to cache file
                    val json = gson.toJson(results)
                    val cacheFile =
                        File(PathUtils.getInternalAppCachePath(), "task-result-${taskUUID}.text")

                    BufferedWriter(
                        OutputStreamWriter(
                            FileOutputStream(cacheFile), "UTF-8"
                        )
                    ).use { writer ->
                        writer.write(json)
                    }

                    val humanReadableTask = HumanReadableTask(
                        taskUUID, taskFullName, taskType, System.currentTimeMillis()
                    )

                    AppDatabase.getAppDatabase(activity).generalDao().insert(
                        GeneralEntity(
                            taskUUID.hashCode().toLong(),
                            gson.toJson(humanReadableTask),
                            ObjectSpec.USER_TASK,
                            RecordType.USER_TASK,
                        )
                    )

                    val fileSize = getFileSize(cacheFile)
                    Common.showLog("FetchAllTask fileSize ${fileSize}")

                    withContext(Dispatchers.Main) {
                        onEnd(humanReadableTask, results)
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    // Handle exceptions accordingly
                }
            }
        }
    }

    open fun onEnd(humanReadableTask: HumanReadableTask, results: List<Item>) {
        activity.findCurrentFragmentOrNull()?.pushFragment(
            R.id.navigation_cache_list, CacheFileFragmentArgs(task = humanReadableTask).toBundle()
        )
        ToastUtils.show("全部结束")
        Common.showLog("FetchAllTask all end ${this.results.size}")
    }
}

fun loadIllustsFromCache(taskUUID: String): List<Illust>? {
    val cacheFile = File(PathUtils.getInternalAppCachePath(), "task-result-${taskUUID}.text")
    return if (cacheFile.exists()) {
        try {
            val json = BufferedReader(
                InputStreamReader(
                    FileInputStream(cacheFile), "UTF-8"
                )
            ).use { reader ->
                reader.readText()
            }
            val type = object : TypeToken<List<Illust>>() {}.type
            Gson().fromJson<List<Illust>>(json, type)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    } else {
        null
    }
}

fun loadNovelsFromCache(taskUUID: String): List<Novel>? {
    val cacheFile = File(PathUtils.getInternalAppCachePath(), "task-result-${taskUUID}.text")
    return if (cacheFile.exists()) {
        try {
            val json = BufferedReader(
                InputStreamReader(
                    FileInputStream(cacheFile), "UTF-8"
                )
            ).use { reader ->
                reader.readText()
            }
            val type = object : TypeToken<List<Novel>>() {}.type
            Gson().fromJson<List<Novel>>(json, type)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    } else {
        null
    }
}