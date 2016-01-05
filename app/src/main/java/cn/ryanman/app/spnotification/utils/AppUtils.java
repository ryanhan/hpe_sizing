package cn.ryanman.app.spnotification.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Calendar;

import cn.ryanman.app.spnotification.model.AppInfo;
import cn.ryanman.app.spnotification.model.Item;
import cn.ryanman.app.spnotification.model.Request;

public class AppUtils {

    public static void startPollingService(Context context, int seconds, Class<?> cls) {
        //获取AlarmManager系统服务
        AlarmManager manager = (AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);

        //包装需要执行Service的Intent
        Intent intent = new Intent(context, cls);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0,
                intent, 0);
        Calendar cal = Calendar.getInstance();
        //使用AlarmManger的setRepeating方法设置定期执行的时间间隔（seconds秒）和需要执行的Service
        manager.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
                seconds * 1000, pendingIntent);
    }

    //停止轮询服务
    public static void stopPollingService(Context context, Class<?> cls) {
        AlarmManager manager = (AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, cls);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0,
                intent, 0);
        //取消正在执行的服务
        manager.cancel(pendingIntent);
    }

    public static void restartPollingService(Context context, int seconds, Class<?> cls) {
        stopPollingService(context, cls);
        startPollingService(context, seconds, cls);
    }

    public static void shareWeixin(Context context, Request request) {
        StringBuffer sb = new StringBuffer();
        sb.append("The following project is assigned to you!\n").append("PPMID: ").append(request.getPpmid())
                .append("\nPlanning Cycle: ").append(request.getPlanningCycle())
                .append("\nProject Name: ").append(request.getProjectName()).append("\n")
                .append("Please reply after received!");


        Intent intent = new Intent();
        ComponentName comp = new ComponentName("com.tencent.mm", "com.tencent.mm.ui.tools.ShareImgUI");
        intent.setComponent(comp);
        intent.setAction("android.intent.action.SEND");
        intent.setType("text/plain");
        intent.setFlags(0x3000001);
        intent.putExtra(intent.EXTRA_TEXT, sb.toString());
        context.startActivity(intent);
    }

    public static Item parseEdited(String edited) {
        String[] split = edited.split(Value.SPLIT);
        Item item = new Item();
        if (split.length > 1) {
            for (int i = 1; i < split.length; i++) {
                if (split[i].startsWith(Value.OLDEDITED)) {
                    item.setOldValue(split[i].replaceAll(Value.OLDEDITED, ""));
                } else {
                    item.setNewValue(split[i]);
                }
            }
            return item;
        } else {
            return null;
        }
    }

    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetworkInfo = connectivityManager
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetworkInfo.isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    public static AppInfo getAppInfo() throws Exception {

        AppInfo appInfo = new AppInfo();
        URL url = new URL(Value.FIR_IM_URL + Value.FIR_IM_ID + "?api_token=" + Value.FIR_IM_TOKEN);

        String result = GetJson(url);

        JSONObject json = new JSONObject(result);
        appInfo.setVersion(json.getString("version"));
        appInfo.setVersionShort(json.getString("versionShort"));
        appInfo.setUrl(json.getString("installUrl"));

        return appInfo;
    }


    private static String GetJson(URL url) throws IOException {

        HttpURLConnection urlConn = null;
        try {
            urlConn = (HttpURLConnection) url.openConnection();
            urlConn.addRequestProperty("accept", "application/json");
            urlConn.setConnectTimeout(Value.CONNECT_TIMEOUT);
            urlConn.setReadTimeout(Value.READ_TIMEOUT);
            int responseCode = urlConn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return readInputStream(urlConn);
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new IOException(e);
        } finally {
            if (urlConn != null) {
                urlConn.disconnect();
            }
        }
    }

    private static String readInputStream(HttpURLConnection urlConn)
            throws IOException {
        Charset charset = Charset.forName("UTF-8");
        InputStreamReader stream = new InputStreamReader(
                urlConn.getInputStream(), charset);
        BufferedReader reader = new BufferedReader(stream);
        StringBuffer responseBuffer = new StringBuffer();
        String read = "";
        while ((read = reader.readLine()) != null) {
            responseBuffer.append(read);
        }
        return responseBuffer.toString();
    }
}
