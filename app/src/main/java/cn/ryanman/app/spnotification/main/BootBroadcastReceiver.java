package cn.ryanman.app.spnotification.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cn.ryanman.app.spnotification.utils.AppUtils;
import cn.ryanman.app.spnotification.utils.Value;

public class BootBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context,Intent intent){
        AppUtils.startPollingService(context, Value.INTERVAL, GetEmailService.class, Value.SIZINGEMAIL);
        Log.d("SPNotification", "开机自启动服务");
    }

}
