package com.cax.pmk.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

// a highly modified version of AbsSeekBar.java
public class Slider extends SeekBar {

	private Drawable mThumb;
    private OnSeekBarChangeListener mOnSeekBarChangeListener;

	// setThumbOffset prevents thumb overlapping with progress left/right border
	public Slider(Context context) { super(context); setThumbOffset(0);} 
	public Slider(Context context, AttributeSet attrs) { super(context, attrs); setThumbOffset(0);}

	@Override
	public void setThumb(Drawable thumb) {
	    super.setThumb(thumb);
	    mThumb = thumb;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
	    if (event.getAction() == MotionEvent.ACTION_DOWN) {

	    	// disallow touches outside thumb
	        if (	   event.getX() >= mThumb.getBounds().left
	                && event.getX() <= mThumb.getBounds().right
	                && event.getY() <= mThumb.getBounds().bottom
	                && event.getY() >= mThumb.getBounds().top) {

	            //super.onTouchEvent(event);
	        	// setPressed instead of passing event: don't initiate moving on just touching thumb !
	        	setPressed(true);
	        }
	        else {
	        	return false;
	        }
	    } else if (event.getAction() ==  MotionEvent.ACTION_MOVE) {
           setPressed(true);
           trackTouchEvent(event);
	    } else if (event.getAction() == MotionEvent.ACTION_UP) {
	        return false;
	    } else {
	        super.onTouchEvent(event);
	    }
	
	    return true;
	}
	
   private void trackTouchEvent(MotionEvent event) {
        final int width = getWidth();
        final int available = width - getPaddingLeft() - getPaddingRight();
        int x = (int)event.getX();
        float scale;
        float progress = 0;

        if (x < getPaddingLeft()) {
            scale = 0.0f;
        } else if (x > width - getPaddingRight()) {
            scale = 1.0f;
        }
        else {
            scale = (float)(x - getPaddingLeft()) / (float)available;
            progress = 0; // was mTouchProgressOffset;
        }

        final int max = getMax();
        //System.out.println("progressBefore=" + progress + ", scale="+scale + ", total=" + (progress + scale * max + 0.5));
        // Spread thresholds more evenly - e.g. for (0,1,2) switch convert scale thresholds from 0.25/0.75 to e.g. 0.05/0.95
        scale = (float) ((float)((scale - 0.5) / 0.25 * 0.45) + 0.5); 
        progress += scale * max + 0.5;
        
        int prevProgress = getProgress();
        setProgress((int) progress);
        if (prevProgress != getProgress()) {
        	mOnSeekBarChangeListener.onProgressChanged(this, getProgress(), true);
        }
    }

   public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
       mOnSeekBarChangeListener = l;
   }

}