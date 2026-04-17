package hrln.interactive.sidenote

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import java.util.UUID

class EditNoteActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOTE_ID = "note_id"
    }

    private lateinit var titleEdit: EditText
    private lateinit var contentEdit: EditText
    private var noteId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_note)

        titleEdit = findViewById(R.id.edit_title)
        contentEdit = findViewById(R.id.edit_content)

        noteId = intent.getStringExtra(EXTRA_NOTE_ID)

        val deleteBtn = findViewById<View>(R.id.btn_delete)

        if (noteId != null) {
            val note = NoteStorage.loadNotes(this).find { it.id == noteId }
            note?.let {
                titleEdit.setText(it.title)
                contentEdit.setText(it.content)
            }
            deleteBtn.visibility = View.VISIBLE
            deleteBtn.setOnClickListener {
                NoteStorage.deleteNote(this, noteId!!)
                finish()
            }
        }

        // Focus content if opening a new note, title if editing existing
        if (noteId == null) {
            titleEdit.requestFocus()
        }

        findViewById<View>(R.id.btn_back).setOnClickListener {
            saveAndExit()
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        saveAndExit()
    }

    private fun saveAndExit() {
        val title = titleEdit.text.toString().trim()
        val content = contentEdit.text.toString().trim()
        if (title.isNotEmpty() || content.isNotEmpty()) {
            val note = Note(
                id = noteId ?: UUID.randomUUID().toString(),
                title = title.ifEmpty { "Untitled" },
                content = content,
                updatedAt = System.currentTimeMillis()
            )
            NoteStorage.saveNote(this, note)
        }
        finish()
    }
}
