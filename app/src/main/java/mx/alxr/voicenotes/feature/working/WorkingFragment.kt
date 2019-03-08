package mx.alxr.voicenotes.feature.working

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_working.*
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.feature.SettingsFragment
import mx.alxr.voicenotes.feature.home.HomeFragment
import mx.alxr.voicenotes.utils.logger.ILogger
import org.koin.android.ext.android.inject

class WorkingFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_working, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bottom_navigation.itemIconTintList = null
        bottom_navigation.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.menu_home -> applyFragmentsVisibility(R.id.container1)
                R.id.menu_settings -> applyFragmentsVisibility(R.id.container2)
            }
            true
        }
        applyFragmentsVisibility(R.id.container1)

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
        val fragment: Fragment
        when (id) {
            R.id.container1 -> fragment = HomeFragment()
            else -> fragment = SettingsFragment()
        }
        childFragmentManager.beginTransaction().add(id, fragment).commit()
    }

}