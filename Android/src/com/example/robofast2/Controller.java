package com.example.robofast2;

import android.app.AlertDialog;
import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Handler;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by drakechng on 29/1/15.
 */
public class Controller extends Application implements BluetoothResponse {

    static final UUID DEFAULT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public BluetoothActivity currentActivity;
    public MainActivity mainActivity;
    private BluetoothManager mBluetoothManager;
    private BroadcastReceiver mReceiver;
    private Boolean completed;

    public Controller(){
        // Start Bluetooth Manager [true - Server Mode | false - Client Mode]
        mBluetoothManager = new BluetoothManager(Controller.this, false);
    }
    
    public void setCurrentActivity(BluetoothActivity currentActivity) {
        this.currentActivity = currentActivity;
    }

    public boolean isBluetoothReady(){
        return mBluetoothManager.getStatus();
    }

    public void sendMessage(String message){
        mBluetoothManager.SendMessage(message);
    }

    @Override
    public void onReconnect(String connectingClient) {
        currentActivity.showReconnecting(connectingClient);
    }

    @Override
    public void onReConnectFailed(String connectingClient) {
        currentActivity.showReconnectFailed(connectingClient);
    }

    @Override
    public void onConnected(String connectedClient) {
        currentActivity.showConnected(connectedClient);
    }

    @Override
    public void onDisconnected() {
        currentActivity.showDisconnected();
    }

    @Override
    public void onReceived(String receivedMessage) {
        currentActivity.showReceived(receivedMessage);
        mainActivity.showReceived(receivedMessage);
        mainActivity.processMessage(receivedMessage);
        completed = true;
        if(receivedMessage.equals("ready")){
            currentActivity.ready();
            mainActivity.ready();
        }
    }

    @Override
    public void onError(String errorMessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
        builder.setMessage("Look at this dialog!")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //do things
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void ScanDevices(){

        RegisterBluetoothReceiver();
        mBluetoothManager.ScanDevices();
    }

    public CharSequence[] GetDevices(){

        List<String> list;
        CharSequence[] cs;

        list = new ArrayList<String>(mBluetoothManager.getDevices().keySet());
        cs = list.toArray(new CharSequence[list.size()]);

        return cs;
    }

    public void RegisterBluetoothReceiver(){

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        currentActivity.registerReceiver(mBluetoothManager.GetBluetoothReceiver(), filter); // Don't forget to unregister during onDestroy
    }

    public void ConnectToServer(String serverName){
        mBluetoothManager.setServer(serverName);
        mBluetoothManager.StartConnection();
    }

    public void StopBluetooth(){
        mBluetoothManager.Close();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        unregisterReceiver(mBluetoothManager.GetBluetoothReceiver());
    }

	public void setCurrentActivity(MainActivity mainActivity) {
		this.mainActivity = mainActivity;
	}

	public Boolean getCompleted() {
		return completed;
	}

	public void setCompleted(Boolean completed) {
		this.completed = completed;
	}
	
	

}
