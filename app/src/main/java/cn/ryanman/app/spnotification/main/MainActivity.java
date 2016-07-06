package cn.ryanman.app.spnotification.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import cn.ryanman.app.spnotification.R;
import cn.ryanman.app.spnotification.customview.XListView;
import cn.ryanman.app.spnotification.customview.XListView.IXListViewListener;
import cn.ryanman.app.spnotification.dao.DatabaseDao;
import cn.ryanman.app.spnotification.dao.SizingDatabaseDao;
import cn.ryanman.app.spnotification.model.Request;
import cn.ryanman.app.spnotification.utils.AppUtils;
import cn.ryanman.app.spnotification.utils.Value;

public class MainActivity extends Activity implements IXListViewListener {

    private GetEmailService getEmailService;
    private SizingListAdapter adapter;
    private List<Request> requests;
    private boolean isRefreshing;
    private XListView sizingListView;
    private SharedPreferences pref;
    private boolean isShowMarked;
    private LinearLayout loadingLayout;
    private DatabaseDao sizingDatabaseDao;
    private EmailReceiver receiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sizingDatabaseDao = new SizingDatabaseDao();
        loadingLayout = (LinearLayout) findViewById(R.id.loading_layout);
        sizingListView = (XListView) findViewById(R.id.sizing_listview);
        pref = this.getSharedPreferences(Value.APPINFO, Context.MODE_PRIVATE);
        requests = new ArrayList<Request>();
        isRefreshing = false;
        isShowMarked = false;
        getActionBar().setDisplayShowHomeEnabled(false);
        adapter = new SizingListAdapter(this, requests, sizingDatabaseDao);
        sizingListView.setAdapter(adapter);
        sizingListView.setPullRefreshEnable(true);
        sizingListView.setPullLoadEnable(false);
        sizingListView.setXListViewListener(this);
        sizingListView.setRefreshTime(pref.getString(Value.UPDATETIME, getString(R.string.never)));
        sizingListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final int index = (int) id;
                if (!requests.get(index).isRead()) {
                    sizingDatabaseDao.updateRequestRead(MainActivity.this, requests.get(index).getPpmid(), Request.READ);
                }
                Intent intent = new Intent();
                intent.setClass(MainActivity.this,
                        DetailActivity.class);
                intent.putExtra(Value.PPMID, requests.get(index)
                        .getPpmid());
                startActivity(intent);
            }
        });

        sizingListView.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {

                final int index = (int) id;
                final String ppmid = requests.get(index).getPpmid();
                //int status = requests.get(index).getStatus();
                String title = null;
                String[] items = null;
                if (!requests.get(index).isAssigned()) {
                    title = getString(R.string.not_assigned);
                    items = new String[2];
                    items[0] = getString(R.string.assign);
                    items[1] = getString(R.string.share);
                } else {
                    title = requests.get(index).getResource();
                    items = new String[3];
                    items[0] = getString(R.string.change_assign);
                    items[1] = getString(R.string.remove_assign);
                    items[2] = getString(R.string.share);
                }
                final int itemsCount = items.length;

                new AlertDialog.Builder(MainActivity.this).setTitle(title).setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                new AlertDialog.Builder(MainActivity.this).setTitle(getString(R.string.assign_to)).setItems(Value.RESOURCES, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        sizingDatabaseDao.updateAssginedTo(MainActivity.this, ppmid, Value.RESOURCES[which]);
                                        updateList();
                                    }
                                }).show();

                                break;
                            case 1:
                                if (itemsCount == 2) {
                                    AppUtils.shareWeixin(MainActivity.this, requests.get(index));
                                    break;
                                } else if (itemsCount == 3) {
                                    sizingDatabaseDao.removeAssignee(MainActivity.this, ppmid);
                                    updateList();
                                }
                                break;
                            case 2:
                                AppUtils.shareWeixin(MainActivity.this, requests.get(index));
                                break;
                        }
                    }
                }).show();
                return true;
            }
        });

        bindService();
    }

    private void bindService() {
        Intent intent = new Intent(this,
                GetEmailService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("SPNotification", "Service Disconnect!");
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            getEmailService = ((GetEmailService.GetEmailBinder) service)
                    .getService();
            registerReceiver();
            Log.d("SPNotification", "Service Connect!");
        }
    };

    private void registerReceiver() {
        receiver = new EmailReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Value.EMAILRECEIVER);
        registerReceiver(receiver, intentFilter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.show_marked:
                //MenuItem menuItem = (MenuItem) findViewById(R.id.show_marked);
                if (!isShowMarked) {
                    isShowMarked = true;
                    item.setTitle(R.string.menu_show_all);
                } else {
                    isShowMarked = false;
                    item.setTitle(R.string.menu_show_marked);
                }
                updateList();
                break;
            case R.id.start_service:
                if (!getEmailService.isGettingEmail()) {
                    AppUtils.restartPollingService(MainActivity.this, Value.INTERVAL, GetEmailService.class);
                }
                Toast.makeText(this, getString(R.string.start_service), Toast.LENGTH_SHORT).show();
                break;
            case R.id.stop_service:
                AppUtils.stopPollingService(MainActivity.this, GetEmailService.class);
                Toast.makeText(this, getString(R.string.stop_service), Toast.LENGTH_SHORT).show();
                updateList();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pref.getBoolean(Value.FIRST, true)) {
            Log.d("SPNotification", "First Login");
            AppUtils.startPollingService(this, Value.INTERVAL, GetEmailService.class);
            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean(Value.FIRST, false);
            editor.commit();
            loadingLayout.setVisibility(View.VISIBLE);
            sizingListView.setVisibility(View.INVISIBLE);
        } else {
            updateList();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(conn);
        unregisterReceiver(receiver);
    }

    @Override
    public void onRefresh() {
        if (!isRefreshing) {
            //updateList();
            if (!getEmailService.isGettingEmail()) {
                AppUtils.restartPollingService(MainActivity.this, Value.INTERVAL, GetEmailService.class);
                Log.d("SPNotification", "Refresh List Started.");
            }
            isRefreshing = true;
        }
    }

    @Override
    public void onLoadMore() {

    }

    private void stopRefresh() {
        sizingListView.stopRefresh();
        sizingListView.setRefreshTime(pref.getString(Value.UPDATETIME, getString(R.string.never)));
        isRefreshing = false;
        Log.d("SPNotification", "Refresh List Finished.");
    }

    private void updateList() {
        SizingListAsyncTask task = new SizingListAsyncTask();
        task.execute(isShowMarked);
    }

    private class SizingListAsyncTask extends AsyncTask<Boolean, Integer, List<Request>> {

        @Override
        protected List<Request> doInBackground(Boolean... params) {
            boolean isShowMarked = params[0];
            if (isShowMarked) {
                return sizingDatabaseDao.getMarkedRequests(MainActivity.this);
            } else {
                return sizingDatabaseDao.getAllRequests(MainActivity.this);
            }
        }

        @Override
        protected void onPostExecute(List<Request> result) {
            if (result != null) {
                requests.clear();
                requests.addAll(result);
                adapter.notifyDataSetChanged();
                loadingLayout.setVisibility(View.GONE);
                sizingListView.setVisibility(View.VISIBLE);
            }
        }
    }

    private class EmailReceiver extends BroadcastReceiver {


        @Override
        public void onReceive(Context context, Intent intent) {
            String command = intent.getStringExtra(Value.COMMAND);
            if (command.equals(Value.EMAIL_SUCCESS) || command.equals(Value.EMAIL_FAILED)) {
                updateList();
                stopRefresh();
            }

        }
    }

}
