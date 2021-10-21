package com.andruav.dummyclasses;

import android.database.sqlite.SQLiteDatabase;

import com.andruav.interfaces.IDataBase;

/**
 * Created by mhefny on 2/13/16.
 */
public class DummyDatabase implements IDataBase {
    @Override
    public SQLiteDatabase getDatabaseforRead() {
        return null;
    }

    @Override
    public SQLiteDatabase getDatabaseforWrite() {
        return null;
    }
}
