package com.example.robofast2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements SensorEventListener {
	
	private static final int BLUETOOTH_SCREEN = 1001;
	private static final int CONFIG_MODE = 2001;
	protected static final String TAG = "debugging";
	
	private ImageButton buttonUp, buttonDown, buttonLeft, buttonRight,
				buttonF1, buttonF2, buttonStartLocate, buttonUpdateMap;
	private Button buttonConnect, buttonConfig, buttonExplore, buttonRun;
	private TextView statusView, messageText;
	private Switch enableAutoUpdate, startPositionSwitch, tiltSwitch;
	private Controller controller;
	
	private RelativeLayout mapContainer;
	private Arena canvas;
	private String gridString = "GRID 15 20 1 1 2 1 0 0 0 0 0 0 0 0";
	private String gridString2 = "GRID 15 20 1 1 2 1 0 0 0 0 0 0 0 0";
	private int[] intArray = new int[300];
	private int[] tempIntArray = new int[300];
	private boolean isStarted = false;
	private boolean tiltEnabled = false;
	private boolean startIndicated = false;
	private boolean directionIndicated = false;
	private String recentCommand = "";
	private SensorManager sensorManager;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		controller = (Controller) getApplicationContext();
		controller.setCurrentActivity(this);
		init();

	}

	private void init() {
		
		buttonUp = (ImageButton) findViewById(R.id.buttonUp);
		buttonDown = (ImageButton) findViewById(R.id.buttonDown);
		buttonLeft = (ImageButton) findViewById(R.id.buttonLeft);
		buttonRight = (ImageButton) findViewById(R.id.buttonRight);
		
		buttonConnect = (Button) findViewById(R.id.buttonConnect);
		buttonConfig = (Button) findViewById(R.id.buttonConfig);
		
		buttonStartLocate = (ImageButton) findViewById(R.id.buttonBlue);
		buttonUpdateMap = (ImageButton) findViewById(R.id.buttonGreen);
		buttonF1 = (ImageButton) findViewById(R.id.buttonYellow);
		buttonF2 = (ImageButton) findViewById(R.id.buttonRed);
		buttonExplore = (Button) findViewById(R.id.buttonExplore);
		buttonRun = (Button) findViewById(R.id.buttonRun);
		buttonExplore.setBackgroundColor(this.getResources().getColor(android.R.color.holo_green_light));
		buttonRun.setBackgroundColor(this.getResources().getColor(android.R.color.holo_green_light));
		
		messageText = (TextView) findViewById(R.id.bluetoothMessage);
		messageText.setMovementMethod(new ScrollingMovementMethod());
		statusView = (TextView) findViewById(R.id.statusTextView);
		enableAutoUpdate = (Switch) findViewById(R.id.updateMapSwitch);
		startPositionSwitch = (Switch) findViewById(R.id.startPositon);
		tiltSwitch = (Switch) findViewById(R.id.tiltSensorSwitch);
		
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		sensorManager.registerListener(this,sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_NORMAL);
		
		canvas = new Arena(this);
		canvas.setClickable(false);
		tempIntArray = toIntArray(gridString2);
		intArray = toIntArray(gridString);
		canvas.setGridArray(intArray);
		mapContainer = (RelativeLayout) findViewById(R.id.mapView);
		mapContainer.addView(canvas);
		canvas.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				/*if (canvas.isClickable()){
					int x = (int) event.getX();
					int y = (int) event.getY();
					canvas.startPosition(x/canvas.getSize(), y/canvas.getSize());
					
				}			
				return true;*/
				if (canvas.isClickable()) {
					int x = (int) event.getX();
				    int y = (int) event.getY();
	
				    switch (event.getAction()) {
				    case MotionEvent.ACTION_DOWN:
				    	canvas.startPosition(x/canvas.getSize(), y/canvas.getSize());
				    	return true;
				    case MotionEvent.ACTION_MOVE:
				    	boolean directed = canvas.setDirection(x/canvas.getSize(), y/canvas.getSize());
				    	if (directed) {
				    		startPositionSwitch.setChecked(false);
				    	}
				    	break;
				    case MotionEvent.ACTION_UP:
				    	// nothing to do
				    	break;
				    default:
				    	return false;
				    }
				    // Schedules a repaint.
				    v.invalidate();
				    return true;
				}
				return false;
			}
		});
		
		buttonUp.setOnClickListener(new ProcessButton());
		buttonDown.setOnClickListener(new ProcessButton());
		buttonLeft.setOnClickListener(new ProcessButton());
		buttonRight.setOnClickListener(new ProcessButton());
		
		buttonConnect.setOnClickListener(new ProcessButton());
		buttonConfig.setOnClickListener(new ProcessButton());
		buttonExplore.setOnClickListener(new ProcessButton());
		buttonRun.setOnClickListener(new ProcessButton());
		
		buttonStartLocate.setOnClickListener(new ProcessButton());
		buttonUpdateMap.setOnClickListener(new ProcessButton());
		buttonF1.setOnClickListener(new ProcessButton());
		buttonF2.setOnClickListener(new ProcessButton());
		
		statusView.setText("I am ready!");
		
		enableAutoUpdate.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked) {
				    makeToast("Auto update map");
				    buttonUpdateMap.setEnabled(false);
				    canvas.setGridArray(tempIntArray);
				}else {
					buttonUpdateMap.setEnabled(true);
				    makeToast("Manual update map");
				}
			}
		});
		startPositionSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked) {
					makeToast("Touch to indicate start position");
					canvas.setClickable(true);
				} else {
					makeToast("Start position ready to send");
					canvas.setClickable(false);
				}
			}
		});
		tiltSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					makeToast("Tilt sensor enabled");
					tiltEnabled = true;
				} else {
					makeToast("Tilt sensor disabled");
					tiltEnabled = false;
				}
			}
		});
		
	}

	public class ProcessButton implements OnClickListener {
		@Override
		public void onClick(View v) {
			switch(v.getId()) {
			case R.id.buttonUp: move("Forward"); canvas.moveForward(); break;
			case R.id.buttonDown: move("Backward"); break;
			case R.id.buttonLeft: move("Left"); canvas.turnLeft(); break;
			case R.id.buttonRight: move("Right"); canvas.turnRight(); break;
			
			case R.id.buttonConnect: bluetoothScan(); break;
			case R.id.buttonConfig: configMode(); break;
			case R.id.buttonExplore: startExplore(); break;
			case R.id.buttonRun: startFastestRun(); break;
			
			case R.id.buttonYellow: launchConfig(loadSharedPreference("f1")); break;
			case R.id.buttonRed: launchConfig(loadSharedPreference("f2")); break;
			case R.id.buttonGreen: updateGridArray(); break;
			case R.id.buttonBlue: sendStartPosition(); break;
			}
		}
	}

	public void updateGridArray() {
		canvas.setGridArray(tempIntArray);
	}
	
    public void sendStartPosition() {
    	String direction;
    	if (intArray[4] == intArray[2]) {
    		if (intArray[5]>intArray[3]) {
    			direction = "E";
    		} else {
    			direction = "W";
    		}
    	} else {
    		if (intArray[4]>intArray[2]) {
    			direction = "N";
    		} else {
    			direction = "S";
    		}
    	}
    	int row = 20 - intArray[2];
    	int col = intArray[3] - 1;
    	String mess = Integer.toString(row)+":"+Integer.toString(col)+":"+direction;
		//controller.sendMessage("BP>"+mess);
		updateStatus("Start Position sent: " + mess);
	}


	public void startExplore() {
		String direction;
    	if (intArray[4] == intArray[2]) {
    		if (intArray[5]>intArray[3]) {
    			direction = "E";
    		} else {
    			direction = "W";
    		}
    	} else {
    		if (intArray[4]>intArray[2]) {
    			direction = "N";
    		} else {
    			direction = "S";
    		}
    	}
    	int row = 20 - intArray[2];
    	int col = intArray[3] - 1;
    	String mess = Integer.toString(row)+":"+Integer.toString(col)+":"+direction;
		controller.sendMessage("BP<"+mess);
		updateStatus("Start Exploring at: " + mess);
		//controller.sendMessage("BP<18:1:W");
		buttonExplore.setEnabled(false);
        /*if(!isStarted) {
            controller.sendMessage("BP<18:1:W");
            buttonExplore.setBackgroundColor(getApplicationContext().getResources().getColor(android.R.color.holo_red_light));
            buttonExplore.setText("Stop Exploring");
            isStarted = true;
            updateStatus("Start to explore");
            
        }else{
            controller.sendMessage("BP<stop");
            buttonExplore.setBackgroundColor(getApplicationContext().getResources().getColor(android.R.color.holo_green_light));
            buttonExplore.setText("Start Explore");
            isStarted = false;
        }*/
	}


	public void startFastestRun() {
		controller.sendMessage("BP<fp");
		buttonExplore.setEnabled(false);
		buttonRun.setEnabled(false);
        /*if(!isStarted) {
            controller.sendMessage("BP<fp");
            buttonRun.setBackgroundColor(getApplicationContext().getResources().getColor(android.R.color.holo_red_light));
            buttonRun.setText("Stop Running");
            isStarted = true;
            updateStatus("Start fastest run");
        }
        else{
            controller.sendMessage("BP<stop");
            buttonRun.setBackgroundColor(getApplicationContext().getResources().getColor(android.R.color.holo_green_light));
            buttonRun.setText("Fastest Run");
            isStarted = false;
        }*/
	}


	public void showReceived(String message) {
        String existing = messageText.getText().toString();
        messageText.setText(message + "\n" + existing);
    }

    public void ready(){
        //buttonUpdateMap.setEnabled(true);
        //sendButton.setEnabled(true);
    	buttonExplore.setEnabled(true);
        messageText.setEnabled(true);
    }

	public int[] toIntArray(String s) {
		String[] stringArray = s.split(" ");
		int length = stringArray.length - 1;
		int[] intArray = new int[length];
		
		for (int i=1; i <= length; i++) {
			intArray[i-1] = Integer.parseInt(stringArray[i]);
		}
		
		int bodyX = intArray[2];
    	int bodyY = intArray[3];
    	int headX = intArray[4];
    	int headY = intArray[5];
    	boolean vertical = false;
    	boolean positive = false;
    	if (bodyX==headX) { vertical = true; }
    	if (headX > bodyX || headY > bodyY) { positive = true; }
    	if (bodyX < 2) { bodyX = 2; }
    	if (bodyY < 2) { bodyY = 2; }
    	if (vertical) {
    		headX = bodyX;
    		if (positive) { 
    			headY = bodyY + 2;
    		} else {
    			headY = bodyY - 2;
    		}
    	} else {
    		headY = bodyY;
    		if (positive) {
    			headX = bodyX + 2;
    		} else {
    			headX = bodyX - 2;
    		}
    	}
    	intArray[2] = bodyX;
    	intArray[3] = bodyY;
    	intArray[4] = headX;
    	intArray[5] = headY;
		
		return intArray;
	}

	@SuppressLint("DefaultLocale")
	public void launchConfig(String config) {
		String[] command = config.split("");
		String direction;
		for (int i=1; i < command.length; i++) {
			direction = command[i].toUpperCase();
			move(direction);
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}


	public void makeToast(String output) {
		Toast.makeText(getApplicationContext(), output, 0).show();
	}

	public void bluetoothScan() {
		Intent intent = new Intent(this, BluetoothActivity.class);
		startActivityForResult(intent, BLUETOOTH_SCREEN);
	}

	public void configMode() {
		Intent intent = new Intent(this, ConfigurationActivity.class);
		startActivityForResult(intent, CONFIG_MODE);
	}

	public void updateStatus(String toDisplay){
		statusView.setText(toDisplay);
	}
	
	public void move(String movement){
		//mapView.setGridString(newString);
		controller.sendMessage("A<" + movement.substring(0,1) );
		updateStatus("Moving " + movement + "...");
		//recentCommand = movement;
	}
	
	private String loadSharedPreference(String key) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		String valueString = sharedPreferences.getString(key,"default");
		//String valueString2 = sharedPreferences.getString("f2","default");
		return valueString;	
	}

	public void processMessage(String receivedMessage) {
		if (receivedMessage.equals("finish")) {
			buttonRun.setEnabled(true);
			buttonExplore.setEnabled(false);
			controller.sendMessage("BP<Ready to Run!");
		} else if (recentCommand.equals("Forward")){
			canvas.moveForward();
			recentCommand = "";
		} else if (recentCommand.equals("Right")) {
			canvas.turnRight();
			recentCommand = "";
		} else if (recentCommand.equals("Left")){
			canvas.turnLeft();
			recentCommand = "";
		}

	}


	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		float x = event.values[0];
		float y = event.values[1];
		if (tiltEnabled) {
			if (y < -5) {
				move("Forward");			
			} else if (x < -5) {
				move("Right");
			} else if (x > 5) {
				move ("Left");
			}
		}
	}
}
