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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
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
import ubic.gemma.model.genome.gene.phenotype.valueObject.PhenotypeValueObject;
import ubic.gemma.persistence.AbstractDao;
import ubic.gemma.util.EntityUtils;

/**
 * TODO Document Me
 * 
 * @author Nicolas
 * @version $Id$
 */
@Repository
public class PhenotypeAssociationDaoImpl extends AbstractDao<PhenotypeAssociation> implements PhenotypeAssociationDao {

    private static final String DISCRIMINATOR_CLAUSE = "('ubic.gemma.model.association.phenotype.LiteratureEvidenceImpl',"
            + "'ubic.gemma.model.association.phenotype.GenericEvidenceImpl',"
            + "'ubic.gemma.model.association.phenotype.ExperimentalEvidenceImpl',"
            + "'ubic.gemma.model.association.phenotype.DifferentialExpressionEvidenceImpl',"
            + "'ubic.gemma.model.association.phenotype.UrlEvidenceImpl') ";

    private static Log log = LogFactory.getLog( PhenotypeAssociationDaoImpl.class );

    @Autowired
    public PhenotypeAssociationDaoImpl( SessionFactory sessionFactory ) {
        super( PhenotypeAssociation.class );
        super.setSessionFactory( sessionFactory );
    }

    @Override
    /** counts the evidence that from neurocarta that came from a specific MetaAnalysis */
    public Long countEvidenceWithGeneDifferentialExpressionMetaAnalysis( Long geneDifferentialExpressionMetaAnalysisId ) {
        Long numDifferentialExpressionEvidence = ( Long ) this
                .getHibernateTemplate()
                .find( "select count (d) from DifferentialExpressionEvidenceImpl as d where d.geneDifferentialExpressionMetaAnalysisResult "
                        + "in (select r from GeneDifferentialExpressionMetaAnalysisImpl as g join g.results as r where g.id="
                        + geneDifferentialExpressionMetaAnalysisId + ")" ).iterator().next();

        return numDifferentialExpressionEvidence;
    }

    /** find category terms currently used in the database by evidence */
    @Override
    public Collection<CharacteristicValueObject> findEvidenceCategoryTerms() {

        Collection<CharacteristicValueObject> mgedCategory = new TreeSet<CharacteristicValueObject>();

        String queryString = "SELECT distinct CATEGORY_URI, category FROM PHENOTYPE_ASSOCIATION "
                + "join INVESTIGATION on PHENOTYPE_ASSOCIATION.EXPERIMENT_FK = INVESTIGATION.ID "
                + "join CHARACTERISTIC on CHARACTERISTIC.INVESTIGATION_FK= INVESTIGATION.ID";
        org.hibernate.SQLQuery queryObject = this.getSessionFactory().getCurrentSession().createSQLQuery( queryString );

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

    @Override
    /** return the list of the owners that have evidence in the system */
    public Collection<String> findEvidenceOwners() {

        Set<String> owners = new HashSet<String>();

        String sqlQuery = "select distinct sid.SID from ACLOBJECTIDENTITY aoi join ACLENTRY ace on ace.OBJECTIDENTITY_FK = "
                + "aoi.ID join ACLSID sid on sid.ID = aoi.OWNER_SID_FK where aoi.OBJECT_CLASS "
                + "in  "
                + DISCRIMINATOR_CLAUSE;

        SQLQuery queryObject = this.getSessionFactory().getCurrentSession().createSQLQuery( sqlQuery );

        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );

        while ( results.next() ) {
            String owner = ( String ) results.get( 0 );
            owners.add( owner );
        }

        return owners;
    }

    /** delete all evidences from a specific external database */
    @Override
    public Collection<PhenotypeAssociation> findEvidencesWithExternalDatabaseName( String externalDatabaseName ) {

        // Criteria geneQueryCriteria = super.getSessionFactory().getCurrentSession()
        // .createCriteria( PhenotypeAssociation.class )
        // .setResultTransformer( CriteriaSpecification.DISTINCT_ROOT_ENTITY ).createCriteria( "evidenceSource" )
        // .createCriteria( "externalDatabase" ).add( Restrictions.like( "name", externalDatabaseName ) );
        //
        // return geneQueryCriteria.list();

        return this
                .getHibernateTemplate()
                .findByNamedParam(
                        "select p from PhenotypeAssociation as p fetch all properties  join p.evidenceSource es join es.externalDatabase ed where ed.name=:name",
                        "name", externalDatabaseName );

    }

