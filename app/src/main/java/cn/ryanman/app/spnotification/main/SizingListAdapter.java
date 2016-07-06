package cn.ryanman.app.spnotification.main;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import cn.ryanman.app.spnotification.R;
import cn.ryanman.app.spnotification.dao.DatabaseDao;
import cn.ryanman.app.spnotification.model.Item;
import cn.ryanman.app.spnotification.model.Request;
import cn.ryanman.app.spnotification.utils.AppUtils;
import cn.ryanman.app.spnotification.utils.Value;

public class SizingListAdapter extends ArrayAdapter<Request> {

    private Context context;
    private LayoutInflater mInflater;
    private DatabaseDao databaseDao;

    public final class ViewHolder {
        public LinearLayout layout;
        public TextView sizingBar;
        public TextView sizingPpmid;
        public TextView planningCycle;
        public TextView sizingProjectName;
        public TextView contact;
        public TextView status;
        public TextView assignee;
        public ImageView click;
    }

    public SizingListAdapter(Context context, List<Request> objects, DatabaseDao databaseDao) {
        super(context, 0, objects);
        mInflater = LayoutInflater.from(context);
        this.context = context;
        this.databaseDao = databaseDao;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        ViewHolder holder = null;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = mInflater.inflate(
                    R.layout.adapter_listview_sizing, null);

            holder.sizingBar = (TextView) convertView.findViewById(R.id.sizing_list_bar);
            holder.sizingPpmid = (TextView) convertView
                    .findViewById(R.id.sizing_list_ppmid);
            holder.planningCycle = (TextView) convertView
                    .findViewById(R.id.sizing_list_planning_cycle);
            holder.sizingProjectName = (TextView) convertView.findViewById(R.id.sizing_list_project_name);
            holder.contact = (TextView) convertView.findViewById(R.id.sizing_list_contact);
            holder.layout = (LinearLayout) convertView.findViewById(R.id.sizing_list_layout);
            holder.assignee = (TextView) convertView.findViewById(R.id.sizing_list_assignee);
            holder.status = (TextView) convertView.findViewById(R.id.sizing_list_status);
            holder.click = (ImageView) convertView.findViewById(R.id.sizing_list_click);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.sizingPpmid.setText(getItem(position).getPpmid());

        if (getItem(position).getPlanningCycle() != null) {
            if (!getItem(position).getPlanningCycle().contains(Value.SPLIT)) {
                holder.planningCycle.setText(getItem(position).getPlanningCycle());
            } else {
                Item item = AppUtils.parseEdited(getItem(position).getPlanningCycle());
                if (item != null && item.getNewValue() != null) {
                    holder.planningCycle.setText(item.getNewValue());
                }
            }
        }

        if (getItem(position).getProjectName() != null) {
            if (!getItem(position).getProjectName().contains(Value.SPLIT)) {
                holder.sizingProjectName.setText(getItem(position).getProjectName());
            } else {
                Item item = AppUtils.parseEdited(getItem(position).getProjectName());
                if (item != null && item.getNewValue() != null) {
                    holder.sizingProjectName.setText(item.getNewValue());
                }
            }
        }

        if (getItem(position).getContact() != null) {
            if (!getItem(position).getContact().contains(Value.SPLIT)) {
                holder.contact.setText(getItem(position).getContact());
            } else {
                Item item = AppUtils.parseEdited(getItem(position).getContact());
                if (item != null && item.getNewValue() != null) {
                    holder.contact.setText(item.getNewValue());
                }
            }
        }

        if (!getItem(position).isRead()) {
            holder.sizingBar.setVisibility(View.VISIBLE);
            if (getItem(position).getOperation() == Request.ADD) {
                holder.sizingBar.setBackgroundColor(context.getResources().getColor(R.color.red));
            } else if (getItem(position).getOperation() == Request.CHANGE) {
                holder.sizingBar.setBackgroundColor(context.getResources().getColor(R.color.light_blue));
            }
        } else {
            holder.sizingBar.setVisibility(View.GONE);
        }

        if (getItem(position).isAssigned()) {
            holder.assignee.setText(getItem(position).getResource());
            holder.assignee.setTextColor(context.getResources().getColor(R.color.light_blue));
            holder.layout.setBackgroundResource(R.drawable.listview_read_bg);
        } else {
            holder.assignee.setText(context.getString(R.string.not_assigned));
            holder.assignee.setTextColor(context.getResources().getColor(R.color.dark_grey));
            holder.layout.setBackgroundResource(R.drawable.listview_new_bg);
        }

        String packageName = Value.PACKAGENAME;
        int resId = context.getResources().getIdentifier(Request.workingStatusMap.get(getItem(position).getWorkingStatus()), "string", packageName);
        holder.status.setText(context.getString(resId));

        if (getItem(position).isImportant()) {
            holder.click.setImageDrawable(context.getResources().getDrawable(R.drawable.red_flag));
        } else {
            holder.click.setImageDrawable(context.getResources().getDrawable(R.drawable.grey_flag));
        }

        holder.click.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getItem(position).isImportant()) {
                    getItem(position).setImportant(false);
                    databaseDao.updateImportant(context, getItem(position).getPpmid(), 0);
                    if (v instanceof ImageView) {
                        ((ImageView) v).setImageDrawable(context.getResources().getDrawable(R.drawable.grey_flag));
                    }
                } else {
                    getItem(position).setImportant(true);
                    databaseDao.updateImportant(context, getItem(position).getPpmid(), 1);
                    if (v instanceof ImageView) {
                        ((ImageView) v).setImageDrawable(context.getResources().getDrawable(R.drawable.red_flag));
                    }
                }
            }
        });

        return convertView;
    }
}
