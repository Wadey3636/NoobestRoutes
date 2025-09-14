package modid.ui


import modid.features.impl.render.ClickGUIModule
import modid.ui.util.shader.GaussianBlurShader
import modid.utils.render.scale
import modid.utils.render.scaleFactor
import modid.utils.render.translate
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11


abstract class Screen : GuiScreen() {
    protected fun scaleUI(){
        translate(0f, 0f, 200f)
        scale(1f / scaleFactor, 1f / scaleFactor, 1f)
    }

    protected fun setupBlending(){
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
    }

    protected fun backgroundCapture(){
        if (ClickGUIModule.blur) GaussianBlurShader.captureBackground()
    }
    protected fun backgroundCleanup(){
        if (ClickGUIModule.blur) GaussianBlurShader.cleanup()
    }

    abstract fun draw()

    open fun onScroll(amount: Int) {}

    final override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        draw()
    }

    final override fun handleMouseInput() {
        super.handleMouseInput()
        val scrollEvent = Mouse.getEventDWheel()
        if (scrollEvent != 0) {
            onScroll(scrollEvent)
        }
    }

    final override fun doesGuiPauseGame(): Boolean {
        return false
    }
}