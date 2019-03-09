package mx.alxr.voicenotes.feature.selector

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.repository.language.LanguageEntity
import mx.alxr.voicenotes.utils.logger.ILogger

class LanguageAdapter(
    private val inflater: LayoutInflater,
    private val logger: ILogger,
    private val callback:SelectionCallback
) :
    PagedListAdapter<LanguageEntity, LanguageAdapter.LanguageViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val view = inflater.inflate(R.layout.item_language, parent, false)
        return LanguageViewHolder(view)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        val entity: LanguageEntity? = getItem(position)
        entity?.apply {
            holder.text.text = name
        }
    }

    companion object {
        private val DIFF_CALLBACK = object :
            DiffUtil.ItemCallback<LanguageEntity>() {
            override fun areItemsTheSame(
                oldLang: LanguageEntity,
                newLang: LanguageEntity
            ) = oldLang.code == newLang.code

            override fun areContentsTheSame(
                oldLang: LanguageEntity,
                newLang: LanguageEntity
            ) = oldLang.code == newLang.code
        }
    }

    inner class LanguageViewHolder(
        view: View,
        val text: TextView = view.findViewById(R.id.text)
    ) : RecyclerView.ViewHolder(view), View.OnClickListener{

        init {
            view.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            callback.onSelected(getItem(adapterPosition))
        }
    }

}

interface SelectionCallback{
    fun onSelected(entity: LanguageEntity?)
}