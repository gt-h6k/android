package com.nextcloud.client.media

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.lang.IllegalStateException
import com.nextcloud.client.media.PlayerStateMachine.Event
import com.nextcloud.client.media.PlayerStateMachine.State
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    TestPlayerStateMachine.Constructor::class,
    TestPlayerStateMachine.EventHandling::class,
    TestPlayerStateMachine.Stopped::class,
    TestPlayerStateMachine.Downloading::class,
    TestPlayerStateMachine.Preparing::class,
    TestPlayerStateMachine.Playing::class,
    TestPlayerStateMachine.Paused::class
)
class TestPlayerStateMachine {

    abstract class Base {
        @Mock
        protected lateinit var delegate: PlayerStateMachine.Delegate
        protected lateinit var fsm: PlayerStateMachine

        fun setUp(initialState: State) {
            MockitoAnnotations.initMocks(this)
            fsm = PlayerStateMachine(initialState, delegate)
        }
    }

    class Constructor {

        private val delegate: PlayerStateMachine.Delegate = mock()

        @Test
        fun `default state is stopped`() {
            val fsm = PlayerStateMachine(delegate)
            assertEquals(State.STOPPED, fsm.state)
        }

        @Test
        fun `inital state can be set`() {
            val fsm = PlayerStateMachine(State.PREPARING, delegate)
            assertEquals(State.PREPARING, fsm.state)
        }
    }

    class EventHandling : Base() {

        @Before
        fun setUp() {
            super.setUp(State.STOPPED)
        }

        @Test
        fun `can post one event from callback`() {
            whenever(delegate.isDownloaded).thenReturn(false)
            whenever(delegate.onStartDownloading()).then {
                fsm.post(Event.ERROR)
            }

            // WHEN
            //      an event is posted from a state machine callback
            fsm.post(PlayerStateMachine.Event.PLAY) // posts error() in callback

            // THEN
            //      event is handled
            assertEquals(PlayerStateMachine.State.STOPPED, fsm.state)
            verify(delegate).onStartDownloading()
        }

        @Test(expected = IllegalStateException::class)
        fun `only one event can be posted from callback`() {
            whenever(delegate.isDownloaded).thenReturn(false)
            whenever(delegate.onStartDownloading()).then {
                fsm.post(Event.PLAY)
                fsm.post(Event.ERROR)
            }

            // WHEN
            //      an event is posted from a state machine callback
            fsm.post(Event.PLAY) // posts 2 events from callback

            // THEN
            //      throws
        }
    }

    class Stopped : Base() {

        @Before
        fun setUp() {
            super.setUp(State.STOPPED)
        }

        @Test
        fun `initiall state is stopped`() {
            assertEquals(PlayerStateMachine.State.STOPPED, fsm.state)
        }

        @Test
        fun `playing remote media triggers downloading`() {
            // GIVEN
            //      media is not downloaded
            whenever(delegate.isDownloaded).thenReturn(false)

            // WHEN
            //      play is requested
            fsm.post(Event.PLAY)

            // THEN
            //      media stream download starts
            assertEquals(PlayerStateMachine.State.DOWNLOADING, fsm.state)
            verify(delegate).onStartDownloading()
        }

        @Test
        fun `playing local media triggers player preparation`() {
            // GIVEN
            //      media is downloaded
            whenever(delegate.isDownloaded).thenReturn(true)

            // WHEN
            //      play is requested
            fsm.post(Event.PLAY)

            // THEN
            //      player preparation starts
            assertEquals(PlayerStateMachine.State.PREPARING, fsm.state)
            verify(delegate).onPrepare()
        }
    }

    class Downloading : Base() {

        // GIVEN
        //      player is downloading stream URL
        @Before
        fun setUp() {
            setUp(State.DOWNLOADING)
        }

        @Test
        fun `stream url download is successfull`() {
            // WHEN
            //      stream url downloaded
            fsm.post(Event.DOWNLOADED)

            // THEN
            //      player is preparing
            assertEquals(State.PREPARING, fsm.state)
            verify(delegate).onPrepare()
        }

