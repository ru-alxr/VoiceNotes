package mx.alxr.voicenotes.feature.working.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
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
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.android.synthetic.main.fragment_home.*
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.utils.extensions.*
import mx.alxr.voicenotes.utils.logger.ILogger
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel

class HomeFragment : Fragment(), Observer<Model>, View.OnTouchListener, PermissionListener {

    companion object {
        const val SCALE: Float = 1.5F
        const val LIMIT_FACTOR = 1.0F

        const val STOP_RECORDING_ANIMATION_DURATION = 100L
        const val HIDE_CANCEL_VIEW_ANIMATION_DURATION = 300L
        const val START_RECORDING_ANIMATION_DURATION = 600L

        const val START_RECORDING_VIBRATION_DURATION = 50L
        const val STOP_RECORDING_VIBRATION_DURATION = 50L
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
        if (model.isStopRecordingRequested) {
            stopRecording()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when (event?.action) {
            ACTION_DOWN -> startRecording()
            ACTION_UP -> mViewModel.onStopRecordingRequested()
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
            .setCustomDuration(STOP_RECORDING_ANIMATION_DURATION)
            .setCustomFillAfter(true)
        record_audio_view.startAnimation(anim)
        activity?.vibrate(STOP_RECORDING_VIBRATION_DURATION)
    }

    private fun startRecording() {
        activity?.apply {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                PackageManager.PERMISSION_GRANTED == checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            ) {
                startRecordingWithPermissionGranted()
            } else {
                Dexter
                    .withActivity(this)
                    .withPermission(Manifest.permission.RECORD_AUDIO)
                    .withListener(this@HomeFragment)
                    .check()
            }
        }
    }

    private fun startRecordingWithPermissionGranted() {
        mViewModel.onRecordingStarted()
        val anim = ScaleAnimation(
            1f, SCALE,
            1f, SCALE,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
            .onAnimationEnd {
                mViewModel.onRecordingUIReady()
                activity?.vibrate(START_RECORDING_VIBRATION_DURATION)
            }
            .setCustomDuration(START_RECORDING_ANIMATION_DURATION)
            .setCustomFillAfter(true)
        record_audio_view.startAnimation(anim)
    }

    private fun hideCancelRecordingView(recordingInProgress: Boolean) {
        if (recordingInProgress) return
        if (cancel_recording_view.visibility == View.INVISIBLE) return
        cancel_recording_view
            .startAnimation(
                AlphaAnimation(1F, 0F)
                    .setCustomDuration(HIDE_CANCEL_VIEW_ANIMATION_DURATION)
                    .onAnimationEnd {
                        cancel_recording_view.visibility = View.INVISIBLE
                        mViewModel.onCancelRecordingHandled()
                    }
            )
    }

    override fun onPermissionGranted(response: PermissionGrantedResponse?) {
    }

    override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest?, token: PermissionToken?) {
        showDualSelectorDialog(
            message = getString(R.string.record_audio_permission_rationale),
            negativeLabel = R.string.record_audio_permission_rationale_negative,
            positiveLabel = R.string.record_audio_permission_rationale_positive,
            positive = { token?.continuePermissionRequest() },
            negative = { token?.cancelPermissionRequest() }
        )
    }

    override fun onPermissionDenied(response: PermissionDeniedResponse?) {
        if (response?.isPermanentlyDenied != false) {
            showDualSelectorDialog(
                message = getString(R.string.record_audio_permission_permanently_denied_message),
                negativeLabel = R.string.record_audio_permission_permanently_denied_negative,
                positiveLabel = R.string.record_audio_permission_permanently_denied_positive,
                positive = { activity?.goAppSettings() }
            )
        }
    }

    override fun onPause() {
        super.onPause()
        mViewModel.onStopRecordingRequested()
    }
}