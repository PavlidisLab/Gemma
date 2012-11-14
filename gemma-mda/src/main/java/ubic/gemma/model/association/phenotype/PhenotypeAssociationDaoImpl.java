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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ExternalDatabaseStatisticsValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GeneEvidenceValueObject;
import ubic.gemma.persistence.AbstractDao;

@Repository
public class PhenotypeAssociationDaoImpl extends AbstractDao<PhenotypeAssociation> implements PhenotypeAssociationDao {

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
    /** find PhenotypeAssociation satisfying the given filters: paIds, taxonId and limit */
    public Collection<PhenotypeAssociation> findPhenotypeAssociationWithIds( Collection<Long> paIds, Long taxonId,
            Integer limit ) {
        if ( limit == null ) throw new IllegalArgumentException( "Limit must not be null" );
        if ( limit == 0 || ( paIds != null && paIds.size() == 0 ) ) return new ArrayList<PhenotypeAssociation>();
        Session s = this.getSession();
        String queryString = "select p from PhenotypeAssociationImpl p " + "join p.status s "
                + ( paIds != null || taxonId != null ? "where " : "" )
                + ( paIds == null ? "" : "p.id in (:paIds) " + ( taxonId == null ? "" : "and " ) )
                + ( taxonId == null ? "" : "p.gene.taxon.id = " + taxonId ) + " " + "order by s.lastUpdateDate "
                + ( limit < 0 ? "asc" : "desc" );

        Query q = s.createQuery( queryString );
        if ( paIds != null ) {
            q.setParameterList( "paIds", paIds );
        }
        q.setMaxResults( Math.abs( limit ) );
        return q.list();
    }

    @Override
    /** find all PhenotypeAssociation for a specific gene id */
    public Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneId( Long geneId ) {

        Criteria geneQueryCriteria = super.getSession().createCriteria( PhenotypeAssociation.class )
                .setResultTransformer( CriteriaSpecification.DISTINCT_ROOT_ENTITY ).createCriteria( "gene" )
                .add( Restrictions.like( "id", geneId ) );

        return geneQueryCriteria.list();
    }

    @Override
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

        Collection<CharacteristicValueObject> mgedCategory = new TreeSet<CharacteristicValueObject>();

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

    /** find all evidence that doesn't come from an external course */
    @SuppressWarnings("unchecked")
    @Override
    public Collection<PhenotypeAssociation> findEvidencesWithoutExternalDatabaseName() {
        Criteria geneQueryCriteria = super.getSession().createCriteria( PhenotypeAssociation.class )
                .setResultTransformer( CriteriaSpecification.DISTINCT_ROOT_ENTITY )
                .add( Restrictions.isNull( "evidenceSource" ) );

        return geneQueryCriteria.list();
    }

    /** finds all external databases used by neurocarta */
    @SuppressWarnings("unchecked")
    @Override
    public Collection<ExternalDatabase> findNeurocartaExternalDatabases() {
        return this.getHibernateTemplate().find(
                "select distinct p.evidenceSource.externalDatabase from PhenotypeAssociationImpl as p" );
    }

    /** find statistics for a neurocarta external database (numGene, numPhenotypes, etc.) */
    @SuppressWarnings("unchecked")
    @Override
    public ExternalDatabaseStatisticsValueObject findStatisticsOnDatabase( ExternalDatabase externalDatabase ) {

        Long numEvidence = ( Long ) this
                .getHibernateTemplate()
                .find( "select count (p) from PhenotypeAssociationImpl as p where p.evidenceSource.externalDatabase.id ="
                        + externalDatabase.getId() ).iterator().next();

        Long numGenes = ( Long ) this
                .getHibernateTemplate()
                .find( "select count (distinct g) from GeneImpl as g inner join g.phenotypeAssociations as p where p.evidenceSource.externalDatabase.id ="
                        + externalDatabase.getId() ).iterator().next();

        Long numPhenotypes = ( Long ) this
                .getHibernateTemplate()
                .find( "select count (distinct c.valueUri) from PhenotypeAssociationImpl as p inner join p.phenotypes as c where p.evidenceSource.externalDatabase.id ="
                        + externalDatabase.getId() ).iterator().next();

        HibernateTemplate tpl = this.getHibernateTemplate();
        tpl.setMaxResults( 1 );
        String lastUpdateDate = ( ( Timestamp ) tpl
                .find( "select p.status.lastUpdateDate from PhenotypeAssociationImpl as p inner join p.phenotypes as c where p.evidenceSource.externalDatabase.id ="
                        + externalDatabase.getId() + " order by p.status.lastUpdateDate desc" ).iterator().next() )
                .toString();

        ExternalDatabaseStatisticsValueObject externalDatabaseStatisticsValueObject = new ExternalDatabaseStatisticsValueObject(
                externalDatabase.getName(), numEvidence, numGenes, numPhenotypes, lastUpdateDate );

        return externalDatabaseStatisticsValueObject;

    }

