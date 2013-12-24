package com.example.drawfriends;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;

import android.os.Handler;
 
/**
	Splash Screen Activity:
	This is called when the application launches and displays a splash screen for 5 seconds.
	After 5 seconds, a thread runs to start the menu screen activity.
 */
public class Splash extends Activity {
 
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
 
        //Sets view to splash layout
        setContentView(R.layout.splash);
 
        Handler handler = new Handler();
        
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
 
        //Runs a thread after 3 seconds to start the menu screen
        handler.postDelayed(new Runnable() {
 
        	//Method delayed for 3 seconds to displays splash for an appropriate length of time
            @Override
            public void run() {
 
            	//Ensures splash screen is closed so user does not come back to splash when back key is pressed
                finish();
                
                //Starts the menu screen
                Intent intent = new Intent(Splash.this, MenuScreen.class);
                Splash.this.startActivity(intent);
 
            }
            
            //Time in milliseconds until the run() method will be called
        }, 3000); 
 
    }
 
}
