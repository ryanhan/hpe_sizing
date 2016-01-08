package cn.ryanman.app.spnotification.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;

import cn.ryanman.app.spnotification.listener.OnDataFinishedListener;
import cn.ryanman.app.spnotification.model.Request;

public class MailAsyncTask extends AsyncTask<Void, Integer, Integer> {

    private String protocol = "imap";
    private String username = "gis_hpe@yahoo.com";
    private String password = "gis123456";

    private Context context;
    private SharedPreferences pref;
    private OnDataFinishedListener onDataFinishedListener;

    public MailAsyncTask(Context context) {
        this.context = context;
        this.pref = context.getSharedPreferences(Value.APPINFO, Context.MODE_PRIVATE);
    }

    public void setOnDataFinishedListener(
            OnDataFinishedListener onDataFinishedListener) {
        this.onDataFinishedListener = onDataFinishedListener;
    }

    @Override
    protected Integer doInBackground(Void... params) {
        try {
            return getEmail();
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

    private int getEmail() throws Exception {

        Properties props = System.getProperties();
        props.setProperty("mail.store.protocol", protocol);
        props.setProperty("mail.imap.host", "imap.mail.yahoo.com");
        props.setProperty("mail.imap.port", "993");
        props.put("mail.imap.ssl.enable", true);

        Session session = Session.getInstance(props);

        Store store = session.getStore(protocol);
        store.connect(username, password);
        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_WRITE);

        String lastEmailTime = pref.getString(Value.LASTEMAILTIME, null);
        Message messages[] = inbox.getMessages();
        if (lastEmailTime != null) {
            Date lastDate = AppUtils.parseDate(lastEmailTime);
            for (int i = messages.length - 1; i >= 0; i--) {
                Date date = messages[i].getSentDate();
                if (lastDate.getTime() >= date.getTime()) {
                    messages = Arrays.copyOfRange(messages, i + 1, messages.length);
                    break;
                    //i + 1 to messages.length - 1;
                }
            }
        }

        //Message messages[] = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
        Log.d("SPNotification", "收件箱中共" + messages.length + "封未读邮件!");

        if (messages.length == 0) {
            return 0;
        }

        List<Request> requests = EmailUtils.parseEmailBody(messages);
        if (requests != null) {
            DatabaseUtils.addRecords(context, requests);
        }
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(Value.LASTEMAILTIME, AppUtils.formatDate(messages[messages.length].getSentDate()));
        editor.commit();
        inbox.close(false);
        store.close();
        return requests.size();
    }


}

