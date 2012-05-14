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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
    public Collection<Gene> findGeneWithPhenotypes( Set<String> phenotypesValueUri, String taxon ) {

        for(String url : phenotypesValueUri){
            if(url.isEmpty()) phenotypesValueUri.remove( url );
        }
        if(phenotypesValueUri.isEmpty()) return new ArrayList<Gene>();
        
        Criteria geneQueryCriteria = super.getSession().createCriteria( Gene.class );

        geneQueryCriteria.setResultTransformer( CriteriaSpecification.DISTINCT_ROOT_ENTITY )
                .createCriteria( "phenotypeAssociations" ).createCriteria( "phenotypes" )
                .add( Restrictions.in( "valueUri", phenotypesValueUri ) );

        if ( taxon != null && !taxon.equalsIgnoreCase( "" ) ) {
            geneQueryCriteria.createCriteria( "taxon" ).add( Restrictions.like( "commonName", taxon ) );
        }
        return geneQueryCriteria.list();
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

    @Override
    @SuppressWarnings("unchecked")
    /** find all PhenotypeAssociation for a specific gene id */
    public Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneId( Long geneId ) {

        Criteria geneQueryCriteria = super.getSession().createCriteria( PhenotypeAssociation.class )
                .setResultTransformer( CriteriaSpecification.DISTINCT_ROOT_ENTITY ).createCriteria( "gene" )
                .add( Restrictions.like( "id", geneId ) );

        return geneQueryCriteria.list();
    }

    @Override
    @SuppressWarnings("unchecked")
    /** find all PhenotypeAssociation for a specific NCBI id */
    public Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneNCBI( Integer geneNCBI ) {

        Criteria geneQueryCriteria = super.getSession().createCriteria( PhenotypeAssociation.class )
                .setResultTransformer( CriteriaSpecification.DISTINCT_ROOT_ENTITY ).createCriteria( "gene" )
                .add( Restrictions.like( "ncbiGeneId", geneNCBI ) );

        return geneQueryCriteria.list();
    }

    /** find MGED category terms currently used in the database by evidence */
    @Override
    public Collection<CharacteristicValueObject> findEvidenceMgedCategoryTerms() {

        Collection<CharacteristicValueObject> mgedCategory = new ArrayList<CharacteristicValueObject>();

        String queryString = "SELECT distinct CATEGORY_URI, category FROM PHENOTYPE_ASSOCIATION join INVESTIGATION on PHENOTYPE_ASSOCIATION.EXPERIMENT_FK = INVESTIGATION.ID join CHARACTERISTIC on CHARACTERISTIC.INVESTIGATION_FK= INVESTIGATION.ID";
        org.hibernate.SQLQuery queryObject = this.getSession().createSQLQuery( queryString );

        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        while ( results.next() ) {

            CharacteristicValueObject characteristicValueObject = new CharacteristicValueObject();
            characteristicValueObject.setCategoryUri( ( String ) results.get( 0 ) );
            characteristicValueObject.setCategory( ( String ) results.get( 1 ) );
            mgedCategory.add( characteristicValueObject );
        }
        results.close();

        return mgedCategory;
    }

    /** delete all evidences from a specific external database */
    @SuppressWarnings("unchecked")
    @Override
    public Collection<PhenotypeAssociation> findEvidencesWithExternalDatabaseName( String externalDatabaseName ) {

        Criteria geneQueryCriteria = super.getSession().createCriteria( PhenotypeAssociation.class )
                .setResultTransformer( CriteriaSpecification.DISTINCT_ROOT_ENTITY ).createCriteria( "evidenceSource" )
                .createCriteria( "externalDatabase" ).add( Restrictions.like( "name", externalDatabaseName ) );

        return geneQueryCriteria.list();
    }

    /** find all public phenotypes associated with genes */
    @Override
    public HashMap<String, HashSet<Integer>> findPublicPhenotypesGenesAssociations() {

        HashMap<String, HashSet<Integer>> phenotypesGenesAssociations = new HashMap<String, HashSet<Integer>>();

        String queryString = "SELECT CHROMOSOME_FEATURE.NCBI_GENE_ID,CHARACTERISTIC.VALUE_URI FROM CHARACTERISTIC join PHENOTYPE_ASSOCIATION on CHARACTERISTIC.PHENOTYPE_ASSOCIATION_FK=PHENOTYPE_ASSOCIATION.ID join CHROMOSOME_FEATURE on CHROMOSOME_FEATURE.id=PHENOTYPE_ASSOCIATION.GENE_FK  join acl_object_identity on PHENOTYPE_ASSOCIATION.id = acl_object_identity.object_id_identity join acl_entry on acl_entry.acl_object_identity = acl_object_identity.id join acl_class on acl_class.id=acl_object_identity.object_id_class where sid=4 and mask=1 and acl_class.class in ('ubic.gemma.model.association.phenotype.LiteratureEvidenceImpl','ubic.gemma.model.association.phenotype.GenericEvidenceImpl','ubic.gemma.model.association.phenotype.ExperimentalEvidenceImpl','ubic.gemma.model.association.phenotype.DifferentialExpressionEvidenceImpl','ubic.gemma.model.association.phenotype.UrlEvidenceImpl')";

        org.hibernate.SQLQuery queryObject = this.getSession().createSQLQuery( queryString );

        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        while ( results.next() ) {

            Integer geneNcbiId = ( Integer ) results.get( 0 );
            String valueUri = ( String ) results.get( 1 );

            if ( phenotypesGenesAssociations.containsKey( valueUri ) ) {
                phenotypesGenesAssociations.get( valueUri ).add( geneNcbiId );
            } else {
                HashSet<Integer> genesNCBI = new HashSet<Integer>();
                genesNCBI.add( geneNcbiId );
                phenotypesGenesAssociations.put( valueUri, genesNCBI );
            }
        }
        results.close();

        return phenotypesGenesAssociations;
    }

    /** find all phenotypes associated with genes for a user */
    @Override
    public HashMap<String, HashSet<Integer>> findPrivatePhenotypesGenesAssociations( String userName ) {

        HashMap<String, HashSet<Integer>> phenotypesGenesAssociations = new HashMap<String, HashSet<Integer>>();

        String queryString = "SELECT CHROMOSOME_FEATURE.NCBI_GENE_ID,CHARACTERISTIC.VALUE_URI FROM CHARACTERISTIC join PHENOTYPE_ASSOCIATION on CHARACTERISTIC.PHENOTYPE_ASSOCIATION_FK=PHENOTYPE_ASSOCIATION.ID join CHROMOSOME_FEATURE on CHROMOSOME_FEATURE.id=PHENOTYPE_ASSOCIATION.GENE_FK  join acl_object_identity on PHENOTYPE_ASSOCIATION.id = acl_object_identity.object_id_identity join acl_entry on acl_entry.acl_object_identity = acl_object_identity.id join acl_class on acl_class.id=acl_object_identity.object_id_class join acl_sid on acl_sid.id=acl_object_identity.owner_sid where mask=1 and acl_sid.sid='"
                + userName
                + "'and acl_class.class in ('ubic.gemma.model.association.phenotype.LiteratureEvidenceImpl','ubic.gemma.model.association.phenotype.GenericEvidenceImpl','ubic.gemma.model.association.phenotype.ExperimentalEvidenceImpl','ubic.gemma.model.association.phenotype.DifferentialExpressionEvidenceImpl','ubic.gemma.model.association.phenotype.UrlEvidenceImpl')";

        org.hibernate.SQLQuery queryObject = this.getSession().createSQLQuery( queryString );

        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        while ( results.next() ) {

            Integer geneNcbiId = ( Integer ) results.get( 0 );
            String valueUri = ( String ) results.get( 1 );

            if ( phenotypesGenesAssociations.containsKey( valueUri ) ) {
                phenotypesGenesAssociations.get( valueUri ).add( geneNcbiId );
            } else {
                HashSet<Integer> genesNCBI = new HashSet<Integer>();
                genesNCBI.add( geneNcbiId );
                phenotypesGenesAssociations.put( valueUri, genesNCBI );
            }
        }
        results.close();

        return phenotypesGenesAssociations;
    }

    /** find all phenotypes associated with genes */
    @Override
    public HashMap<String, HashSet<Integer>> findAllPhenotypesGenesAssociations() {

        HashMap<String, HashSet<Integer>> phenotypesGenesAssociations = new HashMap<String, HashSet<Integer>>();

        String queryString = "SELECT CHROMOSOME_FEATURE.NCBI_GENE_ID,CHARACTERISTIC.VALUE_URI FROM CHARACTERISTIC join PHENOTYPE_ASSOCIATION on CHARACTERISTIC.PHENOTYPE_ASSOCIATION_FK=PHENOTYPE_ASSOCIATION.ID join CHROMOSOME_FEATURE on CHROMOSOME_FEATURE.id=PHENOTYPE_ASSOCIATION.GENE_FK";

        org.hibernate.SQLQuery queryObject = this.getSession().createSQLQuery( queryString );

        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        while ( results.next() ) {

            Integer geneNcbiId = ( Integer ) results.get( 0 );
            String valueUri = ( String ) results.get( 1 );

            if ( phenotypesGenesAssociations.containsKey( valueUri ) ) {
                phenotypesGenesAssociations.get( valueUri ).add( geneNcbiId );
            } else {
                HashSet<Integer> genesNCBI = new HashSet<Integer>();
                genesNCBI.add( geneNcbiId );
                phenotypesGenesAssociations.put( valueUri, genesNCBI );
            }
        }
        results.close();

        return phenotypesGenesAssociations;
    }

    /** find all public phenotypes associated with genes on a specific taxon and containing the valuesUri */
    @Override
    public HashMap<String, HashSet<Integer>> findPublicPhenotypesGenesAssociations( String taxon, Set<String> valuesUri ) {

        HashMap<String, HashSet<Integer>> phenotypesGenesAssociations = new HashMap<String, HashSet<Integer>>();

        String queryString = "SELECT CHROMOSOME_FEATURE.NCBI_GENE_ID, CHARACTERISTIC.VALUE_URI FROM CHARACTERISTIC join PHENOTYPE_ASSOCIATION on CHARACTERISTIC.PHENOTYPE_ASSOCIATION_FK=PHENOTYPE_ASSOCIATION.ID join CHROMOSOME_FEATURE on CHROMOSOME_FEATURE.id=PHENOTYPE_ASSOCIATION.GENE_FK  join acl_object_identity on PHENOTYPE_ASSOCIATION.id = acl_object_identity.object_id_identity join acl_entry on acl_entry.acl_object_identity = acl_object_identity.id join acl_class on acl_class.id=acl_object_identity.object_id_class join TAXON on TAXON.id=CHROMOSOME_FEATURE.TAXON_FK where sid=4 and mask=1 and acl_class.class in ('ubic.gemma.model.association.phenotype.LiteratureEvidenceImpl','ubic.gemma.model.association.phenotype.GenericEvidenceImpl','ubic.gemma.model.association.phenotype.ExperimentalEvidenceImpl','ubic.gemma.model.association.phenotype.DifferentialExpressionEvidenceImpl','ubic.gemma.model.association.phenotype.UrlEvidenceImpl') and TAXON.COMMON_name='"
                + taxon + "'";

        String queryStringValuesUri = "";

        if ( !valuesUri.isEmpty() ) {

            queryStringValuesUri = " AND CHARACTERISTIC.VALUE_URI in('";

            for ( String value : valuesUri ) {
                queryStringValuesUri = queryStringValuesUri + value + "','";
            }

            queryStringValuesUri = queryStringValuesUri.substring( 0, queryStringValuesUri.length() - 2 ) + ")";
        }

        org.hibernate.SQLQuery queryObject = this.getSession().createSQLQuery( queryString + queryStringValuesUri );

        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        while ( results.next() ) {

            Integer geneNcbiId = ( Integer ) results.get( 0 );
            String valueUri = ( String ) results.get( 1 );

            if ( phenotypesGenesAssociations.containsKey( valueUri ) ) {
                phenotypesGenesAssociations.get( valueUri ).add( geneNcbiId );
            } else {
                HashSet<Integer> genesNCBI = new HashSet<Integer>();
                genesNCBI.add( geneNcbiId );
                phenotypesGenesAssociations.put( valueUri, genesNCBI );
            }
        }
        results.close();

        return phenotypesGenesAssociations;
    }
}
