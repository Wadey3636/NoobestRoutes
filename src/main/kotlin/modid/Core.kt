package modid

import kotlinx.coroutines.*
import modid.config.Config
import modid.features.impl.render.ClickGUIModule
import modid.ui.util.shader.GapOutlineShader
import modid.ui.util.shader.RoundedRect
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.client.event.GuiOpenEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.File
import kotlin.coroutines.EmptyCoroutineContext

object Core {
    @JvmStatic
    val mc: Minecraft = Minecraft.getMinecraft()

    val DEV_MODE = "@DEV_MODE@".toBoolean()
    const val VERSION = "@VER@"
    val scope = CoroutineScope(SupervisorJob() + EmptyCoroutineContext)
    val logger: Logger = LogManager.getLogger("NoobRoutes")

    var display: GuiScreen? = null

    fun init() {

    }

    fun postInit() {
        File(mc.mcDataDir, "config/@MOD_ID@").takeIf { !it.exists() }?.mkdirs()
    }


    fun loadComplete() {
        runBlocking(Dispatchers.IO) {
            launch {
                Config.load()
            }.join()
        }
        RoundedRect.initShaders()
        GapOutlineShader.initShader()
    }

    private var lastChatVisibility: EntityPlayer.EnumChatVisibility? = null
    private var inUI = false

    @SubscribeEvent
    fun onWorldUnload(event: WorldEvent.Unload) {
        lastChatVisibility?.let { mc.gameSettings.chatVisibility = it }
        lastChatVisibility = null
        inUI = false
    }

    @SubscribeEvent
    fun onGuiClose(event: GuiOpenEvent) {
        if (event.gui == null) {
            lastChatVisibility?.let { mc.gameSettings.chatVisibility = it }
            lastChatVisibility = null
            inUI = false
        }
    }

    @SubscribeEvent
    fun onRenderHUD(event: RenderGameOverlayEvent.Pre) {
        if (inUI && event.type == RenderGameOverlayEvent.ElementType.HOTBAR) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (display == null) return
        lastChatVisibility = mc.gameSettings.chatVisibility
        inUI = true
        if (ClickGUIModule.hideChat) {
            mc.gameSettings.chatVisibility = EntityPlayer.EnumChatVisibility.HIDDEN
        }
        mc.displayGuiScreen(display)
        display = null
    }
}