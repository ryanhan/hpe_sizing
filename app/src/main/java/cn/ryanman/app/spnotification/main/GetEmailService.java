package cn.ryanman.app.spnotification.main;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;

import java.util.Date;

import cn.ryanman.app.spnotification.R;
import cn.ryanman.app.spnotification.listener.OnDataFinishedListener;
import cn.ryanman.app.spnotification.listener.OnServiceCompletedListener;
import cn.ryanman.app.spnotification.utils.AppUtils;
import cn.ryanman.app.spnotification.utils.MailAsyncTask;
import cn.ryanman.app.spnotification.utils.Value;

public class GetEmailService extends Service {

    private final IBinder mBinder;
    private OnServiceCompletedListener onServiceCompletedListener;
    private boolean gettingEmail;

    public GetEmailService() {
        super();
        mBinder = new GetEmailBinder();
        gettingEmail = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("SPNotification", "Service binded.");
        return mBinder;
    }

    public void setOnServiceCompletedListener(
            OnServiceCompletedListener onServiceCompletedListener) {
        this.onServiceCompletedListener = onServiceCompletedListener;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("SPNotification", "Service Created.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("SPNotification", "Service Started.");
        if (!gettingEmail) {
            gettingEmail = true;
            MailAsyncTask task = new MailAsyncTask(GetEmailService.this);
            task.setOnDataFinishedListener(new OnDataFinishedListener() {
                @Override
                public void onDataSuccessfully(Object data) {
                    int number = (Integer) data;
                    SharedPreferences pref = getSharedPreferences(Value.APPINFO, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString(Value.UPDATETIME, AppUtils.formatDate(new Date()));
                    editor.commit();
                    if (onServiceCompletedListener != null) {
                        onServiceCompletedListener.onDataSuccessfully();
                    }
                    if (number > 0) {
                        showNotification(number);
                    }
                    Log.d("SPNotification", "Get Email Completed!");
                }

                @Override
                public void onDataFailed() {
                    if (onServiceCompletedListener != null){
                        onServiceCompletedListener.onDataFailed();
                    }
                    Log.d("SPNotification", "failed");
                }
            });
            gettingEmail = false;
            task.execute();
            Log.d("SPNotification", "Service Start to Get Email!");
        }

        return super.onStartCommand(intent, flags, startId);
    }

    public boolean isGettingEmail() {
        return gettingEmail;
    }

    private void showNotification(int number) {
        NotificationManager notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Builder mBuilder = new Builder(this);
        mBuilder.setContentTitle(getString(R.string.app_name)).setSmallIcon(R.mipmap.ic_launcher)
                .setContentText("Sizing Library has " + number + " new changes!").setOngoing(false);
        notifyManager.notify(1, mBuilder.build());
    }


    public class GetEmailBinder extends Binder {

        public GetEmailService getService() {
            return GetEmailService.this;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("SPNotification", "Service Stopped.");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("SPNotification", "Service unbinded.");
        return super.onUnbind(intent);
    }
}
