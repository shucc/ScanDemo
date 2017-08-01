package com.xunao.scandemo.util;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;

/**
 * Created by shucc on 17/8/1.
 * cc@cchao.org
 */
public class ScreenUtil {

    public static class Data {
        public int px;
        public float dp;

        public Data(int px, float density) {
            this.px = px;
            this.dp = px / density;
        }
    }

    /**
     * 获取屏幕
     */
    private static Display getDisplay(Context context) {
        return ((Activity) context).getWindowManager().getDefaultDisplay();
    }

    public static int dipToPx(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 屏幕宽度
     */
    public static Data width(Context context) {
        DisplayMetrics dm = new DisplayMetrics();
        getDisplay(context).getMetrics(dm);
        return new Data(dm.widthPixels, dm.density);
    }

    /**
     * 屏幕高度
     */
    public static Data height(Context context) {
        DisplayMetrics dm = new DisplayMetrics();
        getDisplay(context).getMetrics(dm);
        return new Data(dm.heightPixels, dm.density);
    }
}

