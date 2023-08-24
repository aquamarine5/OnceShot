package org.aquarngd.onceshot

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.MediaStore

class MediaStoreActivity:Activity() {
    var path:String?=null
    private fun shareImage(){
        applicationContext.startActivity(Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM,path)
            type = "image/*"
        },getString(R.string.share_screenshot)))
    }
    private fun deleteImage(){
        application.startActivity(Intent().apply {
            setClass(applicationContext,MediaStoreActivity::class.java)
            putExtra(ForegroundService.intent_path_id,path)
            putExtra(ForegroundService.intent_type_id, ForegroundService.INTENT_ACTIVITY_DELETE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
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