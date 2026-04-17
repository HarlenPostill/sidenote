package hrln.interactive.sidenote

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.UUID

class MainActivity : AppCompatActivity() {

    enum class Screen { LIST, EDIT }

    private var screen = Screen.LIST
    private var currentNoteId: String? = null

    private val notes = mutableListOf<Note>()
    private lateinit var adapter: NotesAdapter

    // Views
    private lateinit var contentArea: FrameLayout
    private lateinit var listView: FrameLayout
    private lateinit var editView: LinearLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var editTitle: EditText
    private lateinit var editContent: EditText
    private lateinit var btnAction: LinearLayout
    private lateinit var arrowLeft: ImageView
    private lateinit var arrowRight: ImageView
    private lateinit var titleChars: LinearLayout
    private lateinit var btnTextChars: LinearLayout
    private lateinit var btnDelete: ImageView

    // Slot animators
    private lateinit var titleAnimator: SlotTextAnimator
    private lateinit var btnTextAnimator: SlotTextAnimator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bind views
        contentArea = findViewById(R.id.content_area)
        listView = findViewById(R.id.list_view)
        editView = findViewById(R.id.edit_view)
        recyclerView = findViewById(R.id.notes_recycler)
        emptyView = findViewById(R.id.empty_view)
        editTitle = findViewById(R.id.edit_title)
        editContent = findViewById(R.id.edit_content)
        btnAction = findViewById(R.id.btn_action)
        arrowLeft = findViewById(R.id.arrow_left)
        arrowRight = findViewById(R.id.arrow_right)
        titleChars = findViewById(R.id.title_chars)
        btnTextChars = findViewById(R.id.btn_text_chars)
        btnDelete = findViewById(R.id.btn_delete)

        // Set up slot animators
        val textPrimary = getColor(R.color.text_primary)
        val white = getColor(R.color.white)

        titleAnimator = SlotTextAnimator(
            container = titleChars,
            textSizeSp = 22f,
            textColor = textPrimary,
            letterSpacingEm = -0.05f
        )
        titleAnimator.init("My Sidenotes")

        btnTextAnimator = SlotTextAnimator(
            container = btnTextChars,
            textSizeSp = 18f,
            textColor = white,
            letterSpacingEm = -0.03f
        )
        btnTextAnimator.init("New Sidenote")

