package org.aquarngd.onceshot

import android.app.Activity
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.MediaStore

class MediaStoreActivity:Activity() {
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.R){ // Android 11+
            val path=intent.getStringExtra(ForegroundService.intent_path_id)
            when(intent.getIntExtra(ForegroundService.intent_type_id,ForegroundService.INTENT_DEFAULT)){
                ForegroundService.INTENT_ACTIVITY_DELETE->{
                    startIntentSenderForResult(MediaStore.createDeleteRequest(contentResolver, listOf(
                        Uri.parse(path)
                    )).intentSender,11,null,0,0,0)
                }
            }
        }
        super.onCreate(savedInstanceState, persistentState)
    }
}