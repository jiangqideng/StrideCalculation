package com.jqd.stridecalculation.ad;

import android.app.Activity;

import com.qq.e.appwall.GdtAppwall;

/**
 * @author jiangqideng@163.com
 * @date 2016-6-27 下午5:04:39
 * @description 广告墙的管理
 */
public class AppWallManager {
	private GdtAppwall appwall;
	public AppWallManager(Activity activity) {
		appwall = new GdtAppwall(activity, "1102312596", "9030705060756483", false);
	}
	
	public void showAppWall() {
		appwall.doShowAppWall();
	}
}
