package cn.ryanman.app.spnotification.dao;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import cn.ryanman.app.spnotification.model.Request;
import cn.ryanman.app.spnotification.utils.DatabaseHelper;
import cn.ryanman.app.spnotification.utils.Value;

public class SizingDatabaseDao implements DatabaseDao {

    @Override
    public void addRecords(Context context, List<Request> requests) {
        DatabaseHelper dbHelper = new DatabaseHelper(context,
                DatabaseHelper.DATABASENAME);
        SQLiteDatabase sqliteDatabase = dbHelper.getWritableDatabase();

        for (Request request : requests) {
            if (!request.isHPSB()) {
                continue;
            }
            if (request.getOperation() == Request.ADD) {
                addRecord(sqliteDatabase, request);
            } else if (request.getOperation() == Request.CHANGE) {
                if (isRecordExisted(sqliteDatabase, request.getPpmid())) {
                    setOldRecord(sqliteDatabase, request.getPpmid());
                    //syncNewRequest(oldRequest, request);
                } else {
                    request.setOperation(Request.ADD);
                }
                addRecord(sqliteDatabase, request);
            } else if (request.getOperation() == Request.DELETE) {
                deleteRecord(sqliteDatabase, request);
            }
            SharedPreferences pref = context.getSharedPreferences(Value.APPINFO, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putLong(Value.LASTEMAILUID, request.getUid());
            editor.commit();
        }
        dbHelper.close();
    }

    private void addRecord(SQLiteDatabase sqliteDatabase, Request request) {
        ContentValues requestValues = createRequestValues(request);
        sqliteDatabase.insert(DatabaseHelper.REQUEST, null, requestValues);

        if (request.getOperation() == Request.ADD) {
            ContentValues progressValues = createProgressValues(request);
            sqliteDatabase.insert(DatabaseHelper.PROGRESS, null, progressValues);
        }

        Log.d("SPNotification", "SQLite " + request.getPpmid() + " inserted");
    }

    private boolean isRecordExisted(SQLiteDatabase sqliteDatabase, String ppmId) {
        Cursor cursor = sqliteDatabase.query(DatabaseHelper.REQUEST, null,
                DatabaseHelper.PPMID + "=? and " + DatabaseHelper.LATEST + "=?",
                new String[]{String.valueOf(ppmId), "1"}, null, null, null);

        return cursor.getCount() > 0;
    }

    private void setOldRecord(SQLiteDatabase sqliteDatabase, String ppmid) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.LATEST, Request.OUT_OF_DATE);
        sqliteDatabase.update(DatabaseHelper.REQUEST, values,
                DatabaseHelper.PPMID + "=?",
                new String[]{ppmid});
    }

    private void deleteRecord(SQLiteDatabase sqliteDatabase, Request request) {
        sqliteDatabase.delete(DatabaseHelper.REQUEST,
                DatabaseHelper.PPMID + "=?",
                new String[]{request.getPpmid()});
        sqliteDatabase.delete(DatabaseHelper.PROGRESS,
                DatabaseHelper.PPMID + "=?",
                new String[]{request.getPpmid()});
    }

    @Override
    public List<Request> getAllRequests(Context context) {
        List<Request> requests = new ArrayList<Request>();
        DatabaseHelper dbHelper = new DatabaseHelper(context,
                DatabaseHelper.DATABASENAME);
        SQLiteDatabase sqliteDatabase = dbHelper.getReadableDatabase();
        //Cursor cursor = sqliteDatabase.query(DatabaseHelper.REQUEST, null,
        //      DatabaseHelper.LATEST + "=?", new String[]{"1"}, null, null, DatabaseHelper.ID + " DESC");

        Cursor cursor = sqliteDatabase.rawQuery("select * from (select * from " + DatabaseHelper.REQUEST + " where " + DatabaseHelper.LATEST + "=? order by " + DatabaseHelper.ID + " DESC) join " + DatabaseHelper.PROGRESS + " using (" + DatabaseHelper.PPMID + ")", new String[]{"1"});

        while (cursor.moveToNext()) {
            Request request = parseCursor(cursor);
            if (request != null) {
                requests.add(request);
                Log.d("SPNotification", "UID: " + request.getUid());
                //request.print();
            }
        }
        dbHelper.close();
        return requests;
    }

    @Override
    public List<Request> getMarkedRequests(Context context) {
        List<Request> requests = new ArrayList<Request>();
        DatabaseHelper dbHelper = new DatabaseHelper(context,
                DatabaseHelper.DATABASENAME);
        SQLiteDatabase sqliteDatabase = dbHelper.getReadableDatabase();
        //Cursor cursor = sqliteDatabase.query(DatabaseHelper.REQUEST, null,
//                DatabaseHelper.IMPORTANT + "=? and " + DatabaseHelper.LATEST + "=?", new String[]{"1", "1"}, null, null, DatabaseHelper.ID + " DESC");

        Cursor cursor = sqliteDatabase.rawQuery("select * from (select * from " + DatabaseHelper.REQUEST + " where " + DatabaseHelper.LATEST + "=? order by " + DatabaseHelper.ID + " DESC) join " + DatabaseHelper.PROGRESS + " using (" + DatabaseHelper.PPMID + ") where " + DatabaseHelper.IMPORTANT + "=?", new String[]{"1", "1"});

        while (cursor.moveToNext()) {
            Request request = parseCursor(cursor);
            if (request != null) {
                requests.add(request);
            }
        }
        dbHelper.close();
        return requests;
    }

    @Override
    public Request getRequestByPpmid(Context context, String ppmid) {
        Request request = new Request();
        DatabaseHelper dbHelper = new DatabaseHelper(context,
                DatabaseHelper.DATABASENAME);
        SQLiteDatabase sqliteDatabase = dbHelper.getReadableDatabase();
//        Cursor cursor = sqliteDatabase.query(DatabaseHelper.REQUEST, null,
//                DatabaseHelper.PPMID + "=? and " + DatabaseHelper.LATEST + "=?", new String[]{ppmid, "1"}, null, null, DatabaseHelper.ID + " DESC");

        Cursor cursor = sqliteDatabase.rawQuery("select * from (select * from " + DatabaseHelper.REQUEST + " where " + DatabaseHelper.LATEST + "=? and " + DatabaseHelper.PPMID + "=? order by " + DatabaseHelper.ID + " DESC) join " + DatabaseHelper.PROGRESS + " using (" + DatabaseHelper.PPMID + ")", new String[]{"1", ppmid});

        while (cursor.moveToNext()) {
            request = parseCursor(cursor);
            break;
        }
        dbHelper.close();
        return request;
    }

    @Override
    public void updateRequestRead(Context context, String ppmid, int read) {
        DatabaseHelper dbHelper = new DatabaseHelper(context,
                DatabaseHelper.DATABASENAME);
        SQLiteDatabase sqliteDatabase = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.READ, read);
        sqliteDatabase.update(DatabaseHelper.REQUEST, values,
                DatabaseHelper.PPMID + "=?",
                new String[]{ppmid});
        dbHelper.close();
    }

    @Override
    public void updateRequestWorkingStatus(Context context, String ppmid, int status) {
        DatabaseHelper dbHelper = new DatabaseHelper(context,
                DatabaseHelper.DATABASENAME);
        SQLiteDatabase sqliteDatabase = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.WORKINGSTATUS, status);
        sqliteDatabase.update(DatabaseHelper.PROGRESS, values,
                DatabaseHelper.PPMID + "=?",
                new String[]{ppmid});
        dbHelper.close();
    }

    @Override
    public void updateImportant(Context context, String ppmid, int isImportant) {
        DatabaseHelper dbHelper = new DatabaseHelper(context,
                DatabaseHelper.DATABASENAME);
        SQLiteDatabase sqliteDatabase = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.IMPORTANT, isImportant);
        sqliteDatabase.update(DatabaseHelper.PROGRESS, values,
                DatabaseHelper.PPMID + "=?",
                new String[]{ppmid});
        dbHelper.close();
    }

    @Override
    public void updateAssginedTo(Context context, String ppmid, String resource) {
        DatabaseHelper dbHelper = new DatabaseHelper(context,
                DatabaseHelper.DATABASENAME);
        SQLiteDatabase sqliteDatabase = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.RESOURCE, resource);
        values.put(DatabaseHelper.ASSIGNED, Request.ASSIGNED);
        values.put(DatabaseHelper.WORKINGSTATUS, Request.WORK_IN_PROGRESS);
        sqliteDatabase.update(DatabaseHelper.PROGRESS, values,
                DatabaseHelper.PPMID + "=?",
                new String[]{ppmid});
        dbHelper.close();
    }

    @Override
    public void removeAssignee(Context context, String ppmid) {
        DatabaseHelper dbHelper = new DatabaseHelper(context,
                DatabaseHelper.DATABASENAME);
        SQLiteDatabase sqliteDatabase = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.RESOURCE, "");
        values.put(DatabaseHelper.ASSIGNED, Request.NOT_ASSIGNED);
        values.put(DatabaseHelper.WORKINGSTATUS, Request.NOT_ASSIGNED);
        sqliteDatabase.update(DatabaseHelper.PROGRESS, values,
                DatabaseHelper.PPMID + "=?",
                new String[]{ppmid});
        dbHelper.close();
    }

    @Override
    public void updateWorkingStatus(Context context, String ppmid, int workingStatus) {
        DatabaseHelper dbHelper = new DatabaseHelper(context,
                DatabaseHelper.DATABASENAME);
        SQLiteDatabase sqliteDatabase = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.WORKINGSTATUS, workingStatus);
        sqliteDatabase.update(DatabaseHelper.PROGRESS, values,
                DatabaseHelper.PPMID + "=?",
                new String[]{ppmid});
        dbHelper.close();
    }

    @Override
    public void markAllUnread(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context,
                DatabaseHelper.DATABASENAME);
        SQLiteDatabase sqliteDatabase = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.READ, Request.UNREAD);
        sqliteDatabase.update(DatabaseHelper.REQUEST, values, null, null);
        dbHelper.close();
    }

    private ContentValues createRequestValues(Request request) {
        ContentValues values = new ContentValues();
        if (request.getPpmid() == null) {
            return null;
        } else {
            values.put(DatabaseHelper.PPMID, request.getPpmid());
        }
        values.put(DatabaseHelper.UID, request.getUid());
        values.put(DatabaseHelper.OPERATION, request.getOperation());
        if (request.getProjectName() != null) {
            values.put(DatabaseHelper.PROJECTNAME, request.getProjectName());
        }
        if (request.getTeams() != null) {
            values.put(DatabaseHelper.TEAMS, request.getTeams());
        }
        if (request.getComplete() != null) {
            values.put(DatabaseHelper.COMPLETE, request.getComplete());
        }
        if (request.getPlanningCycle() != null) {
            values.put(DatabaseHelper.PLANNINGCYCLE, request.getPlanningCycle());
        }
        if (request.getComments() != null) {
            values.put(DatabaseHelper.COMMENTS, request.getComments());
        }
        if (request.getPpmStatus() != null) {
            values.put(DatabaseHelper.PPMSTATUS, request.getPpmStatus());
        }
        if (request.getContact() != null) {
            values.put(DatabaseHelper.CONTACT, request.getContact());
        }
        if (request.getLastmodified() != null) {
            values.put(DatabaseHelper.LASTMODIFY, request.getLastmodified());
        }
        values.put(DatabaseHelper.READ, Request.UNREAD);
        values.put(DatabaseHelper.LATEST, Request.LATEST);
        return values;
    }

    private ContentValues createProgressValues(Request request) {
        ContentValues values = new ContentValues();
        if (request.getPpmid() == null) {
            return null;
        } else {
            values.put(DatabaseHelper.PPMID, request.getPpmid());
        }
        values.put(DatabaseHelper.ASSIGNED, Request.NOT_ASSIGNED);
        values.put(DatabaseHelper.IMPORTANT, Request.NOT_IMPORTANT);
        values.put(DatabaseHelper.WORKINGSTATUS, Request.NOT_ASSIGNED);
        return values;
    }


    private Request parseCursor(Cursor cursor) {
        Request request = new Request();
        request.setUid(cursor.getLong(cursor.
                getColumnIndex(DatabaseHelper.UID)));
        request.setPpmid(cursor.getString(cursor
                .getColumnIndex(DatabaseHelper.PPMID)));
        request.setProjectName(cursor.getString(cursor
                .getColumnIndex(DatabaseHelper.PROJECTNAME)));
        request.setOldPpmid(cursor.getString(cursor
                .getColumnIndex(DatabaseHelper.OLDPPMID)));
        request.setComments(cursor.getString(cursor
                .getColumnIndex(DatabaseHelper.COMMENTS)));
        request.setTeams(cursor.getString(cursor
                .getColumnIndex(DatabaseHelper.TEAMS)));
        request.setComplete(cursor.getString(cursor
                .getColumnIndex(DatabaseHelper.COMPLETE)));
        request.setContact(cursor.getString(cursor
                .getColumnIndex(DatabaseHelper.CONTACT)));
        request.setOperation(cursor.getInt(cursor
                .getColumnIndex(DatabaseHelper.OPERATION)));
        request.setPpmStatus(cursor.getString(cursor
                .getColumnIndex(DatabaseHelper.PPMSTATUS)));
        request.setPlanningCycle(cursor.getString(cursor
                .getColumnIndex(DatabaseHelper.PLANNINGCYCLE)));
        request.setLastmodified(cursor.getString(cursor
                .getColumnIndex(DatabaseHelper.LASTMODIFY)));
        request.setWorkingStatus(cursor.getInt(cursor
                .getColumnIndex(DatabaseHelper.WORKINGSTATUS)));
        if (cursor.getInt(cursor
                .getColumnIndex(DatabaseHelper.IMPORTANT)) == Request.IMPORTANT) {
            request.setImportant(true);
        } else {
            request.setImportant(false);
        }
        if (cursor.getInt(cursor
                .getColumnIndex(DatabaseHelper.READ)) == Request.READ) {
            request.setRead(true);
        } else {
            request.setRead(false);
        }
        if (cursor.getInt(cursor
                .getColumnIndex(DatabaseHelper.ASSIGNED)) == Request.ASSIGNED) {
            request.setAssigned(true);
            request.setResource(cursor.getString(cursor
                    .getColumnIndex(DatabaseHelper.RESOURCE)));
        } else {
            request.setAssigned(false);
        }
        Log.d("SPNotification", "SQLite " + request.getPpmid() + " selected");
        return request;
    }

}