        @Test
        fun `stream url download failed`() {
            // WHEN
            //      download error
            fsm.post(Event.ERROR)

            // THEN
            //      player is stopped
            assertEquals(State.STOPPED, fsm.state)
            verify(delegate).onStopped()
        }

        @Test
        fun `player stopped`() {
            // WHEN
            //      download error
            fsm.post(Event.STOP)

            // THEN
            //      player is stopped
            assertEquals(State.STOPPED, fsm.state)
            verify(delegate).onStopped()
        }
    }

    class Preparing : Base() {

        @Before
        fun setUp() {
            setUp(State.PREPARING)
        }

        @Test
        fun `start in autoplay mode`() {
            // GIVEN
            //      media player is preparing
            //      autoplay is enabled
            whenever(delegate.isAutoplayEnabled).thenReturn(true)

            // WHEN
            //      media player is ready
            fsm.post(Event.PREPARED)

            // THEN
            //      media player is started
            assertEquals(State.PLAYING, fsm.state)
            verify(delegate).onStart()
        }

        @Test
        fun `start in paused mode`() {
            // GIVEN
            //      media player is preparing
            //      autoplay is disabled
            whenever(delegate.isAutoplayEnabled).thenReturn(false)

            // WHEN
            //      media player is ready
            fsm.post(Event.PREPARED)

            // THEN
            //      media player is not started
            assertEquals(State.PAUSED, fsm.state)
            verify(delegate, never()).onStart()
        }

        @Test
        fun `player is stopped during preparation`() {
            // GIVEN
            //      media player is preparing
            // WHEN
            //      stopped
            fsm.post(Event.STOP)

            // THEN
            //      player is stopped
            assertEquals(State.STOPPED, fsm.state)
            verify(delegate).onStopped()
        }

        @Test
        fun `error during preparation`() {
            // GIVEN
            //      media player is preparing
            // WHEN
            //      download error
            fsm.post(Event.ERROR)

            // THEN
            //      player is stopped
            assertEquals(State.STOPPED, fsm.state)
            verify(delegate).onStopped()
        }
    }

    class Playing : Base() {

        @Before
        fun setUp() {
            setUp(State.PLAYING)
        }

        @Test
        fun pause() {
            // GIVEN
            //      media player is playing
            // WHEN
            //      media player is paused
            fsm.post(Event.PAUSE)

            // THEN
            //      media player enters paused state
            assertEquals(State.PAUSED, fsm.state)
            verify(delegate).onPause()
        }

        @Test
        fun stop() {
            // GIVEN
            //      media player is playing
            // WHEN
            //      stopped
            fsm.post(Event.STOP)

            // THEN
            //      player is stopped
            assertEquals(State.STOPPED, fsm.state)
            verify(delegate).onStopped()
        }

        @Test
        fun error() {
            // GIVEN
            //      media player is playing
            // WHEN
            //      error
            fsm.post(Event.ERROR)

            // THEN
            //      player is stopped
            assertEquals(State.STOPPED, fsm.state)
            verify(delegate).onStopped()
        }
    }

    class Paused : Base() {

        @Before
        fun setUp() {
            setUp(State.PAUSED)
        }

        @Test
        fun pause() {
            // GIVEN
            //      media player is paused
            // WHEN
            //      media player is resumed
            fsm.post(Event.START)

            // THEN
            //      media player enters playing state
            //      media player is resumed
            assertEquals(State.PLAYING, fsm.state)
            verify(delegate).onResume()
        }

        @Test
        fun stop() {
            // GIVEN
            //      media player is playing
            // WHEN
            //      stopped
            fsm.post(Event.STOP)

            // THEN
            //      player is stopped
            assertEquals(State.STOPPED, fsm.state)
            verify(delegate).onStopped()
        }

        @Test
        fun error() {
            // GIVEN
            //      media player is playing
            // WHEN
            //      error
            fsm.post(Event.ERROR)

            // THEN
            //      player is stopped
            assertEquals(State.STOPPED, fsm.state)
            verify(delegate).onStopped()
        }
    }
}
