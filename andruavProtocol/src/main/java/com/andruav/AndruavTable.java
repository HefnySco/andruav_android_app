package com.andruav;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by mhefny on 2/12/16.
 */
public class AndruavTable {


    protected SQLiteDatabase db;
    protected String sqlCreate;
    protected String dbTableName;


    public String getTableName ()
    {
        return dbTableName;
    }

    public String getSchema ()
    {
        return sqlCreate;
    }



    public AndruavTable() {

    }


    protected void addRecords (final ContentValues values)
    {
        AndruavEngine.getDabase().addRecord(dbTableName, values);
    }


    protected int getRecordCount (String whereCondition)
    {
       return  AndruavEngine.getDabase().getRecordCount(dbTableName, whereCondition);
    }

    public void truncateTable ()
    {
        AndruavEngine.getDabase().truncateTable(this.dbTableName);
    }

    protected Cursor getRecords ()
    {
        return AndruavEngine.getDabase().getRecords(this.dbTableName, null);
    }

    protected Cursor getRecords (String whereCondition)
    {
       return  AndruavEngine.getDabase().getRecords(this.dbTableName, whereCondition);

    }
}
