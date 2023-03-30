package com.example.virtualgymkotlin

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.virtualgymkotlin.ml.LiteModelMovenetSingleposeLightningTfliteFloat164
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import kotlin.math.abs
import kotlin.math.atan2


class MainActivity : AppCompatActivity() {

     var paint =Paint()

    lateinit var imageProcessor: ImageProcessor

    lateinit var model:LiteModelMovenetSingleposeLightningTfliteFloat164
    lateinit var bitmap :Bitmap
    lateinit var imageView:ImageView
    lateinit var handler : Handler
    lateinit var handlerThread: HandlerThread
    lateinit var textureView: TextureView
    lateinit var cameraManager: CameraManager
    lateinit var details : TextView
    lateinit var yoga : Button
    lateinit var counter: Button
    lateinit var coordinatesx :ArrayList<Float>
    lateinit var coordinatesy :ArrayList<Float>

     var  boolyoga :Boolean =false
     var boolcount : Boolean =false
    var state : String="NONE"
    var bicepcounter : Int=0



    //@SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        get_permissions()

        imageProcessor =ImageProcessor.Builder().add(ResizeOp(192,192,ResizeOp.ResizeMethod.BILINEAR)).build()

        model = LiteModelMovenetSingleposeLightningTfliteFloat164.newInstance(this)


        imageView =findViewById(R.id.imageView)
        imageView.setBackgroundColor(resources.getColor(android.R.color.transparent))
        textureView=findViewById(R.id.textureView)
        cameraManager=getSystemService(CAMERA_SERVICE) as  CameraManager
        handlerThread= HandlerThread("videoThread")
        handlerThread.start()
        handler=Handler((handlerThread).looper)
        paint.setColor(Color.BLUE)

         details=findViewById(R.id.details)
         yoga = findViewById(R.id.yoga)
         counter=findViewById(R.id.exercise)


        boolcount=false
        boolyoga =false
        state = "NONE"









        textureView.surfaceTextureListener= object:TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                open_camera()
            }

            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {

            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                return false
            }

            @SuppressLint("SetTextI18n")
            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
                bitmap = textureView.bitmap!!

                var tensorImage = TensorImage(DataType.UINT8)
                tensorImage.load(bitmap)

                tensorImage=imageProcessor.process(tensorImage)

// Creates inputs for reference.
                val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 192, 192, 3), DataType.UINT8)
                inputFeature0.loadBuffer(tensorImage.buffer)

