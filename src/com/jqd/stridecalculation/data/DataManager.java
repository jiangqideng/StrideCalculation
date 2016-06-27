package com.jqd.stridecalculation.data;

import java.util.Calendar;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.jqd.stridecalculation.model.StepCount;
import com.jqd.stridecalculation.task.AsyncMainTask;
import com.jqd.stridecalculation.ui.MainActivity;

/**
 * @author jiangqideng@163.com
 * @date 2016-6-27 下午5:05:51
 * @description 数据库的操作相关，使用SQLite
 */
public class DataManager {
	public float userWeight = 50f;// kg
	public float pathLength = 60.0f;// cm
	public int calorie = 0;
	public int distance = 0;
	public int fatCost = 0;
	public String dateToday;
	public int mDay;
	public Calendar c;
	public float sensorDegree = 40;
	
	private volatile static DataManager dataManager = null;
	public static DataManager getInstance() {
		if (dataManager == null) {
			synchronized (DataManager.class) {
				if (dataManager == null) {
					dataManager = new DataManager();
				}
			}
		}
		return dataManager;
	}
	
	public void readDatabase(MainActivity activity, AsyncMainTask asyncMainTask) {
		// Log.i(TAG, "readDatabase");
		DatabaseHelper database = new DatabaseHelper(activity);
		SQLiteDatabase myDatabase = null;
		myDatabase = database.getWritableDatabase();
		Cursor cursor = myDatabase.rawQuery(
				"select * from pathToday where dateToday=?",
				new String[] { dateToday });
		StepCount stepCount = StepCount.getInstance();
		if (cursor.moveToFirst()) {
			stepCount.path = cursor.getInt(cursor.getColumnIndex("path"));
			asyncMainTask.activeTime = cursor.getLong(cursor
					.getColumnIndex("activeTime"));
			asyncMainTask.activeTimeLast = asyncMainTask.activeTime;

		} else {
			// 新的日期到了，这时，将旧的写入，当前数据清零
			dateToday = c.get(Calendar.YEAR) + "年"
					+ (c.get(Calendar.MONTH) + 1) + "月" + mDay + "日";
			writeDatabase(activity, asyncMainTask);
			dateToday = c.get(Calendar.YEAR) + "年"
					+ (c.get(Calendar.MONTH) + 1) + "月"
					+ c.get(Calendar.DAY_OF_MONTH) + "日";
			stepCount.path = 0;
			asyncMainTask.activeTime = 0;
			stepCount.timeOfTheDay = 0;
			stepCount.timeOfPathStart = 0;
			stepCount.timeOfPathEnd = 0;
			asyncMainTask.activeTimeLast = 0;
			calorie = 0;
			distance = 0;
			fatCost = 0;
		}

		cursor = myDatabase.query("userData", null, null, null, null, null,
				null);
		if (cursor.moveToFirst()) {
			userWeight = cursor.getFloat(cursor.getColumnIndex("userWeight"));
			pathLength = cursor.getFloat(cursor.getColumnIndex("pathLength"));
			sensorDegree = cursor.getFloat(cursor
					.getColumnIndex("sensorDegree"));
			stepCount.gate = sensorDegree / 10;
			activity.openBackgroundService = cursor.getInt(cursor
					.getColumnIndex("openBackgroundService"));
		}

		cursor.close();
		myDatabase.close();
	}

	public void writeDatabase(MainActivity activity, AsyncMainTask asyncMainTask) {
		// Log.i(TAG, "writeDatabase");
		DatabaseHelper database = new DatabaseHelper(activity);
		SQLiteDatabase myDatabase = null;
		myDatabase = database.getWritableDatabase();
		Cursor cursor = myDatabase.rawQuery(
				"select * from pathToday where dateToday=?",
				new String[] { dateToday });
		if (cursor.moveToFirst()) {
			String sql = "update [pathToday] set path = ?, activeTime =?, "
					+ "calorie =?, fatCost =?, distance =?, "
					+ "userWeight=?, pathLength=? where dateToday=? ";// 修改的SQL语句
			myDatabase.execSQL(
					sql,
					new String[] { String.valueOf(StepCount.getInstance().path),
							String.valueOf(asyncMainTask.activeTime),
							String.valueOf(calorie), String.valueOf(fatCost),
							String.valueOf(distance),
							String.valueOf(userWeight),
							String.valueOf(pathLength), dateToday });// 执行修改,将各数据存储

		} else {
			String sql = "insert into pathToday(dateToday, path, activeTime, calorie, fatCost, distance, userWeight, pathLength) values(?,?,?,?,?,?,?,?);";
			myDatabase.execSQL(
					sql,
					new String[] { dateToday, String.valueOf(StepCount.getInstance().path),
							String.valueOf(asyncMainTask.activeTime),
							String.valueOf(calorie), String.valueOf(fatCost),
							String.valueOf(distance),
							String.valueOf(userWeight),
							String.valueOf(pathLength) });// 新增数据并存储
		}
		cursor.close();
		myDatabase.close();
	}

	// 数据库----管理历史记录，以及activity与service间切换时的数据存储与更新
	public class DatabaseHelper extends SQLiteOpenHelper {

		private static final String DB_NAME = "mydata.db"; // 数据库名称
		private static final int version = 1; // 数据库版本
		private MainActivity activity;

		public DatabaseHelper(MainActivity activity) {
			super(activity, DB_NAME, null, version);
			this.activity = activity;
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			// Log.i(TAG, "CreatDatabase");
			String sql = "create table pathToday("
					+ "dateToday varchar(20) primary key, " + "path INTEGER,"
					+ "activeTime INTEGER," + "calorie INTEGER,"
					+ "fatCost INTEGER," + "distance INTEGER,"
					+ "userWeight float," + "pathLength float,"
					+ "openBackgroundService INTEGE);";
			db.execSQL(sql);
			sql = "create table userData(" + "pathLength float,"
					+ "userWeight float," + "sensorDegree float,"
					+ "openBackgroundService INTEGE);";
			db.execSQL(sql);
			sql = "insert into userData(pathLength, userWeight, sensorDegree, openBackgroundService) values(?,?,?,?);";
			db.execSQL(
					sql,
					new String[] { String.valueOf(pathLength),
							String.valueOf(userWeight),
							String.valueOf(sensorDegree),
							String.valueOf(activity.openBackgroundService) });// 新增数据并存储
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}
	}

}
