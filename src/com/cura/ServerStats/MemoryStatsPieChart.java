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

package com.cura.ServerStats;

/*
 * Description:  This is the class responsible for generating the pie chart seen in the Server Stats module under the "Memory 
 * Usage" section. It produces a chart displaying Used, Free and Total amount of memory on the server.
 */

import org.achartengine.ChartFactory;
import org.achartengine.model.CategorySeries;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.renderer.SimpleSeriesRenderer;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;

public class MemoryStatsPieChart {

	private float total, used, free, usedPercentage, freePercentage;

	public View execute(Context context, String[] data) {
		int[] colors = new int[] { Color.RED, Color.GREEN };
		DefaultRenderer renderer = buildCategoryRenderer(colors);
		total = Float.parseFloat(data[4].replaceAll("\\s", ""));
		used = Float.parseFloat(data[5].replaceAll("\\s", ""));
		free = Float.parseFloat(data[6].replaceAll("\\s", ""));
		// the above are the values that we fetch from the command that we send
		// in the ServerStats activity to get the output of the "free" command
		// after it has been awked and ..had other stuff done to it
		usedPercentage = (used * 100) / total;
		freePercentage = (free * 100) / total;
		// make them percentages so that they can be displayed properly in the
		// chart

		CategorySeries categorySeries = new CategorySeries("Memory Chart");
		categorySeries.add("Free ", freePercentage);
		categorySeries.add("Used", usedPercentage);
		Log.d("account id", "" + freePercentage);
		Log.d("account id", "" + usedPercentage);
		renderer.setPanEnabled(false);
		renderer.setZoomEnabled(false);
		// -------------BUG HERE-------
		// there's still an option here that needs to be disabled so that the
		// image of the pie chart will not zoom in and out while the user is
		// scrolling through the Server Stats activity
		renderer.setInScroll(false);
		renderer.setClickEnabled(false);
		return ChartFactory.getPieChartView(context, categorySeries, renderer);
		// return ChartFactory.getPieChartIntent(context, categorySeries,
		// renderer, null);
	}

	protected DefaultRenderer buildCategoryRenderer(int[] colors) {
		DefaultRenderer renderer = new DefaultRenderer();
		for (int color : colors) {
			SimpleSeriesRenderer r = new SimpleSeriesRenderer();
			r.setColor(color);
			renderer.addSeriesRenderer(r);
		}
		return renderer;
	}
}