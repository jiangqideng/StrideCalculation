package com.example.stridecalculation;

import java.util.Calendar;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;


public class BackgroundService extends Service{

	String TAG = "backgroundService";
	
	private int path=0;
	
	private SensorManager sensorManager;
	private Sensor sensor;
	private boolean cancelThread=false;
	private int DELAY_TIME=200; //2s没有检测到步伐自动关闭计时器
	private boolean startAdd=false;
	private long timeOfTheDay=0;
	private long timeOfPathStart=0;
	private long timeOfPathEnd=0;
	private long activeTime=0;
	private long activeTimeLast=0;
	private Thread timeThread;
	private int pathAtOneTime=0; //连续的步伐数
	private int DELAY_NUM=1;//若检测到的是单独的一步，不认为是对的，将会再减去
	
	private float userWeight=50f;//kg
	private float pathLength=60.0f;//cm
	private int openBackgroundService=1;
	private int calorie=0;
	private int distance=0;
	private int fatCost=0;	
	private String dateToday;
	private float values0=0;
	private float values1=0;
	private float values2=0;
	private boolean firstStart=true;
	private AsyncMain asyncMain;
	private int pathInLastTime;
	private Calendar c;
	
	private float sensorDegree=50;
	
	private PowerManager.WakeLock mWakeLock;
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		firstStart=true;
		cancelThread = false;
//		Log.i(TAG, "service creat!");
		//传感器实例化
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		sensor  =  sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_FASTEST);//注册传感器监听器
		//获取日期
		c = Calendar.getInstance();
		dateToday = c.get(Calendar.YEAR)+"年"+(c.get(Calendar.MONTH)+1)+"月"+c.get(Calendar.DAY_OF_MONTH)+"日";

		
		timeThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while (!cancelThread) {
						Thread.sleep(10);//睡10ms
						timeOfTheDay++;
//						Log.i(TAG, String.valueOf(timeOfTheDay));
						
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		timeThread.start();
		asyncMain = new AsyncMain();
		asyncMain.execute();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
//		Log.i(TAG, "service destroy!");
		writeDatabase();
		cancelThread = true;
		sensorManager.unregisterListener(sensorEventListener);
		releaseWakeLock();
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}  
	
	public void readDatabase() {
//		Log.i(TAG, "readDatabase");
		DatabaseHelper database = new DatabaseHelper(this);
		SQLiteDatabase myDatabase = null;
		myDatabase = database.getWritableDatabase();
		Cursor cursor = myDatabase.rawQuery("select * from pathToday where dateToday=?", new String[]{dateToday});
		if(cursor.moveToFirst()) {
		    path = cursor.getInt(cursor.getColumnIndex("path"));
		    activeTime = cursor.getLong(cursor.getColumnIndex("activeTime"));activeTimeLast = activeTime;
		}
		
		cursor = myDatabase.query("userData", null, null, null, null, null, null);
		if(cursor.moveToFirst()) {
			userWeight = cursor.getFloat(cursor.getColumnIndex("userWeight"));
		    pathLength = cursor.getFloat(cursor.getColumnIndex("pathLength"));
		    sensorDegree = cursor.getFloat(cursor.getColumnIndex("sensorDegree"));
		    gate = sensorDegree/10;
		    openBackgroundService = cursor.getInt(cursor.getColumnIndex("openBackgroundService"));
//		    Log.i("weight=", String.valueOf(userWeight));
//		    Log.i("pathLength=", String.valueOf(pathLength));
//		    Log.i("sensor=", String.valueOf(sensorDegree));		
		}
		
		cursor.close();
		myDatabase.close();
	}
	
	public void writeDatabase() {
//		Log.i(TAG, "writeDatabase");
		DatabaseHelper database = new DatabaseHelper(this);
		SQLiteDatabase myDatabase = null;
		myDatabase = database.getWritableDatabase();
		Cursor cursor = myDatabase.rawQuery("select * from pathToday where dateToday=?", new String[]{dateToday});
		if(cursor.moveToFirst()) {
			String sql = "update [pathToday] set path = ?, activeTime =?, " +
					"calorie =?, fatCost =?, distance =?, " +
					"userWeight=?, pathLength=? where dateToday=? ";//修改的SQL语句
			myDatabase.execSQL(sql, new String[]{String.valueOf(path), 
					String.valueOf(activeTime), String.valueOf(calorie), 
					String.valueOf(fatCost), String.valueOf(distance), 
					String.valueOf(userWeight), String.valueOf(pathLength), 
					 dateToday});//执行修改,将各数据存储

			
		}else {
			String sql = "insert into pathToday(dateToday, path, activeTime, calorie, fatCost, distance, userWeight, pathLength) values(?,?,?,?,?,?,?,?);";          
			myDatabase.execSQL(sql, new String[]{
					dateToday,String.valueOf(path), 
					String.valueOf(activeTime), String.valueOf(calorie), 
					String.valueOf(fatCost), String.valueOf(distance), 
					String.valueOf(userWeight), String.valueOf(pathLength)});// 新增数据并存储
		}
		cursor.close();
		myDatabase.close();
	}
	
    //数据库----管理历史记录，以及activity与service间切换时的数据存储与更新
    public class DatabaseHelper extends SQLiteOpenHelper{

    	private static final String DB_NAME = "mydata.db"; //数据库名称
        private static final int version = 1; //数据库版本
         
        public DatabaseHelper(Context context) {
            super(context, DB_NAME, null, version);
            // TODO Auto-generated constructor stub
        }
     
        @Override
        public void onCreate(SQLiteDatabase db) {
            String sql = "create table pathToday(" +
            		"dateToday varchar(20) primary key, " +
            		"path INTEGER," +
            		"activeTime INTEGER," +
            		"calorie INTEGER," +
            		"fatCost INTEGER," +
            		"distance INTEGER," +
            		"userWeight float,"+
            		"pathLength float,"+
            		"openBackgroundService INTEGER);";          
            db.execSQL(sql);
            sql = "create table userData(" +
            		"pathLength float,"+
            		"userWeight float,"+
            		"sensorDegree float,"+
            		"openBackgroundService INTEGE);";          
            db.execSQL(sql);
        }
     
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // TODO Auto-generated method stub
     
        }
    }
    
    
    class AsyncMain extends AsyncTask<Void, Void, Void>{

		protected Void doInBackground(Void... params) {
			// TODO Auto-generated method stub
			long timeNow = timeOfTheDay;
			long timeLastSava = timeOfTheDay;  
			while (timeOfTheDay-timeNow<=20) {	//延迟200ms，等待service结束
			}
			readDatabase();
			publishProgress();
			while (!cancelThread) {
				
				if (timeOfTheDay-timeNow>=1) {    //100Hz
					timeNow = timeOfTheDay;

					strideCalculation(values0, values1, values2);
					if (timeOfTheDay-timeOfPathEnd>DELAY_TIME && startAdd) {
						startAdd = false;
						activeTimeLast = activeTime;
						
						if (pathAtOneTime<=DELAY_NUM) {
							path=path-DELAY_NUM;
							activeTime = activeTime-200;
							activeTimeLast = activeTimeLast-200;
							
							publishProgress();
						}
						pathAtOneTime = 0;
					}
					if (startAdd) {
						activeTime = (timeOfTheDay-timeOfPathStart)+activeTimeLast;
						publishProgress();
						
					}

				}
				if (timeOfTheDay-timeLastSava>10000) {
					writeDatabase();//每100秒自动存储一次数据
					timeLastSava=timeOfTheDay;
					
					if (path-pathInLastTime==0 && openBackgroundService==4) {//节电模式二
						releaseWakeLock();
					}
					if (path-pathInLastTime>=1) {
						acquireWakeLock2();
						pathInLastTime=path;
					}
					
				}
				
			}
			return null;

		}


		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
//			Log.i("service异步线程：", "结束");
			super.onPostExecute(result);
		}

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
//			Log.i("service异步线程：", "开始");
			super.onPreExecute();
		}


		@Override
		protected void onProgressUpdate(Void... values) {
			// TODO Auto-generated method stub
			if (firstStart) {
				firstStart=false;
				
				switch (openBackgroundService) {
				case 1://唤醒启动模式
//					Log.i(TAG, "唤醒启动模式");
					break;
				case 2://低亮锁屏模式
//					Log.i(TAG, "低亮锁屏模式");
					break;
				case 3://节电模式一
					acquireWakeLock2();//获取电源锁
//					Log.i(TAG, "节电模式一");
					break;
				case 4://节电模式二
					acquireWakeLock2();//获取电源锁
//					Log.i(TAG, "节电模式二");
					break;
					
				default:
					break;
				}
			}
			
			super.onProgressUpdate(values);
		}

	}
    
    
	private float x=0;
	private float y=0;
	private float z=0;
	private float actualZ0=0f;
	private float actualZ;
	private float [] finalZ = {0f, 0f, 0f};
	private float value0, value1, value2;
	
	private float gate = 4.0f;
	private float gate_f=15.0f;
	private float max=0;
	private float min=0;
	private int maxi=0;
	private int mini=0;
	private boolean waittingH = true;
	
	private int index=1;
	
	public void strideCalculation(float data1, float data2, float data3) {

		if (index<=10) {
			index++;
		}
		if (index>=2) {
			x = (38*x + value0 + data1)/40;
			y = (38*y + value1 + data2)/40;
			z = (38*z + value2 + data3)/40;
			actualZ = (x*data1 + y*data2 +z*data3)/10;
		}
		value0 = data1;
		value1 = data2;
		value2 = data3;
		
		if (index>=3) {
			finalZ[0] = finalZ[1];
			finalZ[1] = finalZ[2];
			finalZ[2] = (70*finalZ[2] + 5*actualZ0 + 5*actualZ)/80;
		}
		
		if (index>=2) {
			actualZ0 = actualZ;
		}
		
		if (index>=5) {
			maxi++;
			mini++;
			if (finalZ[0]<=finalZ[1] && finalZ[1]>=finalZ[2]) {
				if (max==0) {
					max = finalZ[1];
				}else if (finalZ[1]-min>=gate && waittingH==true && maxi>=gate_f) {
					max = finalZ[1];
					waittingH = false;
					maxi = 0;
				}else if (finalZ[1]>max) {
					max = finalZ[1];
				}
			}else if (finalZ[0]>=finalZ[1] && finalZ[1]<=finalZ[2]) {
				if (min==0) {
					min = finalZ[1];
				}else if (max-finalZ[1]>=gate && waittingH==false && mini>=gate_f) {
					min = finalZ[1];
					waittingH = true;
					mini = 0;
					path++;
					pathAtOneTime++;
//					Log.i("path=", String.valueOf(path));
					
					if (!startAdd) {
						timeOfPathStart = timeOfTheDay;
					}
					startAdd = true;
					timeOfPathEnd = timeOfTheDay;
				}else if (finalZ[1]<min) {
					min = finalZ[1];
				}
			}
		}
	}
	
	SensorEventListener sensorEventListener = new SensorEventListener() {
		
		@Override
		public void onSensorChanged(SensorEvent event) {
			// TODO Auto-generated method stub
			values0 = event.values[0];
			values1 = event.values[1];
			values2 = event.values[2];
//			Log.i(TAG, String.valueOf(test));
//			test++;
			
		}
		
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub
//			Log.i(TAG, "accuracyChanged!");
		}
	};
	
	//申请设备电源锁
	private void acquireWakeLock2()
	{
	if (null == mWakeLock)
	{
	PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
	mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
	if (null != mWakeLock)
	{
	mWakeLock.acquire();
	}
	}
	}

	//释放设备电源锁
	private void releaseWakeLock()
	{
	if (null != mWakeLock)
	{
	mWakeLock.release();
	mWakeLock = null;
	}
	}

	
}
