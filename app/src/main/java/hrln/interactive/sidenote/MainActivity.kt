package hrln.interactive.sidenote

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private var notes = mutableListOf<Note>()
    private lateinit var adapter: NotesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.notes_recycler)
        emptyView = findViewById(R.id.empty_view)

        adapter = NotesAdapter(notes) { note -> openEditor(note.id) }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Update on every scroll delta — fires continuously during fling/drag
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) = updateOpacities()
        })

        // Also update the instant a new child view is attached (before it paints),
        // which prevents the brief full-opacity flash when items enter from the edges.
        recyclerView.addOnChildAttachStateChangeListener(object : RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) = updateOpacities()
            override fun onChildViewDetachedFromWindow(view: View) {}
        })

        findViewById<View>(R.id.btn_new_note).setOnClickListener { openEditor(null) }
    }

    override fun onResume() {
        super.onResume()
        refreshNotes()
    }

    private fun refreshNotes() {
        notes.clear()
        notes.addAll(NoteStorage.loadNotes(this))
        adapter.notifyDataSetChanged()
        emptyView.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (notes.isEmpty()) View.GONE else View.VISIBLE
        recyclerView.post { updateOpacities() }
    }

    private fun updateOpacities() {
        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i)
            val itemHeight = child.height.toFloat().takeIf { it > 0f } ?: continue
            // Continuous slot: 0.0 = at top of list, 1.0 = one item height below, etc.
            val slot = child.top / itemHeight
            // Each slot drops opacity by 0.2, floored at 0.1 so items never fully vanish
            child.alpha = (1.0f - slot * 0.2f).coerceIn(0.1f, 1.0f)
        }
    }

    private fun openEditor(noteId: String?) {
        val intent = Intent(this, EditNoteActivity::class.java)
        if (noteId != null) intent.putExtra(EditNoteActivity.EXTRA_NOTE_ID, noteId)
        startActivity(intent)
    }
}
