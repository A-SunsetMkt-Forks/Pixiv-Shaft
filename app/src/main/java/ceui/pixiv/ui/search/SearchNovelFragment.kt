package ceui.pixiv.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import ceui.lisa.R
import ceui.lisa.databinding.FragmentPagedListBinding
import ceui.lisa.databinding.ItemRadioButtonsBinding
import ceui.loxia.ObjectType
import ceui.loxia.observeEvent
import ceui.pixiv.paging.pagingViewModel
import ceui.pixiv.ui.common.ListMode
import ceui.pixiv.ui.common.PixivFragment
import ceui.pixiv.ui.common.setUpPagedList
import ceui.pixiv.ui.common.viewBinding
import ceui.pixiv.widgets.DialogViewModel

class SearchNovelFragment : PixivFragment(R.layout.fragment_paged_list) {
    private val searchViewModel by viewModels<SearchViewModel>(ownerProducer = { requireParentFragment() })
    private val dialogViewModel by activityViewModels<DialogViewModel>()
    private val binding by viewBinding(FragmentPagedListBinding::bind)
    private val viewModel by pagingViewModel({
        Pair(
            searchViewModel,
            dialogViewModel
        )
    }) { (vm, dialogVM) ->
        SearchNovelDataSource {
            val count = dialogVM.chosenUsersYoriCount.value
            vm.buildSearchConfig(count, ObjectType.NOVEL)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpPagedList(binding, viewModel, ListMode.VERTICAL)
        searchViewModel.searchNovelEvent.observeEvent(viewLifecycleOwner) {
            viewModel.refresh()
        }

        val layout = ItemRadioButtonsBinding.inflate(LayoutInflater.from(requireContext()))

        binding.listHeader.addView(layout.root)

        val radioTab = layout.radioTab
        val usersYori = layout.usersYori

        radioTab.setTabs(
            listOf(
                "热度预览",
                "从新到旧",
                "从旧到新",
                "热度排序",
            )
        )
        radioTab.setItemCickListener { index ->
            searchViewModel.novelSelectedRadioTabIndex.value = index
            val now = System.currentTimeMillis()
            searchViewModel.triggerSearchNovelEvent(now)
        }
        searchViewModel.novelSelectedRadioTabIndex.observe(viewLifecycleOwner) { index ->
            radioTab.selectTab(index)
            usersYori.isVisible = (index == 1) || (index == 2)
        }
    }
}