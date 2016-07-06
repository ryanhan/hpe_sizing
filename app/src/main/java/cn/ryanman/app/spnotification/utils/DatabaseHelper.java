package cn.ryanman.app.spnotification.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Ryan on 2015/12/17.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final int VERSION = 1;
    public static final String DATABASENAME = "sizing_db";
    public static final String REQUEST = "request";
    public static final String PROGRESS = "progress";
    //Basic Info
    public static final String ID = "id";
    public static final String UID = "uid";
    public static final String PPMID = "ppmid";
    public static final String OLDPPMID = "oldppmid";
    public static final String PROJECTNAME = "projectname";
    public static final String OPERATION = "operation";
    public static final String TEAMS = "teams";
    public static final String COMPLETE = "complete";
    public static final String PLANNINGCYCLE = "planningcycle";
    public static final String COMMENTS = "comments";
    public static final String CONTACT = "contact";
    public static final String PPMSTATUS = "ppmstatus";
    public static final String LASTMODIFY = "lastmodify";
    //Status Record
    public static final String ASSIGNED = "assigned";
    public static final String READ = "read";
    public static final String RESOURCE = "resource";
    public static final String IMPORTANT = "important";
    public static final String LATEST = "latest";
    public static final String WORKINGSTATUS = "workingstatus";

    private Context context;

    public DatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory,
                          int version) {
        super(context, name, factory, version);
        this.context = context;
    }

    public DatabaseHelper(Context context, String name, int version) {
        this(context, name, null, version);
    }

    public DatabaseHelper(Context context, String name) {
        this(context, name, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table if not exists " + REQUEST + " (" + ID
                + " INTEGER PRIMARY KEY AUTOINCREMENT, " + UID + " text, " + PPMID + " text, " + OLDPPMID + " text, "
                + PROJECTNAME + " text, " + PLANNINGCYCLE + " text, " + COMMENTS + " text, "
                + CONTACT + " text, " + PPMSTATUS + " text, " + TEAMS + " text, "
                + COMPLETE + " text, " + OPERATION + " integer, " + LASTMODIFY + " text, "
                + RESOURCE + " text, " + ASSIGNED + " integer, " + READ + " integer, "
                + LATEST + " integer, " + IMPORTANT + " integer, " + WORKINGSTATUS + " integer)");

        db.execSQL("create table if not exists " + PROGRESS + " (" + ID
                + " INTEGER PRIMARY KEY AUTOINCREMENT, " + PPMID + " text, " + RESOURCE + " text, "
                + ASSIGNED + " integer, " + IMPORTANT + " integer, " + WORKINGSTATUS + " integer)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
