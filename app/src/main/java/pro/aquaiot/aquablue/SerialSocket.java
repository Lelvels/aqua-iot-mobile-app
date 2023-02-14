package pro.aquaiot.aquablue;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.Executors;

import pro.aquaiot.aquablue.constants.Constants;

public class SerialSocket implements Runnable {
    private static final UUID BLUETOOTH_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static String TAG = "SerialSocket";
    private final BroadcastReceiver disconnectBroadcastReceiver;
    private final Context context;
    private SerialListener listener;
    private final BluetoothDevice device;
    private BluetoothSocket socket;
    private boolean connected;

    public SerialSocket(Context context, BluetoothDevice device) {
        //TODO: What is this mean ?
        if(context instanceof Activity)
            throw new InvalidParameterException("expected non UI context");
        this.device = device;
        this.context = context;
        disconnectBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(listener != null)
                    listener.onSerialIoError(new IOException("background disconnect"));
                disconnect(); // disconnect now, else would be queued until UI re-attached
            }
        };
    }

    @SuppressLint("MissingPermission")
    String getName(){
        return device.getName() != null ? device.getName() : device.getAddress();
    }

    @Override
    public void run() {
        //1. Create Bluetooth socket with device
        try{
            //Permission required is BLUETOOTH_CONNECT
            socket = device.createRfcommSocketToServiceRecord(BLUETOOTH_SPP);
            socket.connect();
            if(listener != null)
                listener.onSerialConnect();
        } catch (IOException | SecurityException e){
            if(listener != null){
                listener.onSerialConnectError(e);
            }
            Log.e(TAG, "socket.connect() failed!, message: " + e.getMessage());
            try{
                socket.close();
            } catch (Exception e1){
                Log.e(TAG, "socket.close() failed!, message: " + e1.getMessage());
            }
        }
        //2. Receiving messages from device
        connected = true;
        try{
            byte[] buffer = new byte[1024];
            int len;
            //noinspection InfiniteLoopStatement
            while(true){
                len = socket.getInputStream().read(buffer);
                byte[] data = Arrays.copyOf(buffer, len);
                if(listener != null){
                    listener.onSerialRead(data);
                }
            }
        } catch (Exception e){
            connected = false;
            if(listener != null){
                listener.onSerialIoError(e);
            }
            try {
                socket.close();
            } catch (Exception e1){
                Log.e(TAG, "socket.close() failed!, message: " + e1.getMessage());
            }
            socket = null;
        }
    }

    /**
     * connect-success and most connect-errors are returned asynchronously to listener
     */
    void connect(SerialListener listener) throws IOException {
        this.listener = listener;
        context.registerReceiver(disconnectBroadcastReceiver, new IntentFilter(Constants.INTENT_ACTION_DISCONNECT));
        //TODO: https://jenkov.com/tutorials/java-util-concurrent/executorservice.html
        Executors.newSingleThreadExecutor().submit(this);
    }

    void write(byte[] data) throws IOException {
        if (!connected)
            throw new IOException("not connected");
        socket.getOutputStream().write(data);
    }

    void disconnect() {
        listener = null; // ignore remaining data and errors
        // connected = false; // run loop will reset connected
        if(socket != null) {
            try {
                socket.close();
            } catch (Exception ignored) {
            }
            socket = null;
        }
        try {
            context.unregisterReceiver(disconnectBroadcastReceiver);
        } catch (Exception ignored) {
        }
    }
}
