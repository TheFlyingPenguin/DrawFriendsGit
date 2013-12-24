package com.example.drawfriends;

import java.util.ArrayList;
import java.util.LinkedList;

import android.content.Context;
import android.graphics.*;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;

/**
 * Performs all required operations to draw to the draw surface when
 * the user moves their finger across it.
 *
 */
public class DrawSurface extends SurfaceView implements SurfaceHolder.Callback {

	// Member fields
	private DrawCanvasThread canvasthread;
	private ArrayList<Integer> xvals, yvals, sizes, colours;
	private ArrayList<Integer> recXVals, recYVals, recSizes, recColours;
	private ArrayList<Integer> cols;
	private LinkedList<Integer> lineBreaks;
	private boolean endline;
	private int curCol, curSize, recCurCol, recCurSize;
	private Paint drawPaint;
	private float screenWidth, screenHeight, recScreenWidth, recScreenHeight;
	private boolean isRunning = false;
	private boolean sentWidth = false, sentHeight = false;
	private boolean recStartLine = true;
	private int lastValSent;
	private Path path;
	private boolean isDrawing;
	
	//Constant labels for bluetooth data handling
	public final int BLUETOOTH_SCREEN_WIDTH = -1;
	private final int BLUETOOTH_CURRENT_COLOUR = -3;
	public final int BLUETOOTH_SCREEN_HEIGHT = -2;
	private final int BLUETOOTH_CURRENT_SIZE = -4;
	private final int BLUETOOTH_UNDO = -5;
	private final int BLUETOOTH_CLEAR = -6;
	private final int BLUETOOTH_REQUEST_HEIGHT = -7;
	private final int BLUETOOTH_REQUEST_WIDTH = -8;
	
	//Constant labels for sound effects
	public final int STOP_PAINT = 0;
	public final int START_PAINT = 1;
	public final int STOP_ERASE = 2;
	public final int START_ERASE = 3;	
	
	public DrawSurface(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		init();
	}
	
	public DrawSurface(Context context, AttributeSet attr){
		super(context, attr);
		init();
	}
	
	public DrawSurface(Context context){
		super(context);
		init();
	}
	
	/**
	 * Instantiates all fields and initializes values.
	 */
	private void init(){
		getHolder().addCallback(this);
		
		isDrawing = false;
		
		xvals = new ArrayList<Integer>();
		yvals = new ArrayList<Integer>();
		
		recXVals = new ArrayList<Integer>();
		recYVals = new ArrayList<Integer>();
		
		colours = new ArrayList<Integer>();
		sizes = new ArrayList<Integer>();
		
		recColours = new ArrayList<Integer>();
		recSizes = new ArrayList<Integer>();
		
		lineBreaks = new LinkedList<Integer>();
		
		curCol = Color.BLACK;
		curSize = 2;
		
		recCurCol = Color.BLACK;
		recCurSize = 2;
		
		cols = new ArrayList<Integer>();
		setCols();
		
		drawPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		drawPaint.setDither(true);
		drawPaint.setStyle(Style.STROKE);
		drawPaint.setStrokeJoin(Paint.Join.ROUND);    // set the join to round you want
	    drawPaint.setStrokeCap(Paint.Cap.ROUND);      // set the paint cap to round too

	    path = new Path();
	    
		lastValSent = 0;

		canvasthread = new DrawCanvasThread(getHolder(), this);		
		setFocusable(true);
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		
	}

	/**
	 * When the surface is created, starts a canvas thread for drawing
	 * and gets screen dimensions.
	 * @see com.example.drawfriends.DrawCanvasThread#start()
	 * @see com.example.drawfriends.DrawActivity#initDimensions(int, int)
	 */
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (!isRunning){
			canvasthread.setRunning(true);
			canvasthread.start();
			isRunning = true;
		} else {
			canvasthread.onResume();
		}