    /** find all evidence that doesn't come from an external course */
    @Override
    public Collection<PhenotypeAssociation> findEvidencesWithoutExternalDatabaseName() {

        // Criteria geneQueryCriteria = super.getSessionFactory().getCurrentSession()
        // .createCriteria( PhenotypeAssociation.class )
        // .setResultTransformer( CriteriaSpecification.DISTINCT_ROOT_ENTITY )
        // .add( Restrictions.isNull( "evidenceSource" ) );
        // return geneQueryCriteria.list();

        return this.getHibernateTemplate().find(
                "select p from PhenotypeAssociation as p fetch all properties  " + "where p.evidenceSource is null" );

    }

    /** Gets all External Databases that are used with evidence */
    @Override
    public Collection<ExternalDatabase> findExternalDatabasesWithEvidence() {

        Collection<ExternalDatabase> externalDatabasesNames = this.getHibernateTemplate().find(
                "select distinct p.evidenceSource.externalDatabase from PhenotypeAssociation as p" );
        return externalDatabasesNames;
    }

    /*
     * find Genes link to a phenotype taking into account private and public evidence Here on the case : 1- Admin 2-
     * user not logged in 3- user logged in only showing what he has read acces 4- user logged in only showing what he
     * has write acces
     */
    @Override
    public Collection<GeneEvidenceValueObject> findGeneWithPhenotypes( Set<String> phenotypesValueUri, Taxon taxon,
            String userName, Collection<String> groups, boolean isAdmin, boolean showOnlyEditable,
            Collection<Long> externalDatabaseIds ) {

        HashMap<Long, GeneEvidenceValueObject> genesWithPhenotypes = new HashMap<Long, GeneEvidenceValueObject>();

        if ( phenotypesValueUri.isEmpty() ) {
            return genesWithPhenotypes.values();
        }

        String sqlSelectQuery = "select distinct CHROMOSOME_FEATURE.ID, CHROMOSOME_FEATURE.NCBI_GENE_ID, CHROMOSOME_FEATURE.OFFICIAL_NAME, "
                + "CHROMOSOME_FEATURE.OFFICIAL_SYMBOL, tax.COMMON_NAME, CHARACTERISTIC.VALUE_URI ";

        String sqlQuery = sqlSelectQuery + getPhenotypesGenesAssociationsBeginQuery();

        sqlQuery += addValuesUriToQuery( "and", phenotypesValueUri );

        // admin can see all there is no specific condition on security

        if ( !isAdmin ) {
            sqlQuery += addGroupAndUserNameRestriction( userName, groups, showOnlyEditable, true );
        }

        sqlQuery += addTaxonToQuery( "and", taxon );
        sqlQuery += addExternalDatabaseQuery( "and", externalDatabaseIds );

        populateGenesWithPhenotypes( sqlQuery, genesWithPhenotypes );

        return genesWithPhenotypes.values();
    }

    @Override
    /** find all PhenotypeAssociation for a specific gene id */
    public Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneId( Long geneId ) {

        return this.getHibernateTemplate().find(
                "select distinct p from PhenotypeAssociation as p fetch all properties where p.gene.id=" + geneId );

    }

    @Override
    /** find all PhenotypeAssociation for a specific gene id and external Databases ids */
    public Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneIdAndDatabases( Long geneId,
            Collection<Long> externalDatabaseIds ) {

        String findByExternalDatabase = "";
        boolean excludeManualCuration = false;
        boolean excludeExternalDatabase = false;
        Collection<PhenotypeAssociation> manualCuration = new HashSet<PhenotypeAssociation>();
        Collection<PhenotypeAssociation> evidenceWithSource = new HashSet<PhenotypeAssociation>();

        if ( externalDatabaseIds != null && !externalDatabaseIds.isEmpty() ) {
            String ids = "";

            for ( Long id : externalDatabaseIds ) {

                // 1 is Manual Curation excluded
                if ( id.equals( 1L ) ) {
                    excludeManualCuration = true;
                } else {
                    // an External Database excluded
                    excludeExternalDatabase = true;
                    ids = ids + id + ",";
                }
            }

            ids = StringUtils.removeEnd( ids, "," );

            if ( !excludeManualCuration ) {
                // get all manual curated evidence (the ones with no external source)
                manualCuration = this.getHibernateTemplate().find(
                        "select distinct p from PhenotypeAssociation as p fetch all properties where p.gene.id="
                                + geneId + "and p.evidenceSource is null" );
            }

            // if we need to exclude some evidence with an external source
            if ( excludeExternalDatabase ) {
                findByExternalDatabase = " and p.evidenceSource.externalDatabase.id not in (" + ids + ")";
            }
        }

        evidenceWithSource = this.getHibernateTemplate().find(
                "select distinct p from PhenotypeAssociation as p fetch all properties where p.gene.id=" + geneId
                        + findByExternalDatabase );

        evidenceWithSource.addAll( manualCuration );

        return evidenceWithSource;
    }

