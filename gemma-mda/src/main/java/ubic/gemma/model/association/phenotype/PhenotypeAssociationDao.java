package ubic.gemma.model.association.phenotype;

import java.util.Collection;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.BaseDao;

public interface PhenotypeAssociationDao extends BaseDao<PhenotypeAssociation> {

    /** find Genes for a specific phenotype */
    public Collection<Gene> findByPhenotype( String value );
}