// Runs model inference and gets result.
                val outputs = model.process(inputFeature0)
                val outputFeature0 = outputs.outputFeature0AsTensorBuffer.floatArray  //here is our result of a bitmap

                coordinatesx = ArrayList<Float>()
                coordinatesy = ArrayList<Float>()


                var y =0;
                if(outputFeature0.isNotEmpty()){
                    while(y<=49){
                        coordinatesx.add(outputFeature0.get(y));
                        coordinatesy.add(outputFeature0.get(y+1));
                        y=y+3

                    }
                }


                var mutable = bitmap.copy(Bitmap.Config.ARGB_8888,true)



                var canvas = Canvas(mutable)

                var h = bitmap.height
                var w = bitmap.width
                var x =0

                while(x<=49){
                    if(outputFeature0.get(x+2) >0.45){


                            canvas.drawCircle(outputFeature0.get(x+1)*w, outputFeature0.get(x)*h,10f,paint)


                    }
                    x+=3
                }



                imageView.setImageBitmap(mutable)


                if(!boolcount && !boolyoga){

                    details.setText("DETAILS")

                }else if(boolcount && !boolyoga){

                    //for exercise

                    if(coordinatesx.size>0 && coordinatesy.size>0){

                        if(getAngle(coordinatesx.get(5),coordinatesy.get(5),coordinatesx.get(7),coordinatesy.get(7),coordinatesx.get(9),coordinatesy.get(9))>160 && getAngle(coordinatesx.get(6),coordinatesy.get(6),coordinatesx.get(8),coordinatesy.get(8),coordinatesx.get(10),coordinatesy.get(10))>160 ){
                            state ="down";
                        }
                        if(getAngle(coordinatesx.get(5),coordinatesy.get(5),coordinatesx.get(7),coordinatesy.get(7),coordinatesx.get(9),coordinatesy.get(9))<30 && state == "down" && getAngle(coordinatesx.get(6),coordinatesy.get(6),coordinatesx.get(8),coordinatesy.get(8),coordinatesx.get(10),coordinatesy.get(10))<30){
                            state ="up";
                            bicepcounter++;

                        }
                        details.setText(" $bicepcounter")

                    }



                }else if(!boolcount && boolyoga){

                    //for yogas

                    if(isviparitakarani(coordinatesx , coordinatesy)){
                        details.setText("Viparita Karani")
                    }else if(isvirabhadrasana(coordinatesx, coordinatesy)){
                        details.setText("Virabhadrasana")
                    }else{
                        details.setText("UNIDENTIFIED")
                    }

                }






            }
        }


    }

    fun getAngle(
        firstPointx: Float,
        firstPointy: Float ,
        midPointx: Float,
        midPointy: Float ,
        lastPointx: Float,
        lastPointy: Float ,
    ): Double {
        var result = Math.toDegrees(
            atan2(
                (lastPointy - midPointy).toDouble(),
                (lastPointx - midPointx).toDouble()
            )
                    - atan2(
                (firstPointy - midPointy).toDouble(),
                (firstPointx - midPointx).toDouble()
            )
        )
        result = abs(result) // Angle should never be negative
        if (result > 180) {
            result = (360.0 - result) // Always get the acute representation of the angle
        }
        return result
    }

    fun isviparitakarani( coordinatesx : ArrayList<Float>,coordinatesy : ArrayList<Float>) : Boolean{

        if(getAngle(coordinatesx.get(5),coordinatesy.get(5),coordinatesx.get(11),coordinatesy.get(11),coordinatesx.get(13),coordinatesy.get(13))>70&&getAngle(coordinatesx.get(5),coordinatesy.get(5),coordinatesx.get(11),coordinatesy.get(11),coordinatesx.get(13),coordinatesy.get(13))<110
            &&getAngle(coordinatesx.get(6),coordinatesy.get(6),coordinatesx.get(12),coordinatesy.get(12),coordinatesx.get(14),coordinatesy.get(14))>70&& getAngle(coordinatesx.get(6),coordinatesy.get(6),coordinatesx.get(12),coordinatesy.get(12),coordinatesx.get(14),coordinatesy.get(14))<110){
            return true
        }



        return false

    }

    fun isvirabhadrasana(coordinatesx : ArrayList<Float>,coordinatesy : ArrayList<Float>) : Boolean{



          if (getAngle(
                  coordinatesx.get(11),
                  coordinatesy.get(11),
                  coordinatesx.get(13),
                  coordinatesy.get(13),
                  coordinatesx.get(15),
                  coordinatesy.get(15)
              ) > 70 && getAngle(
                  coordinatesx.get(11),
                  coordinatesy.get(11),
                  coordinatesx.get(13),
                  coordinatesy.get(13),
                  coordinatesx.get(15),
                  coordinatesy.get(15)
              ) < 110
          ) {
              if (getAngle(
                      coordinatesx.get(12),
                      coordinatesy.get(12),
                      coordinatesx.get(14),
                      coordinatesy.get(14),
                      coordinatesx.get(16),
                      coordinatesy.get(16)
                  ) > 160 && getAngle(
                      coordinatesx.get(12),
                      coordinatesy.get(12),
                      coordinatesx.get(14),
                      coordinatesy.get(14),
                      coordinatesx.get(16),
                      coordinatesy.get(16)
                  ) < 20) {
                  return true
              }
          } else if (getAngle(
                  coordinatesx.get(12),
                  coordinatesy.get(12),
                  coordinatesx.get(14),
                  coordinatesy.get(14),
                  coordinatesx.get(16),
                  coordinatesy.get(16)
              ) > 70 && getAngle(
                  coordinatesx.get(12),
                  coordinatesy.get(12),
                  coordinatesx.get(14),
                  coordinatesy.get(14),
                  coordinatesx.get(16),
                  coordinatesy.get(16)
              ) < 110
          ) {
              if (getAngle(
                      coordinatesx.get(11),
                      coordinatesy.get(11),
                      coordinatesx.get(13),
                      coordinatesy.get(13),
                      coordinatesx.get(15),
                      coordinatesy.get(15)
                  ) > 170 && getAngle(
                      coordinatesx.get(11),
                      coordinatesy.get(11),
                      coordinatesx.get(13),
                      coordinatesy.get(13),
                      coordinatesx.get(15),
                      coordinatesy.get(15)
                  ) < 20) {
                  return true
              }
          }


        return false

    }

     override fun onDestroy() {
         super.onDestroy()
         // Releases model resources if no longer used.
         model.close()
     }



    @SuppressLint("MissingPermission")
    fun open_camera(){

        cameraManager.openCamera(cameraManager.cameraIdList[0],object :CameraDevice.StateCallback(){
            override fun onOpened(p0: CameraDevice) {
                 val captureRequest = p0.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

                var surface = Surface(textureView.surfaceTexture)
                captureRequest.addTarget(surface)

                p0.createCaptureSession(listOf(surface),object:CameraCaptureSession.StateCallback(){
                    override fun onConfigured(p0: CameraCaptureSession) {
                        p0.setRepeatingRequest(captureRequest.build(),null,null)

                    }

                    override fun onConfigureFailed(p0: CameraCaptureSession) {

                    }
                },handler)

            }

            override fun onDisconnected(p0: CameraDevice) {

            }

            override fun onError(p0: CameraDevice, p1: Int) {

            }
    },handler)
    }

    fun get_permissions(){

        if(checkSelfPermission(android.Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA),101)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(grantResults[0]!=PackageManager.PERMISSION_GRANTED) {
            get_permissions()
        }
    }

    fun detect_yoga(view: View) {

        if(boolyoga==true){
            boolyoga=false
        }else {
            boolyoga=true
            boolcount=false
        }
//

    }
    fun count_reps(view: View) {

        if(boolcount==true){
            boolcount=false
        }else {
            boolcount=true
            bicepcounter=0

            boolyoga=false
        }

//

    }
}