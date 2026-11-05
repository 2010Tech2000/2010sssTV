package om.sstvandroid2;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class AudioSessionManager {
    public interface Monitor {
        void write(float[] pcm, int frames, int rate, int channels) throws IOException;
        void finish() throws IOException;
        void cancel();
        void close();
    }
    public interface SessionCallback {
        void onPcmDataAvailable(float[] buffer, int frameCount, int sampleRate, int channels, long generation);
        void onSourceStateChanged(AudioSource.AudioSourceState state, String message, long generation);
    }

    private final SessionCallback callback;
    private final ExecutorService preparation = Executors.newSingleThreadExecutor(new ThreadFactory() {
        public Thread newThread(Runnable r) { return new Thread(r, "AudioSession"); }
    });
    private volatile long generation;
    private volatile boolean released;
    private AudioSource currentSource;
    private Monitor currentMonitor;
    private CountDownLatch previousRelease = new CountDownLatch(0);

    public AudioSessionManager(SessionCallback callback) { this.callback = callback; }
    public boolean isCurrent(long session) { return !released && generation == session; }
    public void startSession(AudioSource source) { startSession(source, null); }

    public void startSession(final AudioSource source, final Monitor monitor) {
        if (released) throw new IllegalStateException("Session manager is released");
        stopAndReleaseCurrentSession();
        final long session = generation;
        final CountDownLatch waitForPrevious = previousRelease;
        final CountDownLatch sourceReleased = new CountDownLatch(1);
        previousRelease = sourceReleased;
        currentSource = source;
        currentMonitor = monitor;
        source.setAudioDataListener(new AudioSource.AudioDataListener() {
            private boolean monitorFailed;
            private void monitorFailure(Exception error) {
                if (monitorFailed) return;
                monitorFailed = true;
                source.release();
                if (isCurrent(session))
                    callback.onSourceStateChanged(AudioSource.AudioSourceState.ERROR, String.valueOf(error), session);
            }
            public void onAudioDataAvailable(float[] pcm, int frames, int rate, int channels) {
                if (!isCurrent(session) || monitorFailed) return;
                try {
                    if (monitor != null) monitor.write(pcm, frames, rate, channels);
                    if (isCurrent(session)) callback.onPcmDataAvailable(pcm, frames, rate, channels, session);
                } catch (IOException e) { monitorFailure(e); } catch (RuntimeException e) { monitorFailure(e); }
            }
            public void onStatusChanged(AudioSource.AudioSourceState state, String message) {
                if (state == AudioSource.AudioSourceState.COMPLETED && monitor != null && !monitorFailed && isCurrent(session)) {
                    try { monitor.finish(); } catch (IOException e) { monitorFailure(e); } catch (RuntimeException e) { monitorFailure(e); }
                }
                if (state == AudioSource.AudioSourceState.RELEASED) {
                    try { if (monitor != null) monitor.close(); }
                    catch (RuntimeException e) { monitorFailure(e); }
                    finally { sourceReleased.countDown(); }
                }
                if (state == AudioSource.AudioSourceState.COMPLETED || state == AudioSource.AudioSourceState.ERROR
                        || state == AudioSource.AudioSourceState.STOPPED)
                    source.release();
                if (isCurrent(session) && !monitorFailed) callback.onSourceStateChanged(state, message, session);
            }
        });
        preparation.execute(new Runnable() {
            public void run() {
                try {
                    waitForPrevious.await();
                    if (!isCurrent(session)) return;
                    source.prepare();
                    if (isCurrent(session)) source.start();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (IOException e) {
                    if (isCurrent(session) && source.getFailure() == null)
                        callback.onSourceStateChanged(AudioSource.AudioSourceState.ERROR, String.valueOf(e), session);
                    source.release();
                } catch (RuntimeException e) {
                    if (isCurrent(session) && source.getFailure() == null)
                        callback.onSourceStateChanged(AudioSource.AudioSourceState.ERROR, String.valueOf(e), session);
                    source.release();
                } finally {
                    if (!isCurrent(session)) source.release();
                }
            }
        });
    }

    public void stopAndReleaseCurrentSession() {
        ++generation;
        AudioSource previous = currentSource;
        currentSource = null;
        if (currentMonitor != null) currentMonitor.cancel();
        currentMonitor = null;
        if (previous != null) previous.release();
    }

    public void release() {
        if (released) return;
        released = true;
        stopAndReleaseCurrentSession();
        preparation.shutdown();
    }
}
