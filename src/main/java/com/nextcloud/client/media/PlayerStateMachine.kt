package com.nextcloud.client.media

import com.github.oxo42.stateless4j.StateMachine
import com.github.oxo42.stateless4j.StateMachineConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.helpers.NOPLogger
import java.lang.IllegalStateException

class PlayerStateMachine(initialState: State, private val delegate: Delegate) {

    constructor(delegate: Delegate) : this(State.STOPPED, delegate)

    interface Delegate {
        val isDownloaded: Boolean
        val isAutoplayEnabled: Boolean
        fun onStartDownloading()
        fun onPrepare()
        fun onStopped()
        fun onStart()
        fun onPause()
        fun onResume()
    }

    enum class State {
        STOPPED,
        RUNNING,
        DOWNLOADING,
        PREPARING,
        PLAYING,
        PAUSED
    }

    enum class Event {
        PLAY,
        DOWNLOADED,
        PREPARED,
        STOP,
        PAUSE,
        START,
        ERROR
    }

    private var pendingEvent: Event? = null
    private var isProcessing = false
    private val fsm: StateMachine<State, Event>
    val state: State get() {return fsm.state}

    init {
        val config = StateMachineConfig<State, Event>()
        config.configure(State.STOPPED)
            .permitIf(Event.PLAY, State.DOWNLOADING) {!delegate.isDownloaded}
            .permitIf(Event.PLAY, State.PREPARING) {delegate.isDownloaded}
            .onEntry(delegate::onStopped)
        config.configure(State.RUNNING)
            .permit(Event.STOP, State.STOPPED)
            .permit(Event.ERROR, State.STOPPED)
        config.configure(State.DOWNLOADING)
            .substateOf(State.RUNNING)
            .permit(Event.DOWNLOADED, State.PREPARING)
            .onEntry(delegate::onStartDownloading)
        config.configure(State.PREPARING)
            .substateOf(State.RUNNING)
            .permitIf(Event.PREPARED, State.PLAYING, {delegate.isAutoplayEnabled})
            .permitIf(Event.PREPARED, State.PAUSED, {!delegate.isAutoplayEnabled})
            .onEntry(delegate::onPrepare)
        config.configure(State.PLAYING)
            .substateOf(State.RUNNING)
            .permit(Event.PAUSE, State.PAUSED)
            .onEntryFrom(Event.PREPARED, delegate::onStart)
            .onEntryFrom(Event.START, delegate::onResume)
        config.configure(State.PAUSED)
            .substateOf(State.RUNNING)
            .permit(Event.START, State.PLAYING)
            .onEntry(delegate::onPause)
        fsm = StateMachine(initialState, config)
    }

    /**
     * Post state machine event to internal queue (of 1 element).
     * This design ensures that we're not triggering multiple events
     * from state machines callbacks, before the transition is fully
     * completed.
     */
    fun post(event: Event) {
        if (pendingEvent == null) {
            pendingEvent = event
        } else {
            throw IllegalStateException("Event already enqueued")
        }
        if (!isProcessing) {
            isProcessing = true
            while (pendingEvent != null) {
                val processedEvent = pendingEvent
                pendingEvent = null
                fsm.fire(processedEvent)
            }
            isProcessing = false
        }
    }
}
