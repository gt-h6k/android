package com.nextcloud.client.media

import android.accounts.Account
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.MediaController
import com.owncloud.android.datamodel.OCFile

class PlayerService : Service() {

    companion object {
        const val EXTRA_ACCOUNT = "ACCOUNT"
        const val EXTRA_FILE = "FILE"
        const val EXTRA_AUTO_PLAY = "EXTRA_AUTO_PLAY"
        const val EXTRA_START_POSITION_MS = "START_POSITION_MS"

        const val ACTION_PLAY = "PLAY"
        const val ACTION_STOP = "STOP"
    }

    class Binder(val service: PlayerService, val player: MediaController.MediaPlayerControl) : android.os.Binder()

    private val player = Player(this)

    override fun onBind(intent: Intent?): IBinder? {
        return Binder(this, player)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when(intent.action) {
            ACTION_PLAY -> onActionPlay(intent)
            ACTION_STOP -> onActionStop()
        }
        return START_NOT_STICKY
    }

    private fun onActionPlay(intent: Intent) {
        val account: Account = intent.getParcelableExtra(EXTRA_ACCOUNT)
        val file: OCFile = intent.getParcelableExtra(EXTRA_FILE)
        val startPos = intent.getIntExtra(EXTRA_START_POSITION_MS, 0)
        val autoPlay = intent.getBooleanExtra(EXTRA_AUTO_PLAY, true)
        player.play(file = file, startPositionMs = startPos, autoPlay = autoPlay, account = account)
    }

    private fun onActionStop() {
        player.stop()
    }
}
