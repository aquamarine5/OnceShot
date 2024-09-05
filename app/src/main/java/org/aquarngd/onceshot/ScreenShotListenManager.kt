package org.aquarngd.onceshot

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.WindowManager
import java.util.Locale

class ScreenShotListenManager private constructor(context: Context?) {
    private fun handleMediaContentChange(contentUri: Uri) {
        var cursor: Cursor? = null
        try {
            // 数据改变时查询数据库中最后加入的一条数据
            cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val bundle = Bundle()
                bundle.putStringArray(
                    ContentResolver.QUERY_ARG_SORT_COLUMNS,
                    arrayOf(MediaStore.Images.Media.DATE_ADDED)
                )
                bundle.putInt(
                    ContentResolver.QUERY_ARG_SORT_DIRECTION,
                    ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                )
                bundle.putInt(ContentResolver.QUERY_ARG_LIMIT, 1)
                mContext.contentResolver.query(
                    contentUri,
                    MEDIA_PROJECTIONS_API_16,
                    bundle, null
                )
            } else {
                mContext.contentResolver.query(
                    contentUri,
                    MEDIA_PROJECTIONS_API_16,
                    null,
                    null,
                    MediaStore.Images.ImageColumns.DATE_ADDED + " desc limit 1"
                )
            }
            if (cursor == null) {
                Log.e(TAG, "Deviant logic.")
                return
            }
            if (!cursor.moveToFirst()) {
                Log.d(TAG, "Cursor no data.")
                return
            }

            // 获取各列的索引
            val dataIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
            val dateTakenIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_TAKEN)
            val widthIndex: Int = cursor.getColumnIndex(MediaStore.Images.ImageColumns.WIDTH)
            val heightIndex: Int = cursor.getColumnIndex(MediaStore.Images.ImageColumns.HEIGHT)
            val idIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns._ID)
            val idData = cursor.getLong(idIndex)

            // 获取行数据
            val data = cursor.getString(dataIndex)
            val dateTaken = cursor.getLong(dateTakenIndex)
            val width: Int
            val height: Int
            if (widthIndex >= 0 && heightIndex >= 0) {
                width = cursor.getInt(widthIndex)
                height = cursor.getInt(heightIndex)
            } else {
                // API 16 之前, 宽高要手动获取
                val size = getImageSize(data)
                width = size.x
                height = size.y
            }

            // 处理获取到的第一行数据
            handleMediaRowData(data, dateTaken, width, height, idData)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (cursor != null && !cursor.isClosed) {
                cursor.close()
            }
        }
    }

    private fun getImageSize(imagePath: String): Point {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(imagePath, options)
        return Point(options.outWidth, options.outHeight)
    }

    /**
     * 处理获取到的一行数据
     */
    private fun handleMediaRowData(
        data: String,
        dateTaken: Long,
        width: Int,
        height: Int,
        idData: Long
    ) {
        if (checkScreenShot(data, dateTaken, width, height)) {
            Log.d(
                TAG, "ScreenShot: path = " + data + "; size = " + width + " * " + height
                        + "; date = " + dateTaken
            )
            if (!checkCallback(data)) {
                mListener(idData)
                Log.d(TAG, "Call mListener")
            }
        } else {
            // 如果在观察区间媒体数据库有数据改变，又不符合截屏规则，则输出到 log 待分析
            Log.w(
                TAG, "Media content changed, but not screenshot: path = " + data
                        + "; size = " + width + " * " + height + "; date = " + dateTaken
            )
        }
    }

    /**
     * 判断指定的数据行是否符合截屏条件
     */
    private fun checkScreenShot(data: String, dateTaken: Long, width: Int, height: Int): Boolean {
        /*
         * 判断依据一: 时间判断
         */
        // 如果加入数据库的时间在开始监听之前, 或者与当前时间相差大于10秒, 则认为当前没有截屏
        var data = data
        if (dateTaken < mStartListenTime || System.currentTimeMillis() - dateTaken > 10 * 1000) {
            Log.i(TAG, "checkScreenShot: time")
            return false
        }
        val windowManager = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        display.getRealSize(sScreenRealSize)
        /*
         * 判断依据二: 尺寸判断
         */if (sScreenRealSize != null) {
            // 如果图片尺寸超出屏幕, 则认为当前没有截屏
            if (!(width <= sScreenRealSize!!.x && height <= sScreenRealSize!!.y || height <= sScreenRealSize!!.x && width <= sScreenRealSize!!.y)) {
                Log.i(
                    TAG,
                    "checkScreenShot: size" + sScreenRealSize!!.x + " " + sScreenRealSize!!.y
                )
                return false
            }
        }

        /*
         * 判断依据三: 路径判断
         */if (TextUtils.isEmpty(data)) {
            Log.i(TAG, "checkScreenShot: isempty")
            return false
        }
        data = data.lowercase(Locale.getDefault())
        // 判断图片路径是否含有指定的关键字之一, 如果有, 则认为当前截屏了
        for (keyWork in KEYWORDS) {
            if (data.contains(keyWork)) {
                return true
            }
        }
        Log.i(TAG, "checkScreenShot: is")
        return false
    }

    /**
     * 判断是否已回调过, 某些手机ROM截屏一次会发出多次内容改变的通知; <br></br>
     * 删除一个图片也会发通知, 同时防止删除图片时误将上一张符合截屏规则的图片当做是当前截屏.
     */
    private fun checkCallback(imagePath: String): Boolean {
        if (sHasCallbackPaths.contains(imagePath)) {
            Log.d(
                TAG, "ScreenShot: imgPath has done"
                        + "; imagePath = " + imagePath
            )
            return true
        }
        // 大概缓存15~20条记录便可
        if (sHasCallbackPaths.size >= 20) {
            sHasCallbackPaths.subList(0, 5).clear()
        }
        sHasCallbackPaths.add(imagePath)
        return false
    }

    /**
     * 设置截屏监听器
     */
    fun setListener(listener: (Long) -> Unit) {
        mListener = listener
    }

    /**
     * 启动监听
     */
    fun startListen() {
        assertInMainThread()

        // 记录开始监听的时间戳
        mStartListenTime = System.currentTimeMillis()

        // 创建内容观察者
        mInternalObserver = MediaContentObserver(MediaStore.Images.Media.INTERNAL_CONTENT_URI, null)
        mExternalObserver = MediaContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null)

        // 注册内容观察者
        if (Build.VERSION.SDK_INT < 29) { //Android 9及以下版本,否则不会回调onChange()
            // 注册内容观察者
            mContext.contentResolver.registerContentObserver(
                MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                false,
                mInternalObserver!!
            )
            mContext.contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                false,
                mExternalObserver!!
            )
        } else { //Android 10，11以上版本
            // 注册内容观察者
            mContext.contentResolver.registerContentObserver(
                MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                true,
                mInternalObserver!!
            )
            mContext.contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                mExternalObserver!!
            )
        }
    }

    /**
     * 停止监听
     */
    fun stopListen() {
        assertInMainThread()

        // 注销内容观察者
        if (mInternalObserver != null) {
            try {
                mContext.contentResolver.unregisterContentObserver(mInternalObserver!!)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mInternalObserver = null
        }
        if (mExternalObserver != null) {
            try {
                mContext.contentResolver.unregisterContentObserver(mExternalObserver!!)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mExternalObserver = null
        }

        // 清空数据
        mStartListenTime = 0
        //        sHasCallbackPaths.clear();

        //切记！！！:必须设置为空 可能mListener 会隐式持有Activity导致释放不掉
        mListener = {}
    }

    private val mContext: Context

    /**
     * 运行在 UI 线程的 Handler, 用于运行监听器回调
     */
    private val mUiHandler = Handler(Looper.getMainLooper())
    private var mListener: (Long) -> Unit = { }
    private var mStartListenTime: Long = 0

    /**
     * 内部存储器内容观察者
     */
    private var mInternalObserver: MediaContentObserver? = null

    /**
     * 外部存储器内容观察者
     */
    private var mExternalObserver: MediaContentObserver? = null

    init {
        requireNotNull(context) { "The context must not be null." }
        mContext = context

        // 获取屏幕真实的分辨率
        if (sScreenRealSize == null) {
            sScreenRealSize = realScreenSize
            if (sScreenRealSize != null) {
                Log.d(TAG, "Screen Real Size: " + sScreenRealSize!!.x + " * " + sScreenRealSize!!.y)
            } else {
                Log.w(TAG, "Get screen real size failed.")
            }
        }
    }

    private val realScreenSize: Point
        /**
         * 获取屏幕分辨率
         */
        get() {
            var screenSize: Point?
            try {
                screenSize = Point()
                val windowManager =
                    mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val defaultDisplay = windowManager.defaultDisplay
                defaultDisplay.getRealSize(screenSize)
            } catch (e: Exception) {
                throw e
            }
            return screenSize
        }

    /**
     * 媒体内容观察者(观察媒体数据库的改变)
     */
    private inner class MediaContentObserver(private val mContentUri: Uri, handler: Handler?) :
        ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            handleMediaContentChange(mContentUri)
        }
    }

    companion object {
        private const val TAG = "ScreenShotListenManager"

        /**
         * 读取媒体数据库时需要读取的列, 其中 WIDTH 和 HEIGHT 字段在 API 16 以后才有
         */
        private val MEDIA_PROJECTIONS_API_16 = arrayOf(
            MediaStore.Images.ImageColumns.DATA,
            MediaStore.Images.ImageColumns._ID,
            MediaStore.Images.ImageColumns.DATE_TAKEN,
            MediaStore.Images.ImageColumns.WIDTH,
            MediaStore.Images.ImageColumns.HEIGHT
        )

        /**
         * 截屏依据中的路径判断关键字
         */
        private val KEYWORDS = arrayOf(
            "screenshot", "screen_shot", "screen-shot", "screen shot",
            "screencapture", "screen_capture", "screen-capture", "screen capture",
            "screencap", "screen_cap", "screen-cap", "screen cap", "MuMu12"
        )

        /**
         * 已回调过的路径
         */
        private val sHasCallbackPaths: MutableList<String> = ArrayList()
        private var sScreenRealSize: Point? = null
        fun newInstance(context: Context?): ScreenShotListenManager {
            assertInMainThread()
            return ScreenShotListenManager(context)
        }

        private fun assertInMainThread() {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                val elements = Thread.currentThread().stackTrace
                var methodMsg: String? = null
                if (elements.size >= 4) {
                    methodMsg = elements[3].toString()
                }
                throw IllegalStateException("Call the method must be in main thread: $methodMsg")
            }
        }
    }
}