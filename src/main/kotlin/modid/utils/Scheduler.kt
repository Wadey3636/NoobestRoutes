package modid.utils

import modid.Core.mc
import modid.events.impl.MotionUpdateEvent
import modid.events.impl.MoveEntityWithHeadingEvent
import modid.events.impl.PacketEvent
import modid.events.impl.ServerTickEvent
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S29PacketSoundEffect
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent

/**
 * Taken from meow
 */
object Scheduler {
    var runTime = 0L


    private val scheduledPreTickTasks = Tasks()
    private val scheduledPostTickTasks = Tasks()
    private val scheduledHighPreTickTasks = Tasks()
    private val scheduledHighPostTickTasks = Tasks()
    private val scheduledLowestPreTickTasks = Tasks()
    private val scheduledLowestPostTickTasks = Tasks()
    private val scheduledPrePlayerTickTasks = Tasks()
    private val scheduledPostPlayerTickTasks = Tasks()
    private val scheduledLowestC03Tasks = Tasks()
    private val scheduledC03Tasks = Tasks()
    private val scheduledSoundTasks = Tasks()
    private val scheduledFrameTasks = Tasks()
    private val scheduledServerTasks = Tasks()
    private val scheduledPreMotionUpdateTasks = Tasks()
    private val scheduledLowPreMotionUpdateTasks = Tasks()
    private val scheduledPostMotionUpdateTasks = Tasks()


    private val scheduledHighestPostMoveEntityWithHeadingTasks = Tasks()
    private val scheduledHighestPreMoveEntityWithHeadingTasks = Tasks()
    private val scheduledLowPreMoveEntityWithHeadingTasks = Tasks()



    private fun reset() {
        scheduledPreTickTasks.clear()
        scheduledPostTickTasks.clear()
        scheduledHighPreTickTasks.clear()
        scheduledHighPostTickTasks.clear()
        scheduledLowestPreTickTasks.clear()
        scheduledLowestPostTickTasks.clear()
        scheduledPrePlayerTickTasks.clear()
        scheduledPostPlayerTickTasks.clear()
        scheduledC03Tasks.clear()
        scheduledLowestC03Tasks.clear()
        scheduledSoundTasks.clear()
        scheduledHighestPostMoveEntityWithHeadingTasks.clear()
        scheduledFrameTasks.clear()
        scheduledServerTasks.clear()
        scheduledPreMotionUpdateTasks.clear()
        scheduledLowPreMotionUpdateTasks.clear()
        scheduledPostMotionUpdateTasks.clear()
        scheduledHighestPreMoveEntityWithHeadingTasks.clear()
        scheduledLowPreMoveEntityWithHeadingTasks.clear()
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        reset()
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldEvent.Unload) {
        reset()
    }


    @Throws(IndexOutOfBoundsException::class)
    fun schedulePreTickTask(ticks: Int = 0, priority: Int = 0, callback: (Any?) -> Unit = {}) {
        if (ticks < 0) throw kotlin.IndexOutOfBoundsException("Scheduled Negative Number")
        scheduledPreTickTasks.add(Task({ p -> callback(p) }, ticks, priority))
    }

    @Throws(IndexOutOfBoundsException::class)
    fun schedulePreMotionUpdateTask(ticks: Int = 0, priority: Int = 0, callback: (Any?) -> Unit = {}) {
        if (ticks < 0) throw kotlin.IndexOutOfBoundsException("Scheduled Negative Number")
        scheduledPreMotionUpdateTasks.add(Task({ p -> callback(p) }, ticks, priority))
    }
    @Throws(IndexOutOfBoundsException::class)
    fun scheduleLowPreMotionUpdateTask(ticks: Int = 0, priority: Int = 0, callback: (Any?) -> Unit = {}) {
        if (ticks < 0) throw kotlin.IndexOutOfBoundsException("Scheduled Negative Number")
        scheduledLowPreMotionUpdateTasks.add(Task({ p -> callback(p) }, ticks, priority))
    }

