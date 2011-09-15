package ubic.gemma.model.association.phenotype;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

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
    // TODO should be and HSQL query
    public Collection<Long> findByPhenotype( String... phenotypesValues ) {

        Collection<Long> genesNCBI = new HashSet<Long>();

        int numberOfterms = phenotypesValues.length;

        if ( numberOfterms == 0 ) {
            return null;
        }

        String queryStringPart1 = "SELECT chr.id FROM CHROMOSOME_FEATURE chr";

        String queryStringPart2 = "";

        for ( int i = 0; i < numberOfterms; i++ ) {
            queryStringPart2 = queryStringPart2
                    + " INNER JOIN ( SELECT t"
                    + i
                    + ".ID FROM CHROMOSOME_FEATURE t"
                    + i
                    + " WHERE id IN (SELECT gene_fk FROM CHARACTERISTIC INNER JOIN PHENOTYPE_ASSOCIATION ON PHENOTYPE_ASSOCIATION.ID = CHARACTERISTIC.PHENOTYPE_ASSOCIATION_FK WHERE VALUE = '"
                    + phenotypesValues[i] + "'))t" + i + " ";
        }

        String queryStringPart3 = "ON ";

        for ( int i = 0; i < numberOfterms; i++ ) {
            queryStringPart3 = queryStringPart3 + "chr.id = t" + i + ".id ";

            // if not the last one
            if ( i != numberOfterms - 1 ) {
                queryStringPart3 = queryStringPart3 + "AND ";
            }
        }

        String queryString = queryStringPart1 + queryStringPart2 + queryStringPart3;

        org.hibernate.SQLQuery queryObject = this.getSession().createSQLQuery( queryString );
        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        while ( results.next() ) {
            genesNCBI.add( ( ( BigInteger ) results.get( 0 ) ).longValue() );
        }

        return genesNCBI;
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
