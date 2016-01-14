package cn.ryanman.app.spnotification.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import cn.ryanman.app.spnotification.R;
import cn.ryanman.app.spnotification.utils.AppUtils;
import cn.ryanman.app.spnotification.utils.CheckUpdateAsyncTask;

public class SplashActivity extends Activity {

    private final int SPLASH_DISPLAY_LENGHT = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppUtils.createDatabase(this);
        if (AppUtils.isWifiConnected(this)) {
            //Toast.makeText(this, "Wifi Connected", Toast.LENGTH_SHORT).show();
            CheckUpdateAsyncTask checkUpdateAysncTask = new CheckUpdateAsyncTask(this, false);
            checkUpdateAysncTask.execute();
        }
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this,
                        MainActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }

        }, SPLASH_DISPLAY_LENGHT);

    }
}
