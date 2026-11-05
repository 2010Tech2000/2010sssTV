/*
Simple Moving Average

Copyright 2024 Ahmet Inan <xdsopl@gmail.com>
*/

package om.sstvandroid2;

public class SimpleMovingAverage extends SimpleMovingSum {
	public SimpleMovingAverage(int length) {
		super(length);
	}

	public float avg(float input) {
		return sum(input) / length;
	}
}
