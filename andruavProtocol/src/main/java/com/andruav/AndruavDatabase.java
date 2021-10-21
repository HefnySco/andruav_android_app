package com.andruav;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by mhefny on 2/13/16.
 */
public class AndruavDatabase extends SQLiteOpenHelper  {


    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "andruavManager";


    private SQLiteDatabase db;
    private final StringBuilder sqlCreate = new StringBuilder();
    private final StringBuilder sqlDrop = new StringBuilder();


    public AndruavDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

    }

    public AndruavDatabase(Context context, String name, SQLiteDatabase.CursorFactory factory, int version, DatabaseErrorHandler errorHandler) {
        super(context, name, factory, version, errorHandler);
    }


    public void addTable (final AndruavTable andruavTable)
    {
        sqlCreate.append(andruavTable.getSchema()).append("\r\n");
        sqlDrop.append("DROP TABLE IF EXISTS " + andruavTable.dbTableName + "\r\n");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {

            db.execSQL(sqlCreate.toString());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL(sqlDrop.toString());

        // Create tables again
        onCreate(db);

    }



    public void initDB ()
    {
        db = this.getWritableDatabase();
    }



    public void addRecord (final String tableName, final ContentValues values)
    {
        try
        {
            //final SQLiteDatabase db = this.getWritableDatabase();

            db.insert(tableName, null, values);

           // db.close();
        }
        catch (Exception ex)
        {
            AndruavEngine.log().logException(AndruavSettings.Account_SID, "DB", ex);
        }
    }


    public Cursor getRecords (final String tableName, final String whereCondition)
    {
       // this.db = this.getReadableDatabase();
        final Cursor cursor = db.rawQuery("SELECT  * FROM " + tableName + " where 1=1 and " + whereCondition, null);

        return cursor;

    }

    public int getRecordCount (final String tableName, final String whereCondition)
    {
        try {
            final Cursor cursor = getRecords (tableName, null);
            final int count = cursor.getCount();

           // this.db.close();

            return count;
        }
        catch (Exception ex)
        {
            AndruavEngine.log().logException(AndruavSettings.Account_SID, "DB", ex);
            return 0;
        }
    }

    public void truncateTable (String tableName)
    {
        try
        {
           // SQLiteDatabase db  = this.getWritableDatabase();

            db.execSQL("DELETE FROM " + tableName);

          //  db.close();
        }
        catch (Exception ex)
        {
            AndruavEngine.log().logException(AndruavSettings.Account_SID, "DB", ex);
            return ;
        }

    }





}
