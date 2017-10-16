package com.xunao.scandemo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

/**
 * Created by shucc on 17/10/16.
 * cc@cchao.org
 */
public class ScanSuccessActivity extends AppCompatActivity {

    private static final String KEY_RESULT = "key_result";

    public static void launch(Context context, String result) {
        Intent starter = new Intent(context, ScanSuccessActivity.class);
        starter.putExtra(KEY_RESULT, result);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_success);
        TextView textResult = (TextView) findViewById(R.id.text_result);
        textResult.setText(getIntent().getStringExtra(KEY_RESULT));
        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
