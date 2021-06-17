package ubic.gemma.persistence.persister;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;

@Service
public class BioSequence2GeneProductPersister extends AbstractPersister<BioSequence2GeneProduct> {

    @Autowired
    private Persister<BlatAssociation> blatAssociationPersister;

    @Autowired
    public BioSequence2GeneProductPersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    @Transactional
    public BioSequence2GeneProduct persist( BioSequence2GeneProduct bioSequence2GeneProduct ) {
        if ( bioSequence2GeneProduct == null )
            return null;
        if ( !this.isTransient( bioSequence2GeneProduct ) )
            return bioSequence2GeneProduct;

        if ( bioSequence2GeneProduct instanceof BlatAssociation ) {
            return blatAssociationPersister.persist( ( BlatAssociation ) bioSequence2GeneProduct );
        }
        throw new UnsupportedOperationException(
                "Don't know how to deal with " + bioSequence2GeneProduct.getClass().getName() );

    }
}
