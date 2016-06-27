package com.jqd.stridecalculation.ui;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * @author jiangqideng@163.com
 * @date 2016-6-27 下午5:10:06
 * @description 数据显示的Adapter
 */
public class DataDispAdapter extends BaseAdapter {

	private Context context;

	private List<WalkRecord> list;

	public DataDispAdapter(HistoryActivity context, List<WalkRecord> list) {
		super();
		this.context = context;
		this.list = list;
	}

	// getcount 获取数据的个数
	@Override
	public int getCount() {
		return list.size();
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	// getView 需要构建一个View对象来显示数据源中的数据
	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		WalkRecord walkRecord = list.get(position);

		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		ViewGroup group = (ViewGroup) inflater.inflate(R.layout.walk_record,
				null);
		TextView dateTodayTextView = (TextView) group
				.findViewById(R.id.dateToday);
		TextView pathTextView = (TextView) group.findViewById(R.id.path);
		TextView activeTimeTextView = (TextView) group
				.findViewById(R.id.activeTime);
		TextView distanceTextView = (TextView) group
				.findViewById(R.id.distance);
		TextView calorieTextView = (TextView) group.findViewById(R.id.calorie);

		dateTodayTextView.setText(walkRecord.getDateToday());
		pathTextView.setText(String.valueOf(walkRecord.getPath()));
		activeTimeTextView
				.setText(String.valueOf(walkRecord.getActiveTime() / 6000));
		distanceTextView
				.setText(String.valueOf(walkRecord.getDistance() / 100));
		calorieTextView.setText(String.valueOf(walkRecord.getCalorie()));

		return group;
	}

}