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
public class GenericDataDao extends AbstractDao<GenericDataRow, Long> {

    public static final String TABLENAME = "generic";

    public static class Properties {
        public final static Property Id         = new Property(0, Long.class, "id", true, "id");
        public final static Property Type       = new Property(1, Long.class, "type", false, "type");
        public final static Property Data       = new Property(2, String.class, "data", false, "data");
    }

    private DaoSession daoSession;

    public GenericDataDao(DaoConfig config) {
        super(config);
    }

    public GenericDataDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }


    /** Creates the underlying database table. */
    public static void createTable(SQLiteDatabase db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";

        String sqlCreate = "CREATE TABLE " + constraint + "\"" + TABLENAME + "\" (id INTEGER PRIMARY KEY, type INTEGER, data TEXT)";

        db.execSQL(sqlCreate);
    }


    /** Drops the underlying database table. */
    public static void dropTable(SQLiteDatabase db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "\"" + TABLENAME + "\"" ;
        db.execSQL(sql);
    }

    @Override
    protected GenericDataRow readEntity(Cursor cursor, int offset) {
        GenericDataRow entity = new GenericDataRow( //
                cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0), // id
                cursor.getLong(offset + 1), // type
                cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2) // data
        );
        return entity;
    }

    @Override
    protected Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0);
    }

    @Override
    protected void readEntity(Cursor cursor, GenericDataRow entity, int offset) {
        entity.setId(cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0));
        entity.setType(cursor.getLong(offset + 1));
        entity.setData(cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2));

    }

    @Override
    protected void bindValues(SQLiteStatement stmt, GenericDataRow entity) {
        stmt.clearBindings();

        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }

        stmt.bindLong(2, entity.getType());

        String data = entity.getData();
        if (data != null) {
            stmt.bindString(3, data);
        }
    }

    @Override
    protected Long updateKeyAfterInsert(GenericDataRow entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }

    @Override
    protected Long getKey(GenericDataRow entity) {
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
