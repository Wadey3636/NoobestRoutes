package noobestroutes.utils.render

import gg.essential.universal.shader.BlendState
import gg.essential.universal.shader.UShader
import noobestroutes.Core
import noobestroutes.Core.mc
import noobestroutes.utils.Utils.COLOR_NORMALIZER
import noobestroutes.utils.render.ColorUtil.withAlpha
import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.WorldRenderer
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.client.renderer.texture.TextureUtil
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.Entity
import net.minecraft.item.ItemStack
import net.minecraft.util.*
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.util.glu.Cylinder
import java.awt.image.BufferedImage
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin


/**
 * Taken from odin
 */
object RenderUtils {

    val tessellator: Tessellator = Tessellator.getInstance()
    val worldRenderer: WorldRenderer = tessellator.worldRenderer
    private val beaconBeam = ResourceLocation("textures/entity/beacon_beam.png")
    private val renderManager: RenderManager = mc.renderManager

    /**
     * Gets the rendered x-coordinate of an entity based on its last tick and current tick positions.
     *
     * @receiver The entity for which to retrieve the rendered x-coordinate.
     * @return The rendered x-coordinate.
     */
    val Entity.renderX: Double
        get() = prevPosX + (posX - prevPosX) * partialTicks

    /**
     * Gets the rendered y-coordinate of an entity based on its last tick and current tick positions.
     *
     * @receiver The entity for which to retrieve the rendered y-coordinate.
     * @return The rendered y-coordinate.
     */
    val Entity.renderY: Double
        get() = prevPosY + (posY - prevPosY) * partialTicks

    /**
     * Gets the rendered z-coordinate of an entity based on its last tick and current tick positions.
     *
     * @receiver The entity for which to retrieve the rendered z-coordinate.
     * @return The rendered z-coordinate.
     */
    val Entity.renderZ: Double
        get() = prevPosZ + (posZ - prevPosZ) * partialTicks

    /**
     * Gets the rendered position of an entity as a `Vec3`.
     *
     * @receiver The entity for which to retrieve the rendered position.
     * @return The rendered position as a `Vec3`.
     */
    val Entity.renderVec: Vec3
        get() = Vec3(renderX, renderY, renderZ)

    /**
     * Gets the rendered bounding box of an entity based on its last tick and current tick positions.
     *
     * @receiver The entity for which to retrieve the rendered bounding box.
     * @return The rendered bounding box as an `AxisAlignedBB`.
     */
    val Entity.renderBoundingBox: AxisAlignedBB
        get() = AxisAlignedBB(
            renderX - this.width / 2,
            renderY,
            renderZ - this.width / 2,
            renderX + this.width / 2,
            renderY + this.height,
            renderZ + this.width / 2
        )

    fun AxisAlignedBB.outlineBounds(): AxisAlignedBB =
        expand(0.0020000000949949026, 0.0020000000949949026, 0.0020000000949949026)

    inline operator fun WorldRenderer.invoke(block: WorldRenderer.() -> Unit) {
        block.invoke(this)
    }

    fun getPartialEntityBoundingBox(entity: Entity, partialTicks: Float): AxisAlignedBB {
        val lerpX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks
        val lerpY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks
        val lerpZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks

        return entity.entityBoundingBox.offset(lerpX - entity.posX, lerpY - entity.posY, lerpZ - entity.posZ)
    }

    private fun WorldRenderer.addVertex(x: Double, y: Double, z: Double, nx: Float, ny: Float, nz: Float) {
        pos(x, y, z).normal(nx, ny, nz).endVertex()
    }

    private fun preDraw(disableTexture2D: Boolean = true) {
        GlStateManager.enableAlpha()
        GlStateManager.enableBlend()
        GlStateManager.disableLighting()
        if (disableTexture2D) GlStateManager.disableTexture2D() else GlStateManager.enableTexture2D()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        translate(-renderManager.viewerPosX, -renderManager.viewerPosY, -renderManager.viewerPosZ)
    }

    private fun postDraw() {
        GlStateManager.disableBlend()
        GlStateManager.enableTexture2D()
        GlStateManager.resetColor()
    }

