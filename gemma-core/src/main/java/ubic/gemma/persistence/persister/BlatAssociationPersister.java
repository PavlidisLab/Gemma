package ubic.gemma.persistence.persister;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.BlatAssociationDao;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.BlatResultDao;

@Service
public class BlatAssociationPersister extends AbstractPersister<BlatAssociation> {

    @Autowired
    private BlatResultDao blatResultDao;

    @Autowired
    private Persister<BlatResult> blatResultPersister;

    @Autowired
    private BlatAssociationDao blatAssociationDao;

    @Autowired
    private Persister<GeneProduct> geneProductPersister;

    @Autowired
    private Persister<BioSequence> bioSequencePersister;

    @Autowired
    public BlatAssociationPersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    @Transactional
    public BlatAssociation persist( BlatAssociation association ) {
        BlatResult blatResult = association.getBlatResult();
        if ( blatResultPersister.isTransient( blatResult ) ) {
            blatResultDao.create( blatResult );
        }
        if ( AbstractPersister.log.isDebugEnabled() ) {
            AbstractPersister.log.debug( "Persisting " + association );
        }
        association.setGeneProduct( geneProductPersister.persist( association.getGeneProduct() ) );
        association.setBioSequence( bioSequencePersister.persist( association.getBioSequence() ) );
        return blatAssociationDao.create( association );
    }
}
