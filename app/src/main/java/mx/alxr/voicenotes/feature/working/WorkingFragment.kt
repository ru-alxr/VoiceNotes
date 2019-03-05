package mx.alxr.voicenotes.feature.working

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.utils.logger.ILogger
import org.koin.android.ext.android.inject

class WorkingFragment : Fragment() {


    private val mLogger: ILogger by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mLogger.with(this).add("onViewCreated").log()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_working, container, false)
    }

}