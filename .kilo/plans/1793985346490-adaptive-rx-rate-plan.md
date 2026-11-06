# Remaining RX Optimization Plan

## Current Baseline
The stable baseline is the original v0.4.0 implementation: 44100 Hz capture, no UI throttling, and original decoder behavior. Adaptive experiments caused stalls and are not retained.

## Observed Failure
Adaptive versions produced only ~1 scope line/sec despite ~10-30 decoder process calls/sec. The decoder stops producing lines after a few scan lines, suggesting sync-pulse handling or fallback line copying stalls rather than UI refresh.

## Recommended Next Experiments
1. Keep the baseline decoder untouched.
2. Reintroduce only the efficient UI path from v0.4.1:
   - No `UI_MIN_MS` or `SCOPE_MIN_MS` throttling.
   - Reuse `mScopeScratch`.
   - Use direct `Bitmap.setPixels(...)`.
3. Add lightweight decoder counters without changing decoder logic:
   - sync pulses
   - headers
   - fallback copy calls
   - new lines
4. Test fixed lower rates independently (`22050`, `16000`, `11025`) without automatic fallback.
5. Compare whether scope line rate improves at lower fixed rates.
6. If still stalled, inspect `processSyncPulse()` fallback condition and `shiftSamples()` bookkeeping.

## Constraints
- Do not combine rate fallback and UI throttling in the same experiment.
- Change one variable at a time.
- Preserve the stable baseline as the rollback point.

## Validation
- Galaxy S: scope lines/sec and image lines/sec should remain above 1 after the first few lines.
- Galaxy 5: no freeze after 3-4 lines.
- No regression in saved WAV decoding.
