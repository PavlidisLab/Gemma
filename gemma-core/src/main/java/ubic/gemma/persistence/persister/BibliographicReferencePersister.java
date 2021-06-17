package ubic.gemma.persistence.persister;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.persistence.service.common.description.BibliographicReferenceDao;

@Service
public class BibliographicReferencePersister extends AbstractPersister<BibliographicReference> {

    @Autowired
    private Persister<DatabaseEntry> databaseEntryPersister;

    @Autowired
    private BibliographicReferenceDao bibliographicReferenceDao;

    @Autowired
    public BibliographicReferencePersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    @Transactional
    public BibliographicReference persist( BibliographicReference reference ) {
        if ( reference == null )
            return null;
        if ( !isTransient( reference ) )
            return reference;
        databaseEntryPersister.persist( reference.getPubAccession() );
        return this.bibliographicReferenceDao.findOrCreate( reference );
    }
}
