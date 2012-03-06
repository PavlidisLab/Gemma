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
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.CriteriaSpecification;
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
    @Override
    public Collection<Gene> findGeneWithPhenotypes( Set<String> phenotypesValueUri ) {
        return this
                .getHibernateTemplate()
                .findByNamedParam(
                        "select distinct g from GeneImpl as g inner join fetch g.phenotypeAssociations pheAsso inner join fetch pheAsso.phenotypes phe where phe.valueUri in (:phenotypesValueUri)",
                        "phenotypesValueUri", phenotypesValueUri );
    }

    /**
     * count the number of Genes with a public phenotype
     */
    @Override
    public Long countGenesWithPublicPhenotype( Collection<String> phenotypesUri ) {

        // type of evidence
        String queryEvidenceTypes = "('" + LiteratureEvidenceImpl.class.getName() + "','"
                + GenericEvidenceImpl.class.getName() + "','" + ExperimentalEvidenceImpl.class.getName() + "','"
                + DifferentialExpressionEvidenceImpl.class.getName() + "','" + UrlEvidenceImpl.class.getName() + "')";

        String endQuery = queryEvidenceTypes + " and value_uri in(";

        for ( String phenotypeUri : phenotypesUri ) {

            endQuery = endQuery + "'" + phenotypeUri + "', ";
        }

        endQuery = endQuery.substring( 0, endQuery.length() - 2 ) + ")";

        long value = 0;

        String queryString = "select count(distinct gene_fk) from acl_entry join acl_object_identity ON acl_entry.acl_object_identity = acl_object_identity.id join CHARACTERISTIC on CHARACTERISTIC.phenotype_association_fk =acl_object_identity.object_id_identity join PHENOTYPE_ASSOCIATION on PHENOTYPE_ASSOCIATION.id =acl_object_identity.object_id_identity join acl_class on acl_class.id=acl_object_identity.object_id_class where sid=4 and mask=1 and acl_class.class in "
                + endQuery;

        org.hibernate.SQLQuery queryObject = this.getSession().createSQLQuery( queryString );

        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        if ( results.next() ) {
            value = ( ( BigInteger ) results.get( 0 ) ).longValue();
        }

        return new Long( value );
    }

    /** count the number of Genes with a public or private phenotype */
    @Override
    public Long countGenesWithPhenotype( Collection<String> phenotypesUri ) {

        String endQuery = "";

        for ( String phenotypeUri : phenotypesUri ) {

            endQuery = endQuery + "'" + phenotypeUri + "', ";
        }

        endQuery = endQuery.substring( 0, endQuery.length() - 2 ) + ")";

        long value = 0;

        String queryString = "select count(distinct gene_fk) from CHARACTERISTIC join PHENOTYPE_ASSOCIATION on CHARACTERISTIC.phenotype_association_fk=PHENOTYPE_ASSOCIATION.id where value_uri in ("
                + endQuery;

        org.hibernate.SQLQuery queryObject = this.getSession().createSQLQuery( queryString );

        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        if ( results.next() ) {
            value = ( ( BigInteger ) results.get( 0 ) ).longValue();
        }

        return new Long( value );
    }

    /** count the number of Genes with private phenotype */
    @Override
    public Long countGenesWithPrivatePhenotype( Collection<String> phenotypesUri, String userName ) {

        // type of evidence
        String queryEvidenceTypes = "('" + LiteratureEvidenceImpl.class.getName() + "','"
                + GenericEvidenceImpl.class.getName() + "','" + ExperimentalEvidenceImpl.class.getName() + "','"
                + DifferentialExpressionEvidenceImpl.class.getName() + "','" + UrlEvidenceImpl.class.getName() + "')";

        String endQuery = queryEvidenceTypes + " and value_uri in(";

        for ( String phenotypeUri : phenotypesUri ) {

            endQuery = endQuery + "'" + phenotypeUri + "', ";
        }

        endQuery = endQuery.substring( 0, endQuery.length() - 2 ) + ")";

        long value = 0;

        String queryString = "select count(distinct gene_fk) from acl_entry join acl_object_identity ON acl_entry.acl_object_identity = acl_object_identity.id join CHARACTERISTIC on CHARACTERISTIC.phenotype_association_fk =acl_object_identity.object_id_identity join PHENOTYPE_ASSOCIATION on PHENOTYPE_ASSOCIATION.id =acl_object_identity.object_id_identity join acl_class on acl_class.id=acl_object_identity.object_id_class join acl_sid on acl_sid.id=acl_object_identity.owner_sid where mask=1 and acl_sid.sid='"
                + userName + "'and acl_class.class in " + endQuery;

        org.hibernate.SQLQuery queryObject = this.getSession().createSQLQuery( queryString );

        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        if ( results.next() ) {
            value = ( ( BigInteger ) results.get( 0 ) ).longValue();
        }

        return new Long( value );
    }

    /**
     * find all phenotypes
     */
    @Override
    public Set<CharacteristicValueObject> loadAllPhenotypes() {

        Set<CharacteristicValueObject> phenotypes = new HashSet<CharacteristicValueObject>();

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

    /** load all valueURI of Phenotype in the database */
    @Override
    public Set<String> loadAllPhenotypesUri() {
        Set<String> phenotypesURI = new HashSet<String>();

        // TODO make hsql query
        String queryString = "select value_uri from CHARACTERISTIC where phenotype_association_fk is not null group by value";
        org.hibernate.SQLQuery queryObject = this.getSession().createSQLQuery( queryString );

        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        while ( results.next() ) {

            String valueUri = ( String ) results.get( 0 );

            phenotypesURI.add( valueUri );
        }
        results.close();

        return phenotypesURI;
    }

    @Override
    @SuppressWarnings("unchecked")
    /** find PhenotypeAssociations associated with a BibliographicReference */
    public Collection<PhenotypeAssociation> findPhenotypesForBibliographicReference( String pubMedID ) {

        Collection<PhenotypeAssociation> phenotypeAssociationsFound = new HashSet<PhenotypeAssociation>();

        // Literature Evidence have BibliographicReference
        Criteria geneQueryCriteria = super.getSession().createCriteria( LiteratureEvidence.class )
                .setResultTransformer( CriteriaSpecification.DISTINCT_ROOT_ENTITY ).createCriteria( "citation" )
                .createCriteria( "pubAccession" ).add( Restrictions.like( "accession", pubMedID ) );

        phenotypeAssociationsFound.addAll( geneQueryCriteria.list() );

        // Experimental Evidence have a primary BibliographicReference
        geneQueryCriteria = super.getSession().createCriteria( ExperimentalEvidence.class )
                .setResultTransformer( CriteriaSpecification.DISTINCT_ROOT_ENTITY ).createCriteria( "experiment" )
                .createCriteria( "primaryPublication" ).createCriteria( "pubAccession" )
                .add( Restrictions.like( "accession", pubMedID ) );

        phenotypeAssociationsFound.addAll( geneQueryCriteria.list() );

        // Experimental Evidence have relevant BibliographicReference
        geneQueryCriteria = super.getSession().createCriteria( ExperimentalEvidence.class )
                .setResultTransformer( CriteriaSpecification.DISTINCT_ROOT_ENTITY ).createCriteria( "experiment" )
                .createCriteria( "otherRelevantPublications" ).createCriteria( "pubAccession" )
                .add( Restrictions.like( "accession", pubMedID ) );

        phenotypeAssociationsFound.addAll( geneQueryCriteria.list() );

        return phenotypeAssociationsFound;

    }
}
