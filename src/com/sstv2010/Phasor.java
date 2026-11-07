/*
Numerically controlled oscillator

Copyright 2024 Ahmet Inan <xdsopl@gmail.com>
*/

package om.sstvandroid2;

public class Phasor {
	private final Complex value;
	private final Complex delta;
	private int normCount;
	Phasor(double freq, double rate) {
		value = new Complex(1, 0);
		double omega = 2 * Math.PI * freq / rate;
		delta = new Complex((float) Math.cos(omega), (float) Math.sin(omega));
		normCount = 0;
	}
	Complex rotate() {
		value.mul(delta);
		if (++normCount >= 256) {
			normCount = 0;
			float n = value.norm();
			if (n > 0f) { float inv = 1f / (float) Math.sqrt(n); value.real *= inv; value.imag *= inv; }
		}
		return value;
	}
}
