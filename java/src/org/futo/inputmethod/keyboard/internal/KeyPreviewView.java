/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.futo.inputmethod.keyboard.internal;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Path;
import android.graphics.CornerPathEffect;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;

import androidx.annotation.Nullable;

import org.futo.inputmethod.keyboard.Key;
import org.futo.inputmethod.latin.R;
import org.futo.inputmethod.latin.uix.DynamicThemeProvider;
import org.futo.inputmethod.v2keyboard.Direction;
import org.futo.inputmethod.v2keyboard.KeyDataKt;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

import kotlin.Pair;

/**
 * The pop up key preview view.
 */
public class KeyPreviewView extends androidx.appcompat.widget.AppCompatTextView {
    public static final int POSITION_MIDDLE = 0;
    public static final int POSITION_LEFT = 1;
    public static final int POSITION_RIGHT = 2;

    private final Rect mBackgroundPadding = new Rect();
    private static final HashSet<String> sNoScaleXTextSet = new HashSet<>();

    private final DynamicThemeProvider mDrawableProvider;

    public KeyPreviewView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyPreviewView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);

        mDrawableProvider = DynamicThemeProvider.obtainFromContext(context);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    private int getDrawableColor(Drawable drawable) {
        if (drawable == null) {
            return 0xFF4A5A6A;
        }
        if (drawable instanceof android.graphics.drawable.ColorDrawable) {
            return ((android.graphics.drawable.ColorDrawable) drawable).getColor();
        }
        try {
            int w = Math.max(10, drawable.getIntrinsicWidth());
            int h = Math.max(10, drawable.getIntrinsicHeight());
            if (w <= 0 || w > 500) w = 50;
            if (h <= 0 || h > 500) h = 50;
            Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, w, h);
            drawable.draw(canvas);
            int color = bitmap.getPixel(w / 2, h / 2);
            bitmap.recycle();
            return color;
        } catch (Exception e) {
            return 0xFF4A5A6A;
        }
    }

    @Nullable
    private Drawable mIcon;

    private Key currKey;
    public void setPreviewVisual(final Key key, final KeyboardIconsSet iconsSet,
                                 final KeyDrawParams drawParams, int foregroundColor) {
        // What we show as preview should match what we show on a key top in onDraw().
        final String iconId = key.getIconId();
        if (!Objects.equals(iconId, KeyboardIconsSet.ICON_UNDEFINED) && key.getFlickDirection() == null) {
            setCompoundDrawables(null, null, null, key.getPreviewIcon(iconsSet));
            mIcon = key.getPreviewIcon(iconsSet);
            currKey = key;
            setText(null);
            return;
        }

        mIcon = null;
        setCompoundDrawables(null, null, null, null);
        setTextColor(foregroundColor);
        setTextSize(TypedValue.COMPLEX_UNIT_PX, key.selectTextSize(drawParams));
        setTypeface(mDrawableProvider.selectKeyTypeface(key.selectPreviewTypeface(drawParams)));
        // TODO Should take care of temporaryShiftLabel here.
        setTextAndScaleX(key.getWidth(), key.getPreviewLabel());

        currKey = key;
    }

    private boolean drawFlickKeys(final Canvas canvas) {
        if(currKey == null) return false;

        Map<Direction, Key> flickKeys = currKey.getFlickKeys();
        if(flickKeys == null || flickKeys.isEmpty()) return false;

        if(currKey.getFlickDirection() != null) return false;

        int width = canvas.getWidth();
        int height = canvas.getHeight();
        int bottomPadding = getPaddingBottom();
        int effectiveHeight = height - bottomPadding;

        int dim = Math.min(width, effectiveHeight);

        Paint paint = new Paint();
        paint.setTypeface(getTypeface());
        paint.setColor(getCurrentTextColor());
        paint.setTextSize(dim * 0.265f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setAntiAlias(true);

        final float yp = 2.7f; // TODO
        final float offsMul = 0.33f;

        int cx = width / 2;
        int cy = (int)(effectiveHeight / 2 + paint.getTextSize() / yp);

        for(Direction dir : flickKeys.keySet()) {
            Key value = flickKeys.get(dir);
            Pair<Double, Double> vec = KeyDataKt.toVector(dir);

            int x = (int)(cx - (vec.getFirst() * width * offsMul));
            int y = (int)(cy - (vec.getSecond() * effectiveHeight * offsMul));
            canvas.drawText(value.getPreviewLabel(), x, y, paint);
        }

        paint.setTextSize(dim * 0.485f);

        if(mIcon != null) {
            /*int iconWidth = mIcon.getIntrinsicWidth();
            if(iconWidth > width) iconWidth = width;
            iconWidth = iconWidth * 8 / 10;

            mIcon.setBounds(
                    cx - iconWidth / 2,
                    height / 2 - iconWidth / 2,
                    cx + iconWidth / 2,
                    height / 2 + iconWidth / 2
            );
            mIcon.draw(canvas);*/
        } else {
            canvas.drawText(
                    currKey.getPreviewLabel(),
                    cx,
                    (int) (effectiveHeight / 2 + paint.getTextSize() / yp),
                    paint
            );
        }

        return true;
    }


    private Drawable mBackground = null;
    @Override
    public void setBackground(Drawable background) {
        mBackground = background;
        background.getPadding(mBackgroundPadding);
    }

    private Paint mPaint = null;
    private Path mPath = null;

    @Override
    protected void onDraw(final Canvas canvas) {
        if (mBackground == null) {
            super.onDraw(canvas);
            return;
        }

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            super.onDraw(canvas);
            return;
        }

        float density = getResources().getDisplayMetrics().density;
        int backgroundColor = getDrawableColor(mBackground);

        if (mPaint == null) {
            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        }
        mPaint.reset();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(backgroundColor);

        int bottomPadding = getPaddingBottom();

        float shadowPadding = 4f * density;
        float cornerRadius = 8f * density;
        float arrowHeight = 10f * density;

        float left = shadowPadding;
        float top = shadowPadding;
        float right = width - shadowPadding;

        // Vẽ bóng đổ bằng setShadowLayer
        mPaint.setShadowLayer(3f * density, 0, 1.5f * density, 0x26000000);

        if (bottomPadding > arrowHeight + shadowPadding) {
            float rectBottom = height - bottomPadding;
            mPath = new Path();
            mPath.moveTo(left, top);
            mPath.lineTo(right, top);
            mPath.lineTo(right, rectBottom);
            mPath.lineTo(width / 2f, rectBottom + arrowHeight);
            mPath.lineTo(left, rectBottom);
            mPath.close();

            mPaint.setPathEffect(new CornerPathEffect(cornerRadius));
            canvas.drawPath(mPath, mPaint);
        } else {
            mPaint.setPathEffect(new CornerPathEffect(cornerRadius));
            mPath = new Path();
            mPath.moveTo(left, top);
            mPath.lineTo(right, top);
            mPath.lineTo(right, height - shadowPadding);
            mPath.lineTo(left, height - shadowPadding);
            mPath.close();
            canvas.drawPath(mPath, mPaint);
        }

        if(!drawFlickKeys(canvas)) {
            super.onDraw(canvas);
        }
    }

    private void setTextAndScaleX(int maxWidth, final String text) {
        setTextScaleX(1.0f);
        setText(text);
        if (sNoScaleXTextSet.contains(text)) {
            return;
        }

        final float width = getTextWidth(text, getPaint());
        if (width <= maxWidth) {
            sNoScaleXTextSet.add(text);
            return;
        }
        setTextScaleX(maxWidth / width);
    }

    public static void clearTextCache() {
        sNoScaleXTextSet.clear();
    }

    private static float getTextWidth(final String text, final TextPaint paint) {
        if (TextUtils.isEmpty(text)) {
            return 0.0f;
        }
        final int len = text.length();
        final float[] widths = new float[len];
        final int count = paint.getTextWidths(text, 0, len, widths);
        float width = 0;
        for (int i = 0; i < count; i++) {
            width += widths[i];
        }
        return width;
    }

    // Background state set
    private static final int[][][] KEY_PREVIEW_BACKGROUND_STATE_TABLE = {
        { // POSITION_MIDDLE
            {},
            { R.attr.state_has_morekeys }
        },
        { // POSITION_LEFT
            { R.attr.state_left_edge },
            { R.attr.state_left_edge, R.attr.state_has_morekeys }
        },
        { // POSITION_RIGHT
            { R.attr.state_right_edge },
            { R.attr.state_right_edge, R.attr.state_has_morekeys }
        }
    };
    private static final int STATE_NORMAL = 0;
    private static final int STATE_HAS_MOREKEYS = 1;

    public void setPreviewBackground(final boolean hasMoreKeys, final int position) {
        //final Drawable background = getBackground();
        //if (background == null) {
        //    return;
        //}
        //final int hasMoreKeysState = hasMoreKeys ? STATE_HAS_MOREKEYS : STATE_NORMAL;
        //background.setState(KEY_PREVIEW_BACKGROUND_STATE_TABLE[position][hasMoreKeysState]);
    }
}
