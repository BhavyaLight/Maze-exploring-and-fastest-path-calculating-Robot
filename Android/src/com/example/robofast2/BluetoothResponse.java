package com.example.robofast2;

import android.content.Intent;

/**
 * Created by drakechng on 30/1/15.
 */
public interface BluetoothResponse {

    public void onReconnect(String connectingClient);
    public void onReConnectFailed(String connectingClient);
    public void onConnected(String connectedClient);
    public void onReceived(String receivedMessage);
    public void onDisconnected();
    public void onError(String errorMessage);
}
