package ap.andruavmiddlelibrary.database;

import android.database.sqlite.SQLiteDatabase;

import java.util.Map;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.AbstractDaoSession;
import de.greenrobot.dao.identityscope.IdentityScopeType;
import de.greenrobot.dao.internal.DaoConfig;

/**
 * Created by mhefny on 2/13/16.
 */
public class DaoSession extends AbstractDaoSession {

    private final DaoConfig logDaoConfig;
    private final DaoConfig genericDataDaoConfig;


    private final LogDao logDao;
    private final GenericDataDao genericDataDao;

    public DaoSession(SQLiteDatabase db, IdentityScopeType type, Map<Class<? extends AbstractDao<?, ?>>, DaoConfig>
            daoConfigMap) {
        super(db);

        logDaoConfig = daoConfigMap.get(LogDao.class).clone();
        logDaoConfig.initIdentityScope(type);

        genericDataDaoConfig = daoConfigMap.get(GenericDataDao.class).clone();
        genericDataDaoConfig.initIdentityScope(type);


        logDao = new LogDao(logDaoConfig, this);
        genericDataDao = new GenericDataDao(genericDataDaoConfig, this);

        registerDao(LogRow.class, logDao);
        registerDao(GenericDataRow.class, genericDataDao);

    }

    public void clear() {
        logDaoConfig.getIdentityScope().clear();
        genericDataDaoConfig.getIdentityScope().clear();

    }

    public LogDao getLogDao() {
        return logDao;
    }

    public GenericDataDao getCustomerDao() {
        return genericDataDao;
    }


}
