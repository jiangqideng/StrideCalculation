package com.jqd.stridecalculation.model;

/**
 * @author jiangqideng@163.com
 * @date 2016-6-27 下午3:02:22
 * @description 计步算法，核心部分
 */
public class StepCount {
	public int pathAtOneTime = 0; // 连续的步伐数
	public long timeOfTheDay = 0;
	public long timeOfPathStart = 0;
	public long timeOfPathEnd = 0;
	public boolean startAdd = false;
	private float x = 0;
	private float y = 0;
	private float z = 0;
	private float actualZ0 = 0f;
	private float actualZ;
	private float[] finalZ = { 0f, 0f, 0f };
	private float value0, value1, value2;
	public float gate = 4.0f;
	public float gate_f = 15.0f;
	private float max = 0;
	private float min = 0;
	private int maxi = 0;
	private int mini = 0;
	private boolean waittingH = true;
	public int path = 0;
	private int index = 1;

	private volatile static StepCount stepCount = null;
	public static StepCount getInstance() {
		if (stepCount == null) {
			synchronized (StepCount.class) {
				if (stepCount == null) {
					stepCount = new StepCount();
				}
			}
		}
		return stepCount;
	}
	
	/**
	 * @算法思路：
		%1. 低通滤波。
		%2. 寻找波峰波谷。发现一组波峰波谷。使步伐计数器加一。
		%       根据三个时刻的加速度，判断中间时刻是否为极值。
		%           假设是极大值：如果和上一步的最小值相差打过阈值，且满足步频条件，正在等待波峰出现，那么波峰判断成功。
		%           假设是极小值：如果和上一步的最大值相差打过阈值，且满足步频条件，正在等待波谷出现，那么波谷判断成功，步伐加一。
	 */
	public void strideCalculation(float data1, float data2, float data3) {
		if (index <= 10) {
			index++;
		}
		if (index >= 2) { //滤波
			x = (38 * x + value0 + data1) / 40;
			y = (38 * y + value1 + data2) / 40;
			z = (38 * z + value2 + data3) / 40;
			actualZ = (x * data1 + y * data2 + z * data3) / 10;
		}
		value0 = data1;
		value1 = data2;
		value2 = data3;

		if (index >= 3) {
			finalZ[0] = finalZ[1];
			finalZ[1] = finalZ[2];
			finalZ[2] = (70 * finalZ[2] + 5 * actualZ0 + 5 * actualZ) / 80;
		}

		if (index >= 2) {
			actualZ0 = actualZ;
		}

		if (index >= 5) {
			maxi++;
			mini++;
			if (finalZ[0] <= finalZ[1] && finalZ[1] >= finalZ[2]) {
				if (max == 0) {
					max = finalZ[1];
				} else if (finalZ[1] - min >= gate && waittingH == true
						&& maxi >= gate_f) {
					max = finalZ[1];
					waittingH = false;
					maxi = 0;
				} else if (finalZ[1] > max) {
					max = finalZ[1];
				}
			} else if (finalZ[0] >= finalZ[1] && finalZ[1] <= finalZ[2]) {
				if (min == 0) {
					min = finalZ[1];
				} else if (max - finalZ[1] >= gate && waittingH == false
						&& mini >= gate_f) {
					min = finalZ[1];
					waittingH = true;
					mini = 0;
					path++;
					pathAtOneTime++;
					if (!startAdd) {
						timeOfPathStart = timeOfTheDay;
					}
					startAdd = true;
					timeOfPathEnd = timeOfTheDay;
				} else if (finalZ[1] < min) {
					min = finalZ[1];
				}
			}
		}
	}

	
}
