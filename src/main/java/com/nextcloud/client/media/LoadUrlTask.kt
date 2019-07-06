package com.nextcloud.client.media

import android.os.AsyncTask
import com.owncloud.android.files.StreamMediaFileOperation
import com.owncloud.android.lib.common.OwnCloudClient


class LoadUrlTask(
    private val client: OwnCloudClient,
    private val fileId: String,
    private val onResult: (String?) -> Unit
) : AsyncTask<Void, Void, String>() {

    override fun doInBackground(vararg args: Void): String? {
        val sfo = StreamMediaFileOperation(fileId)
        val result = sfo.execute(client)
        return when(result.isSuccess) {
            true -> result.data[0] as String
            false -> null
        }
    }

    override fun onPostExecute(url: String?) {
        if(!isCancelled) {
            onResult(url)
        }
    }
}