    fun depth(depth: Boolean) {
        if (depth) GlStateManager.enableDepth() else GlStateManager.disableDepth()
        GlStateManager.depthMask(depth)
    }

    private fun resetDepth() {
        GlStateManager.enableDepth()
        GlStateManager.depthMask(true)
    }

    fun Color.bind() {
        GlStateManager.resetColor()
        GlStateManager.color(r * COLOR_NORMALIZER, g * COLOR_NORMALIZER, b * COLOR_NORMALIZER, a * COLOR_NORMALIZER)
    }

    /**
     * Draws a filled Axis Aligned Bounding Box (AABB).
     *
     * @param aabb The bounding box to draw.
     * @param color The color to use for drawing.
     * @param depth Whether to enable depth testing.
     */
    fun drawFilledAABB(aabb: AxisAlignedBB, color: Color, depth: Boolean = false) {
        if (color.isTransparent) return

        GlStateManager.pushMatrix()
        GlStateManager.disableCull()
        preDraw()
        depth(depth)
        color.bind()
        addVertexesForFilledBox(aabb)
        tessellator.draw();
        if (!depth) resetDepth()
        postDraw()
        GlStateManager.enableCull()
        GlStateManager.popMatrix()
    }

    /**
     * Draws an outlined Axis Aligned Bounding Box (AABB).
     *
     * @param aabb The bounding box to draw.
     * @param color The color to use for drawing.
     * @param thickness The thickness of the outline.
     * @param depth Whether to enable depth testing.
     */
    fun drawOutlinedAABB(
        aabb: AxisAlignedBB,
        color: Color,
        thickness: Number = 3f,
        depth: Boolean = false,
        smoothLines: Boolean = true
    ) {
        if (color.isTransparent) return
        GlStateManager.pushMatrix()
        preDraw()

        if (smoothLines) {
            GL11.glEnable(GL11.GL_LINE_SMOOTH)
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST)
        }

        GL11.glLineWidth(thickness.toFloat())
        depth(depth)
        color.bind()
        addVertexesForOutlinedBox(aabb)
        tessellator.draw()

        if (smoothLines) GL11.glDisable(GL11.GL_LINE_SMOOTH)

