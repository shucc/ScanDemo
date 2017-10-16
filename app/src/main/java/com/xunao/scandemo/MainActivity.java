package com.xunao.scandemo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import xunao.zxing.library.BaseScanActivity;

public class MainActivity extends BaseScanActivity {

    private ImageView imgFlash;
    private ImageView imgHDR;

    //是否开启HDR模式
    private boolean isOpenHDR = false;

    //是否开始闪光灯
    private boolean isOpenLight = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imgFlash = (ImageView) findViewById(R.id.img_flash);
        imgHDR = (ImageView) findViewById(R.id.img_hdr);

        initScan();
        bindEvent();
    }

    private void bindEvent() {
        imgHDR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isOpenHDR) {
                    getCameraManager().closeHDR();
                    isOpenHDR = false;
                    imgHDR.setImageResource(R.drawable.ic_hdr_off);
                } else {
                    if (getCameraManager().openHDR()) {
                        isOpenHDR = true;
                        imgHDR.setImageResource(R.drawable.ic_hdr_on);
                    } else {
                        Toast.makeText(MainActivity.this, "当前设备不支持HDR模式", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        imgFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isOpenLight) {
                    getCameraManager().closeLight();
                    imgFlash.setImageResource(R.drawable.ic_flash_off);
                } else {
                    getCameraManager().openLight();
                    imgFlash.setImageResource(R.drawable.ic_flash_on);
                }
                isOpenLight = !isOpenLight;
            }
        });
    }

    @Override
    public void getResult(String result) {
        if (!TextUtils.isEmpty(result)) {
            ScanSuccessActivity.launch(this, result);
        }
        stopScan();
        resumeScen();
    }
}
