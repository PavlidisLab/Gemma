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

import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GeneEvidenceValueObject;
import ubic.gemma.persistence.AbstractDao;

@Repository
public class PhenotypeAssociationDaoImpl extends AbstractDao<PhenotypeAssociation> implements PhenotypeAssociationDao {

    private final static String TYPE_OF_EVIDENCE_SQL = "and acl_class.class in('ubic.gemma.model.association.phenotype.LiteratureEvidenceImpl','ubic.gemma.model.association.phenotype.GenericEvidenceImpl','ubic.gemma.model.association.phenotype.ExperimentalEvidenceImpl','ubic.gemma.model.association.phenotype.DifferentialExpressionEvidenceImpl','ubic.gemma.model.association.phenotype.UrlEvidenceImpl') ";

    @Autowired
    public PhenotypeAssociationDaoImpl( SessionFactory sessionFactory ) {
        super( PhenotypeAssociationImpl.class );
        super.setSessionFactory( sessionFactory );
    }

    /** load all valueURI of Phenotype in the database */
    @Override
    public Set<String> loadAllPhenotypesUri() {
        Set<String> phenotypesURI = new HashSet<String>();

        String queryString = "select distinct value_uri from CHARACTERISTIC where phenotype_association_fk is not null";
        org.hibernate.SQLQuery queryObject = this.getSession().createSQLQuery( queryString );

        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        while ( results.next() ) {
            phenotypesURI.add( ( String ) results.get( 0 ) );
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

    /** find all phenotypes associated with genes, doesnt take into account security */
    @Override
    public HashMap<String, HashSet<Integer>> findAllPhenotypesGenesAssociations( Taxon taxon ) {

        HashMap<String, HashSet<Integer>> phenotypesGenesAssociations = new HashMap<String, HashSet<Integer>>();

        String sqlQuery = "select CHROMOSOME_FEATURE.NCBI_GENE_ID, CHARACTERISTIC.VALUE_URI ";
        sqlQuery += getPhenotypesGenesAssociationsBeginQuery();

        sqlQuery += addTaxonToQuery( "where", taxon );

        populateGenesAssociations( sqlQuery, phenotypesGenesAssociations );

        return phenotypesGenesAssociations;
    }

    /** find all public phenotypes associated with genes on a specific taxon and containing the valuesUri */
    @Override
    public HashMap<String, HashSet<Integer>> findPublicPhenotypesGenesAssociations( Taxon taxon, Set<String> valuesUri ) {

        HashMap<String, HashSet<Integer>> phenotypesGenesAssociations = new HashMap<String, HashSet<Integer>>();

        String sqlQuery = "select CHROMOSOME_FEATURE.NCBI_GENE_ID, CHARACTERISTIC.VALUE_URI ";
        sqlQuery += getPhenotypesGenesAssociationsBeginQuery();

        // rule to find public
        sqlQuery += "where acl_entry.mask = 1 and acl_entry.sid = 4 ";
        sqlQuery += TYPE_OF_EVIDENCE_SQL;
        sqlQuery += addTaxonToQuery( "and", taxon );
        sqlQuery += addValuesUriToQuery( "and", valuesUri );

        populateGenesAssociations( sqlQuery, phenotypesGenesAssociations );

        return phenotypesGenesAssociations;
    }

    /** find all phenotypes associated with genes for a user */
    @Override
    public HashMap<String, HashSet<Integer>> findPrivatePhenotypesGenesAssociations( Taxon taxon,
            boolean showOnlyEditable, String userName, Collection<String> groups ) {

        HashMap<String, HashSet<Integer>> phenotypesGenesAssociations = new HashMap<String, HashSet<Integer>>();

        findPhenotypesGenesAssociationsOwnedByUser( userName, taxon, phenotypesGenesAssociations );

        if ( groups != null && !groups.isEmpty() ) {
            findPhenotypesGenesAssociationsSharedToUser( groups, taxon, showOnlyEditable, phenotypesGenesAssociations );
        }
        return phenotypesGenesAssociations;
    }

    /**
     * find Genes link to a phenotype taking into account private and public evidence Here on the case : 1- Admin 2-
     * user not logged in 3- user logged in only showing what he has read acces 4- user logged in only showing what he
     * has write acces
     */
    @Override
    public Collection<GeneEvidenceValueObject> findGeneWithPhenotypes( Set<String> phenotypesValueUri, Taxon taxon,
            String userName, Collection<String> groups, boolean isAdmin, boolean showOnlyEditable ) {

        HashMap<Long, GeneEvidenceValueObject> genesWithPhenotypes = new HashMap<Long, GeneEvidenceValueObject>();

        if ( phenotypesValueUri.isEmpty() ) {
            return genesWithPhenotypes.values();
        }

        String sqlSelectQuery = "select distinct CHROMOSOME_FEATURE.ID, CHROMOSOME_FEATURE.NCBI_GENE_ID, CHROMOSOME_FEATURE.OFFICIAL_NAME, CHROMOSOME_FEATURE.OFFICIAL_SYMBOL, TAXON.COMMON_NAME, CHARACTERISTIC.VALUE_URI ";

        String sqlQuery = sqlSelectQuery + getPhenotypesGenesAssociationsBeginQuery();

        sqlQuery += addValuesUriToQuery( "where", phenotypesValueUri );

        // admin can see all there is no specific condition on security

        // not log on
        if ( userName == null || userName.equals( "" ) ) {
            // show public only
            sqlQuery += "and acl_entry.sid = 4 and mask = 1 ";
        }
        // logging user
        else if ( !isAdmin ) {

            if ( showOnlyEditable ) {
                // only show what is owned by the user + group shared write permission
                sqlQuery += "and acl_sid.sid = '" + userName + "' ";
            } else {
                // show public + owned
                sqlQuery += " and ((acl_entry.sid = 4 and mask = 1) or acl_sid.sid='" + userName + "') ";
            }

            if ( groups != null && !groups.isEmpty() ) {
                // find the evidence the user has group permission, genesWithPhenotypes will be populate
                findPhenotypesGenesAssociationsSharedToUser( sqlSelectQuery, groups, taxon, showOnlyEditable,
                        genesWithPhenotypes );
            }
        }

        sqlQuery += addTaxonToQuery( "and", taxon );
        sqlQuery += TYPE_OF_EVIDENCE_SQL;

        populateGenesWithPhenotypes( sqlQuery, genesWithPhenotypes );

        return genesWithPhenotypes.values();
    }

    private void findPhenotypesGenesAssociationsOwnedByUser( String userName, Taxon taxon,
            HashMap<String, HashSet<Integer>> phenotypesGenesAssociations ) {

        String sqlQuery = "select CHROMOSOME_FEATURE.NCBI_GENE_ID, CHARACTERISTIC.VALUE_URI ";
        sqlQuery += getPhenotypesGenesAssociationsBeginQuery();
        sqlQuery += "where acl_sid.sid = '" + userName + "' ";
        sqlQuery += TYPE_OF_EVIDENCE_SQL;
        sqlQuery += addTaxonToQuery( "and", taxon );

        populateGenesAssociations( sqlQuery, phenotypesGenesAssociations );
    }

    private void findPhenotypesGenesAssociationsSharedToUser( Collection<String> groups, Taxon taxon,
            boolean showOnlyEditable, HashMap<String, HashSet<Integer>> phenotypesGenesAssociations ) {

        String sqlQuery = "select CHROMOSOME_FEATURE.NCBI_GENE_ID, CHARACTERISTIC.VALUE_URI ";
        sqlQuery += buildQueryPhenotypesAssoSharedToUser( showOnlyEditable, taxon, groups );

        populateGenesAssociations( sqlQuery, phenotypesGenesAssociations );
    }

    private void findPhenotypesGenesAssociationsSharedToUser( String sqlSelectQuery, Collection<String> groups,
            Taxon taxon, boolean showOnlyEditable, HashMap<Long, GeneEvidenceValueObject> genesWithPhenotypes ) {

        String sqlQuery = sqlSelectQuery;
        sqlQuery += buildQueryPhenotypesAssoSharedToUser( showOnlyEditable, taxon, groups );

        populateGenesWithPhenotypes( sqlQuery, genesWithPhenotypes );
    }

    private String buildQueryPhenotypesAssoSharedToUser( boolean showOnlyEditable, Taxon taxon,
            Collection<String> groups ) {

        String sqlQuery = getPhenotypesGenesAssociationsBeginQuery();
        sqlQuery += "join GROUP_AUTHORITY on acl_sid.sid = CONCAT('GROUP_', GROUP_AUTHORITY.AUTHORITY) ";
        sqlQuery += "join USER_GROUP on USER_GROUP.ID = GROUP_AUTHORITY.GROUP_FK ";

        if ( showOnlyEditable ) {
            // write acces
            sqlQuery += "where acl_entry.mask = 2 ";
        } else {
            // read acces
            sqlQuery += "where acl_entry.mask = 1 ";
        }

        sqlQuery += TYPE_OF_EVIDENCE_SQL;

        sqlQuery += addTaxonToQuery( "and", taxon );

        sqlQuery += addGroupsToQuery( "and", groups );

        return sqlQuery;
    }

    private String getPhenotypesGenesAssociationsBeginQuery() {
        String queryString = "";

        queryString += "from CHARACTERISTIC ";
        queryString += "join PHENOTYPE_ASSOCIATION on CHARACTERISTIC.PHENOTYPE_ASSOCIATION_FK = PHENOTYPE_ASSOCIATION.ID ";
        queryString += "join CHROMOSOME_FEATURE on CHROMOSOME_FEATURE.id = PHENOTYPE_ASSOCIATION.GENE_FK ";
        queryString += "join TAXON on TAXON.id = CHROMOSOME_FEATURE.TAXON_FK ";
        queryString += "join acl_object_identity on PHENOTYPE_ASSOCIATION.id = acl_object_identity.object_id_identity ";
        queryString += "join acl_entry on acl_entry.acl_object_identity = acl_object_identity.id ";
        queryString += "join acl_class on acl_class.id = acl_object_identity.object_id_class ";
        queryString += "join acl_sid on acl_sid.id = acl_entry.sid ";

        return queryString;
    }

    /** execute sqlQuery and populate phenotypesGenesAssociations is : phenotype --> genes */
    private void populateGenesAssociations( String sqlQuery,
            HashMap<String, HashSet<Integer>> phenotypesGenesAssociations ) {

        org.hibernate.SQLQuery queryObject = this.getSession().createSQLQuery( sqlQuery );

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
    }

    private String addTaxonToQuery( String keyWord, Taxon taxon ) {
        String taxonSqlQuery = "";

        if ( taxon != null && taxon.getId() != null && !taxon.getId().equals( 0 ) ) {
            taxonSqlQuery = keyWord + " TAXON.id = " + taxon.getId() + " ";
        }
        return taxonSqlQuery;
    }

    private String addValuesUriToQuery( String keyWord, Set<String> valuesUri ) {

        String query = "";

        if ( valuesUri != null && !valuesUri.isEmpty() ) {
            query = keyWord + " CHARACTERISTIC.VALUE_URI in(";

            for ( String value : valuesUri ) {
                query += "'" + value + "',";
            }
            query = query.substring( 0, query.length() - 1 ) + ") ";
        }
        return query;
    }

    private String addGroupsToQuery( String keyWord, Collection<String> groups ) {

        String query = "";

        if ( groups != null && !groups.isEmpty() ) {
            query = keyWord + " USER_GROUP.name in (";

            for ( String group : groups ) {
                query += "'" + group + "',";
            }
            query = query.substring( 0, query.length() - 1 ) + ") ";
        }
        return query;
    }

    /** execute sqlQuery and populate phenotypesGenesAssociations is : phenotype --> genes */
    private void populateGenesWithPhenotypes( String sqlQuery,
            HashMap<Long, GeneEvidenceValueObject> genesWithPhenotypes ) {

        org.hibernate.SQLQuery queryObject = this.getSession().createSQLQuery( sqlQuery );

        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        while ( results.next() ) {

            Long geneId = ( ( BigInteger ) results.get( 0 ) ).longValue();
            Integer nbciGeneId = ( Integer ) results.get( 1 );
            String officialName = ( String ) results.get( 2 );
            String officialSymbol = ( String ) results.get( 3 );
            String taxonCommonName = ( String ) results.get( 4 );
            String valueUri = ( String ) results.get( 5 );

            if ( genesWithPhenotypes.get( geneId ) != null ) {
                genesWithPhenotypes.get( geneId ).getPhenotypesValueUri().add( valueUri );
            } else {
                GeneEvidenceValueObject g = new GeneEvidenceValueObject();
                g.setId( geneId );
                g.setNcbiId( nbciGeneId );
                g.setOfficialName( officialName );
                g.setOfficialSymbol( officialSymbol );
                g.setTaxonCommonName( taxonCommonName );
                g.getPhenotypesValueUri().add( valueUri );
                genesWithPhenotypes.put( geneId, g );
            }
        }
        results.close();
    }
}