    @Override
    /** find all PhenotypeAssociation for a specific NCBI id */
    public Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneNCBI( Integer geneNCBI ) {

        return this.getHibernateTemplate().findByNamedParam(
                "select p from PhenotypeAssociation as p fetch all properties join p.gene as g "
                        + "where g.ncbiGeneId=:n", new String[] { "n" }, new Object[] { geneNCBI } );

    }

    @Override
    /** find all PhenotypeAssociation for a specific NCBI id and phenotypes valueUri */
    public Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneNCBI( Integer geneNCBI, Set<String> phenotype ) {

        Collection<PhenotypeAssociation> phenotypeAssociation = this.getHibernateTemplate().findByNamedParam(
                "select p from PhenotypeAssociation as p fetch all properties join p.phenotypes as phe join p.gene as g "
                        + "where phe.valueUri in (:p) and g.ncbiGeneId=:n", new String[] { "p", "n" },
                new Object[] { phenotype, geneNCBI } );

        return phenotypeAssociation;

    }

    @Override
    /** find PhenotypeAssociation satisfying the given filters: paIds, taxonId and limit */
    public Collection<PhenotypeAssociation> findPhenotypeAssociationWithIds( Collection<Long> paIds, Long taxonId,
            Integer limit ) {
        if ( limit == null ) throw new IllegalArgumentException( "Limit must not be null" );
        if ( limit == 0 || ( paIds != null && paIds.size() == 0 ) ) return new ArrayList<PhenotypeAssociation>();
        Session s = this.getSessionFactory().getCurrentSession();
        String queryString = "select p from PhenotypeAssociation p fetch all properties " + "join p.status s "
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
    /** find PhenotypeAssociations associated with a BibliographicReference */
    public Collection<PhenotypeAssociation> findPhenotypesForBibliographicReference( String pubMedID ) {

        Collection<PhenotypeAssociation> phenotypeAssociationsFound = new HashSet<PhenotypeAssociation>();

        // Literature Evidence have BibliographicReference
        org.hibernate.classic.Session session = super.getSessionFactory().getCurrentSession();
        Criteria geneQueryCriteria = session.createCriteria( LiteratureEvidence.class )
                .setResultTransformer( CriteriaSpecification.DISTINCT_ROOT_ENTITY ).createCriteria( "citation" )
                .createCriteria( "pubAccession" ).add( Restrictions.like( "accession", pubMedID ) );

        phenotypeAssociationsFound.addAll( geneQueryCriteria.list() );

        // Experimental Evidence have a primary BibliographicReference
        geneQueryCriteria = session.createCriteria( ExperimentalEvidence.class )
                .setResultTransformer( CriteriaSpecification.DISTINCT_ROOT_ENTITY ).createCriteria( "experiment" )
                .createCriteria( "primaryPublication" ).createCriteria( "pubAccession" )
                .add( Restrictions.like( "accession", pubMedID ) );

        phenotypeAssociationsFound.addAll( geneQueryCriteria.list() );

        // Experimental Evidence have relevant BibliographicReference
        geneQueryCriteria = session.createCriteria( ExperimentalEvidence.class )
                .setResultTransformer( CriteriaSpecification.DISTINCT_ROOT_ENTITY ).createCriteria( "experiment" )
                .createCriteria( "otherRelevantPublications" ).createCriteria( "pubAccession" )
                .add( Restrictions.like( "accession", pubMedID ) );

        phenotypeAssociationsFound.addAll( geneQueryCriteria.list() );

        // FIXME shortcut until we rewrite the above with hql to get FETCH ALL PROPERTIES
        return load( EntityUtils.getIds( phenotypeAssociationsFound ) );
    }

    /**
     * find private evidence id that the user can modifiable or own
     */
    @Override
    public Set<Long> findPrivateEvidenceId( String userName, Collection<String> groups ) {

        Set<Long> ids = new HashSet<Long>();

        String sqlQuery = "select PHENOTYPE_ASSOCIATION.ID ";
        sqlQuery += getPhenotypesGenesAssociationsBeginQuery();

        sqlQuery += addGroupAndUserNameRestriction( userName, groups, true, false );

        org.hibernate.SQLQuery queryObject = this.getSessionFactory().getCurrentSession().createSQLQuery( sqlQuery );

        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );

        while ( results.next() ) {
            Long geneId = ( ( BigInteger ) results.get( 0 ) ).longValue();
            ids.add( geneId );
        }

        results.close();
        return ids;
    }

