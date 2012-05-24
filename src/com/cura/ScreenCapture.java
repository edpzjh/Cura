package com.cura;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import com.cura.Terminal.TerminalActivity;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Toast;

public class ScreenCapture {

	public ScreenCapture() {
		File snapshots = new File("/sdcard/Cura/Snapshots");
		if (!snapshots.exists()) {
			snapshots.mkdir();
		}
	}
	public void capture(View view, String title, ContentResolver cr)
	{
		View v = view;
        v.setDrawingCacheEnabled(true);
        Bitmap b = v.getDrawingCache();             
        File myPath = new File("/sdcard/Cura/Snapshots/"+title+".png");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(myPath);
            b.compress(Bitmap.CompressFormat.PNG, 80, fos);
            fos.flush();
            fos.close();
            MediaStore.Images.Media.insertImage(cr, b, "Screen", "screen");
        }catch (Exception ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }
	}

}
