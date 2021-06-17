package ubic.gemma.persistence.persister;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseDao;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExternalDatabasePersister extends AbstractPersister<ExternalDatabase> {

    private final Map<Object, ExternalDatabase> seenDatabases = new ConcurrentHashMap<>();

    @Autowired
    private ExternalDatabaseDao externalDatabaseDao;

    @Autowired
    public ExternalDatabasePersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    @Transactional
    public ExternalDatabase persist( ExternalDatabase database ) {
        if ( database == null )
            return null;
        if ( !this.isTransient( database ) )
            return database;

        String name = database.getName();

        if ( seenDatabases.containsKey( name ) ) {
            return seenDatabases.get( name );
        }

        ExternalDatabase existingDatabase = externalDatabaseDao.find( database );

        // don't use findOrCreate to avoid flush.
        if ( existingDatabase == null ) {
            database = externalDatabaseDao.create( database );
        } else {
            database = existingDatabase;
        }

        seenDatabases.put( database.getName(), database );
        return database;
    }
}
