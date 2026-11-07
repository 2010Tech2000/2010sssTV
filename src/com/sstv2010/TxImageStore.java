package om.sstvandroid2;

import android.graphics.Bitmap;
import java.util.ArrayList;

public final class TxImageStore {
    // Rendered bitmap for TX/preview (mode-ready or fit-rendered)
    private static Bitmap sRendered;
    // Editable editor state (kept in RAM across Done -> Edit)
    private static Bitmap sEditOriginal; // true unmodified source for Reset
    private static Bitmap sEditBase;
    private static ArrayList sEditItems; // ArrayList<EditorCanvasView.TextItem>
    private static boolean sEditStretch;
    private static boolean sHasEdit;

    private TxImageStore() {}

    public static synchronized void put(Bitmap b) { sRendered = b; }
    public static synchronized Bitmap get() { return sRendered; }

    public static synchronized void putEdit(Bitmap original, Bitmap base, ArrayList items, boolean stretch) {
        sEditOriginal = original;
        sEditBase = base;
        sEditItems = items;
        sEditStretch = stretch;
        sHasEdit = (base != null || original != null);
    }
    public static synchronized Bitmap getEditOriginal() { return sEditOriginal; }
    public static synchronized Bitmap getEditBase() { return sEditBase; }
    public static synchronized ArrayList getEditItems() { return sEditItems; }
    public static synchronized boolean getEditStretch() { return sEditStretch; }
    public static synchronized boolean hasEdit() { return sHasEdit && (sEditBase != null || sEditOriginal != null); }

    public static synchronized void clear() { sRendered = null; clearEdit(); }
    public static synchronized void clearEdit() { sEditOriginal = null; sEditBase = null; sEditItems = null; sEditStretch = false; sHasEdit = false; }
}