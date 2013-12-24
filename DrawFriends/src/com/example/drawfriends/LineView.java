package com.example.drawfriends;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class LineView extends SurfaceView implements SurfaceHolder.Callback{
	
	private Paint paint;
	private LineViewThread canvasthread;
	private boolean isRunning = false;
	
	public LineView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	public LineView(Context context, AttributeSet attrs){
		super(context, attrs);
		init();
	}
	
	public LineView(Context context){
		super(context);
		init();
	}
	
	private void init() {
		getHolder().addCallback(this);
		
		paint = new Paint();
		paint.setColor(Color.BLACK);
		paint.setStrokeWidth(5);
		
		canvasthread = new LineViewThread(getHolder(), this);
		setFocusable(true);
	}
	
	public void setSize(int size){
		paint.setStrokeWidth(size);
		invalidate();
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		if (paint != null && canvas != null){
			canvas.drawLine(this.getWidth()/2, 0, this.getWidth()/2, this.getHeight(), paint);
			postInvalidate();
		}
		
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (!isRunning){
			canvasthread.setRunning(true);
			canvasthread.start();
			isRunning = true;
		} else {
			canvasthread.onResume();
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		boolean retry = true;
		canvasthread.setRunning(false);
		while(retry){
			try{
				canvasthread.join();
				retry = false;
			} catch (InterruptedException e){
				
			}
		}	
	}
}
