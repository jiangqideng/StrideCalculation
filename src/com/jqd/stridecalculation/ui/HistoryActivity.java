package com.jqd.stridecalculation.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.qq.e.ads.AdRequest;
import com.qq.e.ads.AdSize;
import com.qq.e.ads.AdView;

/**
 * @author jiangqideng@163.com
 * @date 2016-6-27 下午5:08:58
 * @description 历史数据的显示
 */
public class HistoryActivity extends Activity {

	private ImageButton imageButtonTitleHistoryButton;
	String TAG = "HistoryActivity";
	private String dateToday;
	private int path;
	private long activeTime;
	private int distance;
	private int calorie;
	private boolean finished=false;
	private ListView historyListView;
	
	private List<WalkRecord> historyList = new ArrayList<WalkRecord>();
	private WalkRecord walkRecord;
	
	private AdView bannerAD;
	private RelativeLayout extraLayout;
	private SharedPreferences adPreferences;
	private boolean hideAD1 = true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); 
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE); //声明使用自定义标题 
		setContentView(R.layout.activity_history); 
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_setting);//自定义布局赋值
		
		TextView textView = (TextView) findViewById(R.id.textViewTitleSetting);
		textView.setText("历史记录");
		extraLayout = (RelativeLayout) findViewById(R.id.extraLayout);
		
		imageButtonTitleHistoryButton = (ImageButton) findViewById(R.id.imageButtonTitleSetting);
		imageButtonTitleHistoryButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();//返回按钮
			}
		});
		
		adPreferences = getSharedPreferences("test", MODE_PRIVATE);
		hideAD1 = adPreferences.getBoolean("hideAD1", true);
		
		historyListView = (ListView) findViewById(R.id.listView1);
		
		if (tabbleIsExist("pathToday")) {
			
			readDatabase();
			DataDispAdapter adapter = new DataDispAdapter(this, historyList);	
			historyListView.setAdapter(adapter);
		}
		
		
		
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		if (!hideAD1) {
			showBannerAD();
		}else {
			SharedPreferences.Editor prefEditor = adPreferences.edit();
			prefEditor.putBoolean("hideAD1", false);
			prefEditor.apply();
		}
		super.onResume();
	}



	public void readDatabase() {

		DatabaseHelper database = new DatabaseHelper(this);
		SQLiteDatabase myDatabase = null;
		myDatabase = database.getWritableDatabase();

		Cursor cursor = myDatabase.query("pathToday", null, null, null, null, null, null);
		
		if (cursor.moveToLast()) {
			while (true) {
				dateToday = cursor.getString(cursor.getColumnIndex("dateToday"));
			    path = cursor.getInt(cursor.getColumnIndex("path"));
			    activeTime = cursor.getLong(cursor.getColumnIndex("activeTime"));
			    distance = cursor.getInt(cursor.getColumnIndex("distance"));
			    calorie = cursor.getInt(cursor.getColumnIndex("calorie"));
			    
			    walkRecord = new WalkRecord(dateToday, path, activeTime, distance, calorie);
			    historyList.add(walkRecord);
			    
//			    Log.i("dateToday=", String.valueOf(dateToday));
//			    Log.i("path=", String.valueOf(path));
//			    Log.i("activeTime=", String.valueOf(activeTime));
//			    Log.i("calorie=", String.valueOf(calorie));
			    
			    if (finished || cursor.isFirst()) {
					break;
				}

			    cursor.moveToPrevious();//移动到上一条记录
			    if (cursor.isFirst()) {
					finished=true;
				}
			}
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
//        	Log.i(TAG, "CreatDatabase");
            String sql = "create table pathToday(" +
            		"dateToday varchar(20) primary key, " +
            		"path INTEGER," +
            		"activeTime INTEGER," +
            		"calorie INTEGER," +
            		"fatCost INTEGER," +
            		"distance INTEGER," +
            		"userWeight float,"+
            		"pathLength float,"+
            		"openBackgroundService INTEGE);";          
            db.execSQL(sql); 
        }
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // TODO Auto-generated method stub
     
        }
    }
    
    
    /**
     * 判断某张表是否存在
     * @param tabName 表名
     * @return
     */
    public boolean tabbleIsExist(String tableName){
            boolean result = false;
            if(tableName == null){
                    return false;
            }

            Cursor cursor = null;
            try {
	            	DatabaseHelper database = new DatabaseHelper(this);
	        		SQLiteDatabase db1 = null;
	        		db1 = database.getWritableDatabase();
                    String sql = "select count(*) as c from sqlite_master where type ='table' and name ='pathToday'; ";
                    cursor = db1.rawQuery(sql, null);
                    if(cursor.moveToNext()){
                            int count = cursor.getInt(0);
                            if(count>0){
                                    result = true;
                            }
                    }
                    
            } catch (Exception e) {
                    // TODO: handle exception
            }                
            return result;
    }
    
    
    
	private void showBannerAD() {
		bannerAD = new AdView(this, AdSize.BANNER, "1102312596", "5090707080952461");
//		bannerAD = new AdView(this, AdSize.BANNER, "1101983001", "9079537216591129292");
		AdRequest adRequest = new AdRequest();
		adRequest.setTestAd(false);
		adRequest.setRefresh(31);
		adRequest.setShowCloseBtn(false);
		
		extraLayout.removeAllViews();
		extraLayout.addView(bannerAD);
		bannerAD.fetchAd(new AdRequest());
//		Log.i(TAG, "ok");
	}

	
}

class WalkRecord {

	private String dateToday;
	private int path;
	private long activeTime;
	private int distance;
	private int calorie;
	
	public WalkRecord(String dateToday, int path, long activeTime, int distance, int calorie) {
		this.dateToday = dateToday;
		this.path = path;
		this.activeTime = activeTime;
		this.distance = distance;
		this.calorie = calorie;
	}

	/**
	 * @return the dateToday
	 */
	public String getDateToday() {
		return dateToday;
	}

	/**
	 * @param dateToday the dateToday to set
	 */
	public void setDateToday(String dateToday) {
		this.dateToday = dateToday;
	}

	/**
	 * @return the path
	 */
	public int getPath() {
		return path;
	}

	/**
	 * @param path the path to set
	 */
	public void setPath(int path) {
		this.path = path;
	}

	/**
	 * @return the activeTime
	 */
	public long getActiveTime() {
		return activeTime;
	}

	/**
	 * @param activeTime the activeTime to set
	 */
	public void setActiveTime(long activeTime) {
		this.activeTime = activeTime;
	}

	/**
	 * @return the distance
	 */
	public int getDistance() {
		return distance;
	}

	/**
	 * @param distance the distance to set
	 */
	public void setDistance(int distance) {
		this.distance = distance;
	}

	/**
	 * @return the calorie
	 */
	public int getCalorie() {
		return calorie;
	}

	/**
	 * @param calorie the calorie to set
	 */
	public void setCalorie(int calorie) {
		this.calorie = calorie;
	}
	
	
	
}