    @Throws(IndexOutOfBoundsException::class)
    fun schedulePostMotionUpdateTask(ticks: Int = 0, priority: Int = 0, callback: (Any?) -> Unit = {}) {
        if (ticks < 0) throw kotlin.IndexOutOfBoundsException("Scheduled Negative Number")
        scheduledPostMotionUpdateTasks.add(Task({ p -> callback(p) }, ticks, priority))
    }

    @Throws(IndexOutOfBoundsException::class)
    fun scheduleSoundTask(ticks: Int = 0, priority: Int = 0, callback: (Any?) -> Unit = {}) {
        if (ticks < 0) throw kotlin.IndexOutOfBoundsException("Scheduled Negative Number")
        scheduledSoundTasks.add(Task({ p -> callback(p) }, ticks, priority))
    }

    @Throws(IndexOutOfBoundsException::class)
    fun scheduleFrameTask(ticks: Int = 0, priority: Int = 0, callback: (Any?) -> Unit = {}) {
        if (ticks < 0) throw kotlin.IndexOutOfBoundsException("Scheduled Negative Number")
        scheduledFrameTasks.add(Task({ p -> callback(p) }, ticks, priority))
    }

    @Throws(IndexOutOfBoundsException::class)
    fun scheduleServerTickTask(ticks: Int = 0, priority: Int = 0, callback: (Any?) -> Unit = {}) {
        if (ticks < 0) throw kotlin.IndexOutOfBoundsException("Scheduled Negative Number")
        scheduledServerTasks.add(Task({ p -> callback(p) }, ticks, priority))
    }


    @Throws(IndexOutOfBoundsException::class)
    fun schedulePostTickTask(ticks: Int = 0, priority: Int = 0, callback: (Any?) -> Unit = {}) {
        if (ticks < 0) throw kotlin.IndexOutOfBoundsException("Scheduled Negative Number")
        scheduledPostTickTasks.add(Task({ p -> callback(p) }, ticks, priority))
    }
    @Throws(IndexOutOfBoundsException::class)
    fun scheduleHighPreTickTask(ticks: Int = 0, priority: Int = 0, callback: (Any?) -> Unit = {}) {
        if (ticks < 0) throw kotlin.IndexOutOfBoundsException("Scheduled Negative Number")
        scheduledHighPreTickTasks.add(Task({ p -> callback(p) }, ticks, priority))
    }


    @Throws(IndexOutOfBoundsException::class)
    fun scheduleHighPostTickTask(ticks: Int = 0, priority: Int = 0, callback: (Any?) -> Unit = {}) {
        if (ticks < 0) throw kotlin.IndexOutOfBoundsException("Scheduled Negative Number")
        scheduledHighPostTickTasks.add(Task({ p -> callback(p) }, ticks, priority))
    }

    @Throws(IndexOutOfBoundsException::class)
    fun scheduleLowestPreTickTask(ticks: Int = 0, priority: Int = 0, callback: (Any?) -> Unit = {}) {
        if (ticks < 0) throw kotlin.IndexOutOfBoundsException("Scheduled Negative Number")
        scheduledLowestPreTickTasks.add(Task({ p -> callback(p) }, ticks, priority))
    }

    @Throws(IndexOutOfBoundsException::class)
    fun scheduleLowestPostTickTask(ticks: Int = 0, priority: Int = 0, callback: (Any?) -> Unit = {}) {
        if (ticks < 0) throw kotlin.IndexOutOfBoundsException("Scheduled Negative Number")
        scheduledLowestPostTickTasks.add(Task({ p -> callback(p) }, ticks, priority))
    }

    @Throws(IndexOutOfBoundsException::class)
    fun schedulePrePlayerTickTask(ticks: Int = 0, priority: Int = 0, callback: (Any?) -> Unit = {}) {
        if (ticks < 0) throw kotlin.IndexOutOfBoundsException("Scheduled Negative Number")
        scheduledPrePlayerTickTasks.add(Task({ p -> callback(p) }, ticks, priority))
    }

