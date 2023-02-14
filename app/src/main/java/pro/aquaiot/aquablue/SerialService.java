package pro.aquaiot.aquablue;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.ArrayDeque;

import pro.aquaiot.aquablue.constants.Constants;

/**
 * create notification and queue serial data while activity is not in the foreground
 * use listener chain: SerialSocket -> SerialService -> UI fragment
 */
public class SerialService extends Service implements SerialListener {

    class SerialBinder extends Binder{
        SerialService getService() {return SerialService.this;}
    }

    private enum QueueType {Connect, ConnectError, Read, IoError}

    private static class QueueItem {
        QueueType type;
        ArrayDeque<byte[]> datas;
        Exception e;
        QueueItem(QueueType type){
            this.type = type;
            if(type == QueueType.Read)
                init();
        }
        QueueItem(QueueType type, Exception e){
            this.type = type;
            this.e = e;
        }
        QueueItem(QueueType type, ArrayDeque<byte[]> datas){
            this.type = type;
            this.datas = datas;
        }

        void init() { datas = new ArrayDeque<>(); }
        void add(byte[] data){datas.add(data);}
    }
    private static final String TAG = "SerialService";
    private final Handler mainLooper;
    private final IBinder binder;
    //Why he need up to 2 queue ?
    private final ArrayDeque<QueueItem> queue1, queue2;
    private final QueueItem lastRead;

    private SerialSocket socket;
    private SerialListener listener;
    private boolean connected;

    /**
     * Life cylce
     */
    public SerialService(){
        mainLooper = new Handler(Looper.getMainLooper());
        binder = new SerialBinder();
        queue1 = new ArrayDeque<>();
        queue2 = new ArrayDeque<>();
        lastRead = new QueueItem(QueueType.Read);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /**
     * Service Api
     */
    public void connect(SerialSocket socket) throws IOException{
        //TODO: Listener is a Service which listening to bluetooth
        //Listener manages the state of the system ?, socket send the state by calling function
        //in the interface
        socket.connect(this);
        this.socket = socket;
        connected = true;
    }

    public void disconnect(){
        connected = false;
        cancelNotification();
        if(socket != null){
            socket.disconnect();
            socket = null;
        }
    }

    public void write(byte[] data) throws IOException{
        if(!connected){
            Log.e(TAG, "write() error! Bluetooth not connected");
            throw new IOException("Not connected");
        }
        socket.write(data);
    }

    /**
     * This function is called when the Terminal Fragment firstly created
     * @param listener : service that attach to the fragment, which is itself
     */
    public void attach(SerialListener listener){
        if(Looper.getMainLooper().getThread() != Thread.currentThread()){
            Log.e(TAG, "attach() error! Not in main thread");
            throw new IllegalArgumentException("Not in main thread");
        }
        cancelNotification();
        // use synchronized() to prevent new items in queue2
        // new items will not be added to queue1 because mainLooper.post and attach() run in main thread
        synchronized (this){
            this.listener = listener;
        }
        for (QueueItem item : queue1){
            switch (item.type){
                case Connect:       listener.onSerialConnect        (); break;
                case ConnectError:  listener.onSerialConnectError   (item.e); break;
                case Read:          listener.onSerialRead           (item.datas); break;
                case IoError:       listener.onSerialIoError        (item.e); break;
            }
        }
        for (QueueItem item : queue2){
            switch (item.type){
                case Connect:       listener.onSerialConnect        (); break;
                case ConnectError:  listener.onSerialConnectError   (item.e); break;
                case Read:          listener.onSerialRead           (item.datas); break;
                case IoError:       listener.onSerialIoError        (item.e); break;
            }
        }
        queue1.clear();
        queue2.clear();
    }

    public void detach(){
        if(connected)
            createNotification();
        // items already in event queue (posted before detach() to mainLooper) will end up in queue1
        // items occurring later, will be moved directly to queue2
        // detach() and mainLooper.post run in the main thread, so all items are caught
        listener = null;
    }

    private void createNotification() {
        //Apply with android version higher android version 8 (Oreo)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel nc = new NotificationChannel(Constants.NOTIFICATION_CHANNEL,
                    "Background service",
                    NotificationManager.IMPORTANCE_LOW);
            nc.setShowBadge(false);
            NotificationManager mn = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mn.createNotificationChannel(nc);
        }
        //TODO: what do these intent look like in the app ?
        Intent disconnectIntent = new Intent()
                .setAction(Constants.INTENT_ACTION_DISCONNECT);

