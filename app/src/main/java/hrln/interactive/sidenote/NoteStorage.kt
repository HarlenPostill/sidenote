package hrln.interactive.sidenote

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object NoteStorage {

    private const val PREFS_NAME = "sidenote_prefs"
    private const val KEY_NOTES = "notes"

    fun loadNotes(context: Context): MutableList<Note> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_NOTES, "[]") ?: "[]"
        val array = JSONArray(json)
        val notes = mutableListOf<Note>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            notes.add(
                Note(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    content = obj.getString("content"),
                    updatedAt = obj.getLong("updatedAt")
                )
            )
        }
        return notes
    }

    fun saveNote(context: Context, note: Note) {
        val notes = loadNotes(context)
        val index = notes.indexOfFirst { it.id == note.id }
        if (index >= 0) {
            notes[index] = note
        } else {
            notes.add(0, note)
        }
        persistNotes(context, notes)
    }

    fun deleteNote(context: Context, id: String) {
        val notes = loadNotes(context)
        notes.removeAll { it.id == id }
        persistNotes(context, notes)
    }

    private fun persistNotes(context: Context, notes: List<Note>) {
        val array = JSONArray()
        for (note in notes) {
            val obj = JSONObject()
            obj.put("id", note.id)
            obj.put("title", note.title)
            obj.put("content", note.content)
            obj.put("updatedAt", note.updatedAt)
            array.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NOTES, array.toString())
            .apply()
    }
}
