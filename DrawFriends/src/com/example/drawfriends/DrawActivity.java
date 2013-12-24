package com.example.drawfriends;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.example.drawfriends.ColourDialog;
import com.example.drawfriends.BluetoothService;
import com.example.drawfriends.DeviceListActivity;
import com.example.drawfriends.R;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

/**
 * Handles all operations needed for the drawing activity. Contains a
 * drawing surface and buttons. Handles and processes data that is sent
 * between the BluetoothService class and the DrawSurface class.
 * @author PENGUIN
 *
 */
@SuppressLint("HandlerLeak")
public class DrawActivity extends Activity {
	
	//SurfaceView object that is drawn on
	DrawSurface drawSurface = null;
	//Variables to store the width and height of the draw surface
	private int drawWidth = 0;
	private int drawHeight = 0;
	
	//Fields required for screen shots
	private Canvas canvas;
	private Bitmap bmp;
	private FileOutputStream fout;
	private Dialog screenshotDialog;
	private TextView filename;
	private String outputFileName;
	private final String invalidChars = "\\/:*?\"<>|";
	private String oldFileName;
	private boolean doSave;
	
	//Fields required for line size picker
	private Dialog lineDialog;
	private int lineSize = 2;
	private LineView lineView;
	
	//Fields used for colour picker
	private Button colourButton;
	private int colPicked = Color.BLACK;
	
	//Media player fields
	MediaPlayer mpButton = null;
	MediaPlayer mpClear = null;
	MediaPlayer mpConnected = null;
	MediaPlayer mpDisconnected = null;
	MediaPlayer mpErase = null;
	MediaPlayer mpPaint = null;
	MediaPlayer mpScreenshot = null;
	
	private boolean backPressed = false;

	// Name of the connected device
	private String mConnectedDeviceName = null;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the chat services
	private BluetoothService mBluetoothService = null;
	
	private boolean isConnected = false;
	private boolean isBluetooth = true;

	// Message types sent from the BluetoothService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;

	// Key names received from the BluetoothService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";
	
	// Values sent when buttons are pressed
	private static final int BUTTON_CLEAR = 0;
	private static final int BUTTON_BLACK = 1;
	private static final int BUTTON_BROWN = 2;
	private static final int BUTTON_BLUE = 3;
	private static final int BUTTON_RED = 4;
	private static final int BUTTON_YELLOW = 5;
	private static final int BUTTON_ORANGE = 6;
	private static final int BUTTON_PURPLE = 7;
	private static final int BUTTON_GREEN = 8;
	private static final int BUTTON_WHITE = 9;
	private static final int BUTTON_GREY = 10;
	private static final int BUTTON_UNDO = 13;
	
	/**
	 * Called when the activity is first inflated.
	 * Sets up window characteristics, sets the layout, and checks 
	 * if the phone supports bluetooth.
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {	
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

		setContentView(R.layout.activity_draw);
		
		initRes();

		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "No Bluetooth adapter detected", Toast.LENGTH_LONG).show();
			isBluetooth = false;
		}else {
			if (!mBluetoothAdapter.isEnabled()) 
				Log.v("test", "hi");
				//startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
			
			else if (mBluetoothService == null) 
				initBlue();
		}		
		
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
	}
	
	/**
	 * Called when the activity first starts, after onCreate().
	 * Checks if bluetooth is enabled. If not, begins an intent to
	 * request it to be enabled. Otherwise, it calls the initializing
	 * method.
	 * @see android.app.Activity#onStart()
	 * @see android.app.Activity#onCreate(Bundle savedInstanceState)
	 * @see com.example.drawfriends.DrawActivity#init()
	 */
	@Override
	public void onStart() {
		super.onStart();
	}

