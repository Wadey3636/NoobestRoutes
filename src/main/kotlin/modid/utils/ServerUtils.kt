package modid.utils

import modid.Core.mc
import modid.events.impl.PacketEvent
import modid.utils.clock.Executor
import modid.utils.clock.Executor.Companion.register
import net.minecraft.network.Packet
import net.minecraft.network.play.client.C16PacketClientStatus
import net.minecraft.network.play.server.S01PacketJoinGame
import net.minecraft.network.play.server.S03PacketTimeUpdate
import net.minecraft.network.play.server.S37PacketStatistics
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.jvm.javaClass
import kotlin.ranges.coerceIn
import kotlin.text.split
import kotlin.text.toIntOrNull

/**
 * Taken from Odin
 */
object ServerUtils {
    private val packets = kotlin.collections.ArrayList<Packet<*>>()

    @JvmStatic
    fun handleSendPacket(packet: Packet<*>): Boolean {
        return packets.remove(packet)
    }

    private val serverPacketRegex = Regex("^S\\d+")

    @JvmStatic
    fun isServerPacket(packet: Packet<*>): Boolean {
        return serverPacketRegex.containsMatchIn(packet.javaClass.simpleName)
    }

    private fun sendPacketNoEvent(packet: Packet<*>) {
        packets.add(packet)
        mc.netHandler?.addToSendQueue(packet)
    }

    private var prevTime = 0L
    var averageTps = 20.0
    var averagePing = 0.0
    private var isPinging = false
    private var pingStartTime = 0L
    val fps get() = mc.debug.split(" ")[0].toIntOrNull() ?: 0

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        reset()
    }

    init {
        Executor(2000, "ClientUtils") {
            sendPing()
        }.register()
    }

    @SubscribeEvent
    fun onPacket(event: PacketEvent.Receive) {
        when (event.packet) {
            is S37PacketStatistics -> averagePing = (System.nanoTime() - pingStartTime) / 1e6

            is S01PacketJoinGame -> averagePing = 0.0

            is S03PacketTimeUpdate -> {
                if (prevTime != 0L)
                    averageTps = (20_000.0 / (System.currentTimeMillis() - prevTime + 1)).coerceIn(0.0, 20.0)

                prevTime = System.currentTimeMillis()
            }
            else -> return
        }
        isPinging = false
    }

    private fun sendPing() {
        if (isPinging || mc.thePlayer == null) return
        if (pingStartTime - System.nanoTime() > 10e6) reset()
        pingStartTime = System.nanoTime()
        isPinging = true
        sendPacketNoEvent(C16PacketClientStatus(C16PacketClientStatus.EnumState.REQUEST_STATS))
    }

    private fun reset() {
        prevTime = 0L
        averageTps = 20.0
        averagePing = 0.0
    }
}