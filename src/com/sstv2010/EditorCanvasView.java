package om.sstvandroid2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public class EditorCanvasView extends View {
    public static final int MODE_TEXT = 0;
    public static final int MODE_CROP = 1;
    public interface OnSelectionChanged { void onSelectionChanged(TextItem item); }
    private OnSelectionChanged mSelCb;

    public static class TextItem {
        public String text = "Text";
        public float x = 40;
        public float y = 60;
        public int color = Color.WHITE;
        public float sizePx = 36f;
        public float strokePx = 2f;
        public Typeface face = Typeface.DEFAULT;
        public TextItem copy(){ TextItem n=new TextItem(); n.text=text; n.x=x; n.y=y; n.color=color; n.sizePx=sizePx; n.strokePx=strokePx; n.face=face; return n; }
    }

    private Bitmap mBase;
    private final ArrayList mItems = new ArrayList();
    private int mMode = MODE_TEXT;
    private int mActive = -1;
    private float mLastX, mLastY;
    private final Matrix mDraw = new Matrix();
    private final Matrix mInv = new Matrix();
    private final RectF mImgRect = new RectF();
    private final RectF mCrop = new RectF();
    private final Paint mBmpPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint mTextFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTextStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mCropPaint = new Paint();
    private final Paint mDimPaint = new Paint();
    private int mCropHandle = -1;
    private float mPinchStartDist;
    private RectF mCropStartRect = new RectF();
    private PointF mPinchMid = new PointF();

    public EditorCanvasView(Context c) { super(c); init(); }
    public EditorCanvasView(Context c, AttributeSet a) { super(c, a); init(); }
    private void init() {
        mCropPaint.setColor(Color.GREEN);
        mCropPaint.setStyle(Paint.Style.STROKE);
        mCropPaint.setStrokeWidth(2f);
        mDimPaint.setColor(0x88000000);
        mTextStroke.setStyle(Paint.Style.STROKE);
    }

    public void setOnSelectionChanged(OnSelectionChanged cb){ mSelCb = cb; }
    private void notifySel(){ if (mSelCb!=null) try { mSelCb.onSelectionChanged(getActiveItem()); } catch(Throwable ignore){} }
    public void setBase(Bitmap b) { mBase = b; mActive = -1; if (b != null) mCrop.set(0,0,b.getWidth(),b.getHeight()); computeMatrix(); invalidate(); notifySel(); }
    public Bitmap getBase() { return mBase; }
    private int mTargetW, mTargetH;
    private boolean mStretchPreview;
    private final RectF mFrame = new RectF(); // mode-aspect preview frame (view coords)

    private Bitmap mSnapBase;
    private ArrayList mSnapItems = new ArrayList();

    public void saveSnapshot(){
        mSnapBase = (mBase != null) ? copyBmp(mBase) : null;
        mSnapItems = copyItems();
    }
    public boolean restoreSnapshot(){
        if (mSnapBase == null) return false;
        mBase = copyBmp(mSnapBase);
        mItems.clear();
        for (int i=0;i<mSnapItems.size();i++) mItems.add(((TextItem)mSnapItems.get(i)).copy());
        mActive = -1;
        mCrop.set(0,0,mBase.getWidth(),mBase.getHeight());
        computeMatrix(); invalidate(); notifySel();
        return true;
    }
    public void setBaseKeepText(Bitmap b){
        if (b == null) return;
        // Restore text positions/styles from crop-entry snapshot if available (undoes apply/rotate coord shifts)
        if (mSnapBase != null && mSnapItems != null) {
            mItems.clear();
            for (int i=0;i<mSnapItems.size();i++) mItems.add(((TextItem)mSnapItems.get(i)).copy());
            mActive = -1;
            notifySel();
        }
        mBase = copyBmp(b);
        mCrop.set(0,0,mBase.getWidth(),mBase.getHeight());
        computeMatrix(); invalidate();
    }
    private Bitmap copyBmp(Bitmap b){ try { return b.copy(b.getConfig()!=null?b.getConfig():Bitmap.Config.ARGB_8888, true);} catch(Throwable t){ return Bitmap.createBitmap(b); } }
    public ArrayList copyItems(){ ArrayList a=new ArrayList(); for(int i=0;i<mItems.size();i++) a.add(((TextItem)mItems.get(i)).copy()); return a; }
    public void setItems(ArrayList items){
        mItems.clear(); mActive = -1;
        if (items != null) for (int i=0;i<items.size();i++) mItems.add(((TextItem)items.get(i)).copy());
        invalidate(); notifySel();
    }
    public Bitmap copyBase(){ return mBase != null ? copyBmp(mBase) : null; }
    public boolean removeActiveItem(){
        if (mActive>=0 && mActive<mItems.size()){ mItems.remove(mActive); mActive=-1; invalidate(); notifySel(); return true; }
        return false;
    }
    public void resetCropRect(){
        if (mBase==null) return;
        mCropHandle = -1;
        mCrop.set(0,0,mBase.getWidth(),mBase.getHeight());
        invalidate();
    }
    public void setPreviewMode(int targetW, int targetH, boolean stretch){ mTargetW=targetW; mTargetH=targetH; mStretchPreview=stretch; computeMatrix(); invalidate(); }
    public void setMode(int mode) { mMode = mode; mCropHandle = -1; invalidate(); }
    public int getMode() { return mMode; }
    public ArrayList getItems() { return mItems; }
    public int addTextItem(TextItem it) { mItems.add(it); mActive = mItems.size()-1; invalidate(); notifySel(); return mActive; }
    public void setActive(int idx){ mActive = (idx>=0 && idx<mItems.size())?idx:-1; invalidate(); notifySel(); }
    public TextItem getActiveItem() { return (mActive >= 0 && mActive < mItems.size()) ? (TextItem)mItems.get(mActive) : null; }
    public void clearActive() { mActive = -1; invalidate(); notifySel(); }

    protected void onSizeChanged(int w, int h, int ow, int oh) { computeMatrix(); }

    private void computeMatrix() {
        mDraw.reset(); mImgRect.setEmpty(); mFrame.setEmpty();
        if (mBase == null || getWidth() <= 0 || getHeight() <= 0) return;
        float bw = mBase.getWidth(), bh = mBase.getHeight();
        float vw = getWidth(), vh = getHeight();
        // Mode-aspect frame in the view so preview matches TX output framing.
        float aspect = (mTargetW>0 && mTargetH>0) ? (((float)mTargetW)/mTargetH) : (bw/bh);
        float rw = vw, rh = vw / aspect;
        if (rh > vh) { rh = vh; rw = vh * aspect; }
        float rdx = (vw - rw)/2f, rdy = (vh - rh)/2f;
        mFrame.set(rdx, rdy, rdx+rw, rdy+rh);
        if (mStretchPreview) {
            mDraw.setRectToRect(new RectF(0,0,bw,bh), mFrame, Matrix.ScaleToFit.FILL);
            mImgRect.set(mFrame);
        } else {
            mDraw.setRectToRect(new RectF(0,0,bw,bh), mFrame, Matrix.ScaleToFit.CENTER);
            mImgRect.set(0,0,bw,bh);
            mDraw.mapRect(mImgRect); // actual image bounds in view coords
        }
        mDraw.invert(mInv);
        if (mCrop.isEmpty()) mCrop.set(0,0,bw,bh);
    }

    private float[] toImage(float sx, float sy) { float[] p = new float[]{sx,sy}; mInv.mapPoints(p); return p; }

    protected void onDraw(Canvas c) {
        super.onDraw(c);
        c.drawColor(0xFF000000);
        if (mBase == null) return;
        c.drawBitmap(mBase, mDraw, mBmpPaint);
        for (int i=0;i<mItems.size();i++) drawItem(c, (TextItem)mItems.get(i), i==mActive);
        if (mMode == MODE_CROP) drawCrop(c);
    }

    private void drawItem(Canvas c, TextItem it, boolean active) {
        mTextFill.setColor(it.color);
        mTextFill.setTextSize(it.sizePx);
        mTextFill.setTypeface(it.face != null ? it.face : Typeface.DEFAULT);
        float[] p = new float[]{it.x,it.y}; mDraw.mapPoints(p);
        float s = mDraw.mapRadius(1f);
        mTextFill.setTextSize(it.sizePx * s);
        if (it.strokePx > 0) {
            mTextStroke.setColor((it.color == Color.BLACK) ? Color.WHITE : Color.BLACK);
            mTextStroke.setTextSize(mTextFill.getTextSize());
            mTextStroke.setTypeface(mTextFill.getTypeface());
            mTextStroke.setStrokeWidth(Math.max(1f, it.strokePx * s));
            c.drawText(it.text, p[0], p[1], mTextStroke);
        }
        c.drawText(it.text, p[0], p[1], mTextFill);
        if (active) {
            Paint box = new Paint(); box.setColor(Color.YELLOW); box.setStyle(Paint.Style.STROKE); box.setStrokeWidth(1f);
            float w = mTextFill.measureText(it.text); Paint.FontMetrics fm = mTextFill.getFontMetrics();
            c.drawRect(p[0]-2, p[1]+fm.top-2, p[0]+w+2, p[1]+fm.bottom+2, box);
        }
    }

    private void drawCrop(Canvas c) {
        RectF r = new RectF(mCrop); mDraw.mapRect(r);
        Path p = new Path(); p.addRect(mImgRect, Path.Direction.CW); p.addRect(r, Path.Direction.CCW);
        c.drawPath(p, mDimPaint);
        c.drawRect(r, mCropPaint);
        drawHandle(c, r.left, r.top); drawHandle(c, r.right, r.top); drawHandle(c, r.left, r.bottom); drawHandle(c, r.right, r.bottom);
    }
    private void drawHandle(Canvas c, float x, float y) { Paint h=new Paint(); h.setColor(Color.GREEN); c.drawRect(x-8,y-8,x+8,y+8,h); }

    public boolean onTouchEvent(MotionEvent ev) {
        if (mBase == null) return false;
        int action = ev.getAction() & MotionEvent.ACTION_MASK;
        if (mMode == MODE_CROP) return touchCrop(ev, action);
        return touchText(ev, action);
    }

    private boolean touchText(MotionEvent ev, int action) {
        float[] ip = toImage(ev.getX(), ev.getY());
        if (action == MotionEvent.ACTION_DOWN) {
            int prev = mActive;
            mActive = findTextAt(ip[0], ip[1]);
            mLastX = ip[0]; mLastY = ip[1];
            invalidate();
            if (mActive != prev) notifySel();
            return mActive >= 0;
        } else if (action == MotionEvent.ACTION_MOVE && mActive >= 0) {
            TextItem it = (TextItem)mItems.get(mActive);
            it.x += (ip[0]-mLastX); it.y += (ip[1]-mLastY);
            mLastX = ip[0]; mLastY = ip[1]; invalidate(); return true;
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            return mActive >= 0;
        }
        return super.onTouchEvent(ev);
    }

    private int findTextAt(float ix, float iy) {
        for (int i=mItems.size()-1;i>=0;i--) {
            TextItem it = (TextItem)mItems.get(i);
            Paint p = new Paint(); p.setTextSize(it.sizePx); p.setTypeface(it.face!=null?it.face:Typeface.DEFAULT);
            float w = p.measureText(it.text); Paint.FontMetrics fm = p.getFontMetrics();
            RectF b = new RectF(it.x-8, it.y+fm.top-8, it.x+w+8, it.y+fm.bottom+8);
            if (b.contains(ix,iy)) return i;
        }
        return -1;
    }

    private boolean touchCrop(MotionEvent ev, int action) {
        if (action == MotionEvent.ACTION_DOWN) {
            RectF r = new RectF(mCrop); mDraw.mapRect(r);
            mCropHandle = pickHandle(ev.getX(), ev.getY(), r);
            float[] ip = toImage(ev.getX(), ev.getY()); mLastX=ip[0]; mLastY=ip[1];
            mCropStartRect.set(mCrop);
            return true;
        } else if (action == MotionEvent.ACTION_POINTER_DOWN && ev.getPointerCount()>=2) {
            mCropHandle = 9;
            mPinchStartDist = dist(ev);
            mid(ev, mPinchMid);
            float[] ip = toImage(mPinchMid.x, mPinchMid.y); mPinchMid.set(ip[0], ip[1]);
            mCropStartRect.set(mCrop);
            return true;
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (mCropHandle == 9 && ev.getPointerCount()>=2) {
                float d = dist(ev);
                if (mPinchStartDist > 5f) {
                    float scale = d / mPinchStartDist;
                    scaleCrop(scale, mPinchMid.x, mPinchMid.y);
                    mPinchStartDist = d;
                }
                invalidate(); return true;
            }
            if (mCropHandle >= 0) {
                float[] ip = toImage(ev.getX(), ev.getY());
                dragCrop(mCropHandle, ip[0]-mLastX, ip[1]-mLastY);
                mLastX=ip[0]; mLastY=ip[1]; invalidate(); return true;
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
            mCropHandle = -1;
        }
        return true;
    }

    private int pickHandle(float x, float y, RectF r) {
        float tol = 28f;
        if (near(x,y,r.left,r.top,tol)) return 0;
        if (near(x,y,r.right,r.top,tol)) return 1;
        if (near(x,y,r.left,r.bottom,tol)) return 2;
        if (near(x,y,r.right,r.bottom,tol)) return 3;
        if (r.contains(x,y)) return 4;
        return -1;
    }
    private boolean near(float x,float y,float hx,float hy,float t){ return Math.abs(x-hx)<t && Math.abs(y-hy)<t; }
    private void dragCrop(int h, float dx, float dy) {
        float min = 20f, bw=mBase.getWidth(), bh=mBase.getHeight();
        if (h==0){ mCrop.left+=dx; mCrop.top+=dy; }
        else if (h==1){ mCrop.right+=dx; mCrop.top+=dy; }
        else if (h==2){ mCrop.left+=dx; mCrop.bottom+=dy; }
        else if (h==3){ mCrop.right+=dx; mCrop.bottom+=dy; }
        else if (h==4){ mCrop.offset(dx,dy); }
        if (mCrop.width()<min) { if (h==0||h==2) mCrop.left=mCrop.right-min; else mCrop.right=mCrop.left+min; }
        if (mCrop.height()<min){ if (h==0||h==1) mCrop.top=mCrop.bottom-min; else mCrop.bottom=mCrop.top+min; }
        if (h==4) {
            if (mCrop.left<0){ mCrop.right-=mCrop.left; mCrop.left=0; }
            if (mCrop.top<0){ mCrop.bottom-=mCrop.top; mCrop.top=0; }
            if (mCrop.right>bw){ mCrop.left-= (mCrop.right-bw); mCrop.right=bw; }
            if (mCrop.bottom>bh){ mCrop.top-= (mCrop.bottom-bh); mCrop.bottom=bh; }
        }
        mCrop.left=Math.max(0,Math.min(mCrop.left,bw-min)); mCrop.top=Math.max(0,Math.min(mCrop.top,bh-min));
        mCrop.right=Math.max(mCrop.left+min,Math.min(mCrop.right,bw)); mCrop.bottom=Math.max(mCrop.top+min,Math.min(mCrop.bottom,bh));
    }
    private void scaleCrop(float s, float cx, float cy) {
        RectF n = new RectF(mCrop);
        n.left = cx + (n.left-cx)*s; n.right = cx + (n.right-cx)*s;
        n.top = cy + (n.top-cy)*s; n.bottom = cy + (n.bottom-cy)*s;
        float bw=mBase.getWidth(), bh=mBase.getHeight();
        if (n.width()<20||n.height()<20||n.width()>bw*2||n.height()>bh*2) return;
        mCrop.set(Math.max(0,n.left), Math.max(0,n.top), Math.min(bw,n.right), Math.min(bh,n.bottom));
    }
    private float dist(MotionEvent ev){ float dx=ev.getX(0)-ev.getX(1), dy=ev.getY(0)-ev.getY(1); return (float)Math.sqrt(dx*dx+dy*dy); }
    private void mid(MotionEvent ev, PointF out){ out.set((ev.getX(0)+ev.getX(1))/2f,(ev.getY(0)+ev.getY(1))/2f); }

    public Bitmap applyCrop() {
        if (mBase==null) return null;
        RectF r = new RectF(mCrop);
        int x=Math.max(0,Math.round(r.left)), y=Math.max(0,Math.round(r.top));
        int w=Math.min(mBase.getWidth()-x, Math.round(r.width()));
        int h=Math.min(mBase.getHeight()-y, Math.round(r.height()));
        if (w<=0||h<=0) return mBase;
        try {
            Bitmap out = Bitmap.createBitmap(mBase, x, y, w, h);
            for (int i=0;i<mItems.size();i++){ TextItem it=(TextItem)mItems.get(i); it.x -= x; it.y -= y; }
            mBase = out; mCrop.set(0,0,out.getWidth(),out.getHeight()); computeMatrix(); invalidate(); return mBase;
        } catch (Throwable t) { // OOM on large crops
            return mBase;
        }
    }

    public Bitmap rotate90() { return transform(new Matrix(){{ postRotate(90); }}); }
    public Bitmap flipH() { Matrix m=new Matrix(); m.setScale(-1,1,mBase!=null?mBase.getWidth()/2f:0,0); return transform(m); }
    public Bitmap flipV() { Matrix m=new Matrix(); m.setScale(1,-1,0,mBase!=null?mBase.getHeight()/2f:0); return transform(m); }
    private Bitmap transform(Matrix op) {
        if (mBase==null) return null;
        try {
            Bitmap out = Bitmap.createBitmap(mBase,0,0,mBase.getWidth(),mBase.getHeight(),op,true);
            float nw=out.getWidth(), nh=out.getHeight();
            for (int i=0;i<mItems.size();i++){
                TextItem it=(TextItem)mItems.get(i);
                float[] pt = new float[]{it.x,it.y};
                op.mapPoints(pt);
                if (pt[0]<0) pt[0]+=nw; if (pt[1]<0) pt[1]+=nh;
                if (pt[0]>nw) pt[0]-=nw; if (pt[1]>nh) pt[1]-=nh;
                it.x=pt[0]; it.y=pt[1];
            }
            mBase = out; mCrop.set(0,0,out.getWidth(),out.getHeight()); computeMatrix(); invalidate(); return mBase;
        } catch (Throwable t){ return mBase; }
    }

    public Bitmap flatten(int targetW, int targetH, boolean stretch) {
        if (mBase == null) return null;
        Bitmap src = mBase;
        int tw = targetW > 0 ? targetW : src.getWidth();
        int th = targetH > 0 ? targetH : src.getHeight();
        if (stretch) {
            // Stretch to exact mode resolution (distorts aspect), scale text to match.
            float sx = ((float)tw)/src.getWidth(), sy=((float)th)/src.getHeight();
            ArrayList scaled = new ArrayList();
            for (int i=0;i<mItems.size();i++){ TextItem o=(TextItem)mItems.get(i); TextItem n=new TextItem(); n.text=o.text; n.color=o.color; n.sizePx=o.sizePx*sy; n.strokePx=o.strokePx*sy; n.face=o.face; n.x=o.x*sx; n.y=o.y*sy; scaled.add(n); }
            Bitmap out = Bitmap.createBitmap(tw,th,Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(out);
            c.drawBitmap(src, new android.graphics.Rect(0,0,src.getWidth(),src.getHeight()), new RectF(0,0,tw,th), new Paint(Paint.FILTER_BITMAP_FLAG));
            drawItemsOn(c, scaled, 1f);
            return out;
        } else {
            // Fit (preserve aspect) into mode resolution, centered on black; no distortion.
            if (src.getWidth() != tw || src.getHeight() != th) {
                float scale = Math.min(((float)tw)/src.getWidth(), ((float)th)/src.getHeight());
                int dw = Math.max(1, Math.round(src.getWidth() * scale));
                int dh = Math.max(1, Math.round(src.getHeight() * scale));
                int dx = (tw - dw) / 2;
                int dy = (th - dh) / 2;
                ArrayList placed = new ArrayList();
                for (int i=0;i<mItems.size();i++){ TextItem o=(TextItem)mItems.get(i); TextItem n=new TextItem(); n.text=o.text; n.color=o.color; n.sizePx=o.sizePx*scale; n.strokePx=o.strokePx*scale; n.face=o.face; n.x=o.x*scale+dx; n.y=o.y*scale+dy; placed.add(n); }
                Bitmap out = Bitmap.createBitmap(tw,th,Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(out);
                c.drawColor(0xFF000000);
                c.drawBitmap(src, new android.graphics.Rect(0,0,src.getWidth(),src.getHeight()), new RectF(dx,dy,dx+dw,dy+dh), new Paint(Paint.FILTER_BITMAP_FLAG));
                drawItemsOn(c, placed, 1f);
                return out;
            }
        }
        Bitmap out = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        c.drawBitmap(src, 0, 0, new Paint(Paint.FILTER_BITMAP_FLAG));
        drawItemsOn(c, mItems, 1f);
        return out;
    }

    private void drawItemsOn(Canvas c, ArrayList items, float s) {
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG); Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG); stroke.setStyle(Paint.Style.STROKE);
        for (int i=0;i<items.size();i++) {
            TextItem it=(TextItem)items.get(i);
            fill.setColor(it.color); fill.setTextSize(it.sizePx*s); fill.setTypeface(it.face!=null?it.face:Typeface.DEFAULT);
            if (it.strokePx>0){ stroke.setColor(it.color==Color.BLACK?Color.WHITE:Color.BLACK); stroke.setTextSize(fill.getTextSize()); stroke.setTypeface(fill.getTypeface()); stroke.setStrokeWidth(Math.max(1f,it.strokePx*s)); c.drawText(it.text,it.x*s,it.y*s,stroke); }
            c.drawText(it.text,it.x*s,it.y*s,fill);
        }
    }

    public RectF getCropRect() { return new RectF(mCrop); }
}