    /** find all private phenotypes associated with genes on a specific taxon and containing the valuesUri */
    @Override
    public Map<String, Set<Integer>> findPrivatePhenotypesGenesAssociations( Taxon taxon, Set<String> valuesUri,
            String userName, Collection<String> groups, boolean showOnlyEditable, Collection<Long> externalDatabaseIds ) {

        Map<String, Set<Integer>> phenotypesGenesAssociations = new HashMap<>();

        /*
         * At this level of the application, we can't access acls. The reason for this so we don't get uneven page
         * numbers. ACESID 4 is anonymous; MASK=1 is read.
         */
        String sqlQuery = "select CHROMOSOME_FEATURE.NCBI_GENE_ID, CHARACTERISTIC.VALUE_URI ";
        sqlQuery += getPhenotypesGenesAssociationsBeginQuery();
        sqlQuery += addGroupAndUserNameRestriction( userName, groups, showOnlyEditable, false );
        // sqlQuery += "and PHENOTYPE_ASSOCIATION.ID "
        // + "not in "
        // + "(select PHENOTYPE_ASSOCIATION.ID from CHARACTERISTIC"
        // + " inner join PHENOTYPE_ASSOCIATION on CHARACTERISTIC.PHENOTYPE_ASSOCIATION_FK = PHENOTYPE_ASSOCIATION.ID "
        // + "inner join CHROMOSOME_FEATURE on CHROMOSOME_FEATURE.ID = PHENOTYPE_ASSOCIATION.GENE_FK "
        // + "inner join TAXON tax on tax.ID = CHROMOSOME_FEATURE.TAXON_FK "
        // + "inner join ACLOBJECTIDENTITY aoi on PHENOTYPE_ASSOCIATION.ID = aoi.OBJECT_ID "
        // + "inner join ACLENTRY ace on ace.OBJECTIDENTITY_FK = aoi.ID "
        // + "inner join ACLSID sid on sid.ID = aoi.OWNER_SID_FK where ace.MASK = 1 and ace.SID_FK = 4 "
        // + "and aoi.OBJECT_CLASS IN " + DISCRIMINATOR_CLAUSE + ") ";
        sqlQuery += addTaxonToQuery( "and", taxon );
        sqlQuery += addValuesUriToQuery( "and", valuesUri );
        sqlQuery += addExternalDatabaseQuery( "and", externalDatabaseIds );

        populateGenesAssociations( sqlQuery, phenotypesGenesAssociations );

        // hack to make this work temporarily.
        Map<String, Set<Integer>> others = findPublicPhenotypesGenesAssociations( taxon, valuesUri, userName, groups,
                showOnlyEditable, externalDatabaseIds );
        phenotypesGenesAssociations.keySet().removeAll( others.keySet() );

        return phenotypesGenesAssociations;
    }

    /** find all public phenotypes associated with genes on a specific taxon and containing the valuesUri */
    @Override
    public Map<String, Set<Integer>> findPublicPhenotypesGenesAssociations( Taxon taxon, Set<String> valuesUri,
            String userName, Collection<String> groups, boolean showOnlyEditable, Collection<Long> externalDatabaseIds ) {

        Map<String, Set<Integer>> phenotypesGenesAssociations = new HashMap<>();

        String sqlQuery = "select CHROMOSOME_FEATURE.NCBI_GENE_ID, CHARACTERISTIC.VALUE_URI ";
        sqlQuery += getPhenotypesGenesAssociationsBeginQuery();

        // rule to find public: anonymous, READ.
        sqlQuery += "and ace.MASK = 1 and ace.SID_FK = 4 ";
        sqlQuery += addTaxonToQuery( "and", taxon );
        sqlQuery += addValuesUriToQuery( "and", valuesUri );
        sqlQuery += addExternalDatabaseQuery( "and", externalDatabaseIds );

        if ( showOnlyEditable ) {
            sqlQuery += "and PHENOTYPE_ASSOCIATION.id in ( select PHENOTYPE_ASSOCIATION.id ";
            sqlQuery += getPhenotypesGenesAssociationsBeginQuery();
            sqlQuery += addGroupAndUserNameRestriction( userName, groups, showOnlyEditable, false );
            sqlQuery += ") ";
        }

        /*
         * explain select CHROMOSOME_FEATURE.NCBI_GENE_ID, CHARACTERISTIC.VALUE_URI from CHARACTERISTIC join
         * PHENOTYPE_ASSOCIATION on CHARACTERISTIC.PHENOTYPE_ASSOCIATION_FK = PHENOTYPE_ASSOCIATION.ID join
         * CHROMOSOME_FEATURE on CHROMOSOME_FEATURE.id = PHENOTYPE_ASSOCIATION.GENE_FK join TAXON tax on tax.ID =
         * CHROMOSOME_FEATURE.TAXON_FK join ACLOBJECTIDENTITY aoi on PHENOTYPE_ASSOCIATION.id = aoi.OBJECT_ID join
         * ACLENTRY ace on ace.OBJECTIDENTITY_FK = aoi.ID join ACLSID sid on sid.ID = aoi.OWNER_SID_FK
         * 
         * where aoi.OBJECT_CLASS IN (
         * 'ubic.gemma.model.association.phenotype.LiteratureEvidenceImpl','ubic.gemma.model.association.phenotype.GenericEvidenceImpl',
         * 'ubic.gemma.model.association.phenotype.ExperimentalEvidenceImpl','ubic.gemma.model.association.phenotype.DifferentialExpressionEvidenceImpl',
         * 'ubic.gemma.model.association.phenotype.UrlEvidenceImpl') and ace.MASK = 1 and ace.SID_FK = 4;
         */
        log.info( sqlQuery );

        populateGenesAssociations( sqlQuery, phenotypesGenesAssociations );

        return phenotypesGenesAssociations;
    }