	/**
	 * Called when this activity is returned to after already being started.
	 * Starts the bluetooth service again if it was not operating.
	 * @see com.example.drawfriends.BluetoothService#getState()
	 * @see com.example.drawfriends.BluetoothService#start()
	 */
	@Override
	public synchronized void onResume() {
		super.onResume();
		
		initSounds();
		
		if (isBluetooth){		
			// Performing this check in onResume() covers the case in which BT was
			// not enabled during onStart(), so we were paused to enable it...
			// onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
			if (mBluetoothAdapter.isEnabled() && mBluetoothService != null) {
				// Only if the state is STATE_NONE, do we know that we haven't started already
				if (mBluetoothService.getState() == BluetoothService.STATE_NONE) {
					// Start the Bluetooth services
					mBluetoothService.start();
				}
			}
		}
	}
	
	private void initBlue(){
		// Initialize the BluetoothService to perform bluetooth connections
		mBluetoothService = new BluetoothService(this, mHandler);
	}
	
	/**
	 * Sets listeners for all the buttons in the activity and
	 * sets colour filters for all colour changing buttons.
	 * Also stores the id of the draw surface.
	 */
	private synchronized void initRes(){
		((Button) findViewById(R.id.clearBtn)).setOnClickListener(mClearListener);

		((Button) findViewById(R.id.blackBtn)).setOnClickListener(mBlackListener);
		((Button) findViewById(R.id.blackBtn)).getBackground().setColorFilter(0xff000000, PorterDuff.Mode.MULTIPLY);

		((Button) findViewById(R.id.brownBtn)).setOnClickListener(mBrownListener);
		((Button) findViewById(R.id.brownBtn)).getBackground().setColorFilter(0xff8b4513, PorterDuff.Mode.MULTIPLY);

		((Button) findViewById(R.id.blueBtn)).setOnClickListener(mBlueListener);
		((Button) findViewById(R.id.blueBtn)).getBackground().setColorFilter(0xff0000ff, PorterDuff.Mode.MULTIPLY);

		((Button) findViewById(R.id.redBtn)).setOnClickListener(mRedListener);
		((Button) findViewById(R.id.redBtn)).getBackground().setColorFilter(0xffff0000, PorterDuff.Mode.MULTIPLY);

		((Button) findViewById(R.id.yellowBtn)).setOnClickListener(mYellowListener);
		((Button) findViewById(R.id.yellowBtn)).getBackground().setColorFilter(0xffffff00, PorterDuff.Mode.MULTIPLY);

		((Button) findViewById(R.id.orangeBtn)).setOnClickListener(mOrangeListener);
		((Button) findViewById(R.id.orangeBtn)).getBackground().setColorFilter(0xffffa500, PorterDuff.Mode.MULTIPLY);

		((Button) findViewById(R.id.purpleBtn)).setOnClickListener(mPurpleListener);
		((Button) findViewById(R.id.purpleBtn)).getBackground().setColorFilter(0xffa020f0, PorterDuff.Mode.MULTIPLY);

		((Button) findViewById(R.id.greenBtn)).setOnClickListener(mGreenListener);
		((Button) findViewById(R.id.greenBtn)).getBackground().setColorFilter(0xff228b22, PorterDuff.Mode.MULTIPLY);

		((Button) findViewById(R.id.whiteBtn)).setOnClickListener(mWhiteListener);
		((Button) findViewById(R.id.whiteBtn)).getBackground().setColorFilter(0xffffffff, PorterDuff.Mode.MULTIPLY);

		((Button) findViewById(R.id.greyBtn)).setOnClickListener(mGreyListener);
		((Button) findViewById(R.id.greyBtn)).getBackground().setColorFilter(0xffbebebe, PorterDuff.Mode.MULTIPLY);

		((Button) findViewById(R.id.lineBtn)).setOnClickListener(mLineListener);
		((Button) findViewById(R.id.undoBtn)).setOnClickListener(mUndoListener);
		
		colourButton = ((Button) findViewById(R.id.colourBtn));
		((Button) findViewById(R.id.colourBtn)).getBackground().setColorFilter(0xff000000, PorterDuff.Mode.MULTIPLY);
		colourButton.setOnClickListener(mColourListener);
		
		initSounds();
		
		if (drawSurface == null){
			drawSurface = (DrawSurface)findViewById(R.id.drawSurface);
			drawSurface.setBackgroundColor(Color.WHITE);
		}
	}
	
