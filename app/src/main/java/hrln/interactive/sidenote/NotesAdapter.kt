package hrln.interactive.sidenote

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.RecyclerView

class NotesAdapter(
    private val notes: List<Note>,
    private val onClick: (Note) -> Unit
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    inner class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val noteCard: View = view.findViewById(R.id.note_card)
        val deleteBg: View = view.findViewById(R.id.delete_bg)
        val titleView: TextView = view.findViewById(R.id.note_title)
        val previewView: TextView = view.findViewById(R.id.note_preview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.titleView.text = note.title.ifBlank { "Untitled" }
        holder.previewView.text = note.content.ifBlank { "No content" }
        holder.noteCard.setOnClickListener { onClick(note) }

        // Reset translation in case this view was recycled mid-swipe
        holder.noteCard.translationX = 0f

        // Size the delete square to match the card height once laid out
        holder.noteCard.doOnLayout { card ->
            val size = card.height
            if (holder.deleteBg.layoutParams.width != size) {
                holder.deleteBg.layoutParams.width = size
                holder.deleteBg.requestLayout()
            }
        }
    }

    override fun getItemCount() = notes.size
}
