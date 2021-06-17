package ubic.gemma.persistence.persister.gene;

import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.persistence.persister.Persister;

public interface GeneProductPersister extends Persister<GeneProduct> {

    void updateGeneProduct( GeneProduct existingGeneProduct, GeneProduct updatedGeneProductInfo );

    void fillInGeneProductAssociations( GeneProduct geneProduct );
}
