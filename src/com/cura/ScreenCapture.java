/*
 Copyright© 2010, 2011 Ahmad Balaa, Oday Maleh

 This file is part of Cura.

	Cura is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Cura is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Cura.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.cura;

/*
 * Description: For snapshotting ability, this activity will be implemented in the Terminal, Server Stats and System Monitor. It
 * is mainly used for proof. So that no one can doubt the user's credibility when they claim that a certain event took place
 * while they were monitoring a certain server using Cura.
 */

import java.io.File;
import java.io.FileOutputStream;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.view.View;

public class ScreenCapture {

 public ScreenCapture() {
  File snapshots = new File("/sdcard/Cura/Snapshots");
  if(!snapshots.exists()) {
   snapshots.mkdir();
  }
 }

 public void capture(View view, String title, ContentResolver cr) {
  View v = view;
  v.setDrawingCacheEnabled(true);
  Bitmap b = v.getDrawingCache();
  File myPath = new File("/sdcard/Cura/Snapshots/" + title + ".png");
  FileOutputStream fos = null;
  try {
   fos = new FileOutputStream(myPath);
   b.compress(Bitmap.CompressFormat.PNG, 80, fos);
   fos.flush();
   fos.close();
   MediaStore.Images.Media.insertImage(cr, b, "Screen", "screen");
  }
  catch (Exception ex) {
   ex.printStackTrace();
  }
 }
}