		screenWidth = getWidth();
		screenHeight = getHeight();
		((DrawActivity)this.getContext()).initDimensions(Math.round(screenWidth), Math.round(screenHeight));
		recScreenWidth = 0;
		recScreenHeight = 0;
	}

	/**
	 * Stops the drawing thread when the surface is destroyed.
	 * @see com.example.drawfriends.DrawCanvasThread#setRunning(boolean)
	 * @see com.example.drawfriends.DrawCanvasThread#join()
	 */
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
	
	/**
	 * Initializes an array of integers that stores colour values.
	 */
	private void setCols(){
		cols.add(0);
		//Black
		cols.add(Color.BLACK);
		//Brown
		cols.add(Color.argb(255, 139, 69, 19));
		//Blue
		cols.add(Color.argb(255, 0, 0, 255));
		//Red
		cols.add(Color.argb(255, 255, 0, 0));
		//Yellow
		cols.add(Color.argb(255, 255, 255, 0));
		//Orange
		cols.add(Color.argb(255, 255, 165, 0));
		//Purple
		cols.add(Color.argb(255, 160, 32, 150));
		//Green
		cols.add(Color.argb(255, 34, 139, 34));
		//White
		cols.add(Color.argb(255, 255, 255, 255));
		//Grey
		cols.add(Color.argb(255, 190, 190, 190));
	}
	
	public void setColour(int colour){
		curCol = colour;
		((DrawActivity)this.getContext()).sendValues(BLUETOOTH_CURRENT_COLOUR, curCol);
	}
	
	/**
	 * Receives a code from the DrawActivity when a button is pressed
	 * and reacts accordingly.
	 * @param n
	 * @see com.example.drawfriends.DrawSurface#clear()
	 * @see com.example.drawfriends.DrawSurface#undo()
	 */
	public void buttonPressed(int n){
		if (n == 0){ 
			((DrawActivity)this.getContext()).btnPressed(0);
			clear();
			((DrawActivity)this.getContext()).sendValues(BLUETOOTH_CLEAR, 1);
		}
		else if (n == 13){
			((DrawActivity)this.getContext()).btnPressed(0);
			undo();
			((DrawActivity)this.getContext()).sendValues(BLUETOOTH_UNDO, 1);
		}
		else{
			((DrawActivity)this.getContext()).btnPressed(1);
			setColour(cols.get(n));
		}
	}
	
	public void setLineSize(int size){
		curSize = (int)Math.floor(((float)size)/100*getWidth()/4);
		((DrawActivity)this.getContext()).sendValues(BLUETOOTH_CURRENT_SIZE, curSize);
	}
	
	public int getLineSize() {
		return Math.round(((float)curSize)*4/getWidth()*100);
	}
	
	/**
	 * Clears all stored coordinates and other line values for this device.
	 */
	private void clear(){
		while (isDrawing){}
		lineBreaks.clear();
		xvals.clear();
		yvals.clear();
		colours.clear();
		sizes.clear();
		lastValSent = 0;
	}

	/**
	 * Removes the last line drawn.
	 * @see com.example.drawfriends.DrawSurface#clear()
	 */
	private void undo(){		
		if (!lineBreaks.isEmpty()){				
			int lastLine = lineBreaks.removeLast();
			
			xvals.subList(lastLine, xvals.size()).clear();
			yvals.subList(lastLine, yvals.size()).clear();
			lastValSent = xvals.size();
			colours.remove(colours.size()-1);
			sizes.remove(sizes.size()-1);
		}
	}
	
	/**
	 * Clears all coordinates and other line values received from the
	 * connected device.
	 */
	private synchronized void recClear(){
		while (isDrawing){}
		recXVals.clear();
		recYVals.clear();
		recColours.clear();
		recSizes.clear();
	}
	
	/**
	 * Deletes the last line received from the connected device.
	 * @see com.example.drawfriends.DrawSurface#recClear()
	 */
	private synchronized void recUndo(){
		int lastLine = -1;
		boolean notLastLine = true;
		while (notLastLine && recXVals.size() >= 1){
			if (recXVals.get(recXVals.size()-1) != -50)
				notLastLine = false;
			else
				recXVals.remove(recXVals.size()-1);
		}
		lastLine = recXVals.lastIndexOf(-50);
		if (lastLine == -1){
			recClear();
		}else{
			recXVals.subList(lastLine + 1, recXVals.size()).clear();
			recYVals.subList(lastLine + 1, recYVals.size()).clear();
			recColours.remove(recColours.size()-1);
			recSizes.remove(recSizes.size()-1);
		}
	}
	
	/**
	 * Draws all all stored lines to the draw surface each time it is called.
	 */
	@Override
	public synchronized void onDraw(Canvas canvas){
		isDrawing = true;
		// Checks if the dimensions have been received by the
		// connected device, sends them if not
		try{
			if (!sentWidth){
				((DrawActivity)this.getContext()).initWidth();
			}else if (!sentHeight){
				((DrawActivity)this.getContext()).initHeight();
			}
			
			// Checks if the dimensions of the connected device have
			// been received, requests them if not
			if (recScreenWidth == 0){
				((DrawActivity)this.getContext()).requestDim(BLUETOOTH_REQUEST_WIDTH);
			}
			
			if (recScreenHeight == 0){
				((DrawActivity)this.getContext()).requestDim(BLUETOOTH_REQUEST_HEIGHT);
			}
		} catch(Exception e){
			
		}
		
		// Draws all lines made by this device
		endline = false;
		int curx = -50;
		int cury = -50;
		int oldx, oldy;
		int lineNum = 0;
		path.reset();
		
		for (int i = 0; i < yvals.size() && i < xvals.size(); i++){
			
			oldx = curx;
			oldy = cury;
			
			curx = xvals.get(i);
			cury = yvals.get(i);
			if (oldx == -50) {
				path.moveTo(curx, cury);
				
				drawPaint.setColor(colours.get(lineNum));
				drawPaint.setStrokeWidth(sizes.get(lineNum));
		
				lineNum++;
			}
			
			if (oldx != -50 && curx != -50){
				path.lineTo(curx, cury);
			}
			if (curx == -50 || i == yvals.size()-1) {
				if (canvas == null) break;
				canvas.drawPath(path, drawPaint);
				path.reset();
			}
			
			//Sends each value not yet sent to the connected device
			while (lastValSent < yvals.size()){
				((DrawActivity)this.getContext()).sendValues(xvals.get(lastValSent), yvals.get(lastValSent));
				lastValSent++;
			}
		}
		
		// Draws all lines received from the connected device
		endline = false;
		curx = -50;
		cury = -50;
		lineNum = 0;
		path.reset();
		
		for (int i = 0; i < recYVals.size() && i < recXVals.size(); i++){
			oldx = curx;
			oldy = cury;
			curx = recXVals.get(i);
			cury = recYVals.get(i);
			
			if (oldx == -50) {
				path.moveTo(curx, cury);
				drawPaint.setColor(recColours.get(lineNum));
				drawPaint.setStrokeWidth(recSizes.get(lineNum));
		
				lineNum++;
			}
			
			if (oldx != -50 && curx != -50){
				path.lineTo(curx, cury);
			}
			if (curx == -50 || i == recYVals.size()-1) {
				if (canvas == null) break;
				canvas.drawPath(path, drawPaint);
				path.reset();
			}
		}
		
		isDrawing = false;
		postInvalidate();
	}

	/**
	 * Stores locally drawn lines when the user touches the screen.
	 */
	@Override
	public boolean onTouchEvent(MotionEvent e) {
		int action = e.getAction();
		
		if (action == MotionEvent.ACTION_DOWN){			
			lineBreaks.add(xvals.size());
			colours.add(curCol);
			sizes.add(curSize);
			
			if (curCol == Color.WHITE) 
				((DrawActivity)this.getContext()).paintSound(START_ERASE);
			else
				((DrawActivity)this.getContext()).paintSound(START_PAINT);
		}

		xvals.add((int)e.getX());
		yvals.add((int)e.getY());
		
		if (action == MotionEvent.ACTION_UP){
			xvals.add(-50);
			yvals.add(-50);
			
			if (curCol == Color.WHITE)
				((DrawActivity)this.getContext()).paintSound(STOP_ERASE);
			else
				((DrawActivity)this.getContext()).paintSound(STOP_PAINT);
		}
		
		return true;
	}
	
	/**
	 * Handles all values received over bluetooth.
	 * @param a
	 * @param b
	 * @see com.example.drawfriends.DrawSurface#recClear()
	 * @see com.example.drawfriends.DrawSurface#recUndo()
	 */
	public synchronized void recVals(int a, int b){
		if (a == BLUETOOTH_SCREEN_WIDTH){
			recScreenWidth = b;
		}else if(a == BLUETOOTH_SCREEN_HEIGHT){
			recScreenHeight = b;
		}else if (a == BLUETOOTH_CURRENT_COLOUR){
			recCurCol = b;
		}else if (a == BLUETOOTH_CURRENT_SIZE){
			recCurSize = (int) (screenWidth/recScreenWidth*b);
		}else if (a == BLUETOOTH_CLEAR){
			recClear();
		}else if (a == BLUETOOTH_UNDO){
			recUndo();
		}else if (a == BLUETOOTH_REQUEST_WIDTH){
			sentWidth = false;
		}else if (a == BLUETOOTH_REQUEST_HEIGHT){
			sentHeight = false;
		}else{
			if (recScreenHeight != 0 && recScreenWidth != 0){
				if (a != -50){
					// If a new line is being started, adds the received
					// size and colour values to the array
					if (recStartLine){
						recColours.add(recCurCol);
						recSizes.add(recCurSize);
						recStartLine = false;
					}
					
					//Scales the coordinates to fit this device's screen
					recXVals.add(Math.round(screenWidth/recScreenWidth*a));
					recYVals.add(Math.round(screenHeight/recScreenHeight*b));
					
				}else if (recXVals.get(recXVals.size()-1) != -50){
					recXVals.add(-50);
					recYVals.add(-50);
					recStartLine = true;
				}
			}
		}
	}
	
	public synchronized void disconnected(){
		xvals.addAll(recXVals);
		recXVals.clear();
		yvals.addAll(recYVals);
		recYVals.clear();
		colours.addAll(recColours);
		recColours.clear();
		sizes.addAll(recSizes);
	}
	
	/**
	 * Notifies the DrawSurface that the width has been sent.
	 */
	public void sentWidth(){
		sentWidth = true;
	}
	
	/**
	 * Notifies the DrawSurface that the height has been sent.
	 */
	public void sentHeight(){
		sentHeight = true;
	}
	
}
