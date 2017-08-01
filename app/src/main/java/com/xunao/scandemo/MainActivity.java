package com.xunao.scandemo;

import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import xunao.zxing.library.BaseScanActivity;

public class MainActivity extends BaseScanActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initScan();
    }

    @Override
    public void getResult(String result) {
        if (!TextUtils.isEmpty(result)) {
            Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
        }
        Log.d("解码222222", "decode22222: " + System.currentTimeMillis());
        stopScan();
        resumeScen();
    }
}
