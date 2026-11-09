BUTTON ICON NOTES - 2010sssTV
=============================
Goal: perfect vertically-centered system icon inside a classic android
Button, height locked to default button, icon free to squish/flip/rotate.

CONSTRAINTS HONORED
- Default classic android 1/2 button style (android.R.drawable.btn_default or plain Button)
- Icon NOT used as button background
- Icon NOT squished/warped by layout (squish only via explicit Bitmap resize)
- Works on API 7-10, Java 1.6

CORE PATTERN (all 3 sets)
-------------------------
1. Build icon as BitmapDrawable at EXACT target size:
     Bitmap bm = processIcon(src, targetW, targetH, flip, rotate);
     BitmapDrawable bd = new BitmapDrawable(res, bm);
     bd.setBounds(0, 0, targetW, targetH);
2. Button b = new Button(ctx);
   b.setGravity(Gravity.CENTER);          // centers drawable+text vertically
   b.setCompoundDrawablesWithIntrinsicBounds(bd, null, null, null);
   b.setCompoundDrawablePadding(0dp);     // no gap between icon and text
   b.setPadding(0, 0, 0, 0);              // no layout padding
   b.setMinHeight(0);
   b.setIncludeFontPadding(true);
   b.setText("");   // icon-only  OR  b.setText("label");  // icon+text
3. Lock height to default button after layout:
     b.setLayoutParams(new LayoutParams(WRAP_CONTENT, defaultBtnHeight));

ICON PROCESSING (squish / flip / rotate)
----------------------------------------
Squish to exact size (no aspect preservation):
    Bitmap.createScaledBitmap(src, tw, th, true);

Flip horizontal (Matrix pre-scale -1,1):
    Matrix m = new Matrix(); m.postScale(-1f, 1f);
    Bitmap.createBitmap(src, 0, 0, w, h, m, true);

Flip vertical:
    Matrix m = new Matrix(); m.postScale(1f, -1f);

Rotate 90:
    Matrix m = new Matrix(); m.postRotate(90);
    Bitmap.createBitmap(src, 0, 0, w, h, m, true);

Trim transparent border first (optional, removes PNG empty space):
    see trimTransparent() below.

SET 1 - ICON-ONLY BUTTON (editor reset / flip / rotate style)
--------------------------------------------------------------
Text: ""  Gravity: CENTER  drawablePadding: 0  pad: 0  fontPad: true
Code:
    Button b = new Button(ctx);
    b.setText("");
    b.setGravity(Gravity.CENTER);
    b.setIncludeFontPadding(true);
    b.setPadding(0, 0, 0, 0);
    b.setMinHeight(0);
    b.setCompoundDrawablesWithIntrinsicBounds(iconBd, null, null, null);
    b.setCompoundDrawablePadding(0);

SET 2 - ICON + TEXT BUTTON (editor add / delete text style)
-----------------------------------------------------------
Text: "label"  Gravity: CENTER  drawablePadding: 4dp  pad: 0  fontPad: true
Code same as Set 1 plus:
    b.setText("label");
    b.setCompoundDrawablePadding(dp(4));

SET 3 - TEXTVIEW + btn_default BACKGROUND (fallback / lightest)
---------------------------------------------------------------
TextView styled as button, Gravity: CENTER, drawablePadding: 0
Code:
    TextView b = new TextView(ctx);
    b.setBackgroundResource(android.R.drawable.btn_default);
    b.setText("label");
    b.setGravity(Gravity.CENTER);
    b.setIncludeFontPadding(true);
    b.setPadding(0, 0, 0, 0);
    b.setCompoundDrawablesWithIntrinsicBounds(iconBd, null, null, null);
    b.setCompoundDrawablePadding(0);

FUNCTIONAL COPY-PASTE CODE
--------------------------
package com.sstv2010;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.Button;

