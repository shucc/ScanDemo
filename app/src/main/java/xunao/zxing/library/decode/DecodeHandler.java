/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xunao.zxing.library.decode;

import android.graphics.Rect;
import android.hardware.Camera.Size;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.xunao.scandemo.R;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import java.util.Map;

import xunao.zxing.library.BaseScanActivity;

class DecodeHandler extends Handler {

    private final BaseScanActivity activity;
    private final MultiFormatReader multiFormatReader;
    private boolean running = true;

    private ImageScanner mImageScanner = null;

    public DecodeHandler(BaseScanActivity activity, Map<DecodeHintType, Object> hints) {
        multiFormatReader = new MultiFormatReader();
        multiFormatReader.setHints(hints);
        this.activity = activity;
    }

    @Override
    public void handleMessage(Message message) {
        if (!running) {
            return;
        }
        if (message.what == R.id.decode) {
            decode((byte[]) message.obj, message.arg1, message.arg2);
        } else if (message.what == R.id.quit) {
            running = false;
            Looper.myLooper().quit();
        }
    }

    /**
     * Decode the data within the viewfinder rectangle, and time how long it
     * took. For efficiency, reuse the same reader objects from one decode to
     * the next.
     *
     * @param data   The YUV preview frame.
     * @param width  The width of the preview frame.
     * @param height The height of the preview frame.
     */
    private void decode(byte[] data, int width, int height) {
        Size size = activity.getCameraManager().getPreviewSize();

        // 这里需要将获取的data翻转一下，因为相机默认拿的的横屏的数据
        byte[] rotatedData = new byte[data.length];
        for (int y = 0; y < size.height; y++) {
            for (int x = 0; x < size.width; x++)
                rotatedData[x * size.height + size.height - y - 1] = data[x + y * size.width];
        }

        // 宽高也要调整
        int tmp = size.width;
        size.width = size.height;
        size.height = tmp;
        Handler handler = activity.getHandler();
        if (handler != null) {
            Rect rect = activity.getCropRect();
            Image barCode = new Image(size.width, size.height, "Y800");
            barCode.setData(rotatedData);
            barCode.setCrop(rect.left, rect.top, rect.width(), rect.height());
            if (mImageScanner == null) {
                mImageScanner = new ImageScanner();
                mImageScanner.setConfig(0, Config.X_DENSITY, 3);
                mImageScanner.setConfig(0, Config.Y_DENSITY, 3);
            }
            int resultCode = mImageScanner.scanImage(barCode);
            String result = null;
            if (resultCode != 0) {
                SymbolSet syms = mImageScanner.getResults();
                for (Symbol sym : syms) {
                    result = sym.getData();
                }
                if (!TextUtils.isEmpty(result)) {
                    Message msg = new Message();
                    msg.obj = result;
                    msg.what = R.id.decode_succeeded;
                    handler.sendMessage(msg);
                } else {
                    handler.sendEmptyMessage(R.id.decode_failed);
                }
            } else {
                handler.sendEmptyMessage(R.id.decode_failed);
            }
        }
    }

}
