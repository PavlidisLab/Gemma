package ubic.gemma.persistence.persister;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceDao;

@Service
public class BioSequencePersister extends AbstractPersister<BioSequence> {

    @Autowired
    private BioSequenceDao bioSequenceDao;

    @Autowired
    private Persister<ExternalDatabase> externalDatabasePersister;

    @Autowired
    private Persister<BioSequence2GeneProduct> bioSequence2GeneProductPersister;

    @Autowired
    private Persister<Taxon> taxonPersister;

    @Autowired
    private Persister<DatabaseEntry> databaseEntryPersister;

    @Autowired
    public BioSequencePersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    @Transactional
    public BioSequence persist( BioSequence bioSequence ) {
        if ( bioSequence == null || !this.isTransient( bioSequence ) )
            return bioSequence;

        BioSequence existingBioSequence = bioSequenceDao.find( bioSequence );

        // try to avoid making the instance 'dirty' if we don't have to, to avoid updates.
        if ( existingBioSequence != null ) {
            if ( AbstractPersister.log.isDebugEnabled() )
                AbstractPersister.log.debug( "Found existing: " + existingBioSequence );
            return existingBioSequence;
        }

        return this.persistNewBioSequence( bioSequence );
    }

    @Override
    @Transactional
    public BioSequence persistOrUpdate( BioSequence bioSequence ) {
        if ( bioSequence == null )
            return null;

        /*
         * Note that this method is only really used by the ArrayDesignSequencePersister: it's for filling in
         * information about probes on arrays.
         */

        BioSequence existingBioSequence = bioSequenceDao.find( bioSequence );

        if ( existingBioSequence == null ) {
            if ( AbstractPersister.log.isDebugEnabled() )
                AbstractPersister.log.debug( "Creating new: " + bioSequence );
            return this.persistNewBioSequence( bioSequence );
        }

        if ( AbstractPersister.log.isDebugEnabled() )
            AbstractPersister.log.debug( "Found existing: " + existingBioSequence );

        // the sequence is the main field we might update.
        if ( bioSequence.getSequence() != null && !bioSequence.getSequence()
                .equals( existingBioSequence.getSequence() ) ) {
            if ( AbstractPersister.log.isDebugEnabled() )
                log.debug( "Updating sequence:" + bioSequence.getName() + "\nFROM:" + existingBioSequence.getSequence()
                        + "\nTO:" + bioSequence.getSequence() + "\n" );
            existingBioSequence.setSequence( bioSequence.getSequence() );
        }

        /*
         * Can do for all fields that might not be the same: anything besides the name and taxon.
         */
        if ( bioSequence.getDescription() != null && !bioSequence.getDescription()
                .equals( existingBioSequence.getDescription() ) ) {
            existingBioSequence.setDescription( bioSequence.getDescription() );
        }

        if ( bioSequence.getType() != null && !bioSequence.getType().equals( existingBioSequence.getType() ) ) {
            existingBioSequence.setType( bioSequence.getType() );
        }

        if ( bioSequence.getFractionRepeats() != null && !bioSequence.getFractionRepeats()
                .equals( existingBioSequence.getFractionRepeats() ) ) {
            existingBioSequence.setFractionRepeats( bioSequence.getFractionRepeats() );
        }

        if ( bioSequence.getLength() != null && !bioSequence.getLength().equals( existingBioSequence.getLength() ) ) {
            existingBioSequence.setLength( bioSequence.getLength() );
        }

        if ( bioSequence.getIsCircular() != null && !bioSequence.getIsCircular()
                .equals( existingBioSequence.getIsCircular() ) ) {
            existingBioSequence.setIsCircular( bioSequence.getIsCircular() );
        }

        if ( bioSequence.getPolymerType() != null && !bioSequence.getPolymerType()
                .equals( existingBioSequence.getPolymerType() ) ) {
            existingBioSequence.setPolymerType( bioSequence.getPolymerType() );
        }

        if ( bioSequence.getSequenceDatabaseEntry() != null && !bioSequence.getSequenceDatabaseEntry()
                .equals( existingBioSequence.getSequenceDatabaseEntry() ) ) {
            existingBioSequence.setSequenceDatabaseEntry(
                    ( DatabaseEntry ) databaseEntryPersister.persist( bioSequence.getSequenceDatabaseEntry() ) );
        }

        // I don't fully understand what's going on here, but if we don't do this we fail to synchronize changes.
        this.getSession().evict( existingBioSequence );
        bioSequenceDao.update( existingBioSequence ); // also tried merge, without the update, doesn't work.
        return existingBioSequence;

    }

    private BioSequence persistNewBioSequence( BioSequence bioSequence ) {
        if ( AbstractPersister.log.isDebugEnabled() )
            AbstractPersister.log.debug( "Creating new: " + bioSequence );

        this.persistBioSequenceAssociations( bioSequence );

        assert bioSequence.getTaxon().getId() != null;
        return bioSequenceDao.create( bioSequence );
    }

    private void persistBioSequenceAssociations( BioSequence bioSequence ) {
        this.fillInBioSequenceTaxon( bioSequence );

        if ( bioSequence.getSequenceDatabaseEntry() != null
                && bioSequence.getSequenceDatabaseEntry().getExternalDatabase().getId() == null ) {
            bioSequence.getSequenceDatabaseEntry().setExternalDatabase(
                    externalDatabasePersister.persist( bioSequence.getSequenceDatabaseEntry().getExternalDatabase() ) );
        }

        for ( BioSequence2GeneProduct bioSequence2GeneProduct : bioSequence.getBioSequence2GeneProduct() ) {
            bioSequence2GeneProductPersister.persist( bioSequence2GeneProduct );
        }
    }

    private void fillInBioSequenceTaxon( BioSequence bioSequence ) {
        Taxon t = bioSequence.getTaxon();
        if ( t == null )
            throw new IllegalArgumentException( "BioSequence Taxon cannot be null" );
        if ( !taxonPersister.isTransient( t ) )
            return;

        bioSequence.setTaxon( taxonPersister.persist( t ) );

    }


}
