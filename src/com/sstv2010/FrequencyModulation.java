/*
Frequency Modulation

Copyright 2024 Ahmet Inan <xdsopl@gmail.com>
*/

package om.sstvandroid2;

public class FrequencyModulation {
	private float prev;
	private final float scale;
	private final float Pi, TwoPi;
	FrequencyModulation(double bandwidth, double sampleRate) {
		this.Pi = (float) Math.PI;
		this.TwoPi = 2 * this.Pi;
		this.scale = (float) (sampleRate / (bandwidth * Math.PI));
	}
	float demod(Complex input) {
		float phase = (float) Math.atan2(input.imag, input.real);
		float delta = phase - prev;
		if (delta < -Pi) delta += TwoPi;
		else if (delta > Pi) delta -= TwoPi;
		prev = phase;
		return scale * delta;
	}
}