public class IconButton {
    // Build an exact-size icon drawable; squish/flip/rotate applied here.
    public static BitmapDrawable makeIcon(Context ctx, int resId, int tw, int th,
                                          boolean flipH, boolean flipV, float rotate) {
        Drawable src = ctx.getResources().getDrawable(resId);
        int w = src.getIntrinsicWidth();
        int h = src.getIntrinsicHeight();
        Bitmap bm = Bitmap.createBitmap(w, h, Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        src.setBounds(0, 0, w, h);
        src.draw(c);

        // optional: trim transparent border (2px inset)
        int[] t = trim(bm, 2);
        bm = Bitmap.createBitmap(bm, t[0], t[1], t[2]-t[0], t[3]-t[1]);
        w = bm.getWidth(); h = bm.getHeight();

        // flip / rotate via matrix
        Matrix m = new Matrix();
        if (flipH) m.postScale(-1f, 1f, w/2f, h/2f);
        if (flipV) m.postScale(1f, -1f, w/2f, h/2f);
        if (rotate != 0f) m.postRotate(rotate, w/2f, h/2f);
        bm = Bitmap.createBitmap(bm, 0, 0, w, h, m, true);

        // squish to exact target size (no aspect preservation)
        bm = Bitmap.createScaledBitmap(bm, tw, th, true);

        BitmapDrawable bd = new BitmapDrawable(ctx.getResources(), bm);
        bd.setBounds(0, 0, tw, th);
        return bd;
    }

    private static int[] trim(Bitmap bm, int inset) {
        int w = bm.getWidth(), h = bm.getHeight();
        int l=w, t=h, r=0, bo=0;
        int[] px = new int[w*h];
        bm.getPixels(px, 0, w, 0, 0, w, h);
        for (int y=0; y<h; y++)
            for (int x=0; x<w; x++)
                if ((px[y*w+x] >>> 24) != 0) {
                    if (x<l) l=x; if (x>r) r=x; if (y<t) t=y; if (y>bo) bo=y;
                }
        l = Math.max(0, Math.min(w, l+inset));
        t = Math.max(0, Math.min(h, t+inset));
        r = Math.max(0, Math.min(w, r+1-inset));
        bo = Math.max(0, Math.min(h, bo+1-inset));
        if (r<=l) r=l+1; if (bo<=t) bo=t+1;
        return new int[]{l,t,r,bo};
    }

    // Apply to an existing Button (icon-only or icon+text).
    public static void apply(Button b, BitmapDrawable icon, String text, int drawablePadPx) {
        b.setText(text);
        b.setGravity(android.view.Gravity.CENTER);
        b.setIncludeFontPadding(true);
        b.setPadding(0, 0, 0, 0);
        b.setMinHeight(0);
        b.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        b.setCompoundDrawablePadding(drawablePadPx);
    }

    // Lock button height to the default button's measured height (call after layout).
    public static void lockHeight(Button b, Button reference) {
        int h = reference.getMeasuredHeight();
        if (h > 0) {
            android.widget.LinearLayout.LayoutParams lp =
                (android.widget.LinearLayout.LayoutParams) b.getLayoutParams();
            lp.height = h;
            b.setLayoutParams(lp);
        }
    }
}

USAGE IN EDITOR (EditImageActivity.setIcon replacement)
-------------------------------------------------------
    int tw = 48, th = 48;   // target icon size
    BitmapDrawable icon = IconButton.makeIcon(this, R.drawable.reset, tw, th, false, false, 0f);
    Button b = (Button) findViewById(R.id.ed_reset_crop);
    IconButton.apply(b, icon, "", 0);
    b.setLayoutParams(new LinearLayout.LayoutParams(56, defaultBtnHeight));

    // flip example:
    BitmapDrawable flip = IconButton.makeIcon(this, R.drawable.flip, tw, th, true, false, 0f);
    // rotate example:
    BitmapDrawable rot  = IconButton.makeIcon(this, R.drawable.rotate, tw, th, false, false, 90f);

WHY THESE 3 SETS
----------------
- Shortest: single makeIcon() + apply() pair, no XML attributes, no custom views.
- Least CPU: icon processed once into a BitmapDrawable; no per-frame scaling, no shadow accumulator.
- Simple to edit: change tw/th/flip/rotate args; gravity/padding are constants.
- Fully squishable: Bitmap.createScaledBitmap forces exact tw x th.
- Flip/rotate: Matrix applied before final scale; no distortion of button layout.
- Height stable: icon is intrinsic-size drawable; button height comes from layout/lockHeight,
  never from the icon, so squishing the icon never stretches the row.
