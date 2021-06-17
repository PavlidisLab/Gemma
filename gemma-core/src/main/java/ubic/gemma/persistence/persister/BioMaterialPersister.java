package ubic.gemma.persistence.persister;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialDao;

@Service
public class BioMaterialPersister extends AbstractPersister<BioMaterial> {

    @Autowired
    private BioMaterialDao bioMaterialDao;

    @Autowired
    private Persister<Taxon> taxonPersister;

    @Autowired
    private Persister<DatabaseEntry> databaseEntryPersister;

    @Autowired
    public BioMaterialPersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    @Transactional
    public BioMaterial persist( BioMaterial entity ) {
        if ( entity == null )
            return null;
        AbstractPersister.log.debug( "Persisting " + entity );
        if ( !this.isTransient( entity ) )
            return entity;

        assert entity.getSourceTaxon() != null;

        AbstractPersister.log.debug( "Persisting " + entity );
        databaseEntryPersister.persist( entity.getExternalAccession() );

        AbstractPersister.log.debug( "db entry done" );
        entity.setSourceTaxon( taxonPersister.persist( entity.getSourceTaxon() ) );

        AbstractPersister.log.debug( "taxon done" );

        AbstractPersister.log.debug( "start save" );
        entity = bioMaterialDao.findOrCreate( entity );
        AbstractPersister.log.debug( "save biomaterial done" );

        return entity;
    }
}
