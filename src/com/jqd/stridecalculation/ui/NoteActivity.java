package com.jqd.stridecalculation.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

/**
 * @author jiangqideng@163.com
 * @date 2016-6-27 下午5:10:47
 * @description 注意事项的显示
 */
public class NoteActivity extends Activity {

	ImageButton imageButtonTitleNoteButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); 
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE); //声明使用自定义标题 
		setContentView(R.layout.activity_note); 
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_setting);//自定义布局赋值
		
		TextView textView = (TextView) findViewById(R.id.textViewTitleSetting);
		textView.setText("注意事项");
		
		imageButtonTitleNoteButton = (ImageButton) findViewById(R.id.imageButtonTitleSetting);
		imageButtonTitleNoteButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();//返回按钮
			}
		});
		
	}


}
