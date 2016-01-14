package cn.ryanman.app.spnotification.main;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.util.Date;
import java.util.HashMap;

import cn.ryanman.app.spnotification.R;
import cn.ryanman.app.spnotification.dao.EmailDao;
import cn.ryanman.app.spnotification.utils.AppUtils;
import cn.ryanman.app.spnotification.utils.Value;

public class GetEmailService extends IntentService {

    private final IBinder mBinder;
    private EmailDao emailDao;
    private HashMap<String, Integer> emailTypeMap;

    public static final int COMMAND_TOTAL_SIZE = 0;
    public static final int COMMAND_PROGRESS_UPDATE = 1;
    public static final int COMMAND_SUCCESS = 2;
    public static final int COMMAND_FAIL = 3;


    public GetEmailService() {
        super("GetEmailService");
        mBinder = new GetEmailBinder();
        emailTypeMap = new HashMap<String, Integer>();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("SPNotification", "Service binded.");
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("SPNotification", "Service Created.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("SPNotification", "Service Started.");
        String emailDaoName = intent.getStringExtra(Value.EMAILDAO);
        emailTypeMap.put(emailDaoName, 1);
        try {
            Class<?> cls = Class.forName(Value.PACKAGENAME + ".dao." + emailDaoName);
            Class[] paramTypes = {Context.class};
            Object[] params = {GetEmailService.this};
            Constructor con = cls.getConstructor(paramTypes);
            emailDao = (EmailDao) con.newInstance(params);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (emailDao == null) {
            return;
        }
        String emailDaoName = intent.getStringExtra(Value.EMAILDAO);
        if (emailTypeMap.containsKey(emailDaoName)) {
            Log.d("SPNotification", "Service Start to Get Email!");
            try {
                int result = emailDao.getEmail();
                SharedPreferences pref = getSharedPreferences(Value.APPINFO, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = pref.edit();
                editor.putString(Value.UPDATETIME, AppUtils.formatDate(new Date()));
                editor.commit();
                if (result > 0) {
                    showNotification(result);
                }
                Log.d("SPNotification", "Get Email Completed!");

            } catch (Exception e) {
                e.printStackTrace();
                Log.d("SPNotification", "Get Email Failed");
            } finally {
                emailTypeMap.remove(intent.getStringExtra(Value.EMAILDAO));
            }
        } else {
            Log.d("SPNotification", Value.EMAILDAO + " Service Already Finished!");
        }
    }

    public boolean isGettingEmail(String emailDaoName) {
        if (emailTypeMap == null){
            return false;
        }
        if (emailTypeMap.containsKey(emailDaoName)){
            return true;
        }
        else {
            return false;
        }
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
