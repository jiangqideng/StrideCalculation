package com.jqd.stridecalculation.ui;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.jqd.stridecalculation.ad.AppWallManager;
import com.jqd.stridecalculation.data.DataManager;
import com.jqd.stridecalculation.model.SensorsModel;
import com.jqd.stridecalculation.model.StepCount;
import com.jqd.stridecalculation.task.AsyncMainTask;
import com.jqd.stridecalculation.task.BackgroundService;
import com.tencent.stat.StatService;

/**
 * @author jiangqideng@163.com
 * @date 2016-6-27 下午5:09:13
 * @description 主界面
 */
public class MainActivity extends Activity {
	private Button startButton;
	private Button stopButton;
	private Button pauseButton;
	private Button continueButton;
	private Button lockScreenButton;
	private ImageButton imageButtonMenu;
	private ImageButton imageButtonNaruto;
	private TextView textView1;
	private TextView textView3;
	private TextView textView12;
	private TextView textView4;
	private TextView textView5;
	private TextView textView10;
	private TextView textView15;
	private SeekBar seekBar;
	private ProgressBar progressBar;
	AlertDialog menuDialog;
	View menuView;
	GridView menuGrid;
	int[] menu_image_array = { R.drawable.ic_menu_recent_history,
			R.drawable.ic_menu_view, R.drawable.ic_menu_preferences,
			R.drawable.ic_menu_help, R.drawable.ic_menu_info_details,
			R.drawable.ic_menu_favorite };
	/** 菜单文字 **/
	String[] menu_name_array = { "历史记录", "注意事项", "设置", "计步原理", "关于", "更多" };

	private DataManager dataManager;
	private SensorsModel sensorsModel;

