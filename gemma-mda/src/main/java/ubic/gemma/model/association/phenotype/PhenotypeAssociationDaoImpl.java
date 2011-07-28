package ubic.gemma.model.association.phenotype;

import java.util.Collection;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.AbstractDao;

@Repository
public class PhenotypeAssociationDaoImpl extends AbstractDao<PhenotypeAssociation> implements PhenotypeAssociationDao {

    @Autowired
    public PhenotypeAssociationDaoImpl( SessionFactory sessionFactory ) {
        super( PhenotypeAssociationImpl.class );
        super.setSessionFactory( sessionFactory );
    }

    // Should it be placed in the GeneDaoImpl ???
    /** find Genes for a specific phenotype */
    @SuppressWarnings("unchecked")
    public Collection<Gene> findByPhenotype( String value ) {
        return this
                .getHibernateTemplate()
                .findByNamedParam(
                        "select distinct g from GeneImpl as g inner join fetch g.phenotypeAssociations pheAsso inner join fetch pheAsso.phenotypes phe where phe.value = :value",
                        "value", value );
    }

}
