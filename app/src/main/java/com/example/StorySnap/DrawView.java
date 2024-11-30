package com.example.assignment4;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class DrawView extends View {
    Bitmap bmp;
    public Bitmap getBitmap()
    {
        bmp = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        c.drawColor(Color.WHITE);
        Paint p = new Paint();
        p.setColor(Color.BLACK);
        p.setStyle(Paint.Style.STROKE);
        p.setAntiAlias(true);
        p.setStrokeWidth(5f);
        c.drawPath(path, p); //path is global. The very same thing that onDraw uses.
        return bmp;
    }

    Path path = new Path();
    public DrawView(Context context) {
        super(context);
    }

    public DrawView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DrawView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DrawView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);
        Paint p = new Paint();
        p.setColor(Color.parseColor("#e305ad"));
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(10f);
        canvas.drawRect(0, 0, getWidth(), getHeight(), p);
        p.setColor(Color.BLACK);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(5f);
/*
Note: Declare path somewhere outside onDraw
Path path = new Path();
*/

        canvas.drawPath(path, p);
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX(), y = event.getY();
        int action = event.getAction();
        if(action == MotionEvent.ACTION_DOWN){
            path.moveTo(x, y); //path is global. Same thing that onDraw uses.
        }
        else if(action == MotionEvent.ACTION_MOVE){
            path.lineTo(x, y);
        }
        return true;
    }


    public void clear() {
        path.reset();
    }
}


