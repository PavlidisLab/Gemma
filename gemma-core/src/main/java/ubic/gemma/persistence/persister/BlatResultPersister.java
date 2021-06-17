package ubic.gemma.persistence.persister;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.BlatResultDao;
import ubic.gemma.persistence.util.SequenceBinUtils;

@Service
public class BlatResultPersister extends AbstractPersister<BlatResult> {

    @Autowired
    private BlatResultDao blatResultDao;

    @Autowired
    private ChromosomePersister chromosomePersister;

    @Autowired
    private Persister<ExternalDatabase> externalDatabasePersister;

    @Autowired
    private Persister<BioSequence> bioSequencePersister;

    @Autowired
    public BlatResultPersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    /**
     * NOTE this method is not a regular 'persist' method: It does not use findOrCreate! A new result is made every
     * time.
     */
    @Override
    @Transactional
    public BlatResult persist( BlatResult blatResult ) {
        if ( !this.isTransient( blatResult ) )
            return blatResult;
        if ( blatResult.getQuerySequence() == null ) {
            throw new IllegalArgumentException( "Blat result with null query sequence" );
        }
        blatResult.setQuerySequence( bioSequencePersister.persist( blatResult.getQuerySequence() ) );
        blatResult.setTargetChromosome( this.chromosomePersister.persistChromosome( blatResult.getTargetChromosome(), null ) );
        blatResult.setSearchedDatabase( externalDatabasePersister.persist( blatResult.getSearchedDatabase() ) );
        if ( blatResult.getTargetAlignedRegion() != null )
            blatResult.setTargetAlignedRegion(
                    this.fillPhysicalLocationAssociations( blatResult.getTargetAlignedRegion() ) );
        return blatResultDao.create( blatResult );
    }

    private PhysicalLocation fillPhysicalLocationAssociations( PhysicalLocation physicalLocation ) {
        physicalLocation.setChromosome( this.chromosomePersister.persistChromosome( physicalLocation.getChromosome(), null ) );

        if ( physicalLocation.getBin() == null && physicalLocation.getNucleotide() != null
                && physicalLocation.getNucleotideLength() != null ) {
            physicalLocation.setBin( SequenceBinUtils.binFromRange( physicalLocation.getNucleotide().intValue(),
                    physicalLocation.getNucleotide().intValue() + physicalLocation.getNucleotideLength() ) );
        }

        return physicalLocation;
    }
}
