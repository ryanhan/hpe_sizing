package cn.ryanman.app.spnotification.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import cn.ryanman.app.spnotification.model.Request;

public class DatabaseUtils {

    public static void createDatabase(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context,
                DatabaseHelper.DATABASENAME);
        SQLiteDatabase sqliteDatabase = dbHelper.getReadableDatabase();
        dbHelper.close();
    }


    public static void addRecords(Context context, List<Request> requests) {
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
                int id = findRecord(sqliteDatabase, request.getPpmid());
                if (id == -1) {
                    addRecord(sqliteDatabase, request);
                } else {
                    updateRecord(sqliteDatabase, request);
                }
            } else if (request.getOperation() == Request.DELETE) {
                deleteRecord(sqliteDatabase, request);
            }
            SharedPreferences pref = context.getSharedPreferences(Value.APPINFO, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString(Value.LASTEMAILTIME, request.getSentDate());
            editor.commit();
        }
        dbHelper.close();
    }

    private static void addRecord(SQLiteDatabase sqliteDatabase, Request request) {
        ContentValues values = createContentValues(request);
        values.put(DatabaseHelper.OPERATION, Request.ADD);
        values.put(DatabaseHelper.READ, Request.UNREAD);
        values.put(DatabaseHelper.STATUS, Request.NOT_STARTED);
        values.put(DatabaseHelper.TIME, Long.toString(System.currentTimeMillis()));
        sqliteDatabase.insert(DatabaseHelper.REQUEST, null, values);
        Log.d("SPNotification", "SQLite " + request.getPpmid() + " inserted");
    }

    private static int findRecord(SQLiteDatabase sqliteDatabase, String ppmid) {
        Cursor cursor = sqliteDatabase
                .query(true,
                        DatabaseHelper.REQUEST,
                        new String[]{DatabaseHelper.ID},
                        DatabaseHelper.PPMID + "=?",
                        new String[]{String.valueOf(ppmid)}, null,
                        null, DatabaseHelper.ID, null);
        List<Integer> ids = new ArrayList<Integer>();
        while (cursor.moveToNext()) {
            ids.add(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.ID)));
        }
        if (ids.size() == 1) {
            return ids.get(0);
        }
        return -1;
    }

    private static void updateRecord(SQLiteDatabase sqliteDatabase, Request request) {
        ContentValues values = createContentValues(request);
        String currentPpmid;
        if (request.getOldPpmid() == null) {
            currentPpmid = request.getPpmid();
        } else {
            currentPpmid = request.getOldPpmid();
        }
        values.put(DatabaseHelper.OPERATION, Request.CHANGE);
        values.put(DatabaseHelper.READ, Request.UNREAD);
        values.put(DatabaseHelper.TIME, Long.toString(System.currentTimeMillis()));
        sqliteDatabase.update(DatabaseHelper.REQUEST, values, DatabaseHelper.PPMID + "=?", new String[]{currentPpmid});
        Log.d("SPNotification", "SQLite " + request.getPpmid() + " updated");
    }

    private static void deleteRecord(SQLiteDatabase sqliteDatabase, Request request) {
        sqliteDatabase.delete(DatabaseHelper.REQUEST,
                DatabaseHelper.PPMID + "=?",
                new String[]{request.getPpmid()});
    }

    public static List<Request> getAllRequests(Context context) {
        List<Request> requests = new ArrayList<Request>();
        DatabaseHelper dbHelper = new DatabaseHelper(context,
                DatabaseHelper.DATABASENAME);
        SQLiteDatabase sqliteDatabase = dbHelper.getReadableDatabase();
        Cursor cursor = sqliteDatabase.query(DatabaseHelper.REQUEST, null,
                null, null, null, null, DatabaseHelper.TIME + " DESC");

        while (cursor.moveToNext()) {
            Request request = parseCursor(cursor);
            if (request != null) {
                requests.add(request);
                //request.print();
            }
        }
        dbHelper.close();
        return requests;
    }

    public static List<Request> getMarkedRequests(Context context) {
        List<Request> requests = new ArrayList<Request>();
        DatabaseHelper dbHelper = new DatabaseHelper(context,
                DatabaseHelper.DATABASENAME);
        SQLiteDatabase sqliteDatabase = dbHelper.getReadableDatabase();
        Cursor cursor = sqliteDatabase.query(DatabaseHelper.REQUEST, null,
                DatabaseHelper.IMPORTANT + "=?", new String[]{"1"}, null, null, DatabaseHelper.TIME + " DESC");

        while (cursor.moveToNext()) {
            Request request = parseCursor(cursor);
            if (request != null) {
                requests.add(request);
            }
        }
        dbHelper.close();
        return requests;
    }

    public static Request getRequestByPpmid(Context context, String ppmid) {
        Request request = new Request();
        DatabaseHelper dbHelper = new DatabaseHelper(context,
                DatabaseHelper.DATABASENAME);
        SQLiteDatabase sqliteDatabase = dbHelper.getReadableDatabase();
        Cursor cursor = sqliteDatabase.query(DatabaseHelper.REQUEST, null,
                DatabaseHelper.PPMID + "=?", new String[]{ppmid}, null, null, DatabaseHelper.ID);

        while (cursor.moveToNext()) {
            request = parseCursor(cursor);
            break;
        }
        dbHelper.close();
        return request;
    }

    public static void updateRequestRead(Context context, String ppmid, int read) {
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

    public static void updateRequestStatus(Context context, String ppmid, int status) {
        DatabaseHelper dbHelper = new DatabaseHelper(context,
                DatabaseHelper.DATABASENAME);
        SQLiteDatabase sqliteDatabase = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.STATUS, status);
        sqliteDatabase.update(DatabaseHelper.REQUEST, values,
                DatabaseHelper.PPMID + "=?",
                new String[]{ppmid});
        dbHelper.close();
    }

    public static void updateImportant(Context context, String ppmid, int isImportant) {
        DatabaseHelper dbHelper = new DatabaseHelper(context,
                DatabaseHelper.DATABASENAME);
        SQLiteDatabase sqliteDatabase = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.IMPORTANT, isImportant);
        sqliteDatabase.update(DatabaseHelper.REQUEST, values,
                DatabaseHelper.PPMID + "=?",
                new String[]{ppmid});
        dbHelper.close();
    }

    public static void updateAssginedTo(Context context, String ppmid, String resource) {
        DatabaseHelper dbHelper = new DatabaseHelper(context,
                DatabaseHelper.DATABASENAME);
        SQLiteDatabase sqliteDatabase = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.RESOURCE, resource);
        values.put(DatabaseHelper.STATUS, Request.ASSIGNED);
        sqliteDatabase.update(DatabaseHelper.REQUEST, values,
                DatabaseHelper.PPMID + "=?",
                new String[]{ppmid});
        dbHelper.close();
    }

    public static void removeAssignee(Context context, String ppmid) {
        DatabaseHelper dbHelper = new DatabaseHelper(context,
                DatabaseHelper.DATABASENAME);
        SQLiteDatabase sqliteDatabase = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.RESOURCE, "");
        values.put(DatabaseHelper.STATUS, Request.NOT_STARTED);
        sqliteDatabase.update(DatabaseHelper.REQUEST, values,
                DatabaseHelper.PPMID + "=?",
                new String[]{ppmid});
        dbHelper.close();
    }

    public static void markAllUnread(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context,
                DatabaseHelper.DATABASENAME);
        SQLiteDatabase sqliteDatabase = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.READ, Request.UNREAD);
        sqliteDatabase.update(DatabaseHelper.REQUEST, values, null, null);
        dbHelper.close();
    }

    private static ContentValues createContentValues(Request request) {
        ContentValues values = new ContentValues();
        if (request.getPpmid() == null) {
            return null;
        } else {
            values.put(DatabaseHelper.PPMID, request.getPpmid());
        }
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
        values.put(DatabaseHelper.IMPORTANT, 0);
        return values;
    }

    private static Request parseCursor(Cursor cursor) {
        Request request = new Request();
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
        if (cursor.getInt(cursor
                .getColumnIndex(DatabaseHelper.IMPORTANT)) == 1) {
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
        request.setStatus(cursor.getInt(cursor
                .getColumnIndex(DatabaseHelper.STATUS)));
        request.setResource(cursor.getString(cursor
                .getColumnIndex(DatabaseHelper.RESOURCE)));

        Log.d("SPNotification", "SQLite " + request.getPpmid() + " selected");
        return request;
    }

}