        // RecyclerView setup
        adapter = NotesAdapter(notes) { note -> openNote(note.id) }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val swipeController = SwipeController { position ->
            if (position != RecyclerView.NO_POSITION) {
                val note = notes[position]
                NoteStorage.deleteNote(this, note.id)
                notes.removeAt(position)
                adapter.notifyItemRemoved(position)
                updateEmptyState()
            }
        }
        ItemTouchHelper(swipeController).attachToRecyclerView(recyclerView)

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) = updateOpacities()
        })

        recyclerView.addOnChildAttachStateChangeListener(object : RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) = updateOpacities()
            override fun onChildViewDetachedFromWindow(view: View) {}
        })

        // Button action
        btnAction.setOnClickListener {
            if (screen == Screen.LIST) {
                openNote(null)
            } else {
                closeNote()
            }
        }

        // Delete button
        btnDelete.setOnClickListener {
            currentNoteId?.let { id ->
                NoteStorage.deleteNote(this, id)
            }
            closeNote(skipSave = true)
        }

        refreshNotes()
    }

    override fun onBackPressed() {
        if (screen == Screen.EDIT) {
            closeNote()
        } else {
            super.onBackPressed()
        }
    }

    fun openNote(noteId: String?) {
        currentNoteId = noteId

        if (noteId != null) {
            val note = notes.find { it.id == noteId }
            editTitle.setText(note?.title ?: "")
            editContent.setText(note?.content ?: "")
            btnDelete.visibility = View.VISIBLE
        } else {
            editTitle.setText("")
            editContent.setText("")
            btnDelete.visibility = View.GONE
        }

        transitionTo(Screen.EDIT, forward = true)

        if (noteId == null) {
            editTitle.postDelayed({
                editTitle.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(editTitle, InputMethodManager.SHOW_IMPLICIT)
            }, 400)
        }
    }

    fun closeNote(skipSave: Boolean = false) {
        if (!skipSave) {
            saveCurrentNote()
        }

        // Hide keyboard
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val focused = currentFocus
        if (focused != null) {
            imm.hideSoftInputFromWindow(focused.windowToken, 0)
            focused.clearFocus()
        }

        transitionTo(Screen.LIST, forward = false)

        btnDelete.visibility = View.GONE
        currentNoteId = null

        btnAction.postDelayed({
            refreshNotes()
        }, 400)
    }

    fun saveCurrentNote() {
        val title = editTitle.text?.toString()?.trim() ?: ""
        val content = editContent.text?.toString()?.trim() ?: ""

        if (title.isBlank() && content.isBlank()) return

        val id = currentNoteId ?: UUID.randomUUID().toString().also { currentNoteId = it }
        val note = Note(
            id = id,
            title = title,
            content = content,
            updatedAt = System.currentTimeMillis()
        )
        NoteStorage.saveNote(this, note)
    }

    fun transitionTo(newScreen: Screen, forward: Boolean) {
        screen = newScreen

        val contentWidth = contentArea.width.toFloat()

        if (forward) {
            // LIST → EDIT
            // Set up edit_view starting position (off to the right)
            editView.translationX = if (contentWidth > 0f) contentWidth else resources.displayMetrics.widthPixels.toFloat()
            editView.visibility = View.VISIBLE

            // Slide list out to left, edit in from right
            listView.animate()
                .translationX(-(if (contentWidth > 0f) contentWidth else resources.displayMetrics.widthPixels.toFloat()))
                .setDuration(350)
                .setInterpolator(DecelerateInterpolator())
                .start()

            editView.animate()
                .translationX(0f)
                .setDuration(350)
                .setInterpolator(DecelerateInterpolator())
                .start()

            // Title: "My Sidenotes" → "Sidenote"
            titleAnimator.animateTo("Sidenote", forward = true)

            // Button text: "New Sidenote" → "My Sidenotes"
            btnTextAnimator.animateTo("My Sidenotes", forward = true)

            // Arrow right: translate right + fade out
            arrowRight.animate()
                .translationX(60f)
                .alpha(0f)
                .setDuration(280)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction { arrowRight.visibility = View.INVISIBLE }
                .start()

            // Arrow left: enter from left + fade in
            arrowLeft.translationX = -60f
            arrowLeft.alpha = 0f
            arrowLeft.visibility = View.VISIBLE
            arrowLeft.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(280)
                .setInterpolator(DecelerateInterpolator())
                .start()

        } else {
            // EDIT → LIST
            val slideWidth = if (contentWidth > 0f) contentWidth else resources.displayMetrics.widthPixels.toFloat()

            // Slide edit out to right, list back in from left (or rather, list returns to 0)
            editView.animate()
                .translationX(slideWidth)
                .setDuration(350)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction { editView.visibility = View.INVISIBLE }
                .start()

            listView.animate()
                .translationX(0f)
                .setDuration(350)
                .setInterpolator(DecelerateInterpolator())
                .start()

            // Title: "Sidenote" → "My Sidenotes"
            titleAnimator.animateTo("My Sidenotes", forward = false)

            // Button text: "My Sidenotes" → "New Sidenote"
            btnTextAnimator.animateTo("New Sidenote", forward = false)

            // Arrow left: translate left + fade out
            arrowLeft.animate()
                .translationX(-60f)
                .alpha(0f)
                .setDuration(280)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction { arrowLeft.visibility = View.INVISIBLE }
                .start()

            // Arrow right: enter from right + fade in
            arrowRight.translationX = 60f
            arrowRight.alpha = 0f
            arrowRight.visibility = View.VISIBLE
            arrowRight.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(280)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    fun refreshNotes() {
        notes.clear()
        notes.addAll(NoteStorage.loadNotes(this))
        adapter.notifyDataSetChanged()
        updateEmptyState()
        recyclerView.post { updateOpacities() }
    }

    fun updateOpacities() {
        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i)
            val itemHeight = child.height.toFloat().takeIf { it > 0f } ?: continue
            val slot = child.top / itemHeight
            child.alpha = (1.0f - slot * 0.2f).coerceIn(0.1f, 1.0f)
        }
    }

    fun updateEmptyState() {
        emptyView.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (notes.isEmpty()) View.GONE else View.VISIBLE
    }
}
