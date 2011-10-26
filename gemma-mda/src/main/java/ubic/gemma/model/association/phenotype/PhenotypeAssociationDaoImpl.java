/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.association.phenotype;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;

import org.hibernate.Criteria;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Restrictions;
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

    /** find Genes link to a phenotype */
    @SuppressWarnings("unchecked")
    @Override
    public Collection<PhenotypeAssociation> findByPhenotype( String phenotypeValue ) {

        Criteria geneQueryCriteria = super.getSession().createCriteria( PhenotypeAssociation.class )
                .setResultTransformer( CriteriaSpecification.DISTINCT_ROOT_ENTITY ).createCriteria( "phenotypes" )
                .add( Restrictions.like( "value", phenotypeValue ) );

        return geneQueryCriteria.list();

    }

    /**
     * count the number of Genes with a phenotype
     */
    @Override
    public Long countGenesWithPhenotype( String phenotypeValue ) {

        Long value = null;

        // TODO make hsql query
        String queryString = "select count( distinct GENE_FK) from PHENOTYPE_ASSOCIATION where id in( SELECT PHENOTYPE_ASSOCIATION_FK FROM CHARACTERISTIC where value='"
                + phenotypeValue + "')";
        org.hibernate.SQLQuery queryObject = this.getSession().createSQLQuery( queryString );

        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        if ( results.next() ) {
            value = ( ( BigInteger ) results.get( 0 ) ).longValue();
        }

        return value;
    }

    /**
     * find all phenotypes
     */
    @Override
    public Collection<CharacteristicValueObject> loadAllPhenotypes() {

        Collection<CharacteristicValueObject> phenotypes = new HashSet<CharacteristicValueObject>();

        // TODO make hsql query
        String queryString = "select distinct value,value_uri,category,category_uri from CHARACTERISTIC where phenotype_association_fk is not null group by value";
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