    @Override
    public Collection<PhenotypeAssociation> load( Collection<Long> ids ) {
        if ( ids.isEmpty() ) return new HashSet<>();
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "from PhenotypeAssociation fetch all properties where id in (:ids)" )
                .setParameterList( "ids", ids ).list();
    }

    @Override
    public PhenotypeAssociation load( Long id ) {
        return ( PhenotypeAssociation ) this.getSessionFactory().getCurrentSession()
                .createQuery( "from PhenotypeAssociation fetch all properties where id = :id" ).setParameter( "id", id )
                .uniqueResult();
    }

    /** find all phenotypes in Neurocarta */
    @Override
    public Collection<PhenotypeValueObject> loadAllNeurocartaPhenotypes() {

        Collection<PhenotypeValueObject> phenotypeValueObjects = new HashSet<PhenotypeValueObject>();

        String sqlQuery = "SELECT DISTINCT VALUE_URI,VALUE FROM CHARACTERISTIC WHERE PHENOTYPE_ASSOCIATION_FK is not null";

        org.hibernate.SQLQuery queryObject = this.getSessionFactory().getCurrentSession().createSQLQuery( sqlQuery );

        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        while ( results.next() ) {

            String valueUri = ( String ) results.get( 0 );
            String value = ( String ) results.get( 1 );

            PhenotypeValueObject p = new PhenotypeValueObject( value, valueUri );
            phenotypeValueObjects.add( p );
        }

        return phenotypeValueObjects;
    }

    /** load all valueURI of Phenotype in the database */
    @Override
    public Set<String> loadAllPhenotypesUri() {
        Set<String> phenotypesURI = new HashSet<String>();

        String queryString = "select distinct value_uri from CHARACTERISTIC where phenotype_association_fk is not null";
        org.hibernate.SQLQuery queryObject = this.getSessionFactory().getCurrentSession().createSQLQuery( queryString );

        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        while ( results.next() ) {
            phenotypesURI.add( ( String ) results.get( 0 ) );
        }
        results.close();

        return phenotypesURI;
    }

    /**
     * returns a Collection<DifferentialExpressionEvidence> for a geneDifferentialExpressionMetaAnalysisId if one exists
     * (can be used to find the threshold and phenotypes for a GeneDifferentialExpressionMetaAnalysis)
     */
    @Override
    public Collection<DifferentialExpressionEvidence> loadEvidenceWithGeneDifferentialExpressionMetaAnalysis(
            Long geneDifferentialExpressionMetaAnalysisId, Long maxResults ) {

        HibernateTemplate tpl = new HibernateTemplate( this.getSessionFactory() );

        if ( maxResults != null ) {
            tpl.setMaxResults( maxResults.intValue() );
        }
        List<DifferentialExpressionEvidence> differentialExpressionEvidenceCollection = tpl
                .find( "select d from DifferentialExpressionEvidenceImpl as d where d.geneDifferentialExpressionMetaAnalysisResult "
                        + "in (select r from GeneDifferentialExpressionMetaAnalysisImpl as g join g.results as r where g.id="
                        + geneDifferentialExpressionMetaAnalysisId + ")" );

        return differentialExpressionEvidenceCollection;
    }

    /** find statistics all evidences */
    @Override
    public ExternalDatabaseStatisticsValueObject loadStatisticsOnAllEvidence() {

        Long numEvidence = ( Long ) this.getHibernateTemplate()
                .find( "select count (p) from PhenotypeAssociation as p" ).iterator().next();

        Long numGenes = ( Long ) this.getHibernateTemplate()
                .find( "select count (distinct g) from GeneImpl as g inner join g.phenotypeAssociations as p" )
                .iterator().next();

        Long numPhenotypes = ( Long ) this
                .getHibernateTemplate()
                .find( "select count (distinct c.valueUri) from PhenotypeAssociation as p inner join p.phenotypes as c" )
                .iterator().next();

        Collection<String> publicationsLiterature = this.getHibernateTemplate().find(
                "select distinct l.citation.pubAccession.accession from LiteratureEvidenceImpl as l" );

        // find all primary pubmed for ExperimentalEvidence
        Collection<String> publicationsExperimentalPrimary = this
                .getHibernateTemplate()
                .find( "select distinct ex.experiment.primaryPublication.pubAccession.accession from ExperimentalEvidenceImpl as ex" );

        // find all secondary pubmed for ExperimentalEvidence
        Collection<String> publicationsExperimentalSecondary = this
                .getHibernateTemplate()
                .find( "select distinct o.pubAccession.accession from ExperimentalEvidenceImpl as ex join ex.experiment.otherRelevantPublications as o" );

        Set<String> publications = new HashSet<String>();
        publications.addAll( publicationsLiterature );
        publications.addAll( publicationsExperimentalPrimary );
        publications.addAll( publicationsExperimentalSecondary );

        Long numPublications = new Long( publications.size() );

        ExternalDatabaseStatisticsValueObject externalDatabaseStatisticsValueObject = new ExternalDatabaseStatisticsValueObject(
                "Total (unique)", "", "", numEvidence, numGenes, numPhenotypes, numPublications, null );

        return externalDatabaseStatisticsValueObject;
    }

    @Override
    public Collection<ExternalDatabaseStatisticsValueObject> loadStatisticsOnExternalDatabases() {

        HashMap<String, ExternalDatabaseStatisticsValueObject> externalDatabasesStatistics = new HashMap<String, ExternalDatabaseStatisticsValueObject>();

        List<Object[]> numEvidence = this.getHibernateTemplate().find(
                "select p.evidenceSource.externalDatabase, count (*), p.status.lastUpdateDate from PhenotypeAssociation "
                        + "as p group by p.evidenceSource.externalDatabase order by p.status.lastUpdateDate desc" );

        for ( Object[] o : numEvidence ) {

            ExternalDatabase externalDatabase = ( ExternalDatabase ) o[0];
            Long count = ( Long ) o[1];

            ExternalDatabaseStatisticsValueObject externalDatabaseStatistics = new ExternalDatabaseStatisticsValueObject();
            externalDatabaseStatistics.setDescription( externalDatabase.getDescription() );
            externalDatabaseStatistics.setName( externalDatabase.getName() );
            externalDatabaseStatistics.setLastUpdateDate( ( Date ) o[2] );
            externalDatabaseStatistics.setWebUri( externalDatabase.getWebUri() );
            externalDatabaseStatistics.setNumEvidence( count );
            externalDatabasesStatistics.put( externalDatabase.getName(), externalDatabaseStatistics );
        }

        List<Object[]> numGenes = this.getHibernateTemplate().find(
                "select p.evidenceSource.externalDatabase.name, count (distinct g) from GeneImpl as g join g.phenotypeAssociations "
                        + "as p group by p.evidenceSource.externalDatabase" );

        for ( Object[] o : numGenes ) {
            String externalDatabaseName = ( String ) o[0];
            externalDatabasesStatistics.get( externalDatabaseName ).setNumGenes( ( Long ) o[1] );
        }

        List<Object[]> numPhenotypes = this.getHibernateTemplate().find(
                "select p.evidenceSource.externalDatabase.name, count (distinct c.valueUri) "
                        + "from PhenotypeAssociation as p join p.phenotypes as c "
                        + "group by p.evidenceSource.externalDatabase" );

        for ( Object[] o : numPhenotypes ) {
            String externalDatabaseName = ( String ) o[0];
            externalDatabasesStatistics.get( externalDatabaseName ).setNumPhenotypes( ( Long ) o[1] );
        }

        List<Object[]> numPublicationsLiterature = this.getHibernateTemplate().find(
                "select l.evidenceSource.externalDatabase.name, count (distinct l.citation.pubAccession.accession) "
                        + "from LiteratureEvidenceImpl as l group by l.evidenceSource.externalDatabase" );

        for ( Object[] o : numPublicationsLiterature ) {
            String externalDatabaseName = ( String ) o[0];
            externalDatabasesStatistics.get( externalDatabaseName ).addNumPublications( ( Long ) o[1] );
        }

        List<Object[]> numPublicationsExperimentalPrimary = this
                .getHibernateTemplate()
                .find( "select ex.evidenceSource.externalDatabase.name, count (distinct ex.experiment.primaryPublication.pubAccession.accession) "
                        + "from ExperimentalEvidenceImpl as ex " + "group by ex.evidenceSource.externalDatabase" );

        for ( Object[] o : numPublicationsExperimentalPrimary ) {
            String externalDatabaseName = ( String ) o[0];
            externalDatabasesStatistics.get( externalDatabaseName ).addNumPublications( ( Long ) o[1] );
        }

        List<Object[]> numPublicationsExperimentalSecondary = this.getHibernateTemplate().find(
                "select ex.evidenceSource.externalDatabase.name, count (o.pubAccession.accession) "
                        + "from ExperimentalEvidenceImpl as ex join ex.experiment.otherRelevantPublications as o"
                        + " group by ex.evidenceSource.externalDatabase" );

        for ( Object[] o : numPublicationsExperimentalSecondary ) {
            String externalDatabaseName = ( String ) o[0];
            externalDatabasesStatistics.get( externalDatabaseName ).addNumPublications( ( Long ) o[1] );
        }

        return externalDatabasesStatistics.values();
    }

    /** find statistics for a neurocarta manual curation (numGene, numPhenotypes, etc.) */
    @Override
    public ExternalDatabaseStatisticsValueObject loadStatisticsOnManualCuration() {

        Long numEvidence = ( Long ) this.getHibernateTemplate()
                .find( "select count (p) from PhenotypeAssociation as p where p.evidenceSource is null" ).iterator()
                .next();

        Long numGenes = ( Long ) this
                .getHibernateTemplate()
                .find( "select count (distinct g) from GeneImpl as g inner join g.phenotypeAssociations as p where p.evidenceSource is null" )
                .iterator().next();

        Long numPhenotypes = ( Long ) this
                .getHibernateTemplate()
                .find( "select count (distinct c.valueUri) from PhenotypeAssociation as p inner join p.phenotypes as c where p.evidenceSource is null" )
                .iterator().next();

        HibernateTemplate tpl = new HibernateTemplate( this.getSessionFactory() );
        tpl.setMaxResults( 1 );

        Date lastUpdatedDate = ( ( Timestamp ) tpl
                .find( "select p.status.lastUpdateDate from PhenotypeAssociation as p "
                        + "where p.evidenceSource is null order by p.status.lastUpdateDate desc" ).iterator().next() );

        Collection<String> publicationsLiterature = this
                .getHibernateTemplate()
                .find( "select distinct l.citation.pubAccession.accession from LiteratureEvidenceImpl as l where l.evidenceSource is null" );

        // find all primary pubmed for ExperimentalEvidence
        Collection<String> publicationsExperimentalPrimary = this
                .getHibernateTemplate()
                .find( "select distinct ex.experiment.primaryPublication.pubAccession.accession from ExperimentalEvidenceImpl as ex where ex.evidenceSource is null" );

        // find all secondary pubmed for ExperimentalEvidence
        Collection<String> publicationsExperimentalSecondary = this.getHibernateTemplate().find(
                "select distinct o.pubAccession.accession from ExperimentalEvidenceImpl as ex "
                        + "join ex.experiment.otherRelevantPublications as o where ex.evidenceSource is null" );

        Set<String> publications = new HashSet<String>();
        publications.addAll( publicationsLiterature );
        publications.addAll( publicationsExperimentalPrimary );
        publications.addAll( publicationsExperimentalSecondary );

        Long numPublications = new Long( publications.size() );

        ExternalDatabaseStatisticsValueObject externalDatabaseStatisticsValueObject = new ExternalDatabaseStatisticsValueObject(
                "Manual Curation", "Evidence curated manually through literature review", "", numEvidence, numGenes,
                numPhenotypes, numPublications, lastUpdatedDate );

        return externalDatabaseStatisticsValueObject;
    }

    private String addExternalDatabaseQuery( String keyWord, Collection<Long> externalDatabaseIds ) {

        String externalDatabaseSqlQuery = "";
        String listIds = "";
        Boolean excludeManualCuration = false;
        Boolean excludeExternalDatabase = false;

        if ( externalDatabaseIds != null && !externalDatabaseIds.isEmpty() ) {

            for ( Long id : externalDatabaseIds ) {

                if ( id.equals( 1L ) ) {
                    excludeManualCuration = true;
                } else {
                    listIds = listIds + id + ",";
                    excludeExternalDatabase = true;
                }
            }

            listIds = StringUtils.removeEnd( listIds, "," );

            if ( excludeManualCuration && excludeExternalDatabase ) {
                externalDatabaseSqlQuery = keyWord
                        + " PHENOTYPE_ASSOCIATION.EVIDENCE_SOURCE_FK in (SELECT id FROM DATABASE_ENTRY dbe where dbe.EXTERNAL_DATABASE_FK not in ("
                        + listIds + ")) ";
            } else if ( excludeExternalDatabase ) {
                externalDatabaseSqlQuery = keyWord
                        + " (PHENOTYPE_ASSOCIATION.EVIDENCE_SOURCE_FK is null or PHENOTYPE_ASSOCIATION.EVIDENCE_SOURCE_FK "
                        + "not in (SELECT id FROM DATABASE_ENTRY dbe where dbe.EXTERNAL_DATABASE_FK in (" + listIds
                        + "))) ";
            } else if ( excludeManualCuration ) {
                externalDatabaseSqlQuery = keyWord + " PHENOTYPE_ASSOCIATION.EVIDENCE_SOURCE_FK is not null";
            }

        }
        return externalDatabaseSqlQuery;

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
                sqlQuery += "and ((sid.SID = '" + userName + "' ";
            } else {
                sqlQuery += "and (sid.SID = '" + userName + "' ";
            }

            if ( groups != null && !groups.isEmpty() ) {
                // find what acl group the user is in
                sqlQuery += "or (ace.SID_FK in (";
                sqlQuery += "select sid.ID from USER_GROUP ug ";
                sqlQuery += "join GROUP_AUTHORITY on USER_GROUP.ID = GROUP_AUTHORITY.GROUP_FK ";
                sqlQuery += "join ACLSID sid on sid.SID=CONCAT('GROUP_', GROUP_AUTHORITY.AUTHORITY) ";
                sqlQuery += "where ug.name in(" + groupToSql( groups ) + ") ";
                if ( showOnlyEditable ) {
                    sqlQuery += ") and ace.MASK = 2) ";
                } else {
                    sqlQuery += ") and (ace.MASK = 1 or ace.MASK = 2)) ";
                }
            }
            sqlQuery += ") ";

            if ( showPublic && !showOnlyEditable ) {
                sqlQuery += "or (ace.SID_FK = 4 and ace.MASK = 1)) ";
            }
        } else if ( showPublic && !showOnlyEditable ) {
            sqlQuery += "and (ace.SID_FK = 4 and ace.MASK = 1) ";
        }

        return sqlQuery;
    }

    private String addTaxonToQuery( String keyWord, Taxon taxon ) {
        String taxonSqlQuery = "";

        if ( taxon != null && taxon.getId() != null && !taxon.getId().equals( 0 ) ) {
            taxonSqlQuery = keyWord + " tax.ID = " + taxon.getId() + " ";
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

    /** basic sql command to deal with security */
    private String getPhenotypesGenesAssociationsBeginQuery() {
        String queryString = "";

        queryString += "from CHARACTERISTIC ";
        queryString += "join PHENOTYPE_ASSOCIATION on CHARACTERISTIC.PHENOTYPE_ASSOCIATION_FK = PHENOTYPE_ASSOCIATION.ID ";
        queryString += "join CHROMOSOME_FEATURE on CHROMOSOME_FEATURE.id = PHENOTYPE_ASSOCIATION.GENE_FK ";
        queryString += "join TAXON tax on tax.ID = CHROMOSOME_FEATURE.TAXON_FK ";
        queryString += "join ACLOBJECTIDENTITY aoi on PHENOTYPE_ASSOCIATION.id = aoi.OBJECT_ID ";
        queryString += "join ACLENTRY ace on ace.OBJECTIDENTITY_FK = aoi.ID ";
        queryString += "join ACLSID sid on sid.ID = aoi.OWNER_SID_FK ";
        queryString += "where aoi.OBJECT_CLASS IN " + DISCRIMINATOR_CLAUSE;

        return queryString;
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

    /** execute sqlQuery and populate phenotypesGenesAssociations is : phenotype --> genes */
    private void populateGenesAssociations( String sqlQuery, Map<String, Set<Integer>> phenotypesGenesAssociations ) {

        SQLQuery queryObject = this.getSessionFactory().getCurrentSession().createSQLQuery( sqlQuery );

        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        while ( results.next() ) {

            Integer geneNcbiId = ( Integer ) results.get( 0 );
            String valueUri = ( String ) results.get( 1 );

            if ( phenotypesGenesAssociations.containsKey( valueUri ) ) {
                phenotypesGenesAssociations.get( valueUri ).add( geneNcbiId );
            } else {
                Set<Integer> genesNCBI = new HashSet<Integer>();
                genesNCBI.add( geneNcbiId );
                phenotypesGenesAssociations.put( valueUri, genesNCBI );
            }
        }
        results.close();
    }

    /** execute sqlQuery and populate phenotypesGenesAssociations is : phenotype --> genes */
    private void populateGenesWithPhenotypes( String sqlQuery,
            HashMap<Long, GeneEvidenceValueObject> genesWithPhenotypes ) {

        org.hibernate.SQLQuery queryObject = this.getSessionFactory().getCurrentSession().createSQLQuery( sqlQuery );

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
