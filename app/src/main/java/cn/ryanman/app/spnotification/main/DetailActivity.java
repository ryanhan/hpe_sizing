package cn.ryanman.app.spnotification.main;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import cn.ryanman.app.spnotification.R;
import cn.ryanman.app.spnotification.model.Item;
import cn.ryanman.app.spnotification.model.Request;
import cn.ryanman.app.spnotification.utils.AppUtils;
import cn.ryanman.app.spnotification.utils.DatabaseUtils;
import cn.ryanman.app.spnotification.utils.Value;

public class DetailActivity extends Activity {

    private String id;
    private Button assign;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Bundle bundle = getIntent().getExtras();
        id = bundle.getString(Value.PPMID);
        ActionBar actionBar = this.getActionBar();
        actionBar.setTitle("PPMID: " + id);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayUseLogoEnabled(false);

        DetailAsyncTask task = new DetailAsyncTask();
        task.execute(id);
    }

    private void setContent(final Request request) {
        TextView ppmid = (TextView) findViewById(R.id.detail_ppmid);
        TextView oldPpmid = (TextView) findViewById(R.id.detail_old_ppmid);
        TextView planningCycle = (TextView) findViewById(R.id.detail_planning_cycle);
        TextView oldPlanningCycle = (TextView) findViewById(R.id.detail_old_planning_cycle);
        TextView projectName = (TextView) findViewById(R.id.detail_project_name);
        TextView oldProjectName = (TextView) findViewById(R.id.detail_old_project_name);
        TextView contact = (TextView) findViewById(R.id.detail_contact);
        TextView oldContact = (TextView) findViewById(R.id.detail_old_contact);
        TextView comments = (TextView) findViewById(R.id.detail_comments);
        TextView oldComments = (TextView) findViewById(R.id.detail_old_comments);
        TextView teams = (TextView) findViewById(R.id.detail_teams);
        TextView oldTeams = (TextView) findViewById(R.id.detail_old_teams);
        TextView complete = (TextView) findViewById(R.id.detail_complete);
        TextView oldComplete = (TextView) findViewById(R.id.detail_old_complete);
        TextView status = (TextView) findViewById(R.id.detail_status);
        TextView oldStatus = (TextView) findViewById(R.id.detail_old_status);
        TextView lastmodify = (TextView) findViewById(R.id.detail_last_modify);

        assign = (Button) findViewById(R.id.button_assign);
        Button share = (Button) findViewById(R.id.button_share);

        if (request.getPpmid() != null) {
            ppmid.setText(request.getPpmid());
        }
        if (request.getOldPpmid() != null) {
            oldPpmid.setText(request.getOldPpmid());
            oldPpmid.setVisibility(View.VISIBLE);
            oldPpmid.getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG);
            oldPpmid.getPaint().setAntiAlias(true);
        }
        setTextView(planningCycle, oldPlanningCycle, request.getPlanningCycle());
        setTextView(projectName, oldProjectName, request.getProjectName());
        setTextView(contact, oldContact, request.getContact());
        if (request.getComments() != null) {
            setTextView(comments, oldComments, request.getComments().replaceAll(Value.BR, "\n"));
        }
        setTextView(teams, oldTeams, request.getTeams());
        setTextView(complete, oldComplete, request.getComplete());
        setTextView(status, oldStatus, request.getPpmStatus());
        lastmodify.setText(request.getLastmodified());

        if (request.isAssigned()) {
            assign.setText(request.getResource());
        } else {
            assign.setText(getString(R.string.assign));
        }

        assign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(DetailActivity.this).setTitle(getResources().getString(R.string.assign_to)).setItems(Value.RESOURCES, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DatabaseUtils.updateAssginedTo(DetailActivity.this, id, Value.RESOURCES[which]);
                        assign.setText(Value.RESOURCES[which]);
                    }
                }).show();
            }
        });

        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppUtils.shareWeixin(DetailActivity.this, request);
            }
        });

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setTextView(TextView newTextView, TextView oldTextView, String value) {
        if (value != null) {
            if (value.contains(Value.SPLIT)) {
                Item item = AppUtils.parseEdited(value);
                if (item != null) {
                    if (item.getOldValue() != null && !item.getOldValue().equals("")) {
                        oldTextView.setText(item.getOldValue());
                        oldTextView.setVisibility(View.VISIBLE);
                        oldTextView.getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG);
                        oldTextView.getPaint().setAntiAlias(true);
                    }
                    if (item.getNewValue() != null && !item.getNewValue().equals("")) {
                        newTextView.setText(item.getNewValue());
                        newTextView.setTextColor(getResources().getColor(R.color.light_blue));
                        newTextView.setTypeface(null, Typeface.BOLD);
                    }
                }
            } else {
                newTextView.setText(value);
            }
        }
    }

    private class DetailAsyncTask extends AsyncTask<String, Integer, Request> {

        @Override
        protected Request doInBackground(String... params) {
            return DatabaseUtils.getRequestByPpmid(DetailActivity.this, params[0]);
        }

        @Override
        protected void onPostExecute(Request result) {
            if (result != null) {
                setContent(result);
            }
        }
    }
}
