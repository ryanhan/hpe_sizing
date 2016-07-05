package cn.ryanman.app.spnotification.utils;

import android.content.Context;
import android.os.AsyncTask;

import cn.ryanman.app.spnotification.dao.EmailDao;
import cn.ryanman.app.spnotification.dao.SizingEmailDao;
import cn.ryanman.app.spnotification.listener.OnDataFinishedListener;

public class MailAsyncTask extends AsyncTask<Void, Integer, Integer> {

    private Context context;
    private EmailDao emailDao;
    private OnDataFinishedListener onDataFinishedListener;

    public MailAsyncTask(Context context) {
        this.context = context;
        this.emailDao = new SizingEmailDao(context);
    }

    public void setOnDataFinishedListener(
            OnDataFinishedListener onDataFinishedListener) {
        this.onDataFinishedListener = onDataFinishedListener;
    }

    @Override
    protected Integer doInBackground(Void... params) {
        try {
            return emailDao.getEmail();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    protected void onPostExecute(Integer result) {
        if (result != -1) {
            if (onDataFinishedListener != null) {
                onDataFinishedListener.onDataSuccessfully(result);
            }
        } else {
            if (onDataFinishedListener != null) {
                onDataFinishedListener.onDataFailed();
            }
        }
    }

}