    @Throws(IndexOutOfBoundsException::class)
    fun schedulePostPlayerTickTask(ticks: Int = 0, priority: Int = 0, callback: (Any?) -> Unit = {}) {
        if (ticks < 0) throw kotlin.IndexOutOfBoundsException("Scheduled Negative Number")
        scheduledPostPlayerTickTasks.add(Task({ p -> callback(p) }, ticks, priority))
    }

    @Throws(IndexOutOfBoundsException::class)
    fun scheduleHighestPostMoveEntityWithHeadingTask(ticks: Int = 0, priority: Int = 0, callback: (Any?) -> Unit = {}) {
        if (ticks < 0) throw kotlin.IndexOutOfBoundsException("Scheduled Negative Number")
        scheduledHighestPostMoveEntityWithHeadingTasks.add(Task({ p -> callback(p) }, ticks, priority))
    }

    @Throws(IndexOutOfBoundsException::class)
    fun scheduleHighestPreMoveEntityWithHeadingTask(ticks: Int = 0, priority: Int = 0, callback: (Any?) -> Unit = {}) {
        if (ticks < 0) throw kotlin.IndexOutOfBoundsException("Scheduled Negative Number")
        scheduledHighestPreMoveEntityWithHeadingTasks.add(Task({ p -> callback(p) }, ticks, priority))
    }

    @Throws(IndexOutOfBoundsException::class)
    fun scheduleLowPreMoveEntityWithHeadingTask(ticks: Int = 0, priority: Int = 0, callback: (Any?) -> Unit = {}) {
        if (ticks < 0) throw kotlin.IndexOutOfBoundsException("Scheduled Negative Number")
        scheduledLowPreMoveEntityWithHeadingTasks.add(Task({ p -> callback(p) }, ticks, priority))
    }

    @Throws(IndexOutOfBoundsException::class)
    fun scheduleC03Task( ticks: Int = 0, cancel: Boolean = false, priority: Int = 0, callback: (Any?) -> Unit = {}) {
        if (ticks < 0) throw kotlin.IndexOutOfBoundsException("Scheduled Negative Number")
        scheduledC03Tasks.add(Task({ p -> callback(p) }, ticks, priority, cancel))
    }

