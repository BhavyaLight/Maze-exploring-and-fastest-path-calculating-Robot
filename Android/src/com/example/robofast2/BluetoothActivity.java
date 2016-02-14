package com.example.robofast2;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.method.ScrollingMovementMethod;
import android.text.style.RelativeSizeSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class BluetoothActivity extends Activity {

    private Controller controller;
    private TextView statusText, messageText;
    private EditText commandInput;
    private Button sendButton, startStopButton;
    private MenuItem connectButton, disconnectButton;
    private String[] messages = {"B<Command ","B<Command ","A<Command ","B<Command ","A<Command "};
    private ProgressDialog progressBar;
    private int count;
    private boolean isStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_layout);

        controller = (Controller) getApplicationContext();
        controller.setCurrentActivity(this);

        statusText = (TextView) findViewById(R.id.lblStatus);
        commandInput = (EditText) findViewById(R.id.txtCommand);
        sendButton = (Button) findViewById(R.id.btnSend);
        startStopButton = (Button) findViewById(R.id.btnStartStop);
        messageText = (TextView) findViewById(R.id.lblReceived);
        messageText.setMovementMethod(new ScrollingMovementMethod());

//        final Runnable r = new Runnable() {
//            @Override
//            public void run(){
//                try {
//                    int i = 0, count = 1;
//                    while (true) {
//                        controller.sendMessage(messages[i++ % 5] + count++);
//                        Thread.sleep(500);
//                    }
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        };

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                controller.sendMessage(commandInput.getText().toString());
                //new Thread( r ).start();
            }
        });

        startStopButton.setBackgroundColor(this.getResources().getColor(android.R.color.holo_green_light));
        startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setStartStop();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        connectButton = menu.getItem(0);
        disconnectButton = menu.getItem(1);
        disconnectButton.setEnabled(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id){
            case R.id.action_connect:
                connect_Click();
                return true;
            case R.id.action_disconnect:
                disconnect_Click();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setStartStop(boolean setStatus){
        isStarted = setStatus;
        setStartStop();
    }

    private void setStartStop(){
        if(!isStarted) {
            controller.sendMessage("BP<start");
            startStopButton.setBackgroundColor(getApplicationContext().getResources().getColor(android.R.color.holo_red_light));
            startStopButton.setText("Stop");
            sendButton.setEnabled(false);
            isStarted = true;
            commandInput.setEnabled(false);
        }else{
            controller.sendMessage("BP<stop");
            startStopButton.setBackgroundColor(getApplicationContext().getResources().getColor(android.R.color.holo_green_light));
            startStopButton.setText("Start");
            sendButton.setEnabled(true);
            isStarted = false;
            commandInput.setEnabled(true);
        }
    }

    public void showReconnecting(String client){
        hideProgress();
        statusText.setText("Reconnecting to: " + client);
        disconnectButton.setEnabled(false);
        connectButton.setEnabled(false);
        showProgress("Attempting to Reconnect\nPlease Wait...");
    }

    public void showReconnectFailed(String client){
        hideProgress();
        statusText.setText("Failed to Connect to: " + client);
        disconnectButton.setEnabled(false);
        connectButton.setEnabled(true);
        showToast("Failed to Connect");
    }

    public void showConnected(String client) {
        statusText.setText("Connected: " + client);
        disconnectButton.setEnabled(true);
        connectButton.setEnabled(false);
        hideProgress();
        showToast("Connected");
    }

    public void showDisconnected() {
        statusText.setText("Disconnected - Total Received: " + count);
        disconnectButton.setEnabled(false);
        connectButton.setEnabled(true);
        showToast("Disconnected");
        setStartStop(true);
    }

    public void showReceived(String message) {
        String existing = messageText.getText().toString();
        messageText.setText(message + "\n" + existing);
        ++count;
    }

    public void ready(){
        startStopButton.setEnabled(true);
        sendButton.setEnabled(true);
        messageText.setEnabled(true);
    }

    private void connect_Click(){
    	
    	if (!controller.isBluetoothReady()){
        	Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(intent,1);
    	}

        if(controller.isBluetoothReady()) {

            // 1. Show Scanning Dialog
            showProgress("Scanning for\nBluetooth Devices...");

            // 2. Scan for Devices
            controller.ScanDevices();

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // 2. Retrieve Devices
                    final CharSequence[] items = controller.GetDevices();

                    //AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    AlertDialog.Builder builder = new AlertDialog.Builder(BluetoothActivity.this);
                    builder.setTitle("Select Bluetooth Device");
                    builder.setItems(items, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            // Do something with the selection
                            count = 0;
                            controller.ConnectToServer(items[item].toString());
                            showProgress("Connecting to: " + items[item].toString() + "\nPlease Wait...");
                        }
                    });
                    builder.setCancelable(false);
                    builder.setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });

                    AlertDialog alert = builder.create();
                    hideProgress();
                    alert.show();
                }
            }, 5000);

        }else{
            showToast("Bluetooth needs to be enabled first");
        }
    }

    private void showProgress(String message){
        // prepare for a progress bar dialog
        progressBar = new ProgressDialog(this);
        progressBar.setCancelable(false);
        progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        SpannableString ss = new SpannableString(message);
        ss.setSpan(new RelativeSizeSpan(1.3f), 0, ss.length(), 0);
        progressBar.setMessage(ss);
        progressBar.show();
    }

    private void hideProgress(){
        progressBar.dismiss();
    }

    private void disconnect_Click() {
        controller.StopBluetooth();

        disconnectButton.setEnabled(false);
        connectButton.setEnabled(true);
    }

    public void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
}
