package ubic.gemma.model.association.phenotype;

import java.util.Collection;
import java.util.HashSet;

import org.hibernate.Criteria;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.persistence.AbstractDao;

@Repository
public class PhenotypeAssociationDaoImpl extends AbstractDao<PhenotypeAssociation> implements PhenotypeAssociationDao {

    @Autowired
    public PhenotypeAssociationDaoImpl( SessionFactory sessionFactory ) {
        super( PhenotypeAssociationImpl.class );
        super.setSessionFactory( sessionFactory );
    }

    /** find Genes link to a phenotype */
    @SuppressWarnings("unchecked")
    public Collection<Gene> findByPhenotype( String phenotypeValue ) {

        Criteria genes = super.getSession().createCriteria( Gene.class );
        genes.setResultTransformer( Criteria.DISTINCT_ROOT_ENTITY ).createCriteria( "phenotypeAssociations" )
                .createCriteria( "phenotypes" ).add( Restrictions.like( "value", phenotypeValue ) );

        return genes.list();

    }

    /** find all phenotypes */
    public Collection<CharacteristicValueObject> findAllPhenotypes() {

        Collection<CharacteristicValueObject> phenotypes = new HashSet<CharacteristicValueObject>();

        // TODO make hsql query
        String queryString = "select value,value_uri,category,category_uri from CHARACTERISTIC where phenotype_association_fk is not null group by value";
        org.hibernate.SQLQuery queryObject = this.getSession().createSQLQuery( queryString );

        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        while ( results.next() ) {

            String value = ( String ) results.get( 0 );
            String valueUri = ( String ) results.get( 1 );
            String category = ( String ) results.get( 2 );
            String categoryUri = ( String ) results.get( 3 );

            CharacteristicValueObject characteristicValueObject = new CharacteristicValueObject( value, category,
                    valueUri, categoryUri );
            phenotypes.add( characteristicValueObject );
        }
        results.close();

        return phenotypes;
    }
}
