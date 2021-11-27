package ap.andruavmiddlelibrary.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.internal.DaoConfig;

/**
 * Created by mhefny on 2/13/16.
 */
public class LogDao  extends AbstractDao<LogRow, Long> {

    public static final String TABLENAME = "log";

    public static class Properties {
        public final static Property Id         = new Property(0, Long.class, "id", true, "id");
        public final static Property UserName   = new Property(1, String.class, "name", false, "UserNAME");
        public final static Property Tag        = new Property(2, String.class, "name", false, "Tag");
        public final static Property Error      = new Property(3, String.class, "name", false, "Error");
    }

    private DaoSession daoSession;

    public LogDao(DaoConfig config) {
        super(config);
    }

    public LogDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }


    /** Creates the underlying database table. */
    public static void createTable(SQLiteDatabase db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";

        String sqlCreate = "CREATE TABLE " + constraint + "\"" + TABLENAME + "\" (id INTEGER PRIMARY KEY, userName TEXT, tag TEXT, error TEXT)";

        db.execSQL(sqlCreate);
    }


    /** Drops the underlying database table. */
    public static void dropTable(SQLiteDatabase db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "\"" + TABLENAME + "\"" ;
        db.execSQL(sql);
    }


    @Override
    protected LogRow readEntity(Cursor cursor, int offset) {
        LogRow entity = new LogRow( //
                cursor.isNull(offset) ? null : cursor.getLong(offset), // id
                cursor.getString(offset + 1), // username
                cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2), // tag
                cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3) // error
        );
        return entity;
    }

    @Override
    protected Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset) ? null : cursor.getLong(offset);
    }

    @Override
    protected void readEntity(Cursor cursor, LogRow entity, int offset) {
        entity.setId(cursor.isNull(offset) ? null : cursor.getLong(offset));
        entity.setUserName(cursor.getString(offset + 1));
        entity.setTag(cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2));
        entity.setError(cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3));
    }

    @Override
    protected void bindValues(SQLiteStatement stmt, LogRow entity) {
        stmt.clearBindings();

        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }

        stmt.bindString(2, entity.getUserName());

        String tag = entity.getTag();
        if (tag != null) {
            stmt.bindString(3, tag);
        }

        String error = entity.getError();
        if (error != null) {
            stmt.bindString(4, error);
        }
    }

    @Override
    protected Long updateKeyAfterInsert(LogRow entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }

    @Override
    protected Long getKey(LogRow entity) {
        if(entity != null) {
            return entity.getId();
        } else {
            return null;
        }
    }

    @Override
    protected boolean isEntityUpdateable() {
        return true;
    }





}
