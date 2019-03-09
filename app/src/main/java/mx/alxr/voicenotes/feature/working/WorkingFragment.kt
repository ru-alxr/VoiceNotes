package mx.alxr.voicenotes.feature.working

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_working.*
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.feature.working.home.HomeFragment
import mx.alxr.voicenotes.feature.working.settings.SettingsFragment
import mx.alxr.voicenotes.utils.logger.ILogger
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel

class WorkingFragment : Fragment(), Observer<Model> {

    private val mLogger: ILogger by inject()

    private val mViewModel: WorkingViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_working, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mLogger.with(this).add("onViewCreated").log()
        mViewModel.getLiveModel().observe(this, this)
        bottom_navigation.itemIconTintList = null
        bottom_navigation.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.menu_home -> mViewModel.onTabSelected(TAB_HOME)
                R.id.menu_settings -> mViewModel.onTabSelected(TAB_SETTINGS)
            }
            true
        }
    }

    override fun onChanged(model: Model?) {
        mLogger.with(this).add("onChanged $model").log()
        model?.apply {
            when (selectedTab) {
                TAB_HOME -> applyFragmentsVisibility(R.id.container1)
                TAB_SETTINGS -> applyFragmentsVisibility(R.id.container2)
            }
        }
    }

    private fun applyFragmentsVisibility(id: Int) {
        val containers = arrayOf(container1, container2)
        for (view in containers) {
            val flag = id == view.id
            view.visibility = if (flag) View.VISIBLE else View.INVISIBLE
            if (!flag) continue
            if (view.childCount > 0) continue
            addChild(id)
        }
    }

    private fun addChild(id: Int) {
        mLogger.with(this).add("addChild $id").log()
        var fragment: Fragment? = childFragmentManager.findFragmentById(id)
        if (fragment == null) {
            when (id) {
                R.id.container1 -> fragment = HomeFragment()
                else -> fragment = SettingsFragment()
            }
        }
        childFragmentManager.beginTransaction().add(id, fragment).commit()
    }

}