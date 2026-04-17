package hrln.interactive.sidenote

import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class SwipeController(
    private val onDelete: (Int) -> Unit
) : ItemTouchHelper.Callback() {

    private var swipeCompleted = false

    override fun getMovementFlags(rv: RecyclerView, vh: RecyclerView.ViewHolder) =
        makeMovementFlags(0, ItemTouchHelper.LEFT)

    override fun onMove(
        rv: RecyclerView,
        vh: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ) = false

    override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
        swipeCompleted = true
        val position = vh.adapterPosition
        val noteCard = vh.itemView.findViewById<View>(R.id.note_card)
        // Slide the card fully off to the left, then delete
        noteCard.animate()
            .translationX(-vh.itemView.width.toFloat())
            .setDuration(150)
            .withEndAction { onDelete(position) }
            .start()
    }

    override fun onChildDraw(
        c: android.graphics.Canvas,
        rv: RecyclerView,
        vh: RecyclerView.ViewHolder,
        dX: Float, dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val noteCard = vh.itemView.findViewById<View>(R.id.note_card)
        // Max natural drag = width of the delete square (= item height)
        val maxDx = -vh.itemView.height.toFloat()

        val effectiveDx = if (dX < maxDx) {
            // Rubber-band resistance once the square is fully revealed
            maxDx + (dX - maxDx) * 0.2f
        } else {
            dX
        }

        noteCard.translationX = effectiveDx
        // Don't call super — we drive translation ourselves on the inner card only
    }

    override fun clearView(rv: RecyclerView, vh: RecyclerView.ViewHolder) {
        // Don't call super — we handle the return animation ourselves
        val noteCard = vh.itemView.findViewById<View>(R.id.note_card)
        if (!swipeCompleted) {
            // Spring back with a little overshoot bounce
            noteCard.animate()
                .translationX(0f)
                .setDuration(380)
                .setInterpolator(OvershootInterpolator(1.8f))
                .start()
        }
        swipeCompleted = false
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder) = 0.3f
    override fun getSwipeEscapeVelocity(defaultValue: Float) = defaultValue * 1.2f
    override fun isItemViewSwipeEnabled() = true
}
