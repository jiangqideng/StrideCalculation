package com.jqd.stridecalculation.model;

import java.util.List;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import com.jqd.stridecalculation.task.AsyncMainTask;
import com.jqd.stridecalculation.ui.MainActivity;

/**
 * @author jiangqideng@163.com
 * @date 2016-6-27 下午5:06:21
 * @description 传感器的的初始处理
 */
public class SensorsModel {
	public SensorManager sensorManager;
	public Sensor sensor;

	public void sensorInit(MainActivity activity, AsyncMainTask asyncMainTask) {
		sensorManager = (SensorManager) activity
				.getSystemService(Context.SENSOR_SERVICE);
		List<Sensor> sensors = sensorManager
				.getSensorList(Sensor.TYPE_ACCELEROMETER);
		if (sensors.size() == 0) {
			activity.showNoSensorsInfo();
		} else {
			sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		}
	}

}