    /** find all public phenotypes associated with genes on a specific taxon and containing the valuesUri */
    @Override
    public HashMap<String, HashSet<Integer>> findPublicPhenotypesGenesAssociations( Taxon taxon, Set<String> valuesUri,
            String userName, Collection<String> groups, boolean showOnlyEditable ) {

        HashMap<String, HashSet<Integer>> phenotypesGenesAssociations = new HashMap<String, HashSet<Integer>>();

        String sqlQuery = "select CHROMOSOME_FEATURE.NCBI_GENE_ID, CHARACTERISTIC.VALUE_URI ";
        sqlQuery += getPhenotypesGenesAssociationsBeginQuery();

        // rule to find public
        sqlQuery += "and acl_entry.mask = 1 and acl_entry.sid = 4 ";

        sqlQuery += addTaxonToQuery( "and", taxon );
        sqlQuery += addValuesUriToQuery( "and", valuesUri );

        if ( showOnlyEditable ) {
            sqlQuery += "and PHENOTYPE_ASSOCIATION.id in ( select PHENOTYPE_ASSOCIATION.id ";
            sqlQuery += getPhenotypesGenesAssociationsBeginQuery();
            sqlQuery += addGroupAndUserNameRestriction( userName, groups, showOnlyEditable, false );
            sqlQuery += ") ";
        }

        populateGenesAssociations( sqlQuery, phenotypesGenesAssociations );

        return phenotypesGenesAssociations;
    }

    /** find all private phenotypes associated with genes on a specific taxon and containing the valuesUri */
    @Override
    public HashMap<String, HashSet<Integer>> findPrivatePhenotypesGenesAssociations( Taxon taxon,
            Set<String> valuesUri, String userName, Collection<String> groups, boolean showOnlyEditable ) {

        HashMap<String, HashSet<Integer>> phenotypesGenesAssociations = new HashMap<String, HashSet<Integer>>();

        String sqlQuery = "select CHROMOSOME_FEATURE.NCBI_GENE_ID, CHARACTERISTIC.VALUE_URI ";
        sqlQuery += getPhenotypesGenesAssociationsBeginQuery();
        sqlQuery += addGroupAndUserNameRestriction( userName, groups, showOnlyEditable, false );
        sqlQuery += "and PHENOTYPE_ASSOCIATION.ID not in (select PHENOTYPE_ASSOCIATION.ID from CHARACTERISTIC join PHENOTYPE_ASSOCIATION on CHARACTERISTIC.PHENOTYPE_ASSOCIATION_FK = PHENOTYPE_ASSOCIATION.ID join CHROMOSOME_FEATURE on CHROMOSOME_FEATURE.id = PHENOTYPE_ASSOCIATION.GENE_FK join TAXON on TAXON.id = CHROMOSOME_FEATURE.TAXON_FK join acl_object_identity on PHENOTYPE_ASSOCIATION.id = acl_object_identity.object_id_identity join acl_entry on acl_entry.acl_object_identity = acl_object_identity.id join acl_class on acl_class.id = acl_object_identity.object_id_class join acl_sid on acl_sid.id = acl_object_identity.owner_sid where acl_entry.mask = 1 and acl_entry.sid = 4 and acl_class.class in('ubic.gemma.model.association.phenotype.LiteratureEvidenceImpl','ubic.gemma.model.association.phenotype.GenericEvidenceImpl','ubic.gemma.model.association.phenotype.ExperimentalEvidenceImpl','ubic.gemma.model.association.phenotype.DifferentialExpressionEvidenceImpl','ubic.gemma.model.association.phenotype.UrlEvidenceImpl'))";
        sqlQuery += addTaxonToQuery( "and", taxon );
        sqlQuery += addValuesUriToQuery( "and", valuesUri );

        populateGenesAssociations( sqlQuery, phenotypesGenesAssociations );

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

        sqlQuery += addValuesUriToQuery( "and", phenotypesValueUri );

        // admin can see all there is no specific condition on security

        if ( !isAdmin ) {
            sqlQuery += addGroupAndUserNameRestriction( userName, groups, showOnlyEditable, true );
        }

        sqlQuery += addTaxonToQuery( "and", taxon );

        populateGenesWithPhenotypes( sqlQuery, genesWithPhenotypes );

        return genesWithPhenotypes.values();
    }

    @Override
    /** find private evidence id that the user can modifiable or own */
    public Set<Long> findPrivateEvidenceId( String userName, Collection<String> groups ) {

        Set<Long> ids = new HashSet<Long>();

        String sqlQuery = "select PHENOTYPE_ASSOCIATION.ID ";
        sqlQuery += getPhenotypesGenesAssociationsBeginQuery();

        sqlQuery += addGroupAndUserNameRestriction( userName, groups, true, false );

        org.hibernate.SQLQuery queryObject = this.getSession().createSQLQuery( sqlQuery );

        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );

        while ( results.next() ) {
            Long geneId = ( ( BigInteger ) results.get( 0 ) ).longValue();
            ids.add( geneId );
        }

