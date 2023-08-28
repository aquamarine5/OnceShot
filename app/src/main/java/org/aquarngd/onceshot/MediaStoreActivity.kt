package org.aquarngd.onceshot

import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.MediaStore
import android.util.Log

class MediaStoreActivity:Activity() {
    companion object{
        const val classTag="MediaStoreActivity"
        const val INTENT_ACTIVITY_DELETE=11
        const val INTENT_SHARE_DELETE=12
    }
    var path:String?=null
    var uri:Uri?=null
    private fun shareImage(){
        val chooserIntent=Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM,uri)
            type = "image/*"
        },getString(R.string.share_screenshot))
        startActivity(chooserIntent)
        Log.d(classTag,"ShareImage")
    }
    private fun deleteImage(){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.R){
            startIntentSenderForResult(MediaStore.createDeleteRequest(contentResolver, listOf(
                uri
            )).intentSender,11,null,0,0,0)
            Log.d("MediaStoreActivity","DeleteIntent")
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        uri= ContentUris.withAppendedId(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            intent.getLongExtra(ForegroundService.intent_path_id,0)
        )
        Log.d("MediaStoreActivity",uri.toString())
        Log.d(classTag,intent.getIntExtra(ForegroundService.intent_type_id,ForegroundService.INTENT_DEFAULT).toString())
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.R){ // Android 11+

            when(intent.getIntExtra(ForegroundService.intent_type_id,ForegroundService.INTENT_DEFAULT)){
                INTENT_SHARE_DELETE->{
                    shareImage()
                    //deleteImage()
                }
            }
        }
        finish()
        super.onCreate(savedInstanceState)
    }
}