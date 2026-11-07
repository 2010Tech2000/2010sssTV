package com.sstv2010;

import android.media.AudioFormat;
import android.media.AudioRecord;
import java.io.IOException;

/** Blocking AudioRecord reads with fixed-scale conversion to the shared PCM contract. */
abstract class AudioRecordSource extends WorkerAudioSource {
	protected final int sampleRate, channels, encoding;
	private final Object recordLock = new Object();
	private AudioRecord record;

	AudioRecordSource(int sampleRate, int channels, int encoding) {
		this.sampleRate = sampleRate;
		this.channels = channels;
		this.encoding = encoding;
	}

	protected abstract AudioRecord createRecord(int channelMask, int bufferBytes);

	@Override protected PcmFormat openInput() throws IOException {
		if (sampleRate <= 0 || (channels != 1 && channels != 2))
			throw new IOException("Unsupported AudioRecord sample rate or channel count");
		if (encoding != AudioFormat.ENCODING_PCM_16BIT) throw new IOException("Unsupported AudioRecord PCM encoding (16-bit required)");
		int mask = channels == 2 ? AudioFormat.CHANNEL_IN_STEREO : AudioFormat.CHANNEL_IN_MONO;
		int min = AudioRecord.getMinBufferSize(sampleRate, mask, encoding);
		if (min <= 0) throw new IOException("Unsupported AudioRecord format: " + min);
		int bytesPerFrame = channels * 2;
		AudioRecord opened = createRecord(mask, Math.max(min, sampleRate / 5 * bytesPerFrame));
		synchronized (recordLock) { record = opened; }
		if (opened.getState() != AudioRecord.STATE_INITIALIZED)
			throw new IOException("AudioRecord initialization failed");
		return new PcmFormat(opened.getSampleRate(), opened.getChannelCount());
	}

	@Override protected void readInput() throws IOException {
		synchronized (recordLock) {
			if (cancelled()) return;
			record.startRecording();
			if (record.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING)
				throw new IOException("AudioRecord did not start");
		}
		int channelCount = getFormat().getChannels();
		int rate = getFormat().getSampleRate();
		int count = Math.max(1, rate / 50) * channelCount;
		float[] pcm = new float[count];
		short[] shorts = new short[count];
		long start = System.nanoTime(), framesSent = 0;
		while (!cancelled()) {
			int n = record.read(shorts, 0, count);
			if (cancelled()) break;
			if (n < 0) throw new IOException("AudioRecord read failed: " + n);
			if (n == 0) continue;
			if (n % channelCount != 0) throw new IOException("Incomplete AudioRecord frame");
			for (int i = 0; i < n; ++i) pcm[i] = shorts[i] / 32768.0f;
			int frames = n / channelCount;
			emit(pcm, frames);
			framesSent += frames;
			long deadline = start + (framesSent * 1000000000L) / rate;
			try {
				long remaining;
				while (!cancelled() && (remaining = deadline - System.nanoTime()) > 0)
					java.util.concurrent.TimeUnit.NANOSECONDS.sleep(remaining);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new java.io.InterruptedIOException("Audio capture cancelled");
			}
		}
	}

	@Override protected void interruptInput() {
		// AudioRecord's native blocking read does not respond to Java interruption.
		// Stop it off the UI thread; release still waits for the read and callback.
		new Thread(new Runnable(){ public void run(){
			synchronized (recordLock) {
				if (record != null && record.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
					try { record.stop(); } catch (IllegalStateException ignored) { }
				}
			}
		}} , "AudioRecordStop").start();
	}

	@Override protected void closeInput() {
		synchronized (recordLock) {
			if (record == null) return;
			try {
				if (record.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) record.stop();
			} finally {
				record.release();
				record = null;
			}
		}
	}
}
