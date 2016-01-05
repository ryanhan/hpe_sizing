package cn.ryanman.app.spnotification.utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat.Builder;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import cn.ryanman.app.spnotification.R;
import cn.ryanman.app.spnotification.model.AppInfo;

/**
 * Created by Ryan on 2015/12/29.
 */
public class CheckUpdateAsyncTask extends AsyncTask<Void, Integer, AppInfo> {

    private Context context;
    private boolean isActive;
    private NotificationManager notifyManager;
    private Builder mBuilder;

    public CheckUpdateAsyncTask(Context context, boolean isActive) {
        this.context = context;
        this.isActive = isActive;
    }

    @Override
    protected void onPreExecute() {
        if (isActive) {
            Toast.makeText(context, context.getString(R.string.checking_update), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected AppInfo doInBackground(Void... params) {
        try {
            AppInfo appInfo = AppUtils.getAppInfo();
            return appInfo;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPostExecute(AppInfo result) {
        if (result == null) {
            if (isActive) {
                Toast.makeText(context, context.getString(R.string.error_retry), Toast.LENGTH_SHORT)
                        .show();
            }
        } else {
            try {
                PackageManager packageManager = context.getPackageManager();
                PackageInfo packInfo = packageManager.getPackageInfo(
                        context.getPackageName(), 0);
                int currentVersion = packInfo.versionCode;
                int latestVersion = Integer.parseInt(result.getVersion());

                System.out.println("Current Version: " + currentVersion);
                System.out.println("Latest Version: " + latestVersion);

                if (currentVersion < latestVersion) {
                    confirmDownload(result.getUrl(), result.getVersionShort());
                } else {
                    if (isActive) {
                        Toast.makeText(context, context.getString(R.string.already_latest_version), Toast.LENGTH_SHORT)
                                .show();
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                if (isActive) {
                    Toast.makeText(context, context.getString(R.string.error_retry), Toast.LENGTH_SHORT)
                            .show();
                }
            }

        }
    }

    private void confirmDownload(final String url, String version) {
        Dialog alertDialog = new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.has_new_version) +" " + version).setMessage(context.getString(R.string.confirm_to_download))
                .setNegativeButton(context.getString(R.string.cancel), null)
                .setPositiveButton(context.getString(R.string.ok), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startDownload(url);
                        Toast.makeText(context, context.getString(R.string.start_download), Toast.LENGTH_SHORT)
                                .show();
                    }

                }).create();
        alertDialog.show();
    }

    private void startDownload(String url) {
        notifyManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new Builder(context);
        mBuilder.setContentTitle(context.getString(R.string.app_name)).setContentText(context.getString(R.string.start_download))
                .setSmallIcon(R.mipmap.ic_launcher).setTicker(context.getString(R.string.start_download))
                .setOngoing(true);

        AppDownloaderAysncTask appDownloader = new AppDownloaderAysncTask();
        appDownloader.execute(url);
    }

    private class AppDownloaderAysncTask extends
            AsyncTask<String, Integer, File> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mBuilder.setProgress(100, 0, false);
            notifyManager.notify(1, mBuilder.build());
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            mBuilder.setContentText(context.getString(R.string.downloading)  +": ( " + values[0] + "% )");
            mBuilder.setProgress(100, values[0], false);
            notifyManager.notify(1, mBuilder.build());
            super.onProgressUpdate(values);
        }

        @Override
        protected File doInBackground(String... params) {
            HttpURLConnection connection = null;
            try {
                int count;

                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                int lenghtOfFile = connection.getContentLength();
                InputStream input = new BufferedInputStream(url.openStream());

                String fileName = "hpe-sizing.apk";
                File downloadDir = new File(context.getExternalCacheDir(),
                        Value.DOWNLOAD_DIRECTORY);
                if (!downloadDir.exists()) {
                    downloadDir.mkdir();
                }

                File filePath = new File(downloadDir, fileName);

                FileOutputStream fos = new FileOutputStream(filePath);

                byte data[] = new byte[1024];
                long total = 0;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    publishProgress((int) ((total * 100) / lenghtOfFile));
                    fos.write(data, 0, count);
                }
                fos.flush();
                fos.close();
                input.close();
                return filePath;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

        }

        @Override
        protected void onPostExecute(File result) {
            if (result == null) {
                mBuilder.setContentText(context.getString(R.string.download_fail));
                mBuilder.setProgress(0, 0, false);
                mBuilder.setOngoing(false);
                notifyManager.notify(1, mBuilder.build());
                Toast.makeText(context, context.getString(R.string.download_fail), Toast.LENGTH_SHORT).show();
            } else {
                mBuilder.setContentText(context.getString(R.string.download_success));
                mBuilder.setProgress(0, 0, false);
                mBuilder.setOngoing(false);

                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(result),
                        "application/vnd.android.package-archive");

                PendingIntent contentIntent = PendingIntent.getActivity(
                        context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                mBuilder.setContentIntent(contentIntent);

                notifyManager.notify(1, mBuilder.build());

                System.out.println(result.getPath());
                Toast.makeText(context, context.getString(R.string.download_success), Toast.LENGTH_SHORT).show();
                context.startActivity(intent);
            }
        }
    }
}
