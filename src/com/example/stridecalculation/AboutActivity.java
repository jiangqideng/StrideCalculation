package com.example.stridecalculation;

import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

public class AboutActivity extends Activity {

	ImageButton imageButtonTitle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); 
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE); //声明使用自定义标题 
		setContentView(R.layout.activity_about); 
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_setting);//自定义布局赋值
		
		TextView textView = (TextView) findViewById(R.id.textViewTitleSetting);
		textView.setText("关于");
		
		imageButtonTitle = (ImageButton) findViewById(R.id.imageButtonTitleSetting);
		imageButtonTitle.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();//返回按钮
			}
		});
	}


}
