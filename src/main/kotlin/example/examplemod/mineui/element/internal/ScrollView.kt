package example.examplemod.mineui.element.internal

import example.examplemod.mineui.core.EventType
import example.examplemod.mineui.core.GuiEventContext
import example.examplemod.mineui.element.BoxElement
import example.examplemod.mineui.element.BoxStyle
import example.examplemod.mineui.style.Overflow
import example.examplemod.mineui.style.PosXY
import example.examplemod.mineui.style.ScrollbarStyle
import example.examplemod.mineui.utils.Size
import example.examplemod.mineui.wrapper.DrawStack
import example.examplemod.mineui.wrapper.translate
import net.minecraft.client.gui.Gui

open class ScrollViewStyle : BoxStyle() {
    var scrollbar: ScrollbarStyle = ScrollbarStyle()
    var overflowX: Overflow = Overflow.Hidden
    var overflowY: Overflow = Overflow.Hidden

    fun overflow(overflow: Overflow) {
        overflowX = overflow
        overflowY = overflow
    }

    fun overflow(x: Overflow, y: Overflow) {
        overflowX = x
        overflowY = y
    }

    fun scrollbar(init: ScrollbarStyle.() -> Unit) {
        scrollbar = ScrollbarStyle().apply(init)
    }
}

enum class Scrolling {
    X, Y
}

abstract class ScrollView<S: ScrollViewStyle>(create: () -> S) : BoxElement<S>(create) {
    private var scrolling: Scrolling? = null
    override fun onClick(x: Double, y: Double, mouseButton: Int, context: GuiEventContext) {
        super.onClick(x, y, mouseButton, context)
        if (scrolling != null) return
        val hooked = context.ui.state.hooked

        val top = absolutePosition.y + absoluteSize.height - style.scrollbar.width
        val bottom = absolutePosition.y + absoluteSize.height
        val left = absolutePosition.x + absoluteSize.width - style.scrollbar.width
        val right = absolutePosition.x + absoluteSize.width

        scrolling = when {
            overflowX && y.toInt() in top..bottom -> Scrolling.X
            overflowY && x.toInt() in left..right -> Scrolling.Y
            else -> return
        }
        if (scrolling != null) {
            hooked[EventType.MouseDrag] = this
            hooked[EventType.MouseRelease] = this
        }
    }

    override fun onDrag(
        mouseX: Double,
        mouseY: Double,
        mouseButton: Int,
        dragX: Double,
        dragY: Double,
        context: GuiEventContext
    ) {
        when (scrolling) {
            Scrolling.X -> {
                context.reflow = true
                scrollX = (scrollX + dragX.toInt())
                    .coerceAtMost(minSize.width - absoluteSize.width)
                    .coerceAtLeast(0)
            }
            Scrolling.Y -> {
                context.reflow = true
                scrollY = (scrollY + dragY.toInt())
                    .coerceAtMost(minSize.height - absoluteSize.height)
                    .coerceAtLeast(0)
            }
            else -> {}
        }

        return super.onDrag(mouseX, mouseY, mouseButton, dragX, dragY, context)
    }

    override fun onScroll(x: Double, y: Double, scroll: Double, context: GuiEventContext) {
        context.reflow = true
        scrollY = (scrollY - (scroll.toInt() * style.scrollbar.speed))
            .coerceAtMost(minSize.height - absoluteSize.height)
            .coerceAtLeast(0)
        super.onScroll(x, y, scroll, context)
    }

    override fun onMouseReleased(x: Double, y: Double, mouseButton: Int, context: GuiEventContext) {
        val hooked = context.ui.state.hooked

        scrolling = null
        hooked.remove(EventType.MouseDrag, this)
        hooked.remove(EventType.MouseRelease, this)
        super.onMouseReleased(x, y, mouseButton, context)
    }

    lateinit var minSize: Size
    var scrollX: Int = 0
    var scrollY: Int = 0
    var overflowX: Boolean = false
    var overflowY: Boolean = false

    override fun reflow(pos: PosXY, size: Size) {
        val min = getMinimumSize()
        overflowX = style.overflowX.isOverflowed(min, size)
        overflowY = style.overflowY.isOverflowed(min, size)
        minSize = min.plus(
            width =  if (overflowY) style.scrollbar.width else 0,
            height = if (overflowX) style.scrollbar.width else 0
        )

        super.reflow(pos.minus(x = scrollX, y = scrollY), size.minusScrollbars())
    }

    override fun draw(stack: DrawStack, size: Size) {
        val cut = style.overflowX != Overflow.Visible || style.overflowY != Overflow.Visible
        if (cut) {
            stack.scissor(0, 0, size.width, size.height)
        }

        super.draw(stack, size)
    }

    override fun drawChildren(stack: DrawStack) {
        super.drawChildren(stack)
        Gui.disableScissor()

        stack.translate {
            stack.translated = absolutePosition
            val size = absoluteSize
            val content = minSize

            if (overflowX) {
                drawXScrollbar(stack, content, size)
            }

            if (overflowY) {
                drawYScrollbar(stack, content, size)
            }
        }
    }

    fun drawXScrollbar(stack: DrawStack, content: Size, size: Size) {
        val style = style.scrollbar
        val y = size.height - style.width
        stack.fillRect(0, y, size.width, style.width, style.track)

        val scale = (size.width.toDouble() / content.width ).coerceAtMost(1.0)
        stack.fillRect(scrollX, y, (size.width * scale).toInt(), style.width, style.thumb)
    }

    fun drawYScrollbar(stack: DrawStack, content: Size, size: Size) {
        val style = style.scrollbar
        val x = size.width - style.width
        stack.fillRect(x, 0, style.width, size.height, style.track)

        val scale = (size.height.toDouble() / content.height).coerceAtMost(1.0)
        stack.fillRect(x, scrollY, style.width, (size.height * scale).toInt(), style.thumb)
    }

    private fun Size.minusScrollbars(): Size {
        return minus(
            width =  if (overflowY) style.scrollbar.width else 0,
            height = if (overflowX) style.scrollbar.width else 0
        )
    }
}