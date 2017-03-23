package com.xpf.backgroundcamera;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private SurfaceView surfaceView;
    private SurfaceHolder mHolder;
    private Camera mCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE); // 取消title
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN); // 设置全屏
        setContentView(R.layout.activity_main);

        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        mHolder = surfaceView.getHolder();
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        // 开启线程进行拍照，因为Activity还未完全显示的时候，是无法进行拍照的，SurfaceView必须先显示
        new Thread(new Runnable() {
            @Override
            public void run() {
                initCamera(); // 初始化camera并对焦拍照
            }
        }).start();

    }

    /**
     * 初始化摄像头
     */
    private void initCamera() {

        // 如果存在摄像头
        if (checkCameraHardware(getApplicationContext())) {
            // 获取摄像头（首选前置，无前置选后置）
            if (openFacingFrontCamera()) {
                Log.e(TAG, "openCameraSuccess");
                autoFocus(); // 对焦
            } else {
                Log.e(TAG, "openCameraFailed");
            }

        }
    }

    /**
     * 自动对焦并拍照
     */
    private void autoFocus() {

        try {
            // 因为开启摄像头需要时间，这里让线程睡两秒
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mCamera.autoFocus(myAutoFocus); // 自动对焦
        mCamera.takePicture(null, null, mPicCallback); // 对焦后拍照
    }


    /**
     * 判断手机是否有摄像头
     *
     * @param context
     * @return
     */
    private boolean checkCameraHardware(Context context) {

        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true; // 有摄像头
        } else {
            return false; // 无摄像头
        }

    }

    /**
     * 得到前置摄像头
     *
     * @return
     */
    private boolean openFacingFrontCamera() {

        // 尝试开启前置摄像头
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int camIdx = 0, cameraCount = Camera.getNumberOfCameras(); camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    Log.e(TAG, "tryToOpenCamera");
                    mCamera = Camera.open(camIdx);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }

        // 如果开启前置失败（无前置）则开启后置
        if (mCamera == null) {
            for (int camIdx = 0, cameraCount = Camera.getNumberOfCameras(); camIdx < cameraCount; camIdx++) {
                Camera.getCameraInfo(camIdx, cameraInfo);
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    try {
                        mCamera = Camera.open(camIdx);
                    } catch (RuntimeException e) {
                        return false;
                    }
                }
            }
        }

        try {
            // 这里的mCamera为已经初始化的Camera对象
            mCamera.setPreviewDisplay(mHolder);
        } catch (IOException e) {
            e.printStackTrace();
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

        mCamera.startPreview();

        return true;
    }

    // 自动对焦回调函数(空实现)
    private Camera.AutoFocusCallback myAutoFocus = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
        }
    };

    /**
     * 拍照成功回调函数
     */
    private Camera.PictureCallback mPicCallback = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            finish(); // 完成拍照后销毁Activity

            // 将得到的照片进行270°旋转，使其竖直
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            Matrix matrix = new Matrix();
            matrix.preRotate(90);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            // 创建并保存图片文件
            File pictureFile = new File(getDir(), getPhotoFileName());
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
            } catch (Exception error) {
                Toast.makeText(MainActivity.this, "拍照失败", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "保存照片失败" + error.toString());
                error.printStackTrace();
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }

            Log.e(TAG, "获取照片成功");
            Toast.makeText(MainActivity.this, "获取照片成功", Toast.LENGTH_SHORT).show();
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    };

    /**
     * 获取照片保存路径
     *
     * @return
     */
    private File getDir() {
        // 得到SD卡根目录
//        File dir = Environment.getExternalStorageDirectory();
        String PHOTO_PATH = "mnt/sdcard/Anloq/Camera/";
        File dir = new File(PHOTO_PATH);

        if (dir.exists()) {
            return dir;
        } else {
            dir.mkdirs();
            return dir;
        }
    }

    /**
     * 获取照片保存名称
     *
     * @return
     */
    public static String getPhotoFileName() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat("'Anloq'_yyyyMMdd_HHmmss");
        return dateFormat.format(date) + ".jpg";
    }

}
