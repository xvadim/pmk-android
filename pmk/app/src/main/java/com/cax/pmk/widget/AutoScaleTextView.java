package com.cax.pmk.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.widget.TextView;
import com.cax.pmk.R;

/**
 * A custom Text View that lowers the text size when the text is to big for the TextView. 
 * Modified version of one found on stackoverflow
 * 
 * @author Andreas Krings - www.ankri.de
 * @version 1.0
 * 
 */
public class AutoScaleTextView extends TextView
{
    private Paint textPaint;

    private float preferredTextSize;
    private float minTextSize;

    private int mMaxWidth = 0;

    public AutoScaleTextView(Context context)
    {
            this(context, null);
    }

    public AutoScaleTextView(Context context, AttributeSet attrs)
    {
            this(context, attrs, R.attr.autoScaleTextViewStyle);

            // Use this constructor, if you do not want use the default style
            // super(context, attrs);
    }

    public AutoScaleTextView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);

        this.textPaint = new Paint();

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AutoScaleTextView, defStyle, 0);
        this.minTextSize = a.getDimension(R.styleable.AutoScaleTextView_minTextSize, 10f);
        a.recycle();

        this.preferredTextSize = this.getTextSize();
    }

    /**
     * Set the minimum text size for this view
     *
     * @param minTextSize
     *            The minimum text size
     */
    public void setMinTextSize(float minTextSize)
    {
        this.minTextSize = minTextSize;
    }

    public void setMaxWidth(int pMaxWidth) {
        mMaxWidth = pMaxWidth;
    }

    /**
     * Resize the text so that it fits
     *
     * @param text
     *            The text. Neither <code>null</code> nor empty.
     * @param textWidth
     *            The width of the TextView. > 0
     */
    private void refitText(String text, int textWidth)
    {
        if (textWidth <= 0 || text == null || text.length() == 0)
            return;

        // the width
        int targetWidth = textWidth - this.getPaddingLeft() - this.getPaddingRight();
        if (mMaxWidth > 0 && mMaxWidth < targetWidth) {
            targetWidth = mMaxWidth;
        }

        final float threshold = 0.5f; // How close we have to be

        this.textPaint.set(this.getPaint());

        if (this.textPaint.measureText(text) >= targetWidth) {
            this.minTextSize = 0; // too big - recalculate the min
        }

        if (targetWidth - this.textPaint.measureText(text) > 50) {
            this.preferredTextSize *= 2; // too small
        }
        while ((this.preferredTextSize - this.minTextSize) > threshold)
        {
            float size = (this.preferredTextSize + this.minTextSize) / 2;
            this.textPaint.setTextSize(size);
            if (this.textPaint.measureText(text) >= targetWidth)
                this.preferredTextSize = size; // too big
            else
                this.minTextSize = size; // too small
        }
        // Use min size so that we undershoot rather than overshoot
        this.setTextSize(TypedValue.COMPLEX_UNIT_PX, this.minTextSize);
    }

    public void refitNow()
    {
        this.minTextSize *= 2;
        this.textPaint.setTextSize(minTextSize);
        this.refitText(this.getText().toString(), this.getWidth());
    }
    @Override
    protected void onTextChanged(final CharSequence text, final int start, final int before, final int after)
    {
        this.refitText(text.toString(), this.getWidth());
    }

    @Override
    protected void onSizeChanged(final int width, int height, int oldwidth, int oldheight)
    {
        if (width != oldwidth) {
            new Handler().postDelayed(new Runnable() {public void run() { refitText(getText().toString(), width); } }, 1);
        }
    }

}