/*
Copyright 2017 Olga Miller <olga.rgb@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.sstv2010.Output;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class AudioOutput implements IOutput {
    private final double mSampleRate;
    private short[] mAudioBuffer;
    private AudioTrack mAudioTrack;
    private int mBufferPos;
    private long mWrittenFrames;

    public AudioOutput(double sampleRate) {
        mSampleRate = sampleRate;
        mBufferPos = 0;
        mWrittenFrames = 0;
    }

    @Override
    public void init(int samples) {
        mAudioBuffer = new short[(5 * (int) mSampleRate) / 2]; // 2.5 seconds of buffer
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                (int) mSampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, mAudioBuffer.length * 2,
                AudioTrack.MODE_STREAM);
        mWrittenFrames = 0;
        mAudioTrack.play();
    }

    @Override
    public double getSampleRate() {
        return mSampleRate;
    }

    @Override
    public void write(double value) {
        if (mBufferPos == mAudioBuffer.length) {
            flush();
        }

        mAudioBuffer[mBufferPos++] = (short) (value * Short.MAX_VALUE);
    }

    private void flush() {
        if (mBufferPos <= 0 || mAudioTrack == null) return;
        int wrote = mAudioTrack.write(mAudioBuffer, 0, mBufferPos);
        if (wrote > 0) mWrittenFrames += wrote;
        mBufferPos = 0;
    }

    public long getPlaybackHeadPosition() {
        try { if (mAudioTrack != null) return mAudioTrack.getPlaybackHeadPosition(); } catch (Throwable ignore) { }
        return mWrittenFrames;
    }

    public long getWrittenFrames() { return mWrittenFrames + mBufferPos; }

    @Override
    public void finish(boolean cancel) {
        if (mAudioTrack != null) {
            if (!cancel)
                drainBuffer();
            try { if (cancel) mAudioTrack.pause(); } catch (Throwable ignore) { }
            try { mAudioTrack.flush(); } catch (Throwable ignore) { }
            try { mAudioTrack.stop(); } catch (Throwable ignore) { }
            try { mAudioTrack.release(); } catch (Throwable ignore) { }
            mAudioTrack = null;
            mAudioBuffer = null;
        }
    }

    private void drainBuffer() {
        try {
            flush();
            short[] silence = new short[Math.max(1, mAudioBuffer == null ? 2048 : mAudioBuffer.length)];
            int wrote = mAudioTrack.write(silence, 0, silence.length);
            if (wrote > 0) mWrittenFrames += wrote;
            wrote = mAudioTrack.write(silence, 0, silence.length);
            if (wrote > 0) mWrittenFrames += wrote;
            mBufferPos = 0;
        } catch (Throwable ignore) { }
    }
}
