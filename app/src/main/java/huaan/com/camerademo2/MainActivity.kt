package huaan.com.camerademo2

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import kotlinx.android.synthetic.main.activity_main.*
import java.nio.ByteBuffer
import java.util.ArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private var captureBitmap: Bitmap? = null //待检测的图片bmp
    private var captureWidth: Int = 0 //图片宽
    private var captureHeight: Int = 0 //图片高

    private val executors = Executors.newScheduledThreadPool(1)//执行耗时操作，识别算法
    @Volatile
    private var frameGet = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
    }

    fun initView() {
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {

            override fun onSurfaceTextureAvailable(p0: SurfaceTexture?, p1: Int, p2: Int) {
                CameraManager.getInstance().openCamera(this@MainActivity)
                CameraManager.getInstance().startPreview(p0)

                //获取预览图像宽高
                val cameraInfo = CameraManager.getInstance().cameraInfo ?: return
                captureWidth = cameraInfo.previewWidth
                captureHeight = cameraInfo.previewHeight
                //设置需要取出的bitmap大小,这儿缩小一半，大图检测速度慢
                captureWidth /= 3
                captureHeight /= 3
                //当有旋转角度时需要将图片的宽高互换
                if (cameraInfo.orientation == 90 || cameraInfo.orientation == 270) {
                    val tempH = captureHeight
                    captureHeight = captureWidth
                    captureWidth = tempH
                }
                captureBitmap = Bitmap.createBitmap(captureWidth, captureHeight, Bitmap.Config.ARGB_8888)

                executors.scheduleAtFixedRate(FaceWork(), 1, 2, TimeUnit.MILLISECONDS)
            }

            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture?, p1: Int, p2: Int) {

            }

            override fun onSurfaceTextureUpdated(p0: SurfaceTexture?) {
                synchronized(this) {
                    textureView.getBitmap(captureBitmap) //获取到渲染在textureView上面的图像
                }
                frameGet = true
            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture?): Boolean {
                CameraManager.getInstance().stopCamera()
                return false
            }

        }
    }

    //人脸识别相关调度
    internal inner class FaceWork : Runnable {

        private var argbIn: ByteArray? = null
        private var bgrOut: ByteArray? = null
        private var captureBuffer: ByteBuffer? = null

        override fun run() {
            dataConversion()

            /**
            do something
             */
            frameGet = false
        }

        /**
         * 数据转换，将bmp数据转换成bgr，供算法使用
         */
        private fun dataConversion() {
            synchronized(MainActivity::class.java) {
                //将抓拍bmp中的数据读到buffer中
                if (captureBuffer == null) {
                    val count = captureBitmap!!.byteCount
                    captureBuffer = ByteBuffer.allocate(count)
                }
                captureBuffer!!.position(0)
                captureBitmap!!.copyPixelsToBuffer(captureBuffer)
            }
            //用来存放bmp中的argb数据
            if (argbIn == null)
                argbIn = ByteArray(captureWidth * captureHeight * 4)

            captureBuffer!!.position(0)
            captureBuffer!!.get(argbIn)

            //用来存放转换后的bgr数据
            if (bgrOut == null)
                bgrOut = ByteArray(captureWidth * captureHeight * 3) // Allocate for BGR


        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executors.shutdown()
    }

}
