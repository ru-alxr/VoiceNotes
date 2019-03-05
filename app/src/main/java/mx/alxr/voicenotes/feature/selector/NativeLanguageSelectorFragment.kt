package mx.alxr.voicenotes.feature.selector


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.utils.logger.ILogger
import org.koin.android.ext.android.inject

class NativeLanguageSelectorFragment : Fragment() {


    private val mLogger: ILogger by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mLogger.with(this).add("onViewCreated").log()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_native_language_selector, container, false)
    }


}
