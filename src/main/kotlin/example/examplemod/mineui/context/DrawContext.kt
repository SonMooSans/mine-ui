package example.examplemod.mineui.context

import com.mojang.blaze3d.vertex.PoseStack
import example.examplemod.mineui.RenderContext

data class DrawContext(
    val stack: PoseStack,
    override val mouseX: Int,
    override val mouseY: Int
): MouseContext {
    fun RenderContext.draw() = with (drawer) {
        this@DrawContext.draw()
    }
}