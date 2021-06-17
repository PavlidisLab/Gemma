package ubic.gemma.persistence.persister;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.model.genome.sequenceAnalysis.SequenceSimilaritySearchResult;

@Service
public class SequenceSimilaritySearchResultPersister extends AbstractPersister<SequenceSimilaritySearchResult> {

    @Autowired
    private Persister<BlatResult> blatResultPersister;

    @Autowired
    public SequenceSimilaritySearchResultPersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    @Transactional
    public SequenceSimilaritySearchResult persist(
            SequenceSimilaritySearchResult result ) {
        if ( result instanceof BlatResult ) {
            return blatResultPersister.persist( ( BlatResult ) result );
        }
        throw new UnsupportedOperationException( "Don't know how to persist a " + result.getClass().getName() );

    }
}