	private synchronized void initSounds(){
		while (mpScreenshot == null){
		Log.v("test", "init sounds");
		mpButton = MediaPlayer.create(getApplicationContext(), R.raw.button1);
		mpClear = MediaPlayer.create(getApplicationContext(), R.raw.button2);
		mpConnected = MediaPlayer.create(getApplicationContext(), R.raw.connected);
		mpDisconnected = MediaPlayer.create(getApplicationContext(), R.raw.disconnect);
		mpErase = MediaPlayer.create(getApplicationContext(), R.raw.eraser);
		if (mpErase != null)
			mpErase.setLooping(true);
		mpPaint = MediaPlayer.create(getApplicationContext(), R.raw.paint);
		if (mpPaint != null)
			mpPaint.setLooping(true);
		mpScreenshot = MediaPlayer.create(getApplicationContext(), R.raw.screenshot);
		}
	}

	/**
	 * Sets the variables to be used as the width and height of the
	 * draw surface in this class.
	 * @param w
	 * @param h
	 */
	public void initDimensions(int w, int h){
		drawWidth = w;
		drawHeight = h;
	}

	/**
	 * Stops a running bluetooth service and exits the activity when
	 * the back button is pressed.
	 * @see com.example.drawfriends.BluetoothService#stop()
	 */
	@Override
	public void onBackPressed() {		
		super.onBackPressed();
		backPressed = true;
		
		// Stop the Bluetooth chat services
		if (mBluetoothService != null){ 
			mBluetoothService.stop();
		}
		/*
		if (mpScreenshot != null){
			mpButton.release();
			mpConnected.release();
			mpDisconnected.release();
			mpErase.release();
			mpPaint.release();
			mpScreenshot.release();
		} */
	}

