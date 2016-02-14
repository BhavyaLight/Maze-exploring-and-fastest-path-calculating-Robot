package com.example.robofast2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by drakechng on 29/1/15.
 */
public class BluetoothManager {

    private BluetoothAdapter mBluetoothAdapter;
    private AcceptThread acceptThread;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private Handler mHandler;
    private boolean isServer, hasConnectedBefore;
    private HashMap<String, String> devices;
    private String serverAddress;
    private BroadcastReceiver mReceiver;
    private int connectionAttempt;

    public BluetoothManager(final BluetoothResponse response, final boolean isServer) {

        this.isServer = isServer;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        connectionAttempt = 0;
        hasConnectedBefore = false;

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case Config.SOCKET_CONNECTED:
                        response.onConnected((String) msg.obj);
                        hasConnectedBefore = true;
                        break;
                    case Config.MESSAGE_RECEIVED:
                        response.onReceived((String) msg.obj);
                        break;
                    case Config.SOCKET_DISCONNECT:
                        response.onDisconnected();
                        Reconnect(response, (String) msg.obj);
                    case Config.CONNECTING_FAILED:
                        if (hasConnectedBefore) {
                            if (connectionAttempt == 0) {
                                response.onReconnect((String) msg.obj);
                            }
                        }
                        Reconnect(response, (String) msg.obj);
                        break;
                }
            }
        };

        if (mBluetoothAdapter == null) {
            response.onError("Bluetooth is not supported");
        } else {
            if (mBluetoothAdapter.isEnabled() && isServer) {
                StartConnection();
            }
        }

        // Define Broadcast Receiver
        mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    devices.put(device.getName(), device.getAddress());
                }
            }
        };
    }

    public boolean getStatus() {
        return mBluetoothAdapter.isEnabled();
    }

    private void LoadPairedDevices() {
        devices = new HashMap<String, String>();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                devices.put(device.getName(), device.getAddress());
            }
        }
    }

    public void ScanDevices() {
        LoadPairedDevices();
        mBluetoothAdapter.startDiscovery();
    }

    public BroadcastReceiver GetBluetoothReceiver() {
        return mReceiver;
    }

    public HashMap<String, String> getDevices() {
        mBluetoothAdapter.cancelDiscovery();
        return devices;
    }

    public void setServer(String serverName) {
        this.serverAddress = devices.get(serverName);
    }

    public void SendMessage(String message) {
        if (connectedThread != null) {
            connectedThread.write(message.getBytes());
        }
    }

    public void Close() {
        if (connectedThread != null) {
            connectedThread.cancel();
        }
    }

    public void StartConnection() {
        System.out.println("Start Connection");
        if (isServer) {
            System.out.println("Start Server");
            acceptThread = new AcceptThread(mBluetoothAdapter, mHandler);
            acceptThread.start();
        } else {
            BluetoothDevice serverDevice = mBluetoothAdapter.getRemoteDevice(this.serverAddress); //"00:02:72:33:8C:C4"
            connectThread = new ConnectThread(serverDevice, mHandler);
            connectThread.start();
        }
    }

    private void Reconnect(BluetoothResponse response, String client) {
        if (connectionAttempt++ < Config.RECONNECT_ATTEMPTS) {
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            StartConnection();
        } else {
            response.onReConnectFailed(client);
            connectionAttempt = 0;
        }
    }

    private void ManageConnectedSocket(BluetoothSocket socket, Handler mHandler) {
        System.out.println("Managed");
        connectedThread = new ConnectedThread(socket, mHandler);
        connectedThread.start();
    }

    private class AcceptThread extends Thread {

        private final BluetoothServerSocket mmServerSocket;
        private final Handler mmHandler;

        public AcceptThread(BluetoothAdapter mBluetoothAdapter, Handler mHandler) {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;
            try {
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("MDPGrp2-RPI", Controller.DEFAULT_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmServerSocket = tmp;
            mmHandler = mHandler;
        }

        public void run() {
            BluetoothSocket socket;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    // Do work to manage the connection (in a separate thread)
                    mmHandler.obtainMessage(Config.SOCKET_CONNECTED, socket.getRemoteDevice().getName()).sendToTarget();
                    ManageConnectedSocket(socket, mmHandler);
                    cancel();
                    break;
                }
            }
        }

        /**
         * Will cancel the listening socket, and cause the thread to finish
         */
        public void cancel() {
            try {
                System.out.println("Accepted Closed");
                mmServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final Handler mHandler;

        public ConnectedThread(BluetoothSocket socket, Handler handler) {
            mmSocket = socket;
            mHandler = handler;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = new BufferedOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    String data = new String(buffer, 0, bytes);
                    mHandler.obtainMessage(Config.MESSAGE_RECEIVED, data).sendToTarget();
                } catch (IOException e) {
                    System.out.println("Connected Thread -  Exception");
                    cancel();
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
                mmOutStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                System.out.println("Connected Thread - cancel");
                mmSocket.close();
                mHandler.obtainMessage(Config.SOCKET_DISCONNECT).sendToTarget();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        //private final BluetoothDevice mmDevice;
        private final Handler mmHandler;

        public ConnectThread(BluetoothDevice device, Handler mHandler) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            //mmDevice = device;
            mmHandler = mHandler;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                //tmp = device.createRfcommSocketToServiceRecord(Controller.DEFAULT_UUID);
                Method m = device.getClass().getMethod("createInsecureRfcommSocket", new Class[]{int.class});
                tmp = (BluetoothSocket) m.invoke(device, 4);
            } catch (Exception e) {
                System.out.println("Connect Thread - Exception");
                mHandler.obtainMessage(Config.CONNECTING_FAILED, device.getName()).sendToTarget();
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            //mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                System.out.println("Exception 1 " + connectException);
                // Unable to connect; close the socket and get out
                mHandler.obtainMessage(Config.CONNECTING_FAILED, mmSocket.getRemoteDevice().getName()).sendToTarget();
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    System.out.println("Exception 2" + closeException);
                    mHandler.obtainMessage(Config.CONNECTING_FAILED, mmSocket.getRemoteDevice().getName()).sendToTarget();
                }
                return;
            }

            mmHandler.obtainMessage(Config.SOCKET_CONNECTED, mmSocket.getRemoteDevice().getName()).sendToTarget();

            // Do work to manage the connection (in a separate thread)
            ManageConnectedSocket(mmSocket, mmHandler);
        }
    }
}