        if (!depth) resetDepth()
        GL11.glLineWidth(1f)
        postDraw()
        GlStateManager.popMatrix()
    }

    /**
     *
     * @param points The vertices to fill.
     * @param color The color to use for drawing.
     * @param thickness The thickness of the outline.
     * @param depth Whether to enable depth testing.
     */
    fun drawFilledVertices(
        points: Collection<Vec3>,
        color: Color,
        thickness: Number = 3f,
        depth: Boolean = false,
        smoothLines: Boolean = true
    ) {
        if (color.isTransparent) return

        GlStateManager.pushMatrix()
        GlStateManager.disableCull()
        preDraw()
        depth(depth)
        color.bind()
        worldRenderer {
            begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_NORMAL)
            for (point in points) {
                addVertex(point.xCoord, point.yCoord, point.zCoord, 0.0f, -1.0f, 0.0f)
            }
        }
        tessellator.draw();
        if (!depth) resetDepth()
        postDraw()
        GlStateManager.enableCull()
        GlStateManager.popMatrix()
    }

    fun drawLines(points: Collection<Vec3>, color: Color, lineWidth: Float, depth: Boolean) {
        if (points.size < 2) return

        GlStateManager.pushMatrix()
        color.bind()
        preDraw()
        depth(depth)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glLineWidth(lineWidth)

        worldRenderer {
            begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION)
            for (point in points) {
                pos(point.xCoord, point.yCoord, point.zCoord).endVertex()
            }
        }
        tessellator.draw()

        if (!depth) resetDepth()
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GL11.glLineWidth(1f)
        postDraw()
        GlStateManager.popMatrix()
    }

    fun drawGradient3DLine(
        points: List<Vec3>,
        startColor: Color,
        endColor: Color,
        lineWidth: Float = 3f,
        depth: Boolean = false
    ) {
        if (points.size < 2) return

        GlStateManager.pushMatrix()
        preDraw()
        depth(depth)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glLineWidth(lineWidth)

        worldRenderer {
            begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR)
            for (i in 1 until points.size) {
                val t = i.toFloat() / (points.size - 1)

                val r = lerp(startColor.redFloat, endColor.redFloat, t)
                val g = lerp(startColor.greenFloat, endColor.greenFloat, t)
                val b = lerp(startColor.blueFloat, endColor.blueFloat, t)
                val a = lerp(startColor.alphaFloat, endColor.alphaFloat, t)

                val start = points[i - 1]
                val end = points[i]

                pos(start.xCoord, start.yCoord, start.zCoord)
                color(r, g, b, a)
                endVertex()

                pos(end.xCoord, end.yCoord, end.zCoord)
                color(r, g, b, a)
                endVertex()
            }
        }

        tessellator.draw()

        if (!depth) resetDepth()
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GL11.glLineWidth(1f)
        postDraw()
        GlStateManager.popMatrix()
    }

    private fun lerp(start: Float, end: Float, t: Float): Float {
        return start * (1 - t) + end * t
    }

    /**
     * Draws text in the world at the specified position with the specified color and optional parameters.
     *
     * @param text            The text to be drawn.
     * @param vec3            The position to draw the text.
     * @param color           The color of the text.
     * @param depthTest       Indicates whether to draw with depth (default is true).
     * @param scale           The scale of the text (default is 0.03).
     * @param shadow          Indicates whether to render a shadow for the text (default is true).
     */
    fun drawStringInWorld(
        text: String,
        vec3: Vec3,
        color: Color = Color.WHITE.withAlpha(1f),
        depthTest: Boolean = true,
        scale: Float = 0.3f,
        shadow: Boolean = false
    ) {
        if (text.isBlank()) return
        GlStateManager.pushMatrix()

        preDraw(false)
        GlStateManager.translate(vec3.xCoord, vec3.yCoord, vec3.zCoord)
        GlStateManager.rotate(-renderManager.playerViewY, 0.0f, 1.0f, 0.0f)
        GlStateManager.rotate(
            renderManager.playerViewX * if (mc.gameSettings.thirdPersonView == 2) -1 else 1,
            1.0f,
            0.0f,
            0.0f
        )
        GlStateManager.scale(-scale, -scale, scale)

        depth(depthTest)

        mc.fontRendererObj.drawString("$text§r", -mc.fontRendererObj.getStringWidth(text) / 2f, 0f, color.rgba, shadow)

        if (!depthTest) resetDepth()
        postDraw()
        GlStateManager.popMatrix()
    }

    /**
     * Draws a cylinder in the world with the specified parameters.
     *
     * @param pos         The position of the cylinder.
     * @param baseRadius  The radius of the base of the cylinder.
     * @param topRadius   The radius of the top of the cylinder.
     * @param height      The height of the cylinder.
     * @param slices      The number of slices for the cylinder.
     * @param stacks      The number of stacks for the cylinder.
     * @param rot1        Rotation parameter.
     * @param rot2        Rotation parameter.
     * @param rot3        Rotation parameter.
     * @param color       The color of the cylinder.
     * @param depth       Indicates whether to phase the cylinder (default is false)
     */
    fun drawCylinder(
        pos: Vec3, baseRadius: Number, topRadius: Number, height: Number,
        slices: Number, stacks: Number, rot1: Number, rot2: Number, rot3: Number,
        color: Color, depth: Boolean = false
    ) {
        GlStateManager.pushMatrix()
        GlStateManager.disableCull()
        GL11.glLineWidth(3.0F)
        preDraw()
        GL11.glLineWidth(3.0F)
        depth(depth)

        color.bind()
        GlStateManager.translate(pos.xCoord, pos.yCoord, pos.zCoord)
        GlStateManager.rotate(rot1.toFloat(), 1f, 0f, 0f)
        GlStateManager.rotate(rot2.toFloat(), 0f, 0f, 1f)
        GlStateManager.rotate(rot3.toFloat(), 0f, 1f, 0f)

        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE)

        Cylinder().draw(baseRadius.toFloat(), topRadius.toFloat(), height.toFloat(), slices.toInt(), stacks.toInt())

        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL)

        postDraw()
        GL11.glLineWidth(1.0F)
        GlStateManager.enableCull()
        if (!depth) resetDepth()
        GlStateManager.popMatrix()
    }

    /**
     * Draws a Texture modal rectangle at the specified position.
     * @param x The x-coordinate of the rectangle.
     * @param y The y-coordinate of the rectangle.
     * @param width The width of the rectangle.
     * @param height The height of the rectangle.
     */
    fun drawTexturedModalRect(
        x: Int, y: Int, width: Int, height: Int,
        u: Float = 0f, v: Float = 0f, uWidth: Int = 1, vHeight: Int = 1,
        tileWidth: Float = 1.0f, tileHeight: Float = 1.0f
    ) {
        val f = 1.0f / tileWidth
        val g = 1.0f / tileHeight
        Color.WHITE.bind()
        worldRenderer {
            begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)
            pos(x.toDouble(), (y + height).toDouble(), 0.0).tex(
                (u * f).toDouble(),
                ((v + vHeight.toFloat()) * g).toDouble()
            ).endVertex()
            pos((x + width).toDouble(), (y + height).toDouble(), 0.0).tex(
                ((u + uWidth.toFloat()) * f).toDouble(),
                ((v + vHeight.toFloat()) * g).toDouble()
            ).endVertex()
            pos((x + width).toDouble(), y.toDouble(), 0.0).tex(
                ((u + uWidth.toFloat()) * f).toDouble(),
                (v * g).toDouble()
            ).endVertex()
            pos(x.toDouble(), y.toDouble(), 0.0).tex((u * f).toDouble(), (v * g).toDouble()).endVertex()
        }
        tessellator.draw()
    }

    fun drawText(
        text: String,
        x: Float,
        y: Float,
        scale: Double = 1.0,
        color: Color = Color.WHITE,
        shadow: Boolean = true,
        center: Boolean = false
    ) {
        GlStateManager.pushMatrix()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        translate(x, y, 0f)
        scale(scale, scale, scale)
        mc.fontRendererObj.drawString(
            "${text}§r",
            if (center) mc.fontRendererObj.getStringWidth(text) / -2f else 0f,
            0f,
            color.rgba,
            shadow
        )
        GlStateManager.resetColor()
        GlStateManager.disableBlend()
        GlStateManager.popMatrix()
    }

    fun IBlockState.drawBlockTexture(x: Float, y: Float, z: Float = 1f, scale: Float) {
        drawBlockTexture(x, y, z, this, scale)
    }

    private fun setupGuiTransform(z: Float) {
        GlStateManager.translate(0f, 0f, 100.0f + z)
        GlStateManager.translate(8.0f, 8.0f, 0.0f)
        GlStateManager.scale(1.0f, 1.0f, -1.0f)
        GlStateManager.scale(0.5f, 0.5f, 0.5f)
        GlStateManager.scale(40.0f, 40.0f, 40.0f)
        GlStateManager.rotate(210.0f, 1.0f, 0.0f, 0.0f)
        GlStateManager.rotate(-135.0f, 0.0f, 1.0f, 0.0f)
    }

    private val blockRenderer = mc.blockRendererDispatcher
    private val textureManager = mc.renderEngine
    fun drawBlockTexture(x: Float, y: Float, z: Float = 1f, state: IBlockState, scale: Float) {
        val model = blockRenderer.getModelFromBlockState(state, mc.theWorld, BlockPos.ORIGIN)

        GlStateManager.pushMatrix()
        scale(scale, scale, 1f)
        translate(x / scale, y / scale, 0f)
        Color.WHITE.bind()

        textureManager.bindTexture(TextureMap.locationBlocksTexture)
        textureManager.getTexture(TextureMap.locationBlocksTexture).setBlurMipmap(false, false)
        GlStateManager.enableRescaleNormal()
        GlStateManager.enableAlpha()
        GlStateManager.alphaFunc(516, 0.1f)
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(770, 771)
        Color.WHITE.bind()
        setupGuiTransform(z)
        GlStateManager.disableLighting()
        RenderHelper.disableStandardItemLighting()


        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK)
        blockRenderer.blockModelRenderer.renderModel(
            mc.theWorld,
            model,
            state,
            BlockPos.ORIGIN,
            worldRenderer,
            false
        )
        tessellator.draw()
        GlStateManager.disableAlpha()
        GlStateManager.disableRescaleNormal()
        GlStateManager.disableLighting()
        GlStateManager.popMatrix()
        textureManager.bindTexture(TextureMap.locationBlocksTexture)
        textureManager.getTexture(TextureMap.locationBlocksTexture).restoreLastBlurMipmap()
    }

    fun ItemStack.drawItem(x: Float = 0f, y: Float = 0f, scale: Float = 1f, z: Float = 200f) {
        GlStateManager.pushMatrix()
        scale(scale, scale, 1f)
        translate(x / scale, y / scale, 0f)
        Color.WHITE.bind()

        RenderHelper.enableStandardItemLighting()
        RenderHelper.enableGUIStandardItemLighting()

        mc.renderItem.zLevel = z
        mc.renderItem.renderItemIntoGUI(this, 0, 0)
        RenderHelper.disableStandardItemLighting()
        GlStateManager.popMatrix()
    }

    /**
     * Draws a beacon beam at the specified position.
     * @param vec3 The position at which to draw the beacon beam.
     * @param color The color of the beacon beam.
     * @param depth Whether to enable depth testing.
     */
    fun drawBeaconBeam(vec3: Vec3, color: Color, depth: Boolean = true, height: Int = 300) {
        if (color.isTransparent) return
        val bottomOffset = 0
        val topOffset = bottomOffset + height
        depth(depth)

        mc.textureManager.bindTexture(beaconBeam)

        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT.toFloat())
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT.toFloat())

        GlStateManager.pushMatrix()

        GlStateManager.disableLighting()
        GlStateManager.enableCull()
        GlStateManager.enableTexture2D()
        GlStateManager.tryBlendFuncSeparate(770, 1, 1, 0)
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        translate(-renderManager.viewerPosX, -renderManager.viewerPosY, -renderManager.viewerPosZ)

        val time: Double = mc.theWorld.worldTime.toDouble() + partialTicks
        val x = vec3.xCoord
        val y = vec3.yCoord
        val z = vec3.zCoord
        val d1 = MathHelper.func_181162_h(-time * 0.2 - floor(-time * 0.1))
        val d2 = time * 0.025 * -1.5
        val d4 = 0.5 + cos(d2 + 2.356194490192345) * 0.2
        val d5 = 0.5 + sin(d2 + 2.356194490192345) * 0.2
        val d6 = 0.5 + cos(d2 + (Math.PI / 4)) * 0.2
        val d7 = 0.5 + sin(d2 + (Math.PI / 4)) * 0.2
        val d8 = 0.5 + cos(d2 + 3.9269908169872414) * 0.2
        val d9 = 0.5 + sin(d2 + 3.9269908169872414) * 0.2
        val d10 = 0.5 + cos(d2 + 5.497787143782138) * 0.2
        val d11 = 0.5 + sin(d2 + 5.497787143782138) * 0.2
        val d14 = -1 + d1
        val d15 = height * 2.5 + d14

        fun WorldRenderer.color(alpha: Float = color.alpha) { // local function is used to simplify this.
            this.color(color.r / 255f, color.g / 255f, color.b / 255f, alpha).endVertex()
        }

        worldRenderer {
            begin(7, DefaultVertexFormats.POSITION_TEX_COLOR)

            pos(x + d4, y + topOffset, z + d5).tex(1.0, d15).color()
            pos(x + d4, y + bottomOffset, z + d5).tex(1.0, d14).color()
            pos(x + d6, y + bottomOffset, z + d7).tex(0.0, d14).color()
            pos(x + d6, y + topOffset, z + d7).tex(0.0, d15).color()
            pos(x + d10, y + topOffset, z + d11).tex(1.0, d15).color()
            pos(x + d10, y + bottomOffset, z + d11).tex(1.0, d14).color()
            pos(x + d8, y + bottomOffset, z + d9).tex(0.0, d14).color()
            pos(x + d8, y + topOffset, z + d9).tex(0.0, d15).color()
            pos(x + d6, y + topOffset, z + d7).tex(1.0, d15).color()
            pos(x + d6, y + bottomOffset, z + d7).tex(1.0, d14).color()
            pos(x + d10, y + bottomOffset, z + d11).tex(0.0, d14).color()
            pos(x + d10, y + topOffset, z + d11).tex(0.0, d15).color()
            pos(x + d8, y + topOffset, z + d9).tex(1.0, d15).color()
            pos(x + d8, y + bottomOffset, z + d9).tex(1.0, d14).color()
            pos(x + d4, y + bottomOffset, z + d5).tex(0.0, d14).color()
            pos(x + d4, y + topOffset, z + d5).tex(0.0, d15).color()
        }
        tessellator.draw()
        GlStateManager.disableCull()
        val d12 = -1 + d1
        val d13 = height + d12
        val alpha = color.alpha
        worldRenderer {
            begin(7, DefaultVertexFormats.POSITION_TEX_COLOR)
            pos(x + 0.2, y + topOffset, z + 0.2).tex(1.0, d13).color(.25f * alpha)
            pos(x + 0.2, y + bottomOffset, z + 0.2).tex(1.0, d12).color(.25f * alpha)
            pos(x + 0.8, y + bottomOffset, z + 0.2).tex(0.0, d12).color(.25f * alpha)
            pos(x + 0.8, y + topOffset, z + 0.2).tex(0.0, d13).color(.25f * alpha)
            pos(x + 0.8, y + topOffset, z + 0.8).tex(1.0, d13).color(.25f * alpha)
            pos(x + 0.8, y + bottomOffset, z + 0.8).tex(1.0, d12).color(.25f * alpha)
            pos(x + 0.2, y + bottomOffset, z + 0.8).tex(0.0, d12).color(.25f * alpha)
            pos(x + 0.2, y + topOffset, z + 0.8).tex(0.0, d13).color(.25f * alpha)
            pos(x + 0.8, y + topOffset, z + 0.2).tex(1.0, d13).color(.25f * alpha)
            pos(x + 0.8, y + bottomOffset, z + 0.2).tex(1.0, d12).color(.25f * alpha)
            pos(x + 0.8, y + bottomOffset, z + 0.8).tex(0.0, d12).color(.25f * alpha)
            pos(x + 0.8, y + topOffset, z + 0.8).tex(0.0, d13).color(.25f * alpha)
            pos(x + 0.2, y + topOffset, z + 0.8).tex(1.0, d13).color(.25f * alpha)
            pos(x + 0.2, y + bottomOffset, z + 0.8).tex(1.0, d12).color(.25f * alpha)
            pos(x + 0.2, y + bottomOffset, z + 0.2).tex(0.0, d12).color(.25f * alpha)
            pos(x + 0.2, y + topOffset, z + 0.2).tex(0.0, d13).color(.25f * alpha)

            endVertex()
        }
        tessellator.draw()
        GlStateManager.resetColor()
        if (!depth) resetDepth()
        GlStateManager.enableCull()
        GlStateManager.popMatrix()
    }

    private val BUF_FLOAT_4 = BufferUtils.createFloatBuffer(4)
    var isRenderingOutlinedEntities = false
        private set

    fun enableOutlineMode() {
        isRenderingOutlinedEntities = true
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL13.GL_COMBINE)
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_COMBINE_RGB, GL11.GL_REPLACE)
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE0_RGB, GL13.GL_CONSTANT)
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND0_RGB, GL11.GL_SRC_COLOR)
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_COMBINE_ALPHA, GL11.GL_REPLACE)
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE0_ALPHA, GL11.GL_TEXTURE)
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND0_ALPHA, GL11.GL_SRC_ALPHA)
    }

    fun disableOutlineMode() {
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE)
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_COMBINE_RGB, GL11.GL_MODULATE)
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE0_RGB, GL11.GL_TEXTURE)
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND0_RGB, GL11.GL_SRC_COLOR)
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_COMBINE_ALPHA, GL11.GL_MODULATE)
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE0_ALPHA, GL11.GL_TEXTURE)
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND0_ALPHA, GL11.GL_SRC_ALPHA)
        isRenderingOutlinedEntities = false
    }

    fun outlineColor(color: Color) {
        BUF_FLOAT_4.put(0, color.redFloat)
        BUF_FLOAT_4.put(1, color.greenFloat)
        BUF_FLOAT_4.put(2, color.blueFloat)
        BUF_FLOAT_4.put(3, color.alphaFloat)
        GL11.glTexEnv(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_COLOR, BUF_FLOAT_4)
    }

    /**
     * Creates a shader from a vertex shader, fragment shader, and a blend state
     *
     * @param vertName The name of the vertex shader's file.
     * @param fragName The name of the fragment shader's file.
     * @param blendState The blend state for the shader
     */
    fun createLegacyShader(vertName: String, fragName: String, blendState: BlendState) =
        UShader.fromLegacyShader(readShader(vertName, "vsh"), readShader(fragName, "fsh"), blendState)

    /**
     * Reads a shader file as a text file, and returns the contents
     *
     * @param name The name of the shader file
     * @param ext The file extension of the shader file (usually fsh or vsh)
     *
     * @return The contents of the shader file at the given path.
     */
    private fun readShader(name: String, ext: String): String =
        Core::class.java.getResource("/shaders/source/$name.$ext")?.readText() ?: ""

    /**
     * Loads a BufferedImage from a path to a resource in the project
     *
     * @param path The path to the image file
     *
     * @returns The BufferedImage of that resource path.
     */
    fun loadBufferedImage(path: String): BufferedImage =
        TextureUtil.readBufferedImage(Core::class.java.getResourceAsStream(path))


    var partialTicks = 0f

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onRenderWorld(event: RenderWorldLastEvent) {
        this.partialTicks = event.partialTicks
    }

    private fun addVertexesForFilledBox(aabb: AxisAlignedBB) {
        val minX = aabb.minX
        val minY = aabb.minY
        val minZ = aabb.minZ
        val maxX = aabb.maxX
        val maxY = aabb.maxY
        val maxZ = aabb.maxZ

        val renderer = tessellator.worldRenderer

        renderer {
            begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_NORMAL)

            // Front face
            addVertex(minX, maxY, minZ, 0f, 0f, -1f)
            addVertex(maxX, maxY, minZ, 0f, 0f, -1f)
            addVertex(maxX, minY, minZ, 0f, 0f, -1f)
            addVertex(minX, minY, minZ, 0f, 0f, -1f)

            // Back face
            addVertex(minX, minY, maxZ, 0f, 0f, 1f)
            addVertex(maxX, minY, maxZ, 0f, 0f, 1f)
            addVertex(maxX, maxY, maxZ, 0f, 0f, 1f)
            addVertex(minX, maxY, maxZ, 0f, 0f, 1f)

            // Bottom face
            addVertex(minX, minY, minZ, 0f, -1f, 0f)
            addVertex(maxX, minY, minZ, 0f, -1f, 0f)
            addVertex(maxX, minY, maxZ, 0f, -1f, 0f)
            addVertex(minX, minY, maxZ, 0f, -1f, 0f)

            // Top face
            addVertex(minX, maxY, maxZ, 0f, 1f, 0f)
            addVertex(maxX, maxY, maxZ, 0f, 1f, 0f)
            addVertex(maxX, maxY, minZ, 0f, 1f, 0f)
            addVertex(minX, maxY, minZ, 0f, 1f, 0f)

            // Left face
            addVertex(minX, minY, maxZ, -1f, 0f, 0f)
            addVertex(minX, maxY, maxZ, -1f, 0f, 0f)
            addVertex(minX, maxY, minZ, -1f, 0f, 0f)
            addVertex(minX, minY, minZ, -1f, 0f, 0f)

            // Right face
            addVertex(maxX, minY, minZ, 1f, 0f, 0f)
            addVertex(maxX, maxY, minZ, 1f, 0f, 0f)
            addVertex(maxX, maxY, maxZ, 1f, 0f, 0f)
            addVertex(maxX, minY, maxZ, 1f, 0f, 0f)
        }
    }

    private fun addVertexesForOutlinedBox(aabb: AxisAlignedBB) {
        val minX = aabb.minX
        val minY = aabb.minY
        val minZ = aabb.minZ
        val maxX = aabb.maxX
        val maxY = aabb.maxY
        val maxZ = aabb.maxZ

        worldRenderer {
            begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION)
            pos(minX, minY, minZ).endVertex()
            pos(minX, minY, maxZ).endVertex()
            pos(maxX, minY, maxZ).endVertex()
            pos(maxX, minY, minZ).endVertex()
            pos(minX, minY, minZ).endVertex()

            pos(minX, maxY, minZ).endVertex()
            pos(minX, maxY, maxZ).endVertex()
            pos(maxX, maxY, maxZ).endVertex()
            pos(maxX, maxY, minZ).endVertex()
            pos(minX, maxY, minZ).endVertex()

            pos(minX, maxY, maxZ).endVertex()
            pos(minX, minY, maxZ).endVertex()
            pos(maxX, minY, maxZ).endVertex()
            pos(maxX, maxY, maxZ).endVertex()
            pos(maxX, maxY, minZ).endVertex()
            pos(maxX, minY, minZ).endVertex()
        }
    }

    fun drawMinecraftLabel(str: String, pos: Vec3, scale: Double, depth: Boolean = true, color: Color = Color.WHITE) {
        GlStateManager.pushMatrix()
        depth(depth)
        GlStateManager.translate(pos.xCoord + 0.0f, pos.yCoord + 2.5f, pos.zCoord)
        GL11.glNormal3f(0.0f, 1.0f, 0.0f)
        GlStateManager.rotate(-this.renderManager.playerViewY, 0.0f, 1.0f, 0.0f)
        GlStateManager.rotate(this.renderManager.playerViewX, 1.0f, 0.0f, 0.0f)
        GlStateManager.scale(-scale, -scale, scale)
        GlStateManager.disableLighting()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        mc.fontRendererObj.drawString(str, -mc.fontRendererObj.getStringWidth(str) / 2f, 0f, color.rgba, true)
        GlStateManager.disableBlend()
        GlStateManager.enableLighting()
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
        if (!depth) resetDepth()
        GlStateManager.popMatrix()
    }

    fun renderDurabilityBar(x: Int, y: Int, percentFilled: Double) {
        val percent = percentFilled.coerceIn(0.0, 1.0).takeIf { it > 0.0 } ?: return
        val barColorIndex = (percent * 255.0).roundToInt()
        GlStateManager.disableLighting()
        GlStateManager.disableDepth()
        GlStateManager.disableTexture2D()
        GlStateManager.disableAlpha()
        GlStateManager.disableBlend()
        draw(x + 2, y + 13, 13, 2, 0, 0, 0, 255)
        draw(x + 2, y + 13, 12, 1, (255 - barColorIndex) / 4, 64, 0, 255)
        draw(x + 2, y + 13, (percent * 13.0).roundToInt(), 1, 255 - barColorIndex, barColorIndex, 0, 255)
        GlStateManager.enableAlpha()
        GlStateManager.enableTexture2D()
        GlStateManager.enableLighting()
        GlStateManager.enableDepth()
    }

    private fun draw(x: Int, y: Int, width: Int, height: Int, red: Int, green: Int, blue: Int, alpha: Int) {
        worldRenderer {
            begin(7, DefaultVertexFormats.POSITION_COLOR)
            pos((x + 0).toDouble(), (y + 0).toDouble(), 0.0).color(red, green, blue, alpha).endVertex()
            pos((x + 0).toDouble(), (y + height).toDouble(), 0.0).color(red, green, blue, alpha).endVertex()
            pos((x + width).toDouble(), (y + height).toDouble(), 0.0).color(red, green, blue, alpha).endVertex()
            pos((x + width).toDouble(), (y + 0).toDouble(), 0.0).color(red, green, blue, alpha).endVertex()
        }
        tessellator.draw()
    }
}