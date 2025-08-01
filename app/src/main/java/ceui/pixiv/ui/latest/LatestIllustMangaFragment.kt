package ceui.pixiv.ui.latest

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import ceui.lisa.R
import ceui.lisa.databinding.FragmentPagedListBinding
import ceui.loxia.Client
import ceui.loxia.Illust
import ceui.loxia.KListShow
import ceui.pixiv.paging.PagingAPIRepository
import ceui.pixiv.paging.pagingViewModel
import ceui.pixiv.ui.common.IllustCardHolder
import ceui.pixiv.ui.common.ListItemHolder
import ceui.pixiv.ui.common.PixivFragment
import ceui.pixiv.ui.common.setUpPagedList
import ceui.pixiv.ui.common.viewBinding

class LatestIllustMangaFragment : PixivFragment(R.layout.fragment_paged_list) {

    private val binding by viewBinding(FragmentPagedListBinding::bind)
    private val safeArgs by navArgs<LatestIllustMangaFragmentArgs>()
    private val viewModel by pagingViewModel({ safeArgs.objectType }) { objectType ->
        object : PagingAPIRepository<Illust>() {
            override suspend fun loadFirst(): KListShow<Illust> {
                return Client.appApi.getLatestIllustManga(objectType)
            }

            override fun mapper(entity: Illust): List<ListItemHolder> {
                return listOf(IllustCardHolder(entity))
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpPagedList(binding, viewModel)
    }
}

