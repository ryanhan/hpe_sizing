package cn.ryanman.app.spnotification.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import java.util.List;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;

import cn.ryanman.app.spnotification.listener.OnDataFinishedListener;
import cn.ryanman.app.spnotification.model.Request;

public class MailAsyncTask extends AsyncTask<Void, Integer, List<Request>> {

    private String protocol = "imap";
    private String username = "gis_hpe@yahoo.com";
    private String password = "gis123456";

    //private Context context;
    private SharedPreferences pref;
    private OnDataFinishedListener onDataFinishedListener;

    public MailAsyncTask(Context context) {
        //this.context = context;
        this.pref = context.getSharedPreferences(Value.APPINFO, Context.MODE_PRIVATE);
    }

    public void setOnDataFinishedListener(
            OnDataFinishedListener onDataFinishedListener) {
        this.onDataFinishedListener = onDataFinishedListener;
    }

    @Override
    protected List<Request> doInBackground(Void... params) {
        try {
            return getEmail();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(List<Request> result) {
        if (result != null) {
            if (onDataFinishedListener != null) {
                onDataFinishedListener.onDataSuccessfully(result);
            }
        } else {
            if (onDataFinishedListener != null) {
                onDataFinishedListener.onDataFailed();
            }
        }
    }

    private List<Request> getEmail() throws Exception {

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
            for (int i = messages.length - 1; i >= 0; i--) {
                messages[i].getSentDate();
            }
        }

        //Message messages[] = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
        Log.d("SPNotification", "收件箱中共" + messages.length + "封未读邮件!");

        List<Request> requests = EmailUtils.parseEmailBody(messages);

        inbox.close(false);
        store.close();
        return requests;
    }


}

