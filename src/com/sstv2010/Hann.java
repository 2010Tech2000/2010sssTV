/*
Hann window

Copyright 2025 Ahmet Inan <xdsopl@gmail.com>
*/

package com.sstv2010;

public class Hann {
	static double window(int n, int N) {
		return 0.5 * (1.0 - Math.cos((2.0 * Math.PI * n) / (N - 1)));
	}
}