    @Throws(IndexOutOfBoundsException::class)
    fun scheduleLowestC03Task( ticks: Int = 0, cancel: Boolean = false, priority: Int = 0, callback: (Any?) -> Unit = {}) {
        if (ticks < 0) throw kotlin.IndexOutOfBoundsException("Scheduled Negative Number")
        scheduledLowestC03Tasks.add(Task({ p -> callback(p) }, ticks, priority, cancel))
    }

    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        when (event.phase) {
            TickEvent.Phase.START -> {
                runTime++
                scheduledPreTickTasks.doTasks(event)
            }
            TickEvent.Phase.END -> {
                scheduledPostTickTasks.doTasks(event)
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onLowestTick(event: ClientTickEvent) {
        when (event.phase) {
            TickEvent.Phase.START -> {
                scheduledLowestPreTickTasks.doTasks(event)
            }
            TickEvent.Phase.END -> {
                scheduledLowestPostTickTasks.doTasks(event)
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onHighestTick(event: ClientTickEvent) {
        when (event.phase) {
            TickEvent.Phase.START -> {
                scheduledHighPreTickTasks.doTasks(event)
            }
            TickEvent.Phase.END -> {
                scheduledHighPostTickTasks.doTasks(event)
            }
        }
    }

    @SubscribeEvent
    fun onPlayerTick(event: TickEvent.PlayerTickEvent) {
        if (event.player != mc.thePlayer || event.player == null) return
        when (event.phase) {
            TickEvent.Phase.START -> {
                scheduledPrePlayerTickTasks.doTasks(event)
            }
            TickEvent.Phase.END -> {
                scheduledPostPlayerTickTasks.doTasks(event)
            }
        }
    }

    @SubscribeEvent
    fun onC03PacketEvent(event: PacketEvent.Send){
        if (event.packet !is C03PacketPlayer) return
        if (scheduledC03Tasks.doTasks(event)) event.isCanceled = true
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onLowestC03PacketEvent(event: PacketEvent.Send){
        if (event.packet !is C03PacketPlayer) return
        if (scheduledLowestC03Tasks.doTasks(event)) event.isCanceled = true
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTickEvent) {
        if (scheduledServerTasks.doTasks(event)) event.isCanceled = true
    }


    @SubscribeEvent
    fun motionUpdateEvent(event: MotionUpdateEvent.Pre){
        if (scheduledPreMotionUpdateTasks.doTasks(event)) event.isCanceled = true
    }
    @SubscribeEvent(priority = EventPriority.LOW)
    fun lowMotionUpdateEvent(event: MotionUpdateEvent.Pre){
        if (scheduledLowPreMotionUpdateTasks.doTasks(event)) event.isCanceled = true
    }

    @SubscribeEvent()
    fun postMotionUpdateEvent(event: MotionUpdateEvent.Post){
        if (scheduledPostMotionUpdateTasks.doTasks(event)) event.isCanceled = true
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun highestMoveEntityWithHeadingPost(event: MoveEntityWithHeadingEvent.Post){
        if (scheduledHighestPostMoveEntityWithHeadingTasks.doTasks(event)) event.isCanceled = true
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun highestMoveEntityWithHeadingPre(event: MoveEntityWithHeadingEvent.Pre){
        if (scheduledHighestPreMoveEntityWithHeadingTasks.doTasks(event)) event.isCanceled = true
    }
    @SubscribeEvent(priority = EventPriority.LOW)
    fun lowMoveEntityWithHeadingPre(event: MoveEntityWithHeadingEvent.Pre){
        if (scheduledLowPreMoveEntityWithHeadingTasks.doTasks(event)) event.isCanceled = true
    }

    @SubscribeEvent
    fun onSound(event: PacketEvent.Receive) {
        if (event.packet is S29PacketSoundEffect) return
        if (scheduledSoundTasks.doTasks(event)) event.isCanceled = true
    }

    @SubscribeEvent
    fun onFrame(event: RenderWorldLastEvent) {
        if (scheduledFrameTasks.doTasks(event)) event.isCanceled = true
    }

    class Task(val callback: (Any?) -> Unit, var ticks: Int = 0, val priority: Int = 0, val cancel: Boolean = false) : Comparable<Task> {
        var originalIndex: Int = -1
        fun execute(arg: Any?) = callback(arg)

        override fun compareTo(other: Task): Int {
            return when {
                this.priority != other.priority -> other.priority - this.priority
                else -> this.originalIndex - other.originalIndex
            }
        }
    }

    class Tasks {
        private val queue: MutableList<Task> = mutableListOf()

        fun add(task: Task) = queue.add(task)

        fun doTasks(arg: Any? = null): Boolean {
            var cancelled = false
            try {
                queue.forEachIndexed { index, task -> task.originalIndex = index }
                queue.sortWith(compareByDescending<Task> { it.priority }.thenBy { it.originalIndex })

                val after = mutableListOf<Task>()
                val iterator = queue.iterator()
                while (iterator.hasNext()) {
                    val task = iterator.next()
                    iterator.remove()

                    if (task.ticks > 0) {
                        task.ticks--
                        after.add(task)
                    } else {
                        if (task.cancel) cancelled = true
                        task.execute(arg)
                    }
                }
                queue.addAll(after)
            } catch (e: Exception) {
                println("Error while doing tasks: ${e.message}")
            }
            return cancelled
        }

        fun clear() {
            queue.clear()
        }
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Unload){
        reset()
    }



}