package hrln.interactive.sidenote

import android.graphics.Typeface
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView

class SlotTextAnimator(
    private val container: LinearLayout,
    private val textSizeSp: Float,
    private val textColor: Int,
    private val letterSpacingEm: Float = -0.05f
) {
    private val stagger = 18L
    private val duration = 180L

    fun init(text: String) {
        container.removeAllViews()
        for (ch in text) {
            container.addView(makeTv(ch))
        }
    }

    fun animateTo(newText: String, forward: Boolean) {
        val scaledDensity = container.context.resources.displayMetrics.scaledDensity
        val lineHeight = textSizeSp * scaledDensity * 1.3f

        // forward: old chars exit upward, new chars enter from below
        // backward: old chars exit downward, new chars enter from above
        val exitY = if (forward) -lineHeight else lineHeight
        val enterY = if (forward) lineHeight else -lineHeight

        val oldCount = container.childCount

        // Animate old characters out
        for (i in 0 until oldCount) {
            container.getChildAt(i)?.animate()
                ?.translationY(exitY)
                ?.alpha(0f)
                ?.setDuration(duration)
                ?.setStartDelay(i * stagger)
                ?.setInterpolator(AccelerateInterpolator())
                ?.start()
        }

        // Build new views now (off-screen), swap them in mid-exit
        val newViews = newText.map { ch ->
            makeTv(ch).also {
                it.translationY = enterY
                it.alpha = 0f
            }
        }

        // Swap views halfway through the exit — creates a clean overlap
        val swapDelay = (oldCount * stagger * 0.5f).toLong().coerceAtLeast(0L)

        container.postDelayed({
            container.removeAllViews()
            newViews.forEach { container.addView(it) }
            // New chars enter immediately after swap, staggered per position
            newViews.forEachIndexed { i, tv ->
                tv.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(duration)
                    .setStartDelay(i * stagger)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
        }, swapDelay)
    }

    private fun makeTv(ch: Char) = TextView(container.context).apply {
        text = ch.toString()
        setTextColor(textColor)
        setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, textSizeSp)
        letterSpacing = letterSpacingEm
        typeface = Typeface.DEFAULT
        includeFontPadding = false
    }
}
