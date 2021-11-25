package ap.andruavmiddlelibrary.database;

import android.database.sqlite.SQLiteDatabase;

import com.andruav.AndruavEngine;

/**
 * Created by mhefny on 2/13/16.
 */
public class DaoManager {

    private static SQLiteDatabase db;
    private static DaoMaster daoMaster;
    private static DaoSession daoSession;

    private static LogDao logDao;
    private static GenericDataDao genericDataDao;


    public static LogDao getLogDao ()
    {
        return  logDao;
    }


    public static GenericDataDao getGenericDataDao ()
    {
        return genericDataDao;
    }

    public static void init () {
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(AndruavEngine.AppContext, "AndruavDB", null);
        db = helper.getWritableDatabase();
        //helper.onUpgrade(db,1000,1001);

        daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();
        logDao = daoSession.getLogDao();
        genericDataDao = daoSession.getCustomerDao();

        //logDao.insert(new LogRow(null, "Me", "Test", "Data"));
       // genericDataDao.insert(new GenericDataRow(null, 1l, "This is a Location"));

      /*  List joes = logDao.queryBuilder()
                .where(LogDao.Properties.Tag.eq("Test"))
                .orderAsc(LogDao.Properties.Id)
                .list();

        List joes2 = genericDataDao.queryBuilder()
                .where(GenericDataDao.Properties.Type.eq(1l))
                .list();

        int a = joes.hashCode();
        a = joes2.hashCode();
        logDao.deleteAll();
        genericDataDao.deleteAll();

        a = joes2.hashCode();
        */
    }

    public static void closeAll ()
    {
        if (db != null)
        {
            db.close();
        }
    }
}