        Intent restartIntent = new Intent()
                .setClassName(this, Constants.INTENT_CLASS_MAIN_ACTIVITY)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER);

        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent disconnectPendingIntent = PendingIntent.getBroadcast(this, 1, disconnectIntent, flags);
        PendingIntent restartPendingIntent = PendingIntent.getActivity(this, 1, restartIntent,  flags);
        //Tạo notification từ service đến thiết bị
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary))
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText(socket != null ? "Connected to "+socket.getName() : "Background Service")
                .setContentIntent(restartPendingIntent)
                .setOngoing(true)
                .addAction(new NotificationCompat.Action(R.drawable.ic_clear_white_24dp, "Disconnect", disconnectPendingIntent));
        // @drawable/ic_notification created with Android Studio -> New -> Image Asset using @color/colorPrimaryDark as background color
        // Android < API 21 does not support vectorDrawables in notifications, so both drawables used here, are created as .png instead of .xml
        Notification notification = builder.build();
        startForeground(Constants.NOTIFY_MANAGER_START_FOREGROUND_SERVICE, notification);
    }

    private void cancelNotification() {
        stopForeground(true);
    }

    /**
     * SerialListener
     */
    public void onSerialConnect(){
        if(connected){
            synchronized (this){
                if(listener != null) {
                    mainLooper.post(()->{
                        //TODO: wait until listener == null then add ?
                        if(listener != null){
                            listener.onSerialConnect();
                        } else {
                            queue1.add(new QueueItem(QueueType.Connect));
                        }
                    });
                } else {
                    queue2.add(new QueueItem(QueueType.Connect));
                }
            }
        }
    }

    @Override
    public void onSerialConnectError(Exception e) {
        if(connected){
            synchronized (this){
                if(listener != null){
                    mainLooper.post(()->{
                       if(listener != null){
                           listener.onSerialConnectError(e);
                       } else {
                           queue1.add(new QueueItem(QueueType.ConnectError, e));
                           disconnect();
                       }
                    });
                } else {
                    queue2.add(new QueueItem(QueueType.ConnectError, e));
                    disconnect();
                }
            }
        }
    }

    /**
     * Why we need to create this function ???
     * @param datas
     */
    @Override
    public void onSerialRead(ArrayDeque<byte[]> datas) {
        throw new UnsupportedOperationException();
    }

    /**
     * reduce number of UI updates by merging data chunks.
     * Data can arrive at hundred chunks per second, but the UI can only
     * perform a dozen updates if receiveText already contains much text.
     *
     * On new data inform UI thread once (1).
     * While not consumed (2), add more data (3).
     */
    @Override
    public void onSerialRead(byte[] data) {
        if(connected){
            synchronized (this){
                if(listener!=null){
                    boolean first;
                    synchronized (lastRead){
                        first = lastRead.datas.isEmpty(); // (1)
                        lastRead.add(data); // (3)
                    }
                    if(first){
                        mainLooper.post(()->{
                            ArrayDeque<byte[]> datas;
                            synchronized (lastRead){
                                datas = lastRead.datas;
                                lastRead.init(); //(2). Empty last item read
                            }
                            if(listener != null){
                                listener.onSerialRead(datas);
                            } else {
                                queue1.add(new QueueItem(QueueType.Read, datas));
                            }
                        });
                    }
                } else {
                    if(queue2.isEmpty() || queue2.getLast().type != QueueType.Read){
                        queue2.add(new QueueItem(QueueType.Read));
                    }
                    queue2.getLast().add(data);
                }
            }
        }
    }

    @Override
    public void onSerialIoError(Exception e) {
        if(connected){
            synchronized (this){
                if(listener != null){
                    mainLooper.post(()->{
                       if(listener != null){
                           listener.onSerialIoError(e);
                       } else {
                           queue1.add(new QueueItem(QueueType.IoError, e));
                           disconnect();
                       }
                    });
                } else {
                    queue2.add(new QueueItem(QueueType.IoError, e));
                    disconnect();
                }
            }
        }
    }
}
