package com.nextcloud.client.media

import android.accounts.Account
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.widget.MediaController
import com.owncloud.android.datamodel.OCFile

class PlayerServiceConnection(private val context: Context) : MediaController.MediaPlayerControl {

    var isConnected: Boolean = false
        private set

    private var service: PlayerService? = null
    private var player: MediaController.MediaPlayerControl? = null

    fun bind() {
        val intent = Intent(context, PlayerService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun unbind() {
        if (isConnected) {
            player = null
            service = null
            isConnected = false
            context.unbindService(connection)
        }
    }

    fun start(account: Account, file: OCFile, playImmediately: Boolean, position: Int) {
        val i = Intent(context, PlayerService::class.java)
        i.putExtra(PlayerService.EXTRA_ACCOUNT, account)
        i.putExtra(PlayerService.EXTRA_FILE, file)
        i.putExtra(PlayerService.EXTRA_AUTO_PLAY, playImmediately)
        i.putExtra(PlayerService.EXTRA_START_POSITION_MS, position)
        i.action = PlayerService.ACTION_PLAY
        context.startService(i)
    }

    fun stop() {
        val i = Intent(context, PlayerService::class.java)
        i.action = PlayerService.ACTION_STOP
        context.startService(i)
    }

    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            isConnected = false
            player = null
            service = null
        }

        override fun onServiceConnected(name: ComponentName?, localBinder: IBinder?) {
            localBinder as PlayerService.Binder
            player = localBinder.player
            service = localBinder.service
            isConnected = true
        }
    }

    // region Media controller

    override fun isPlaying(): Boolean {
        return player?.isPlaying ?: false
    }

    override fun canSeekForward(): Boolean {
        return player?.canSeekForward() ?: false
    }

    override fun getDuration(): Int {
        return player?.duration ?: 0
    }

    override fun pause() {
        player?.pause()
    }

    override fun getBufferPercentage(): Int {
        return player?.bufferPercentage ?: 0
    }

    override fun seekTo(pos: Int) {
        player?.seekTo(pos)
    }

    override fun getCurrentPosition(): Int {
        return player?.currentPosition ?: 0
    }

    override fun canSeekBackward(): Boolean {
        return player?.canSeekBackward() ?: false
    }

    override fun start() {
        player?.start()
    }

    override fun getAudioSessionId(): Int {
        return 0
    }

    override fun canPause(): Boolean {
        return player?.canPause() ?: false
    }

    // endregion
}
