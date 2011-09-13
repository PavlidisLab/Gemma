package ubic.gemma.model.association.phenotype;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.SessionFactory;
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

    /** find Genes for specific phenotypes */
    @SuppressWarnings("unchecked")
    public Collection<Gene> findByPhenotype( String... phenotypesValues ) {

        if ( phenotypesValues.length == 0 ) {
            return null;
        }

        return this
                .getHibernateTemplate()
                .findByNamedParam(
                        "select distinct g from GeneImpl as g inner join fetch g.phenotypeAssociations pheAsso inner join fetch pheAsso.phenotypes phe where phe.value in (:phenotypesValues)",
                        "phenotypesValues", phenotypesValues );
    }

    public Collection<CharacteristicValueObject> findAllPhenotypes() {

        Collection<CharacteristicValueObject> phenotypes = new HashSet<CharacteristicValueObject>();

        String queryString = "select value, value_uri, category, category_uri, count(*) from CHARACTERISTIC where phenotype_association_fk is not null group by value";
        org.hibernate.SQLQuery queryObject = this.getSession().createSQLQuery( queryString );

        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        while ( results.next() ) {

            String value = ( String ) results.get( 0 );
            String valueUri = ( String ) results.get( 1 );
            String category = ( String ) results.get( 2 );
            String categoryUri = ( String ) results.get( 3 );
            long count = ( ( BigInteger ) results.get( 4 ) ).longValue();

            CharacteristicValueObject characteristicValueObject = new CharacteristicValueObject( value, category,
                    valueUri, categoryUri );
            characteristicValueObject.setOccurence( count );
            phenotypes.add( characteristicValueObject );
        }
        results.close();

        return phenotypes;
    }
}
