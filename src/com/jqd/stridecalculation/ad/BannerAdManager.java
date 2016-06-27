package com.jqd.stridecalculation.ad;

import com.jqd.stridecalculation.ui.SettingActivity;
import com.qq.e.ads.AdRequest;
import com.qq.e.ads.AdSize;
import com.qq.e.ads.AdView;

/**
 * @author jiangqideng@163.com
 * @date 2016-6-27 下午5:05:13
 * @description 横幅广告条的管理
 */
public class BannerAdManager {
	private AdView bannerAD;

	public void showBannerAD(SettingActivity activity) {
		bannerAD = new AdView(activity, AdSize.BANNER, "1102312596",
				"7010509050751442");
		// bannerAD = new AdView(this, AdSize.BANNER, "1101983001",
		// "9079537216591129292");
		AdRequest adRequest = new AdRequest();
		adRequest.setTestAd(false);
		adRequest.setRefresh(31);
		adRequest.setShowCloseBtn(false);

		activity.extraLayout.removeAllViews();
		activity.extraLayout.addView(bannerAD);
		bannerAD.fetchAd(new AdRequest());
		// Log.i(TAG, "ok");
	}
}
