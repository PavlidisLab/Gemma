package ubic.gemma.persistence.service.association;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.association.Gene2GeneProteinAssociation;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.AbstractService;

import java.util.Collection;

/**
 * Gene2GeneProteinAssociationService class providing functionality for handling Gene2geneProteinAssociations
 *
 * @author ldonnison
 */
@Service
public class Gene2GeneProteinAssociationServiceImpl extends AbstractService<Gene2GeneProteinAssociation>
        implements Gene2GeneProteinAssociationService {

    private final Gene2GeneProteinAssociationDao gene2GeneProteinAssociationDao;

    @Autowired
    public Gene2GeneProteinAssociationServiceImpl( Gene2GeneProteinAssociationDao mainDao ) {
        super( mainDao );
        this.gene2GeneProteinAssociationDao = mainDao;
    }

    @Override
    public void removeAll( Collection<Gene2GeneProteinAssociation> associations ) {
        this.gene2GeneProteinAssociationDao.remove( associations );
    }

    @Override
    public void thaw( Gene2GeneProteinAssociation association ) {
        this.gene2GeneProteinAssociationDao.thaw( association );
    }

    @Override
    public Collection<Gene2GeneProteinAssociation> findProteinInteractionsForGene( Gene gene ) {
        return this.gene2GeneProteinAssociationDao.findProteinInteractionsForGene( gene );
    }

}