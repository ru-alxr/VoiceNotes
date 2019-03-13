package mx.alxr.voicenotes.feature.working.records

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.repository.record.RecordEntity
import mx.alxr.voicenotes.utils.logger.ILogger
import mx.alxr.voicenotes.utils.widgets.CheckableImageView
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class RecordsAdapter(
    @Suppress("unused") private val logger: ILogger,
    private val inflater: LayoutInflater,
    private val callback: ICallback,
    private val recyclerView: RecyclerView
) :
    PagedListAdapter<RecordEntity, RecordsAdapter.RecordViewHolder>(DIFF_CALLBACK),
    SeekBar.OnSeekBarChangeListener {

    private var mCheckedId: Long = -1
    private var mProgress: Int = 0
    private var mTracking: Boolean = false
    private lateinit var mState: PlaybackState

    fun setState(id: Long, progress: Int, state: PlaybackState, isTracking: Boolean) {
        val oldProgress = mProgress
        mCheckedId = id
        mProgress = progress
        mState = state
        mTracking = isTracking
        if (Math.abs(oldProgress - progress) < 500 && isTracking) return
        recyclerView.post { notifyDataSetChanged() }
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    private fun isCurrentRecord(entity: RecordEntity): Boolean {
        return entity.crc32 == mCheckedId
    }

    private fun Int.invalid(): Boolean {
        return this !in 0..itemCount
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val view = inflater.inflate(R.layout.item_record, parent, false)
        return RecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        if (position.invalid()) return
        val entity: RecordEntity? = getItem(position)
        entity?.apply {
            holder.bind(this)
        }
    }

    companion object {
        private val DIFF_CALLBACK = object :
            DiffUtil.ItemCallback<RecordEntity>() {
            override fun areItemsTheSame(
                oldRecord: RecordEntity,
                newRecord: RecordEntity
            ) = oldRecord.crc32 == newRecord.crc32

            override fun areContentsTheSame(
                oldRecord: RecordEntity,
                newRecord: RecordEntity
            ) = oldRecord.crc32 == newRecord.crc32
        }
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (!fromUser) return
        callback.onSeekBarChange(progress)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        callback.onStartTrackingTouch()
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        callback.onStopTrackingTouch()
    }

    inner class RecordViewHolder(
        view: View,
        private val date: TextView = view.findViewById(R.id.date_view),
        private val status: View = view.findViewById(R.id.synchronization_status_view),
        private val play: CheckableImageView = view.findViewById(R.id.play_view),
        private val seek: SeekBar = view.findViewById(R.id.seek_bar_view),
        private val duration: TextView = view.findViewById(R.id.duration_view),
        private val transcription: TextView = view.findViewById(R.id.transcription_view),
        private val recognize: ImageView = view.findViewById(R.id.recognize_voice_view),
        private val share:View = view.findViewById(R.id.share_record_view)

    ) : RecyclerView.ViewHolder(view), View.OnClickListener {

        init {
            play.setOnClickListener(this)
            status.setOnClickListener(this)
            recognize.setOnClickListener(this)
            share.setOnClickListener(this)
            seek.setOnSeekBarChangeListener(this@RecordsAdapter)
        }

        override fun onClick(v: View?) {
            val position = adapterPosition
            if (position.invalid()) return
            getItem(position)?.apply{
                when (v) {
                    play -> callback.onPlayButtonClick(this)
                    share -> callback.requestShare(this)
                    recognize -> callback.requestGetTranscription(this)
                    status -> callback.requestSynchronize(this)
                }
            }
        }

        fun bind(entity: RecordEntity) {
            date.text = dateFormat.format(Date(entity.date))
            status.isEnabled = !entity.isSynchronized
            if (entity.isTranscribed) {
                transcription.text = entity.transcription
            } else {
                transcription.setText(R.string.transcription_is_not_ready)
            }
            val isCurrentRecord = isCurrentRecord(entity)
            play.isChecked = isCurrentRecord && mState == PlaybackState.Playing
            if (isCurrentRecord) {
                setDuration(mProgress)
                seek.isEnabled = true
                if (!mTracking) seek.progress = (100 * mProgress / entity.duration).toInt()
            } else {
                setDuration(entity.duration)
                seek.isEnabled = false
                seek.progress = 0
            }
            transcription.visibility = if (entity.isTranscribed) View.VISIBLE else View.GONE
            recognize.visibility = if (entity.isTranscribed) View.INVISIBLE else View.VISIBLE
        }

        private fun setDuration(d: Number) {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(d.toLong())
            val seconds = TimeUnit.MILLISECONDS.toSeconds(d.toLong() + 500L) - TimeUnit.MINUTES.toSeconds(minutes)
            duration.text = String.format("%d:%02d", minutes, seconds)
        }
    }

}