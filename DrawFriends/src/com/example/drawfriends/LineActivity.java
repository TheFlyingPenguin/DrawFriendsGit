package com.example.drawfriends;

import android.os.Bundle;
import android.app.Activity;
import android.graphics.Color;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class LineActivity extends Activity {
	
	private SeekBar lineSizeBar;
	private int lineSize;
	private LineView lineView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog_line_size);
		
		((Button)findViewById(R.id.btnLineSubmit)).setOnClickListener(mLineSubmitListener);
		lineSizeBar = (SeekBar) findViewById(R.id.linePickBar);
		lineSizeBar.setOnSeekBarChangeListener(mLineSizeListener);
		lineView = ((LineView)findViewById(R.id.lineView));
		lineView.setBackgroundColor(Color.WHITE);
	}
	
	OnClickListener mLineSubmitListener = new OnClickListener() {
		@Override
		public void onClick (View v) {
			onSubmit(lineSize);
		}
	};
	
	OnSeekBarChangeListener mLineSizeListener = new OnSeekBarChangeListener() {

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			lineSize = progress;
			lineView.setSize(lineSize);
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			
		}
	};
	
	public void onSubmit(int lineWidth) {
		
	}
	
	public void setSize(int size){
		lineSizeBar.setProgress(size);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	

}
