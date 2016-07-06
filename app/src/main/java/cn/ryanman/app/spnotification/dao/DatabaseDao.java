package cn.ryanman.app.spnotification.dao;

import android.content.Context;

import java.util.List;

import cn.ryanman.app.spnotification.model.Request;

public interface DatabaseDao {

    public void addRecords(Context context, List<Request> requests);

    public List<Request> getAllRequests(Context context);

    public List<Request> getMarkedRequests(Context context);

    public Request getRequestByPpmid(Context context, String ppmid);

    public void updateRequestRead(Context context, String ppmid, int read);

    public void updateRequestWorkingStatus(Context context, String ppmid, int status);

    public void updateImportant(Context context, String ppmid, int isImportant);

    public void updateAssginedTo(Context context, String ppmid, String resource);

    public void removeAssignee(Context context, String ppmid);

    public void updateWorkingStatus(Context context, String ppmid, int workingStatus);

    public void markAllUnread(Context context);

}