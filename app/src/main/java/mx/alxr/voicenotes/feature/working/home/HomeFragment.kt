package mx.alxr.voicenotes.feature.working.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.MotionEvent.*
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_home.*
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.utils.extensions.format
import mx.alxr.voicenotes.utils.extensions.vibrate
import mx.alxr.voicenotes.utils.logger.ILogger
import mx.alxr.voicenotes.utils.views.onAnimationEnd
import mx.alxr.voicenotes.utils.views.setCustomDuration
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel

class HomeFragment : Fragment(), Observer<Model>, View.OnTouchListener {

    companion object {
        const val SCALE: Float = 1.5F
        const val LIMIT_FACTOR = 1.0F
    }

    private val mViewModel: HomeViewModel by viewModel()
    private val mLogger: ILogger by inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mLogger.with(this).add("onViewCreated").log()
        mViewModel.getLiveModel().observe(this, this)

        record_audio_view.setOnTouchListener(this)
    }

    override fun onChanged(model: Model?) {
        if (model == null) return
        synchronization_status_view
            .apply {
                isEnabled = model.isSynchronizationEnabled
                text = if (model.isSynchronizationEnabled) {
                    getString(R.string.synchronization_status_enabled)
                } else {
                    getString(R.string.synchronization_status_disabled)
                }
            }

        native_language_view
            .apply {
                text = if (model.language.isEmpty()) getString(R.string.native_language_empty)
                else format(R.string.native_language, model.language)
            }

        cancel_recording_view.visibility = if (model.isPointerOut) View.VISIBLE else View.INVISIBLE
        hideCancelRecordingView(model.isRecordingInProgress)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when (event?.action) {
            ACTION_DOWN -> startRecording()
            ACTION_UP -> stopRecording()
            ACTION_MOVE -> v?.apply { checkIfActionChangeRequired(event, this) }
        }
        return true
    }

    private fun checkIfActionChangeRequired(event: MotionEvent, view: View) {
        val limit = view.width * LIMIT_FACTOR
        val distanceToViewCenterX = view.width / 2 - event.x
        val distanceToViewCenterY = view.width / 2 - event.y
        val isPointerOutX = distanceToViewCenterX > limit
        val isPointerOutY = distanceToViewCenterY > limit
        mViewModel.onTouchOverMoved(isPointerOutX || isPointerOutY)
    }

    private fun stopRecording() {
        mViewModel.onRecordingStopped()
        val anim = ScaleAnimation(
            SCALE, 1f,
            SCALE, 1f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        anim.fillAfter = true
        anim.duration = 300
        record_audio_view.startAnimation(anim)
        activity?.vibrate(50L)
    }

    private fun startRecording() {
        mViewModel.onRecordingStarted()
        val anim = ScaleAnimation(
            1f, SCALE,
            1f, SCALE,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        anim.fillAfter = true
        anim.duration = 600
        record_audio_view.startAnimation(anim)
        activity?.vibrate(50L)
    }

    private fun hideCancelRecordingView(recordingInProgress: Boolean) {
        if (recordingInProgress) return
        if (cancel_recording_view.visibility == View.INVISIBLE) return
        cancel_recording_view
            .startAnimation(
                AlphaAnimation(1F, 0F)
                    .setCustomDuration(600L)
                    .onAnimationEnd {
                    cancel_recording_view.visibility = View.INVISIBLE
                    mViewModel.onCancelRecordingHandled()
                }
            )
    }

}