        results.close();
        return ids;
    }

    @Override
    /** return the list of the owners that have evidence in the system */
    public Collection<String> findEvidenceOwners() {

        Set<String> owners = new HashSet<String>();

        String sqlQuery = "select distinct acl_sid.sid from acl_object_identity join acl_entry on acl_entry.acl_object_identity = acl_object_identity.id join acl_class on acl_class.id = acl_object_identity.object_id_class join acl_sid on acl_sid.id = acl_object_identity.owner_sid where acl_class.class in('ubic.gemma.model.association.phenotype.LiteratureEvidenceImpl','ubic.gemma.model.association.phenotype.GenericEvidenceImpl','ubic.gemma.model.association.phenotype.ExperimentalEvidenceImpl','ubic.gemma.model.association.phenotype.DifferentialExpressionEvidenceImpl','ubic.gemma.model.association.phenotype.UrlEvidenceImpl') ";

        org.hibernate.SQLQuery queryObject = this.getSession().createSQLQuery( sqlQuery );

        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );

        while ( results.next() ) {
            String owner = ( String ) results.get( 0 );
            owners.add( owner );
        }

        return owners;
    }

    /** basic sql command to deal with security */
    private String getPhenotypesGenesAssociationsBeginQuery() {
        String queryString = "";

        queryString += "from CHARACTERISTIC ";
        queryString += "join PHENOTYPE_ASSOCIATION on CHARACTERISTIC.PHENOTYPE_ASSOCIATION_FK = PHENOTYPE_ASSOCIATION.ID ";
        queryString += "join CHROMOSOME_FEATURE on CHROMOSOME_FEATURE.id = PHENOTYPE_ASSOCIATION.GENE_FK ";
        queryString += "join TAXON on TAXON.id = CHROMOSOME_FEATURE.TAXON_FK ";
        queryString += "join acl_object_identity on PHENOTYPE_ASSOCIATION.id = acl_object_identity.object_id_identity ";
        queryString += "join acl_entry on acl_entry.acl_object_identity = acl_object_identity.id ";
        queryString += "join acl_class on acl_class.id = acl_object_identity.object_id_class ";
        queryString += "join acl_sid on acl_sid.id = acl_object_identity.owner_sid ";
        queryString += "where acl_class.class in('ubic.gemma.model.association.phenotype.LiteratureEvidenceImpl','ubic.gemma.model.association.phenotype.GenericEvidenceImpl','ubic.gemma.model.association.phenotype.ExperimentalEvidenceImpl','ubic.gemma.model.association.phenotype.DifferentialExpressionEvidenceImpl','ubic.gemma.model.association.phenotype.UrlEvidenceImpl') ";

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

    /**
     * rules to find private evidence
     * 
     * @param showOnlyEditable show only what the user have write access
     * @param showPublic also show public evidence (wont work if showOnlyEditable is true)
     */
    private String addGroupAndUserNameRestriction( String userName, Collection<String> groups,
            boolean showOnlyEditable, boolean showPublic ) {

        String sqlQuery = "";

        if ( userName != null && !userName.isEmpty() ) {

            if ( showPublic && !showOnlyEditable ) {
                sqlQuery += "and ((acl_sid.sid = '" + userName + "' ";
            } else {
                sqlQuery += "and (acl_sid.sid = '" + userName + "' ";
            }

            if ( groups != null && !groups.isEmpty() ) {
                // find what acl group the user is in
                sqlQuery += "or (acl_entry.sid in (";
                sqlQuery += "select acl_sid.id from USER_GROUP ";
                sqlQuery += "join GROUP_AUTHORITY on USER_GROUP.ID = GROUP_AUTHORITY.GROUP_FK ";
                sqlQuery += "join acl_sid on acl_sid.sid=CONCAT('GROUP_', GROUP_AUTHORITY.AUTHORITY) ";
                sqlQuery += "where USER_GROUP.name in(" + groupToSql( groups ) + ") ";
                if ( showOnlyEditable ) {
                    sqlQuery += ") and acl_entry.mask = 2) ";
                } else {
                    sqlQuery += ") and (acl_entry.mask = 1 or acl_entry.mask = 2)) ";
                }
            }
            sqlQuery += ") ";

            if ( showPublic && !showOnlyEditable ) {
                sqlQuery += "or (acl_entry.sid = 4 and mask = 1)) ";
            }
        } else if ( showPublic && !showOnlyEditable ) {
            sqlQuery += "and (acl_entry.sid = 4 and mask = 1) ";
        }

        return sqlQuery;
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

    private String groupToSql( Collection<String> groups ) {

        String sqlGroup = "";

        if ( groups != null && !groups.isEmpty() ) {

            for ( String group : groups ) {
                sqlGroup += "'" + group + "',";
            }
            sqlGroup = sqlGroup.substring( 0, sqlGroup.length() - 1 );
        }
        return sqlGroup;
    }
}