	private float bright_careful = 0.005f;
	public boolean lockScreen = false;
	private Thread timeThread;
	private int barStart = 100;
	PowerManager.WakeLock mWakeLock = null;
	public int openBackgroundService = 0;
	String TAG = "MainActivity";
	private AsyncMainTask asyncMainTask;
	private boolean firstStart = true;
	private boolean firstCreat = true;
	private float screenBrightnessLock = 0.01f;
	public static final int FLAG_HOMEKEY_DISPATCHED = 0x80000000;
	private SharedPreferences settings;
	private boolean isNewUser = true;
	private AlertDialog forNewUsersDialog;
	private AppWallManager appWallManager;
	private StepCount stepCount;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE); // 声明使用自定义标题
		this.getWindow().setFlags(FLAG_HOMEKEY_DISPATCHED,
				FLAG_HOMEKEY_DISPATCHED);// 允许屏蔽home键
		setContentView(R.layout.activity_main);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title);// 自定义布局赋值
		StatService.trackCustomEvent(this, "onCreate", "");

		initResources();
		initalShow();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onPause() {
		// 存储数据，开启服务继续计步,关闭线程，取消传感器监听
		dataManager.writeDatabase(MainActivity.this, asyncMainTask);
		if (openBackgroundService != 0) {
			Intent intent = new Intent(MainActivity.this,
					BackgroundService.class);
			startService(intent);
		}
		asyncMainTask.cancelThread = true;
		releaseWakeLock();
		super.onPause();
	}

	@Override
	protected void onResume() {
		// 获取日期
		dataManager.dateToday = dataManager.c.get(Calendar.YEAR) + "年"
				+ (dataManager.c.get(Calendar.MONTH) + 1) + "月"
				+ dataManager.c.get(Calendar.DAY_OF_MONTH) + "日";
		textView15.setText(dataManager.dateToday);
		firstStart = true;
		// 开始异步的计时器
		asyncMainTask.cancelThread = false;
		// Log.i(TAG, "MainActivity Resume!");
		timeThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while (!asyncMainTask.cancelThread) {
						Thread.sleep(10);// 睡10ms
						stepCount.timeOfTheDay++;
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		timeThread.start();
		// 关闭服务，显示，开启主异步任务
		Intent intent = new Intent(this, BackgroundService.class);
		stopService(intent);
		asyncMainTask = new AsyncMainTask(MainActivity.this);
		asyncMainTask.execute();
		super.onResume();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		sendBroadcast(new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
	}

	// 两次返回退出
	private long exitTime = 0;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (!lockScreen) {
			if (keyCode == KeyEvent.KEYCODE_BACK
					&& event.getAction() == KeyEvent.ACTION_DOWN) {
				if ((System.currentTimeMillis() - exitTime) > 2000) {
					Toast.makeText(getApplicationContext(), "再次点击“返回”退出",
							Toast.LENGTH_SHORT).show();
					exitTime = System.currentTimeMillis();
				} else {
					finish();
					// System.exit(0); 执行这个的话关闭的比较狠，不会调用Ondestroy方法了,
				}
				return true;
			}
			if (keyCode == KeyEvent.KEYCODE_HOME) {
				Intent intent = new Intent(Intent.ACTION_MAIN);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);// 模拟home键（因为home的功能被屏蔽了）
				intent.addCategory(Intent.CATEGORY_HOME);
				startActivity(intent);
			}

			return super.onKeyDown(keyCode, event);
		} else {
			if (keyCode == KeyEvent.KEYCODE_POWER) {// 模拟电源键（因为电源键的功能被屏蔽了）
				WindowManager.LayoutParams lp = getWindow().getAttributes();
				// Log.i(TAG, "呵呵呵呵");
				lp.screenBrightness = bright_careful;// 设置锁屏时的亮度
				if (bright_careful == 0.005f) {
					bright_careful = -1;
				} else {
					bright_careful = 0.005f;
				}
				getWindow().setAttributes(lp);
			}
			return true;// 屏蔽按键
		}

	}

	// button---listener
	OnClickListener clickListener = new OnClickListener() {
		public void onClick(View v) {
			if (!lockScreen) {

				switch (v.getId()) {
				case R.id.startButton:
					textView12.setText("计步中");
					asyncMainTask.startSensor = true;
					progressBar.setVisibility(View.VISIBLE);
					startButton.setVisibility(View.INVISIBLE);
					pauseButton.setVisibility(View.VISIBLE);

					sensorsModel.sensorManager.registerListener(
							sensorEventListener, sensorsModel.sensor,
							SensorManager.SENSOR_DELAY_FASTEST);
					break;
				case R.id.pauseButton:
					textView12.setText("暂停中");
					asyncMainTask.startSensor = false;
					stepCount.startAdd = false;
					progressBar.setVisibility(View.INVISIBLE);
					pauseButton.setVisibility(View.INVISIBLE);
					continueButton.setVisibility(View.VISIBLE);
					stopButton.setVisibility(View.VISIBLE);
					sensorsModel.sensorManager.unregisterListener(
							sensorEventListener, sensorsModel.sensor);// 取消监听
					break;
				case R.id.continueButton:
					textView12.setText("计步中");
					asyncMainTask.startSensor = true;
					progressBar.setVisibility(View.VISIBLE);
					pauseButton.setVisibility(View.VISIBLE);
					stopButton.setVisibility(View.INVISIBLE);
					continueButton.setVisibility(View.INVISIBLE);
					sensorsModel.sensorManager.registerListener(
							sensorEventListener, sensorsModel.sensor,
							SensorManager.SENSOR_DELAY_FASTEST);
					break;
				case R.id.stopButton:
					textView12.setText("未开始");
					asyncMainTask.startSensor = false;
					stepCount.startAdd = false;
					progressBar.setVisibility(View.INVISIBLE);
					startButton.setVisibility(View.VISIBLE);
					stopButton.setVisibility(View.INVISIBLE);
					continueButton.setVisibility(View.INVISIBLE);
					sensorsModel.sensorManager.unregisterListener(
							sensorEventListener, sensorsModel.sensor);// 取消监听
					stepCount.path = 0;
					stepCount.pathAtOneTime = 0;
					asyncMainTask.activeTime = 0;
					asyncMainTask.activeTimeLast = 0;
					dispActiveTime();
					dispPath();
					dispCalorieAndDistance();
					break;

				default:
					break;
				}

			}
		}
	};

	public void dispActiveTime() {
		int second = (int) (asyncMainTask.activeTime / 100) % 60;
		int minute = (int) asyncMainTask.activeTime / 100 / 60;
		int hour = (int) asyncMainTask.activeTime / 100 / 60 / 60;
		textView3.setText("");
		if (hour < 10) {
			textView3.append("0");
		}
		textView3.append(String.valueOf(hour) + ":");
		if (minute < 10) {
			textView3.append("0");
		}
		textView3.append(String.valueOf(minute) + ":");
		if (second < 10) {
			textView3.append("0");
		}
		textView3.append(String.valueOf(second));

	}

	public void dispPath() {
		textView1.setText(String.valueOf(stepCount.path));
		if (stepCount.path == 10000) {// 网上真机测试说有错误，可能是这个引起的，删掉试试
			Bitmap bmp = BitmapFactory.decodeResource(this.getResources(),
					R.drawable.ic_launcher);
			NotificationManager manager = (NotificationManager) this
					.getSystemService(Context.NOTIFICATION_SERVICE);
			NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
					this)
					.setSmallIcon(R.drawable.ic_launcher)
					.setLargeIcon(bmp)
					.setContentTitle("手机计步神器")
					.setContentText("恭喜您今日步数过万，为自己鼓个掌吧！")
					.setTicker("风一样的驰骋...")
					.setDefaults(
							Notification.DEFAULT_LIGHTS
									| Notification.DEFAULT_SOUND);
			Notification notification = mBuilder.build();
			manager.notify(1, notification);
		}

	}

	public void dispCalorieAndDistance() {
		dataManager.distance = (int) (stepCount.path * dataManager.pathLength);// cm
		dataManager.calorie = (int) (1.036f * dataManager.userWeight
				* dataManager.distance / 100000);// cal
		dataManager.fatCost = (int) (dataManager.calorie / 7.7f);
		textView4.setText(dataManager.calorie + "cal");
		textView5.setText((int) (dataManager.distance / 100) + "m");
		textView10.setText("≈ " + dataManager.fatCost + "克肥肉");
	}

	// 终于弄完主界面了，下面的是移植过来的菜单们，改一改
	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {

		if (menuDialog == null) {
			menuDialog = new AlertDialog.Builder(this).setView(menuView).show();
		} else {
			menuDialog.show();
		}
		return false;// 返回为true 则显示系统menu
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add("menu");// 必须创建一项
		return super.onCreateOptionsMenu(menu);
	}

	// 把和菜单相关的代码打包放下面这个函数
	public void creatMenu() {
		menuView = View.inflate(this, R.layout.my_menu, null);
		// 创建AlertDialog
		menuDialog = new AlertDialog.Builder(this).create();
		// //设置透明度
		// Window window = menuDialog.getWindow();
		// WindowManager.LayoutParams lp = window.getAttributes();
		// lp.alpha = 0.7f;
		// window.setAttributes(lp);

		menuDialog.setView(menuView);
		menuDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {

			public boolean onKey(DialogInterface dialog, int keyCode,
					KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_MENU)// 监听按键
					dialog.dismiss();
				return false;
			}
		});

		menuGrid = (GridView) menuView.findViewById(R.id.gridview);
		menuGrid.setAdapter(getMenuAdapter(menu_name_array, menu_image_array));
		/** 监听menu选项 **/
		menuGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				menuDialog.hide();
				switch (arg2) {
				case 0:// 历史
					openBackgroundService = 0;
					Intent intent = new Intent(MainActivity.this,
							HistoryActivity.class);
					startActivity(intent);
					break;
				case 1:// 注意事项

					Intent intent1 = new Intent(MainActivity.this,
							NoteActivity.class);
					startActivity(intent1);
					break;
				case 2:// 设置
					openBackgroundService = 0;
					Intent intent11 = new Intent(MainActivity.this,
							SettingActivity.class);
					startActivity(intent11);
					break;
				case 3:// 计步原理
					Intent intent3 = new Intent(MainActivity.this,
							TheoryActivity.class);
					startActivity(intent3);
					break;
				case 4:// 详细
					Intent intent4 = new Intent(MainActivity.this,
							AboutActivity.class);
					startActivity(intent4);
					break;
				case 5:// 更多
					appWallManager.showAppWall();
					// Log.i(TAG, "wall");
					break;
				}

			}
		});

		imageButtonMenu.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (!lockScreen) {

					if (menuDialog == null) {
						menuDialog = new AlertDialog.Builder(MainActivity.this)
								.setView(menuView).show();
					} else {
						menuDialog.show();
					}
				}
			}
		});

		imageButtonNaruto.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!lockScreen) {

					openBackgroundService = 0;
					Intent intent5 = new Intent(MainActivity.this,
							HistoryActivity.class);
					startActivity(intent5);
				}
			}
		});

	}

	private SimpleAdapter getMenuAdapter(String[] menuNameArray,
			int[] imageResourceArray) {
		ArrayList<HashMap<String, Object>> data = new ArrayList<HashMap<String, Object>>();
		for (int i = 0; i < menuNameArray.length; i++) {
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("itemImage", imageResourceArray[i]);
			map.put("itemText", menuNameArray[i]);
			data.add(map);
		}
		SimpleAdapter simperAdapter = new SimpleAdapter(this, data,
				R.layout.my_menu_items,
				new String[] { "itemImage", "itemText" }, new int[] {
						R.id.item_image, R.id.item_text });
		return simperAdapter;
	}

	// 申请设备电源锁
	private void acquireWakeLock() {
		if (null == mWakeLock) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
					TAG);
			if (null != mWakeLock) {
				mWakeLock.acquire();
			}
		}
	}

	private void acquireWakeLock2() {
		if (null == mWakeLock) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
			if (null != mWakeLock) {
				mWakeLock.acquire();
			}
		}
	}

	// 释放设备电源锁
	private void releaseWakeLock() {
		if (null != mWakeLock) {
			mWakeLock.release();
			mWakeLock = null;
		}
	}

	private void initResources() {
		appWallManager = new AppWallManager(MainActivity.this);
		stepCount = StepCount.getInstance();
		settings = getSharedPreferences("test", MODE_PRIVATE);
		isNewUser = settings.getBoolean("isNewUser", true);

		// Log.i(TAG, "MainActivity Creat!");
		// 实例化组件
		textView1 = (TextView) findViewById(R.id.textView1);
		textView3 = (TextView) findViewById(R.id.textView3);
		textView4 = (TextView) findViewById(R.id.textView4);
		textView5 = (TextView) findViewById(R.id.textView5);
		textView10 = (TextView) findViewById(R.id.textView10);
		textView12 = (TextView) findViewById(R.id.textView12);
		textView15 = (TextView) findViewById(R.id.textView15);
		startButton = (Button) findViewById(R.id.startButton);
		stopButton = (Button) findViewById(R.id.stopButton);
		pauseButton = (Button) findViewById(R.id.pauseButton);
		continueButton = (Button) findViewById(R.id.continueButton);
		lockScreenButton = (Button) findViewById(R.id.buttonLockScreen);
		imageButtonMenu = (ImageButton) findViewById(R.id.imageButtonMenu);
		imageButtonNaruto = (ImageButton) findViewById(R.id.imageButtonNaruto);
		progressBar = (ProgressBar) findViewById(R.id.progressBar1);
		seekBar = (SeekBar) findViewById(R.id.seekBar1);

		creatMenu();
	}

	private class lockScreenOnClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			seekBar.setVisibility(View.VISIBLE);
			v.setVisibility(View.INVISIBLE);
			lockScreen = true;
			seekBar.setProgress(0);
			WindowManager.LayoutParams lp = getWindow().getAttributes();
			lp.screenBrightness = screenBrightnessLock;// 设置锁屏时的亮度
			getWindow().setAttributes(lp);

			acquireWakeLock();// 获取电源锁，保持常亮

		}
	}

	SensorEventListener sensorEventListener = new SensorEventListener() {

		@Override
		public void onSensorChanged(SensorEvent event) {
			asyncMainTask.values0 = event.values[0];
			asyncMainTask.values1 = event.values[1];
			asyncMainTask.values2 = event.values[2];
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}
	};

	private class seekBarListener implements OnSeekBarChangeListener {
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			int progress = seekBar.getProgress();
			if (progress >= 95 && barStart <= 5) {
				seekBar.setProgress(100);
				lockScreen = false;
				seekBar.setVisibility(View.INVISIBLE);
				lockScreenButton.setVisibility(View.VISIBLE);
				WindowManager.LayoutParams lp = getWindow().getAttributes();
				lp.screenBrightness = -1;
				getWindow().setAttributes(lp);

				releaseWakeLock();// 释放电源锁
			} else {

				seekBar.setProgress(0);

			}
			// Log.i("stop", String.valueOf(barStart));
			barStart = 100;
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// WindowManager.LayoutParams lp = getWindow().getAttributes();
			// lp.screenBrightness = 0.1f;
			// getWindow().setAttributes(lp);
		}

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			// TODO Auto-generated method stub
			if (seekBar.getProgress() >= 40 && seekBar.getProgress() <= 60) {
				barStart = 0;
			}
		}
	}

	private void showForNuwUser() {
		AlertDialog.Builder builder = new Builder(this);
		builder.setTitle("欢迎使用手机计步神器!");
		ViewGroup dialogSettingGroup = (ViewGroup) this.getLayoutInflater()
				.inflate(R.layout.dialog_for_new_users, null);
		Button iKnowButton = (Button) dialogSettingGroup
				.findViewById(R.id.iKnowButton);
		Button notShowAgainButton = (Button) dialogSettingGroup
				.findViewById(R.id.notShowAgainButton);

		iKnowButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				forNewUsersDialog.dismiss();
			}
		});
		notShowAgainButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				SharedPreferences.Editor prefEditor = settings.edit();
				prefEditor.putBoolean("isNewUser", false);
				prefEditor.apply();
				forNewUsersDialog.dismiss();
			}
		});

		builder.setView(dialogSettingGroup);
		forNewUsersDialog = builder.create();
		forNewUsersDialog.show();

	}

	public void showNoSensorsInfo() {
		AlertDialog.Builder builder = new Builder(this);
		builder.setTitle("抱歉!");
		ViewGroup dialogSettingGroup = (ViewGroup) this.getLayoutInflater()
				.inflate(R.layout.dialog_for_new_users, null);
		Button iKnowButton = (Button) dialogSettingGroup
				.findViewById(R.id.iKnowButton);
		Button notShowAgainButton = (Button) dialogSettingGroup
				.findViewById(R.id.notShowAgainButton);
		TextView noSensorTextView = (TextView) dialogSettingGroup
				.findViewById(R.id.textViewForNewUsers);

		noSensorTextView.setText("您的设备中不存在加速度传感器，无法使用本应用.");
		iKnowButton.setText("关闭");
		notShowAgainButton.setText("更多应用");
		iKnowButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				forNewUsersDialog.dismiss();
				finish();
			}
		});
		notShowAgainButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				forNewUsersDialog.dismiss();
				appWallManager.showAppWall();
			}
		});

		builder.setView(dialogSettingGroup);
		forNewUsersDialog = builder.create();
		forNewUsersDialog.show();
	}

	private void initalShow() {
		// 组件绑定监听器
		startButton.setOnClickListener(clickListener);
		stopButton.setOnClickListener(clickListener);
		pauseButton.setOnClickListener(clickListener);
		continueButton.setOnClickListener(clickListener);
		// 模拟锁屏
		lockScreenButton.setOnClickListener(new lockScreenOnClickListener());
		seekBar.setOnSeekBarChangeListener(new seekBarListener());
		if (isNewUser) { // 如果是第一次使用软件，弹出说明对话框
			showForNuwUser();
		}

		dataManager = DataManager.getInstance();
		dataManager.c = Calendar.getInstance();
		dataManager.mDay = dataManager.c.get(Calendar.DAY_OF_MONTH);
		asyncMainTask = new AsyncMainTask(MainActivity.this);
		// 传感器
		sensorsModel = new SensorsModel();
		sensorsModel.sensorInit(MainActivity.this, asyncMainTask);
	}

	public void updateViews() {
		dispActiveTime();
		dispPath();
		dispCalorieAndDistance();
		if (openBackgroundService != 0 && firstCreat) {
			textView12.setText("计步中");
			sensorsModel.sensorManager.registerListener(sensorEventListener,
					sensorsModel.sensor, SensorManager.SENSOR_DELAY_FASTEST);
			firstCreat = false;
			asyncMainTask.startSensor = true;
			progressBar.setVisibility(View.VISIBLE);
			startButton.setVisibility(View.INVISIBLE);
			pauseButton.setVisibility(View.VISIBLE);
			continueButton.setVisibility(View.INVISIBLE);
			stopButton.setVisibility(View.INVISIBLE);
		}
		if (openBackgroundService == 0 && firstCreat) {
			textView12.setText("未开始");
			firstCreat = false;
			asyncMainTask.startSensor = false;
			progressBar.setVisibility(View.INVISIBLE);
			startButton.setVisibility(View.VISIBLE);
			pauseButton.setVisibility(View.INVISIBLE);
			continueButton.setVisibility(View.INVISIBLE);
			stopButton.setVisibility(View.INVISIBLE);
			sensorsModel.sensorManager.unregisterListener(sensorEventListener,
					sensorsModel.sensor);// 取消监听
		}
		if (firstStart) {
			firstStart = false;
			if (openBackgroundService != 2) {
				lockScreenButton.setVisibility(View.INVISIBLE);
			}
			switch (openBackgroundService) {
			case 1:// 唤醒启动模式
					// 什么都不用做，Onpause的时候会判断，然后开启service就行了
					// Log.i(TAG, "唤醒启动模式");
				break;
			case 2:// 常亮模式
				acquireWakeLock(); // 获取电源锁
				// Log.i(TAG, "低亮锁屏模式");
				Toast.makeText(MainActivity.this, "点击锁屏按钮进入低亮锁屏模式",
						Toast.LENGTH_SHORT).show();
				lockScreenButton.setVisibility(View.VISIBLE);
				break;
			case 3:// 节电模式一
				acquireWakeLock2();// 获取电源锁
				// Log.i(TAG, "节电模式一");
				break;
			case 4:// 节电模式二
				acquireWakeLock2();// 获取电源锁
				// Log.i(TAG, "节电模式二");
				break;
			default:
				break;
			}
		}
	}
}