	/**
	 * Stops any running bluetooth service when the activity is destroyed and releases and media players.
	 * @see com.example.drawfriends.BluetoothService#stop()
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		// Stop the Bluetooth chat services
		if (mBluetoothService != null) mBluetoothService.stop();
	}

	/**
	 * Starts an intent to ask the user if they would like to
	 * make their device discoverable by other devices over 
	 * bluetooth for 300 seconds.
	 */
	private void ensureDiscoverable() {
		if (!isBluetooth){
			Toast.makeText(getApplicationContext(), "No Bluetooth adapter detected", Toast.LENGTH_SHORT).show();
			return;
		}
		
		if (!mBluetoothAdapter.isEnabled()) {
			startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
			return;
		}
		
		if (mBluetoothAdapter.getScanMode() !=
				BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}

	/**
	 * Sends the width of the draw surface to the connected device.
	 * @see com.example.drawfriends.DrawActivity#sendValues(int, int)
	 * @see com.example.drawfriends.DrawSurface#sentWidth()
	 */
	public synchronized void initWidth(){
		if (drawSurface != null){
			sendValues(drawSurface.BLUETOOTH_SCREEN_WIDTH, drawWidth);
			drawSurface.sentWidth();
		}
	}

	/**
	 * Sends the height of the draw surface to the connected device.
	 * @see com.example.drawfriends.DrawActivity#sendValues(int, int)
	 * @see com.example.drawfriends.DrawSurface#sentHeight()
	 */
	public synchronized void initHeight(){
		if (drawSurface != null){
			sendValues(drawSurface.BLUETOOTH_SCREEN_HEIGHT, drawHeight);
			drawSurface.sentHeight();
		}
	}

	/**
	 * Sends a bluetooth code that requests the connected device's
	 * draw surface's height or width.
	 * @param code
	 * @see com.example.drawfriends.DrawActivity#sendValues(int, int)
	 */
	public void requestDim(int code){
		sendValues(code, 1);
	}

	/**
	 * Sends two integers to a connected device over bluetooth.
	 * Converts both integers to a single byte array to send them.
	 * @param a
	 * @param b
	 * @see com.example.drawfriends.DrawActivity#intToByteArray(int)
	 * @see com.example.drawfriends.BluetoothService#write(byte[])
	 */
	public void sendValues(int a, int b) {
		
		if (!isConnected) return;
	
		// Get the message bytes and tell the BluetoothService to write
		byte[] send = new byte[8];
		byte[] first = intToByteArray(a);
		byte[] second = intToByteArray(b);
			
		for (int i = 0; i < 4; i++){
			send[i] = first[i];
			send[i+4] = second[i];
		}
		mBluetoothService.write(send);
		
	}

	/**
	 * Reads values sent to this device over bluetooth.
	 * Converts a single received byte array into two integers
	 * and sends these to the draw surface object.
	 * @param rec
	 * @see com.example.drawfriends.DrawActivity#byteArrayToInt(byte[])
	 * @see com.example.drawfriends.DrawSurface#recVals(int, int)
	 * @see com.example.drawfriends.BluetoothService.ConnectedThread#run()
	 */
	private synchronized void readValues(byte[] rec){
		byte[] first = new byte[4];
		byte[] second = new byte[4];
		
		for (int i = 0; i < 4; i++){
			first[i] = rec[i];
			second[i] = rec[i+4];
		}
		
		drawSurface.recVals(byteArrayToInt(first), byteArrayToInt(second));
	}

	/**
	 * Converts a byte array into an integer.
	 * @param b
	 * @return int
	 */
	public static int byteArrayToInt(byte[] b) 
	{
		int value = 0;
		for (int i = 0; i < 4; i++) {
			int shift = (4 - 1 - i) * 8;
			value += (b[i] & 0x000000FF) << shift;
		}
		return value;
	}

	/**
	 * Converts an integer into a byte array.
	 * @param a
	 * @return byte[]
	 */
	public static byte[] intToByteArray(int a)
	{
		byte[] ret = new byte[4];
		ret[3] = (byte) (a & 0xFF);   
		ret[2] = (byte) ((a >> 8) & 0xFF);   
		ret[1] = (byte) ((a >> 16) & 0xFF);   
		ret[0] = (byte) ((a >> 24) & 0xFF);
		return ret;
	}

	/**
	 * The Handler that gets information back from the BluetoothService.
	 */
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			// Called when the state of the bluetooth is changed
			// No action is taken. However, this code has been
			// left in case future updates require it
			case MESSAGE_STATE_CHANGE:
				switch (msg.arg1) {
				case BluetoothService.STATE_CONNECTED:
					if (mpConnected != null) mpConnected.start();
					isConnected = true;
					break;
				case BluetoothService.STATE_CONNECTING:
					break;
				case BluetoothService.STATE_LISTEN:
				case BluetoothService.STATE_NONE:
					if (isConnected){
						isConnected = false;
						mBluetoothService.start();
						drawSurface.disconnected();
						if (!backPressed) mpDisconnected.start();
					}
					break;
				}
				break;
			// Called when the bluetooth service is sending data
			case MESSAGE_WRITE:
				break;
			// Called when the bluetooth service is receiving data
			case MESSAGE_READ:
				byte[] readBuf = (byte[]) msg.obj;
				readValues(readBuf);
				break;
			// Called when the connected device's name has been sent
			case MESSAGE_DEVICE_NAME:
				// Save the connected device's name.
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(), "Connected to "
						+ mConnectedDeviceName, Toast.LENGTH_SHORT).show();
				break;
			// Called when a Toast message should be made
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
						Toast.LENGTH_SHORT).show();
				break;
			}
		}
	};

	/**
	 * Intents with a return code that is to be acquired should be
	 * run using this method.
	 * @see com.example.drawfriends.DrawActivity#init()
	 * @see com.example.drawfriends.BluetoothService#connect(BluetoothDevice)
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				// Get the device MAC address
				String address = data.getExtras()
						.getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				// Get the BLuetoothDevice object
				BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
				// Attempt to connect to the device
				mBluetoothService.connect(device);
			}
			break;
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up a drawing session
				initBlue();
			} else {
				// User did not enable Bluetooth or an error occured
				Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	public void btnPressed(int n){
		if (n == 0) mpClear.start();
		else mpButton.start();
	}
	// Click listeners for each button. When each button is pressed,
	// a certain code is sent to the draw surface object
	OnClickListener mClearListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			drawSurface.buttonPressed(BUTTON_CLEAR);
		}
	};

	OnClickListener mBlackListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			drawSurface.buttonPressed(BUTTON_BLACK);
		}
	};

	OnClickListener mBrownListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			drawSurface.buttonPressed(BUTTON_BROWN);
		}
	};

	OnClickListener mBlueListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			drawSurface.buttonPressed(BUTTON_BLUE);
		}
	};

	OnClickListener mRedListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			drawSurface.buttonPressed(BUTTON_RED);
		}
	};

	OnClickListener mYellowListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			drawSurface.buttonPressed(BUTTON_YELLOW);
		}
	};

	OnClickListener mOrangeListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			drawSurface.buttonPressed(BUTTON_ORANGE);
		}
	};

	OnClickListener mPurpleListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			drawSurface.buttonPressed(BUTTON_PURPLE);
		}
	};

	OnClickListener mGreenListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			drawSurface.buttonPressed(BUTTON_GREEN);
		}
	};

	OnClickListener mWhiteListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			drawSurface.buttonPressed(BUTTON_WHITE);
		}
	};

	OnClickListener mGreyListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			drawSurface.buttonPressed(BUTTON_GREY);
		}
	};

	OnClickListener mLineListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			makeLineDialog();
		}
	};

	OnClickListener mUndoListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			drawSurface.buttonPressed(BUTTON_UNDO);
		}
	};
	
	OnClickListener mColourListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			makeColourDialog();
		}
	};
	
	public void paintSound(int n) {
		if (drawSurface == null) return;
		if (n == drawSurface.STOP_PAINT) {
			mpPaint.pause();
		} else if (n == drawSurface.START_PAINT){
			mpPaint.start();
		} else if (n == drawSurface.STOP_ERASE) {
			mpErase.pause();
		} else if (n == drawSurface.START_ERASE) {
			mpErase.start();
		}
	}
	
	private void makeLineDialog() {
		lineDialog = new Dialog(DrawActivity.this);
		
		lineDialog.setContentView(R.layout.dialog_line_size);
		lineDialog.setTitle("Line Size");
		//lineDialog.setCanceledOnTouchOutside(true);
		
		lineSize = drawSurface.getLineSize();
		
		lineView = ((LineView)lineDialog.findViewById(R.id.lineView));
		lineView.setBackgroundColor(Color.WHITE);
		lineView.setSize(lineSize);
		
		OnClickListener mLineSubmitListener = new OnClickListener() {
			@Override
			public void onClick (View v) {
					drawSurface.setLineSize(lineSize);
					lineView.surfaceDestroyed(lineView.getHolder());
					lineView = null;
					lineDialog.dismiss();
					lineDialog = null;
				}
		};
		
		OnSeekBarChangeListener mLineSizeListener = new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				lineSize = progress;
				lineView.setSize((int)Math.floor(((float)progress)/100*drawSurface.getWidth()/4));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				
			}
		};		
		
		((Button)lineDialog.findViewById(R.id.btnLineSubmit)).setOnClickListener(mLineSubmitListener);
		SeekBar lineBar = ((SeekBar)lineDialog.findViewById(R.id.linePickBar));
		lineBar.setOnSeekBarChangeListener(mLineSizeListener);
		lineBar.setProgress(lineSize);
		
		lineDialog.show();
		
	}
	
	private void makeColourDialog(){
		ColourDialog colourDialog = new ColourDialog((Context)this, colPicked, new ColourDialog.OnColourListener() {
			@Override
			public void onOk(ColourDialog dialog, int colour) {
				colourButton.getBackground().setColorFilter(colour, PorterDuff.Mode.MULTIPLY);
				drawSurface.setColour(colour);
				colPicked = colour;
			}
			@Override
			public void onCancel (ColourDialog dialog) {
				
			}
		});
		
		colourDialog.show();
	}
	
	/**
	 * Saves a .jpeg image of the draw surface to the phone's external storage.
	 */
	private void screenshot(){
		bmp = Bitmap.createBitmap(drawWidth, drawHeight, Config.ARGB_8888);
		canvas = new Canvas(bmp);
		canvas.drawColor(Color.WHITE);
		drawSurface.onDraw(canvas);
		
		mpScreenshot.start();
		
		screenshotDialog = new Dialog(DrawActivity.this);
		screenshotDialog.setContentView(R.layout.dialog_screenshot);
		screenshotDialog.setTitle("Save a Screenshot");
		screenshotDialog.setCanceledOnTouchOutside(false);
		
		filename = (TextView)screenshotDialog.findViewById(R.id.filenameText);
		
		OnClickListener exitListener = new OnClickListener() {
			@Override
			public void onClick(View v){
				bmp.recycle();
				bmp = null;
				canvas = null;
				screenshotDialog.dismiss();
			}
		};
		
		OnClickListener saveListener = new OnClickListener() {
			@Override
			public void onClick(View v){
				
				boolean fileNameOk = false;
				outputFileName = filename.getText().toString().trim();
				
				for (int i = 0; i < outputFileName.length(); i++){
					fileNameOk  = true;
					if (invalidChars.contains(outputFileName.substring(i, i+1))){
						fileNameOk  = false;
						break;
					}
				}
				
				if (fileNameOk){
					File root = null;
					doSave = true;
					File picFile = null;
					try{			
						root = new File(Environment.getExternalStorageDirectory() + File.separator + "DrawFriends" + File.separator);
						root.mkdirs();
						picFile = new File(root, outputFileName + ".jpg");
						if (picFile.exists()){
							if (!outputFileName.equals(oldFileName)){
								oldFileName = new String(outputFileName);
								Toast.makeText(DrawActivity.this, "File already exists. Press Save again to overwrite.", Toast.LENGTH_SHORT).show();
								doSave = false;
							} else{
								doSave = true;
							}
						}
						fout = new FileOutputStream(picFile);
					} catch (FileNotFoundException e) {
						Toast.makeText(DrawActivity.this, "Error occured. Please try again later.", Toast.LENGTH_SHORT).show();
					}
					if (doSave){
						try{
							bmp.compress(Bitmap.CompressFormat.JPEG, 100, fout);
							fout.flush();
							fout.close();
							
							//Publishes image to gallery
						    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
						    Uri contentUri = Uri.fromFile(picFile);
						    mediaScanIntent.setData(contentUri);
						    DrawActivity.this.sendBroadcast(mediaScanIntent);
							
							Toast.makeText(DrawActivity.this,  "Screenshot saved to " + root.toString() , Toast.LENGTH_SHORT).show();
							screenshotDialog.dismiss();
						} catch (Exception e){
							
						}
					} else{
						try {
							fout.flush();
							fout.close();
						} catch (IOException e) {
						}
					}
			
					
				} else {
					Toast.makeText(DrawActivity.this,  "Invalid file name. Please do not include the characters " + invalidChars, Toast.LENGTH_SHORT).show();
				}
			}
		};
		
		((Button) screenshotDialog.findViewById(R.id.screenshotExit)).setOnClickListener(exitListener);
		((Button) screenshotDialog.findViewById(R.id.screenshotSave)).setOnClickListener(saveListener);
		
		screenshotDialog.show();

	}

	/**
	 * Inflates the options menu.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_draw, menu);
		return true;
	}
	
	/**
	 * Runs corresponding code when an item is selected from the
	 * activity's menu.
	 * @see com.example.drawfriends.DrawActivity#ensureDiscoverable()
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.scan:
			if (!isBluetooth){
				Toast.makeText(getApplicationContext(), "No Bluetooth adapter detected", Toast.LENGTH_SHORT).show();
				break;
			}
			if (!mBluetoothAdapter.isEnabled()) {
				startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
				break;
			}
			// Launch the DeviceListActivity to see devices and do scan
			Intent serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			return true;
		case R.id.discoverable:
			if (!isBluetooth) break;
			if (!mBluetoothAdapter.isEnabled()) {
				startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
				break;
			}
			// Ensure this device is discoverable by others
			ensureDiscoverable();
			return true;
		case R.id.screenshot:
			//Take a screenshot
			screenshot();
		}
		
		return false;
	}
}
