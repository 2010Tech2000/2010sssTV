package om.sstvandroid2;

import android.content.Context;
import android.net.Uri;
import java.io.IOException;
import java.io.InputStream;

/** API 7-10: WAV-only RX file input. MP3/FLAC/MediaCodec/MediaExtractor unavailable on API 10. */
final class AudioFileReaders {
    enum Type { WAV }
    private AudioFileReaders() {}
    static Type detect(Context context, Uri uri) throws IOException {
        InputStream input = context.getContentResolver().openInputStream(uri);
        if (input == null) throw new IOException("Cannot open audio file");
        try {
            byte[] header = new byte[12];
            int off = 0;
            while (off < header.length) { int n = input.read(header, off, header.length - off); if (n < 0) break; off += n; }
            if (off >= 12 && header[0]=='R' && header[1]=='I' && header[2]=='F' && header[3]=='F'
                && header[8]=='W' && header[9]=='A' && header[10]=='V' && header[11]=='E') return Type.WAV;
        } finally { try{ input.close(); } catch(IOException ignore){} }
        throw new IOException("Unsupported audio file. Choose a PCM/IEEE-float WAV file (MP3/FLAC not supported on Android 2.x).");
    }
    static PcmFileReader open(Context context, Uri uri, Type type) throws IOException {
        if (type == Type.WAV) return WavFileReader.open(context.getContentResolver().openInputStream(uri));
        throw new IOException("Audio file type has not been identified");
    }
    static void checkInterrupted() throws java.io.InterruptedIOException {
        if (Thread.currentThread().isInterrupted()) throw new java.io.InterruptedIOException("Interrupted");
    }
}
