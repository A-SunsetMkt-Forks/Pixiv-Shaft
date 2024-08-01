package ceui.pixiv.ui.user

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import ceui.lisa.R
import ceui.lisa.databinding.FragmentHomeBinding
import ceui.loxia.Client
import ceui.pixiv.PixivFragment
import ceui.pixiv.pixivListViewModel
import ceui.pixiv.setUpStaggerLayout
import ceui.pixiv.ui.IllustCardHolder
import ceui.refactor.viewBinding

class UserCreatedIllustsFragment : PixivFragment(R.layout.fragment_home) {

    private val binding by viewBinding(FragmentHomeBinding::bind)
    private val args by navArgs<UserCreatedIllustsFragmentArgs>()
    private val viewModel by pixivListViewModel(
        loader = { Client.appApi.getUserCreatedIllusts(args.userId, args.objectType) },
        mapper = { illust -> listOf(IllustCardHolder(illust)) }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpStaggerLayout(binding, viewModel)
    }
}