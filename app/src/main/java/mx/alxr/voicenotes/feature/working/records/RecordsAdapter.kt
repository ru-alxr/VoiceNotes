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
import mx.alxr.voicenotes.feature.player.TICK_PERIOD
import mx.alxr.voicenotes.repository.record.RecordEntity
import mx.alxr.voicenotes.utils.extensions.getPosition
import mx.alxr.voicenotes.utils.logger.ILogger
import mx.alxr.voicenotes.utils.widgets.CheckableImageView
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class RecordsAdapter(
    @Suppress("unused") private val logger: ILogger,
    private val inflater: LayoutInflater,
    private val callback: ICallback,
    private val mMap: MutableMap<String, Int>
) :
    PagedListAdapter<RecordEntity, RecordsAdapter.RecordViewHolder>(DIFF_CALLBACK) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position)?.date ?: - position.toLong()
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
            ) = oldRecord == newRecord
        }
    }

    private var mState: PlaybackState = PlaybackState()

    private val dateFormat = DateFormat
        .getDateTimeInstance(
            DateFormat.SHORT,
            DateFormat.SHORT,
            Locale.getDefault()
        )

    fun setState(state: PlaybackState) {
        mState = state
        if (state.mpState == MediaPlayerState.Stopped) return
        getPosition { position -> getItem(position)?.uniqueId == state.uniqueId}
            .apply {
                if (invalid()) return
                notifyItemChanged(this)
            }
    }

    private fun Int.invalid(): Boolean {
        return this !in 0..itemCount
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val view = inflater.inflate(R.layout.item_record, parent, false)
        return RecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        if (position.invalid()) {
            return
        }
        val entity: RecordEntity? = getItem(position)
        entity?.apply {
            holder.bind(this)
        }
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
        private val share: View = view.findViewById(R.id.share_record_view),
        private val language: TextView = view.findViewById(R.id.language_view),
        private val delete:View = view.findViewById(R.id.delete_record_view)

    ) : RecyclerView.ViewHolder(view), View.OnClickListener, SeekBar.OnSeekBarChangeListener {

        init {
            play.setOnClickListener(this)
            status.setOnClickListener(this)
            recognize.setOnClickListener(this)
            share.setOnClickListener(this)
            language.setOnClickListener(this)
            delete.setOnClickListener(this)
        }

        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            val entity = getItem(adapterPosition) ?: return
            logger.with(this@RecordsAdapter).add("${entity.uniqueId} === $progress").log()
            mMap[entity.uniqueId] = progress
            if (progress == 0) {
                setDuration(seek.max.toLong())
            } else {
                setDuration(progress)
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
            val entity = getItem(adapterPosition) ?: return
            if (mState.uniqueId == entity.uniqueId) callback.onStartTrackingTouch()
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            val entity = getItem(adapterPosition) ?: return
            if (mState.uniqueId == entity.uniqueId) callback.onStopTrackingTouch()
        }

        override fun onClick(v: View?) {
            val position = adapterPosition
            if (position.invalid()) return
            getItem(position)?.apply {
                when (v) {
                    play -> callback.onPlayButtonClick(this)
                    share -> callback.requestShare(this)
                    recognize -> callback.requestGetTranscription(this)
                    status -> callback.requestSynchronize(this)
                    language -> callback.requestLanguageChange(this)
                    delete -> callback.requestDeleteRecord(this)
                }
            }
        }

        fun bind(entity: RecordEntity) {
            play.setImageResource(if (entity.isFileDownloaded) R.drawable.icon_pause_selector else R.drawable.ic_download_normal)
            seek.setOnSeekBarChangeListener(null)
            date.text = dateFormat.format(Date(entity.date))
            status.isEnabled = !entity.isSynchronized
            if (entity.isTranscribed) {
                transcription.text = entity.transcription
            } else {
                transcription.text = null
            }
            if (mMap[entity.uniqueId] == entity.duration.toInt()) {
                mMap[entity.uniqueId] = 0
            }
            val progress = mMap[entity.uniqueId] ?: 0
            val checked = mState.uniqueId == entity.uniqueId && mState.isPlaying() && progress < entity.duration
            play.isChecked = checked
            seek.max = entity.duration.toInt()
            // This trick I have to use because mediaplayer has inaccuracy of MediaPlayer.seekTo(int) method
            // as result it returns MediaPlayer.getCurrentPosition value less than was set in MediaPlayer.seekTo(int)
            // and seek pointer moves back a little, it is confusing
            val d = progress - seek.progress
            if (d > 0 || d < - TICK_PERIOD) seek.progress = progress
            if (checked) {
                setDuration(progress)
            } else {
                setDuration(entity.duration)
            }
            transcription.visibility = if (entity.isTranscribed) View.VISIBLE else View.GONE
            recognize.visibility = if (entity.isTranscribed) View.INVISIBLE else View.VISIBLE
            language.text = entity.languageCode.replace("-", " | ")
            if (mState.mpState == MediaPlayerState.Stopping) callback.onStopped()
            if (mState.mpState == MediaPlayerState.Pausing) callback.onPaused()
            seek.setOnSeekBarChangeListener(this@RecordViewHolder)
        }

        private fun setDuration(d: Number) {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(d.toLong())
            val seconds = TimeUnit.MILLISECONDS.toSeconds(d.toLong() + 500L) - TimeUnit.MINUTES.toSeconds(minutes)
            duration.text = String.format("%d:%02d", minutes, seconds)
        }
    }

}