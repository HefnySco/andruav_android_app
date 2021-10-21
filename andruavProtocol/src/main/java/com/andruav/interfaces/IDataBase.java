package com.andruav.interfaces;

import android.database.sqlite.SQLiteDatabase;

/**
 * Created by mhefny on 2/13/16.
 */
public interface IDataBase {


    SQLiteDatabase getDatabaseforRead();
    SQLiteDatabase getDatabaseforWrite();


}
