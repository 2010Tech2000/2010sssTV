# RX Scope Line Rate Debug Plan

## Observed Metrics
- Capture rate: 11025 Hz
- Decode load: ~1.33
- Audio frames: ~10240/sec
- Decoder process calls: ~32/sec
- Scope lines: ~1/sec
- Image lines: ~0/sec
- UI posts: ~1/sec

## Conclusion
UI throttling is no longer the bottleneck. The decoder produces only about one scope line per second, so scope scrolling cannot be smoother until scan-line completion rate increases.

## Proposed Diagnostics
Add more detailed decoder metrics:
1. Sync pulses detected per second.
2. Header detections per second.
3. Fallback `copyLines` calls per second.
4. Current scan-line sample count.
5. Current mode scan-line sample count.
6. Time between completed scope lines.
7. Demodulator sync-pulse width and frequency offset.

## Candidate Optimizations
1. Reduce audio buffer size / increase `process()` cadence if the source is batching too much.
2. Avoid mode re-detection churn after a mode is locked.
3. Optimize sync-pulse detection thresholds for the target device/sample rate.
4. Consider rendering partial scope progress per audio block instead of waiting for a full scan line.
5. Reduce DSP work in `Demodulator.process()` and `decodeScanLine()`.

## Affected Files
- `src/om/sstvandroid2/ReceiveActivity.java`
- `src/om/sstvandroid2/Decoder.java`
- `src/om/sstvandroid2/Demodulator.java`
- `src/om/sstvandroid2/AudioRecordSource.java` or `MicrophoneAudioSource.java`

## Validation
- Confirm whether scope line rate improves with smaller audio buffers.
- Confirm whether sync-pulse detection is the limiting factor.
- Compare Galaxy S and Galaxy 5 meter readings.
