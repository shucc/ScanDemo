package xunao.zxing.library;

import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.xunao.scandemo.R;
import com.xunao.scandemo.util.ScreenUtil;

import java.io.IOException;
import java.lang.reflect.Field;

import xunao.zxing.library.camera.CameraManager;
import xunao.zxing.library.decode.DecodeThread;
import xunao.zxing.library.utils.BeepManager;
import xunao.zxing.library.utils.CaptureActivityHandler;
import xunao.zxing.library.utils.InactivityTimer;

/**
 * Created by shucc on 16/3/1.
 * cc@cchao.org
 */
public abstract class BaseScanActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private static final String TAG = BaseScanActivity.class.getSimpleName();

    private SurfaceView scanPreview = null;
    private RelativeLayout scanContainer;
    private RelativeLayout scanCropView;
    private ImageView scanLine;

    private CameraManager cameraManager;
    private CaptureActivityHandler handler;
    private InactivityTimer inactivityTimer;
    private BeepManager beepManager;

    private Rect mCropRect = null;

    //是否扫描中
    private boolean isScaning = false;

    private TranslateAnimation animation;

    public Handler getHandler() {
        return handler;
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    private boolean isHasSurface = false;

    protected abstract void getResult(String result);

    static {
        System.loadLibrary("iconv");
    }

    /**
     * 初始化
     */
    protected void initScan() {
        scanPreview = (SurfaceView) findViewById(R.id.capture_preview);
        scanContainer = (RelativeLayout) findViewById(R.id.capture_container);
        scanCropView = (RelativeLayout) findViewById(R.id.capture_crop_view);
        scanLine = (ImageView) findViewById(R.id.capture_scan_line);

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) scanCropView.getLayoutParams();
        params.height = ScreenUtil.width(this).px - ScreenUtil.dipToPx(this, 96);
        scanCropView.setLayoutParams(params);

        inactivityTimer = new InactivityTimer(this);
        beepManager = new BeepManager(this);

        animation = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.9f);
        animation.setDuration(2500);
        animation.setRepeatCount(-1);
        animation.setRepeatMode(Animation.RESTART);
        scanLine.startAnimation(animation);
    }

    public void handleDecode(String rawResult, Bundle bundle) {
        inactivityTimer.onActivity();
        beepManager.playBeepSoundAndVibrate();

        getResult(rawResult);
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumeScen();
    }

    @Override
    protected void onPause() {
        stopScan();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        super.onDestroy();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!isHasSurface) {
            isHasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isHasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    /**
     * 暂停扫码
     */
    protected void stopScan() {
        if (!isScaning) {
            return;
        }
        isScaning = false;
        animation.cancel();
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        inactivityTimer.onPause();
        beepManager.close();
        cameraManager.closeDriver();
        if (!isHasSurface) {
            scanPreview.getHolder().removeCallback(this);
        }
    }

    /**
     * 恢复扫码
     */
    protected void resumeScen() {
        if (isScaning) {
            return;
        }
        isScaning = true;
        if (null != scanLine && null != animation) {
            scanLine.startAnimation(animation);
        }
        cameraManager = new CameraManager(getApplication());
        handler = null;
        if (isHasSurface) {
            initCamera(scanPreview.getHolder());
        } else {
            scanPreview.getHolder().addCallback(this);
            //解决出现的IO异常
            scanPreview.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
        inactivityTimer.onResume();
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a
            // RuntimeException.
            if (handler == null) {
                handler = new CaptureActivityHandler(BaseScanActivity.this, cameraManager, DecodeThread.QRCODE_MODE);
            }

            initCrop();
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e);
            displayFrameworkBugMessageAndExit();
        }
    }

    private void displayFrameworkBugMessageAndExit() {
        Toast.makeText(BaseScanActivity.this, getString(R.string.activity_scan_error_prompt), Toast.LENGTH_SHORT).show();
        finish();
    }

    public void restartPreviewAfterDelay(long delayMS) {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(R.id.restart_preview, delayMS);
        }
    }

    public Rect getCropRect() {
        return mCropRect;
    }

    /**
     * 初始化截取的矩形区域
     */
    private void initCrop() {
        int cameraWidth = cameraManager.getCameraResolution().y;
        int cameraHeight = cameraManager.getCameraResolution().x;

        //获取布局中扫描框的位置信息
        int[] location = new int[2];
        scanCropView.getLocationInWindow(location);

        int cropLeft = location[0];
        int cropTop = location[1] - getStatusBarHeight();

        int cropWidth = scanCropView.getWidth();
        int cropHeight = scanCropView.getHeight();

        // 获取布局容器的宽高
        int containerWidth = scanContainer.getWidth();
        int containerHeight = scanContainer.getHeight();

        // 计算最终截取的矩形的左上角顶点x坐标
        int x = cropLeft * cameraWidth / containerWidth;
        // 计算最终截取的矩形的左上角顶点y坐标
        int y = cropTop * cameraHeight / containerHeight;

        //计算最终截取的矩形的宽度
        int width = cropWidth * cameraWidth / containerWidth;
        //计算最终截取的矩形的高度
        int height = cropHeight * cameraHeight / containerHeight;

        //生成最终的截取的矩形
        mCropRect = new Rect(x, y, width + x, height + y);
    }

    private int getStatusBarHeight() {
        try {
            Class<?> c = Class.forName("com.android.internal.R$dimen");
            Object obj = c.newInstance();
            Field field = c.getField("status_bar_height");
            int x = Integer.parseInt(field.get(obj).toString());
            return getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}
