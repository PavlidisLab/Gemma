package ubic.gemma.persistence.persister;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;

@Service
public class DatabaseEntryPersister extends AbstractPersister<DatabaseEntry> {

    @Autowired
    Persister<ExternalDatabase> externalDatabasePersister;

    @Autowired
    public DatabaseEntryPersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    @Transactional
    public <S extends DatabaseEntry> S persist( S databaseEntry ) {
        if ( !this.isTransient( databaseEntry ) )
            return null;
        if ( databaseEntry == null )
            return null;
        ExternalDatabase tempExternalDb = databaseEntry.getExternalDatabase();
        databaseEntry.setExternalDatabase( null );
        ExternalDatabase persistedDb = externalDatabasePersister.persist( tempExternalDb );
        databaseEntry.setExternalDatabase( persistedDb );
        assert databaseEntry.getExternalDatabase().getId() != null;
        return databaseEntry;
    }
}
