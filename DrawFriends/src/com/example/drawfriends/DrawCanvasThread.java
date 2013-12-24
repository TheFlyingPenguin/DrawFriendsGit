package com.example.drawfriends;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

/**
 * Thread for drawing to the draw surface.
 * 
 */
public class DrawCanvasThread extends Thread {
	private SurfaceHolder _surfaceHolder;
	private DrawSurface _drawSurface;
	private boolean _run = false;
	private Object mPauseLock = new Object();
	private boolean mPaused;
	
	public DrawCanvasThread(SurfaceHolder surfaceHolder, DrawSurface drawSurface){
		_surfaceHolder = surfaceHolder;
		_drawSurface = drawSurface;
	}
	
	/**
	 * Sets whether the thread should be running or not.
	 * @param run
	 */
	public void setRunning(boolean run){
		_run = run;
	}
	
	/**
	 * Locks the canvas, draws to it, and then unlocks it.
	 * @see com.example.drawfriends.DrawSurface#onDraw(Canvas)
	 */
	@Override
	public void run() {
		Canvas c = new Canvas();
		while (_run) {
			c = null;
			try {
				c = _surfaceHolder.lockCanvas(null);
				synchronized (_surfaceHolder){
					_drawSurface.onDraw(c);
				}
			} finally {
					// Do this in a finally so that if an exception is thrown during the above, we don't leave the Surface in an inconsistent state
				if (c != null) {
					_surfaceHolder.unlockCanvasAndPost(c);
				}
				
				// Pauses the running thread
				synchronized (mPauseLock) {
					while (mPaused) {
						try {
							mPauseLock.wait();
						} catch (InterruptedException e){
						}
					}
				}
			}			
		}
	}
	
	/**
	 * Pauses the thread.
	 */
	public void onPause(){
		synchronized (mPauseLock) {
			mPaused = true;
		}
	}
	
	/**
	 *  Resumes the thread.
	 */
	public void onResume() {
		synchronized (mPauseLock){
			mPaused = false;
			mPauseLock.notifyAll();
		}
	}
}
