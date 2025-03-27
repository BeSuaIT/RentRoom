package com.example.timphongtro.Utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ImageUtils {
    public static Uri copyImageToCache(Context context, Uri sourceUri) {
        try {
            InputStream input = context.getContentResolver().openInputStream(sourceUri);
            File outputFile = new File(context.getCacheDir(),
                    "temp_image_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream output = new FileOutputStream(outputFile);

            byte[] buffer = new byte[4 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }

            output.flush();
            output.close();
            input.close();

            return Uri.fromFile(outputFile);
        } catch (Exception e) {
            Log.e("ImageUtils", "Error copying image", e);
            return sourceUri;
        }
    }
}
