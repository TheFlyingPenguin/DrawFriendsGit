package com.example.drawfriends;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import android.widget.Button;

/**
	Menu Screen Activity:
	A thread from the splash activity is run to start this menu screen activity.
	The menu screen layout consists of three buttons: Start, Instructions, and Exit.
	This class also contains a dialog box displaying instructions after clicking the instructions button.
 */

public class MenuScreen extends Activity{
	//Dialog box displays Connectivity Help Information and Canvas's Button Information
	private Dialog dialog;

	//Menu layout set as content view
	//Creates three onClickListeners for three buttons in the menu screen: Start button, Instruction button, Exit button
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_menu);

		Button buttonStart = (Button)findViewById(R.id.buttonStart);
		buttonStart.setOnClickListener(startListener);

		Button buttonExit = (Button)findViewById(R.id.buttonExit);
		buttonExit.setOnClickListener(exitListener);

		Button buttonInstruct = (Button)findViewById(R.id.buttonInstruct);
		buttonInstruct.setOnClickListener(instructListener);

		Button buttonCredit = (Button)findViewById(R.id.buttonCredits);
		buttonCredit.setOnClickListener(creditListener);
		
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
	}

	//Start button begins DrawActivity from menu screen
	private OnClickListener startListener = new OnClickListener() {
		public void onClick(View v) {
			Intent intent = new Intent(MenuScreen.this, DrawActivity.class);
			MenuScreen.this.startActivity(intent);
		}
	};

	//Button allows user to stop and exit application from menu screen
	private OnClickListener exitListener = new OnClickListener() {
		public void onClick(View v) {
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_HOME);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);

		}
	};

	//Instruction button opens dialog box containing help information
	private OnClickListener instructListener = new OnClickListener(){

		//Within dialog, the first layout presents the connection information
		@Override
		public void onClick (View v){
			dialog = new Dialog(MenuScreen.this);
			dialog.setContentView(R.layout.dialog_instruct_1);
			dialog.setTitle("Bluetooth Interaction Help");
			dialog.setCanceledOnTouchOutside(true);

			//Button enables user to exit dialog from first instruction layout 
			Button exitDialog = (Button) dialog.findViewById(R.id.exitDialogButton);
			exitDialog.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v){
					dialog.dismiss();

				}
			});

			//'Next' button opens second layout containing canvas's buttons information
			Button nextDialog1 = (Button) dialog.findViewById(R.id.nextButton1);
			nextDialog1.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v){
					dialog.setTitle("Canvas Buttons Help");
					dialog.setContentView(R.layout.dialog_instruct_2);
					dialog.setCanceledOnTouchOutside(true);

					//Within dialog, button allows user to immediately begin DrawActivity  
					Button startMainActivity = (Button) dialog.findViewById(R.id.startFromDialog);
					startMainActivity.setOnClickListener(new OnClickListener(){
						@Override
						public void onClick(View v){
							Intent intent = new Intent(MenuScreen.this, DrawActivity.class);
							MenuScreen.this.startActivity(intent);

						}
					});

					//Button enables user to exit dialog from second instuction layout
					Button exitDialog2 = (Button) dialog.findViewById(R.id.exitDialogButton2);
					exitDialog2.setOnClickListener(new OnClickListener(){
						@Override
						public void onClick(View v){
							dialog.dismiss();
						}
					});

				}
			});
			//Begins and displays dialog
			dialog.show();
		}

	};

	//Gallery button begins DrawActivity from menu screen
	private OnClickListener creditListener = new OnClickListener() {
		public void onClick(View v) {
			Intent intent = new Intent(MenuScreen.this, CreditsActivity.class);
			startActivity(intent);
		}
	};
}

