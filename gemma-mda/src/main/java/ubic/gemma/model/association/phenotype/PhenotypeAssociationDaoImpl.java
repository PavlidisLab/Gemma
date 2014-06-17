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

import gemma.gsec.util.SecurityUtil;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ExternalDatabaseStatisticsValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GeneEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.PhenotypeValueObject;
import ubic.gemma.persistence.AbstractDao;

/**
 * deals with all basic queries used by Neurocarta
 * 
 * @author Nicolas
 * @version $Id$ TODO: change criteria queries
 *          to hql to be consistent, if parameter use findByNamedParam and StringUtils.join if needed
 */
@Repository
public class PhenotypeAssociationDaoImpl extends AbstractDao<PhenotypeAssociation> implements PhenotypeAssociationDao {

    private static final String DISCRIMINATOR_CLAUSE = "('ubic.gemma.model.association.phenotype.LiteratureEvidenceImpl',"
            + "'ubic.gemma.model.association.phenotype.GenericEvidenceImpl',"
            + "'ubic.gemma.model.association.phenotype.ExperimentalEvidenceImpl',"
            + "'ubic.gemma.model.association.phenotype.DifferentialExpressionEvidenceImpl') ";

    private static Log log = LogFactory.getLog( PhenotypeAssociationDaoImpl.class );

    @Autowired
    public PhenotypeAssociationDaoImpl( SessionFactory sessionFactory ) {
        super( PhenotypeAssociation.class );
        super.setSessionFactory( sessionFactory );
    }

    /**
     * @param sqlQuery
     * @param queryObject
     */
    public void addUserAndGroupParameters( String sqlQuery, SQLQuery queryObject ) {
        if ( SecurityUtil.isUserAnonymous() ) {
            return;
        }

        String userName = SecurityUtil.getCurrentUsername();

        // if user is member of any groups.
        if ( sqlQuery.contains( ":groups" ) ) {
            Collection<String> groups = this
                    .getSessionFactory()
                    .getCurrentSession()
                    .createQuery(
                            "select ug.name from UserGroupImpl ug inner join ug.groupMembers memb where memb.userName = :user" )
                    .setParameter( "user", userName ).list();
            queryObject.setParameterList( "groups", groups );
        }

        if ( sqlQuery.contains( ":userName" ) ) {
            queryObject.setParameter( "userName", userName );
        }

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

        // FIXME only shows owner who is a user, not a grantedauthority. That might be okay.
        String sqlQuery = "select distinct sid.PRINCIPAL from ACLOBJECTIDENTITY aoi join ACLENTRY ace on ace.OBJECTIDENTITY_FK = "
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

    /** loads all evidences from a specific external database */
    @Override
    public Collection<PhenotypeAssociation> findEvidencesWithExternalDatabaseName( String externalDatabaseName,
            Integer limit ) {

        HibernateTemplate tpl = new HibernateTemplate( this.getSessionFactory() );

        if ( limit != null ) {
            tpl.setMaxResults( 10000 );
        }
        return tpl
                .findByNamedParam(
                        "select p from PhenotypeAssociation as p fetch all properties  join p.evidenceSource es join es.externalDatabase ed where ed.name=:name",
                        "name", externalDatabaseName );

    }

    /** find all evidence that doesn't come from an external course */
    @Override
    public Collection<PhenotypeAssociation> findEvidencesWithoutExternalDatabaseName() {

        return this.getHibernateTemplate().find(
                "select p from PhenotypeAssociation as p fetch all properties where p.evidenceSource is null" );

    }

    /** Gets all External Databases that are used with evidence */
    @Override
    public Collection<ExternalDatabase> findExternalDatabasesWithEvidence() {

        Collection<ExternalDatabase> externalDatabasesNames = this.getHibernateTemplate().find(
                "select distinct p.evidenceSource.externalDatabase from PhenotypeAssociation as p" );
        return externalDatabasesNames;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.association.phenotype.PhenotypeAssociationDao#findGenesWithPhenotype(ubic.basecode.ontology.
     * model.OntologyTerm, ubic.gemma.model.genome.Taxon, boolean)
     */
    @Override
    public Map<GeneValueObject, OntologyTerm> findGenesForPhenotype( OntologyTerm term, Long taxon, boolean includeIEA ) {

        Collection<OntologyTerm> children = term.getChildren( false );
        Map<String, OntologyTerm> uris = new HashMap<>();
        uris.put( term.getUri(), term );
        for ( OntologyTerm c : children ) {
            uris.put( c.getUri(), c );
        }

        assert !uris.isEmpty();

        Session sess = this.getSessionFactory().getCurrentSession();

        String q = "select distinct ph.gene, p.valueUri, p.evidenceCode "
                + " from PhenotypeAssociation ph join ph.phenotypes p where p.valueUri in (:t)";

        Query query = sess.createQuery( q );

        Map<GeneValueObject, OntologyTerm> result = new HashMap<>();
        query.setParameterList( "t", uris.keySet() );
        List<?> list = query.list();

        for ( Object o : list ) {

            Object[] oa = ( Object[] ) o;
            Gene g = ( Gene ) oa[0];

            if ( !taxon.equals( g.getTaxon().getId() ) ) continue;

            String uri = ( String ) oa[1];
            GOEvidenceCode ev = ( GOEvidenceCode ) oa[2];

            if ( !includeIEA && ev != null && ev.equals( GOEvidenceCode.IEA ) ) {
                continue;
            }

            GeneValueObject gvo = new GeneValueObject( g );

            OntologyTerm otForUri = uris.get( uri );
            assert otForUri != null;

            /*
             * only clobber if this term is more specific
             */
            if ( result.containsKey( gvo ) && otForUri.getParents( false ).contains( otForUri ) ) {
                continue;
            }
            result.put( gvo, otForUri );
        }

        return result;
    }

    /**
     * Key method: find Genes link to a phenotype taking into account private and public evidence Here on the cases :
     * <ul>
     * <li>1- Admin - can see anything
     * <li>2- user not logged in - only public data
     * <li>3- user logged in only showing what he has read access - public, shared + owned
     * <li>4- user logged in only showing what he has write access - owned.
     * </ul>
     * 
     * @see ubic.gemma.model.association.phenotype.PhenotypeAssociationDao#findGeneWithPhenotypes
     */
    @Override
    public Collection<GeneEvidenceValueObject> findGenesWithPhenotypes( Set<String> phenotypeUris, Taxon taxon,
            boolean showOnlyEditable, Collection<Long> externalDatabaseIds ) {

        if ( phenotypeUris.isEmpty() ) {
            return new HashSet<>();
        }

        // build query.
        // base query; 0: gene id 1: ncbi id 2: name 3: symbol 4: taxon id 5: taxon name 6: characteristic value URI
        String sqlSelectQuery = "select distinct gene.ID as gid, gene.NCBI_GENE_ID, gene.OFFICIAL_NAME, "
                + "gene.OFFICIAL_SYMBOL, tax.ID as taxonid, tax.COMMON_NAME, charac.VALUE_URI ";

        String sqlQuery = sqlSelectQuery + getPhenotypesGenesAssociationsBeginQuery( false, false );

        sqlQuery += addValuesUriToQuery( SecurityUtil.isUserAdmin() ? "" : "and", phenotypeUris );

        if ( !SecurityUtil.isUserAdmin() ) {
            if ( !sqlQuery.trim().endsWith( "where" ) ) {
                sqlQuery += " and ";
            }
            sqlQuery += addGroupAndUserNameRestriction( showOnlyEditable, true );
        }
        sqlQuery += addTaxonToQuery( "and", taxon );
        sqlQuery += addExternalDatabaseQuery( "and", externalDatabaseIds );

        // create query and set parameters.
        SQLQuery queryObject = this.getSessionFactory().getCurrentSession().createSQLQuery( sqlQuery );
        queryObject.setParameterList( "valueUris", phenotypeUris );

        if ( sqlQuery.contains( ":taxonId" ) ) {
            queryObject.setParameter( "taxonId", taxon.getId() );
        }
        addUserAndGroupParameters( sqlQuery, queryObject );

        return populateGenesWithPhenotypes( queryObject );

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
        Collection<PhenotypeAssociation> manualCuration = new HashSet<>();
        Collection<PhenotypeAssociation> evidenceWithSource = new HashSet<>();

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
    public Collection<PhenotypeAssociation> findPhenotypeAssociationWithIds( Collection<Long> paIds ) {

        if ( paIds == null || paIds.isEmpty() ) {
            return new HashSet<PhenotypeAssociation>();
        }

        Session s = this.getSessionFactory().getCurrentSession();
        Query q = s.createQuery( "select p from PhenotypeAssociation p fetch all properties where p.id in (:paIds) " );
        q.setParameterList( "paIds", paIds );

        return q.list();
    }

    @Override
    /** find PhenotypeAssociations associated with a BibliographicReference */
    public Collection<PhenotypeAssociation> findPhenotypesForBibliographicReference( String pubMedID ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select phe from PhenotypeAssociation as phe join phe.phenotypeAssociationPublications as pub "
                        + "join pub.citation as c join c.pubAccession as acc where acc.accession=:pubMedID",
                "pubMedID", pubMedID );
    }

    /**
     * find private evidence id that the user can modifiy
     */
    @Override
    public Set<Long> findPrivateEvidenceId( Long taxonId, Integer limit ) {

        String limitAbs = "";
        String orderBy = "";

        if ( limit < 0 ) {
            limitAbs = "limit " + limit * -1;
            orderBy = "order by LAST_UPDATE_DATE asc ";
        } else {
            orderBy = "order by LAST_UPDATE_DATE desc ";
            limitAbs = "limit " + limit;
        }

        Set<Long> ids = new HashSet<>();

        String sqlQuery = "select distinct phen.ID ";
        sqlQuery += getPhenotypesGenesAssociationsBeginQuery( false, true );

        if ( !SecurityUtil.isUserAdmin() ) { // admins have no restrictions.
            if ( !sqlQuery.trim().endsWith( "where" ) ) {
                sqlQuery += " AND ";
            }
            sqlQuery += addGroupAndUserNameRestriction( true, false );
        }

        if ( taxonId != null ) {
            if ( !sqlQuery.trim().endsWith( "where" ) ) {
                sqlQuery += " AND ";
            }
            sqlQuery += " tax.ID = :taxonId ";
        }

        sqlQuery += orderBy + limitAbs;

        SQLQuery queryObject = this.getSessionFactory().getCurrentSession().createSQLQuery( sqlQuery );

        if ( taxonId != null ) {
            queryObject.setParameter( "taxonId", taxonId );
        }

        addUserAndGroupParameters( sqlQuery, queryObject );

        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );

        while ( results.next() ) {
            Long phenotypeId = ( ( BigInteger ) results.get( 0 ) ).longValue();
            ids.add( phenotypeId );
        }

        results.close();
        return ids;
    }

    /** find all private phenotypes associated with genes on a specific taxon and containing the valuesUri */
    @Override
    public Map<String, Set<Integer>> findPrivatePhenotypesGenesAssociations( Taxon taxon, Set<String> valuesUri,
            boolean showOnlyEditable, Collection<Long> externalDatabaseIds, boolean noElectronicAnnotation ) {

        /*
         * At this level of the application, we can't access acls. The reason for this so we don't get uneven page
         * numbers. ACESID 4 is anonymous; MASK=1 is read.
         */
        String sqlQuery = "select gene.NCBI_GENE_ID, charac.VALUE_URI ";
        sqlQuery += getPhenotypesGenesAssociationsBeginQuery( true, false );
        if ( !sqlQuery.trim().endsWith( "where" ) ) {
            sqlQuery += " and ";
        }
        sqlQuery += addGroupAndUserNameRestriction( showOnlyEditable, false );
        sqlQuery += "and phen.ID not in "
                + "(select phen.ID from CHARACTERISTIC as charac "
                + " inner join PHENOTYPE_ASSOCIATION as phen on charac.PHENOTYPE_ASSOCIATION_FK = phen.ID "
                + "inner join CHROMOSOME_FEATURE as gene on gene.ID = phen.GENE_FK "
                + "inner join TAXON tax on tax.ID = gene.TAXON_FK "
                // ACL
                + "inner join ACLOBJECTIDENTITY aoi on phen.ID = aoi.OBJECT_ID "
                + "inner join ACLENTRY ace on ace.OBJECTIDENTITY_FK = aoi.ID "
                + "inner join ACLSID sid on sid.ID = aoi.OWNER_SID_FK where ace.MASK = 1 and ace.SID_FK = 4 "
                + "and aoi.OBJECT_CLASS IN " + DISCRIMINATOR_CLAUSE + ") ";
        sqlQuery += addTaxonToQuery( "and", taxon );
        sqlQuery += addValuesUriToQuery( "and", valuesUri );
        sqlQuery += addExternalDatabaseQuery( "and", externalDatabaseIds );

        if ( noElectronicAnnotation ) {
            sqlQuery += addNoIEAEvidenceCodeQuery();
        }

        SQLQuery queryObject = this.getSessionFactory().getCurrentSession().createSQLQuery( sqlQuery );

        if ( sqlQuery.contains( ":valueUris" ) ) {
            queryObject.setParameterList( "valueUris", valuesUri );
        }

        if ( sqlQuery.contains( ":taxonId" ) ) {
            queryObject.setParameter( "taxonId", taxon.getId() );
        }

        addUserAndGroupParameters( sqlQuery, queryObject );
        return populateGenesAssociations( queryObject );

    }

    /*
     * find all public phenotypes associated with genes on a specific taxon and containing the valuesUri
     * 
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.association.phenotype.PhenotypeAssociationDao#findPublicPhenotypesGenesAssociations(ubic.gemma
     * .model.genome.Taxon, java.util.Set, java.lang.String, java.util.Collection, boolean, java.util.Collection)
     */
    @Override
    public Map<String, Set<Integer>> findPublicPhenotypesGenesAssociations( Taxon taxon, Set<String> valuesUri,
            boolean showOnlyEditable, Collection<Long> externalDatabaseIds, boolean noElectronicAnnotation ) {

        String sqlQuery = "select gene.NCBI_GENE_ID, charac.VALUE_URI ";
        sqlQuery += getPhenotypesGenesAssociationsBeginQuery( true, false );

        // rule to find public: anonymous, READ.
        if ( !sqlQuery.trim().endsWith( "where" ) ) {
            sqlQuery += " and ";
        }
        sqlQuery += " ace.MASK = 1 and ace.SID_FK = 4 ";
        sqlQuery += addTaxonToQuery( "and", taxon );
        sqlQuery += addValuesUriToQuery( "and", valuesUri );
        sqlQuery += addExternalDatabaseQuery( "and", externalDatabaseIds );

        if ( noElectronicAnnotation ) {
            sqlQuery += addNoIEAEvidenceCodeQuery();
        }

        if ( showOnlyEditable ) {
            sqlQuery += "and phen.ID in ( select phen.ID ";
            sqlQuery += getPhenotypesGenesAssociationsBeginQuery( false, false );
            if ( !sqlQuery.trim().endsWith( "where" ) ) {
                sqlQuery += " and ";
            }
            sqlQuery += addGroupAndUserNameRestriction( showOnlyEditable, false );
            sqlQuery += ") ";
        }

        SQLQuery queryObject = this.getSessionFactory().getCurrentSession().createSQLQuery( sqlQuery );

        if ( sqlQuery.contains( ":valueUris" ) ) {
            queryObject.setParameterList( "valueUris", valuesUri );
        }

        if ( sqlQuery.contains( ":taxonId" ) ) {
            queryObject.setParameter( "taxonId", taxon.getId() );
        }

        addUserAndGroupParameters( sqlQuery, queryObject );
        return populateGenesAssociations( queryObject );

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

    @Override
    public Collection<String> loadAllDescription() {
        return this.getHibernateTemplate().find( "select distinct p.description from PhenotypeAssociation as p " );
    }

    /**
     * find all phenotypes in Neurocarta, this was requested by AspireBD, FIXME this is almost the same
     * asloadAllPhenotypesUri
     */
    @Override
    public Collection<PhenotypeValueObject> loadAllNeurocartaPhenotypes() {

        Collection<PhenotypeValueObject> phenotypeValueObjects = new HashSet<PhenotypeValueObject>();

        List<?> res = this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct c.valueUri,c.value from PhenotypeAssociation p join p.phenotypes c" )
                .list();

        for ( Object o : res ) {
            Object[] oa = ( Object[] ) o;
            String valueUri = ( String ) oa[0];
            String value = ( String ) oa[1];
            PhenotypeValueObject p = new PhenotypeValueObject( value, valueUri );
            phenotypeValueObjects.add( p );
        }

        return phenotypeValueObjects;
    }

    /** load all valueURI of Phenotype in the database */
    // TODO : both method could be the same and return PhenotypeValueObject, returning a set of uri makes the
    // manipulation easy when comparing it to other String uri
    @Override
    public Set<String> loadAllPhenotypesUri() {
        return new HashSet<String>( this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct c.valueUri from PhenotypeAssociation p join p.phenotypes c" )
                .setCacheable( true ).setCacheRegion( null ).list() );
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
    public ExternalDatabaseStatisticsValueObject loadStatisticsOnAllEvidence( String downloadFile ) {

        Long numEvidence = ( Long ) this.getHibernateTemplate()
                .find( "select count (p) from PhenotypeAssociation as p" ).iterator().next();

        Long numGenes = ( Long ) this.getHibernateTemplate()
                .find( "select count (distinct g) from GeneImpl as g inner join g.phenotypeAssociations as p" )
                .iterator().next();

        Long numPhenotypes = ( Long ) this
                .getHibernateTemplate()
                .find( "select count (distinct c.valueUri) from PhenotypeAssociation as p inner join p.phenotypes as c" )
                .iterator().next();

        Collection<String> publications = this.getHibernateTemplate().find(
                "select distinct phe.citation.pubAccession.accession from PhenotypeAssociation as p"
                        + " join p.phenotypeAssociationPublications as phe" );

        Long numPublications = new Long( publications.size() );

        ExternalDatabaseStatisticsValueObject externalDatabaseStatisticsValueObject = new ExternalDatabaseStatisticsValueObject(
                "Total (unique)", "", "", numEvidence, numGenes, numPhenotypes, numPublications, null, downloadFile );

        return externalDatabaseStatisticsValueObject;
    }

    @Override
    public Collection<ExternalDatabaseStatisticsValueObject> loadStatisticsOnExternalDatabases( String downloadPath ) {

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
            externalDatabaseStatistics.setPathToDownloadFile( downloadPath
                    + externalDatabase.getName().replaceAll( " ", "" ) + ".tsv" );
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

        List<Object[]> numPublications = this.getHibernateTemplate().find(
                "select p.evidenceSource.externalDatabase.name, count (distinct pub.citation.pubAccession.accession) "
                        + "from PhenotypeAssociation as p join p.phenotypeAssociationPublications as pub"
                        + " group by p.evidenceSource.externalDatabase" );

        for ( Object[] o : numPublications ) {
            String externalDatabaseName = ( String ) o[0];
            externalDatabasesStatistics.get( externalDatabaseName ).addNumPublications( ( Long ) o[1] );
        }

        return externalDatabasesStatistics.values();
    }

    /** find statistics for manual curation (numGene, numPhenotypes, etc.) */
    @Override
    public ExternalDatabaseStatisticsValueObject loadStatisticsOnManualCuration( String downloadFile ) {

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

        List<?> result = tpl.find( "select p.status.lastUpdateDate from PhenotypeAssociation as p "
                + "where p.evidenceSource is null order by p.status.lastUpdateDate desc" );

        Date lastUpdatedDate = null;
        if ( !result.isEmpty() ) {
            lastUpdatedDate = ( Timestamp ) result.get( 0 );
        }

        // find all secondary pubmed for ExperimentalEvidence
        Collection<String> publications = this.getHibernateTemplate().find(
                "select distinct pub.citation.pubAccession.accession from PhenotypeAssociation as p "
                        + "join p.phenotypeAssociationPublications as pub where p.evidenceSource is null" );

        Long numPublications = new Long( publications.size() );

        ExternalDatabaseStatisticsValueObject externalDatabaseStatisticsValueObject = new ExternalDatabaseStatisticsValueObject(
                "Manual Curation", "Evidence curated manually through literature review", "", numEvidence, numGenes,
                numPhenotypes, numPublications, lastUpdatedDate, downloadFile );

        return externalDatabaseStatisticsValueObject;
    }

    @Override
    /** remove a PhenotypeAssociationPublication **/
    public void removePhenotypePublication( Long phenotypeAssociationPublicationId ) {
        this.getHibernateTemplate().bulkUpdate( "delete from PhenotypeAssociationPublicationImpl p where p.id = ?",
                phenotypeAssociationPublicationId );
    }

    /**
     * @param keyWord
     * @param externalDatabaseIds
     * @return
     */
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
            // SLIGHTLY UNSAFE USE PARAMETER
            if ( excludeManualCuration && excludeExternalDatabase ) {
                externalDatabaseSqlQuery = keyWord
                        + " phen.EVIDENCE_SOURCE_FK in (SELECT id FROM DATABASE_ENTRY dbe where dbe.EXTERNAL_DATABASE_FK not in ("
                        + listIds + ")) ";
            } else if ( excludeExternalDatabase ) {
                externalDatabaseSqlQuery = keyWord + " (phen.EVIDENCE_SOURCE_FK is null or phen.EVIDENCE_SOURCE_FK "
                        + "not in (SELECT id FROM DATABASE_ENTRY dbe where dbe.EXTERNAL_DATABASE_FK in (" + listIds
                        + "))) ";
            } else if ( excludeManualCuration ) {
                externalDatabaseSqlQuery = keyWord + " phen.EVIDENCE_SOURCE_FK is not null";
            }

        }
        return externalDatabaseSqlQuery;

    }

    // same code is in EntityUtils
    private String addGroupAndUserNameRestriction( boolean showOnlyEditable, boolean showPublic ) {

        String sqlQuery = "";

        if ( !SecurityUtil.isUserAnonymous() ) {

            if ( showPublic && !showOnlyEditable ) {
                sqlQuery += "  ((sid.PRINCIPAL = :userName";
            } else {
                sqlQuery += "  (sid.PRINCIPAL = :userName";
            }

            // if ( !groups.isEmpty() ) { // is it possible to be empty? If they are non-anonymous it will not be, there
            // // will at least be GROUP_USER? Or that doesn't count?
            sqlQuery += " or (ace.SID_FK in (";
            // SUBSELECT
            sqlQuery += " select sid.ID from USER_GROUP ug ";
            sqlQuery += " join GROUP_AUTHORITY ga on ug.ID = ga.GROUP_FK ";
            sqlQuery += " join ACLSID sid on sid.GRANTED_AUTHORITY=CONCAT('GROUP_', ga.AUTHORITY) ";
            sqlQuery += " where ug.name in (:groups) ";
            if ( showOnlyEditable ) {
                sqlQuery += ") and ace.MASK = 2) "; // 2 = read-writable
            } else {
                sqlQuery += ") and (ace.MASK = 1 or ace.MASK = 2)) "; // 1 = read only
            }
            // }
            sqlQuery += ") ";

            if ( showPublic && !showOnlyEditable ) {
                // publicly readable data.
                sqlQuery += "or (ace.SID_FK = 4 and ace.MASK = 1)) "; // 4 =IS_AUTHENTICATED_ANONYMOUSLY
            }

        } else if ( showPublic && !showOnlyEditable ) {
            // publicly readable data
            sqlQuery += " (ace.SID_FK = 4 and ace.MASK = 1) "; // 4 = IS_AUTHENTICATED_ANONYMOUSLY
        }

        return sqlQuery;
    }

    private String addNoIEAEvidenceCodeQuery() {
        return " and phen.EVIDENCE_CODE != 'IEA'";
    }

    /**
     * @param keyWord
     * @param taxon
     * @return
     */
    private String addTaxonToQuery( String keyWord, Taxon taxon ) {
        String taxonSqlQuery = "";
        if ( taxon != null && taxon.getId() != null && !taxon.getId().equals( 0 ) ) {
            taxonSqlQuery = keyWord + " tax.ID = :taxonId ";
        }
        return taxonSqlQuery;
    }

    /**
     * Add IN clause for contstraint on valueuris.
     * 
     * @param keyWord either 'and' or '' depending on whether this is the first clause...
     * @param valuesUri
     * @return
     */
    private String addValuesUriToQuery( String keyWord, Set<String> valuesUris ) {

        String query = "";
        if ( valuesUris != null && !valuesUris.isEmpty() ) {
            query = keyWord + " charac.VALUE_URI in (:valueUris) ";
        }
        return query;
    }

    /**
     * basic sql command to deal with security; adds the where clause; delcare aliases charac, phen and gene; ace, aoi,
     * sid
     */
    private String getPhenotypesGenesAssociationsBeginQuery( boolean force, boolean addStatus ) {
        String queryString = "";

        queryString += "from CHARACTERISTIC as charac ";
        queryString += "join PHENOTYPE_ASSOCIATION as phen on charac.PHENOTYPE_ASSOCIATION_FK = phen.ID ";
        if ( addStatus ) {
            queryString += "join STATUS as stat on stat.ID = phen.STATUS_FK "; // what good does this do?
        }
        queryString += "join CHROMOSOME_FEATURE as gene on gene.id = phen.GENE_FK ";
        queryString += "join TAXON tax on tax.ID = gene.TAXON_FK ";

        if ( SecurityUtil.isUserAdmin() && !force ) {
            // no constraint needed.
            queryString += " where ";
        } else {
            // See entityutils for a generalization of this.
            // / non-admin user, need to add constraint on permissions.
            queryString += "join ACLOBJECTIDENTITY aoi on phen.ID = aoi.OBJECT_ID ";
            queryString += "join ACLENTRY ace on ace.OBJECTIDENTITY_FK = aoi.ID ";
            queryString += "join ACLSID sid on sid.ID = aoi.OWNER_SID_FK ";
            queryString += "where aoi.OBJECT_CLASS IN " + DISCRIMINATOR_CLAUSE;
        }

        return queryString;
    }

    /**
     * execute sqlQuery and populate phenotypesGenesAssociations is : phenotype --> genes
     * 
     * @return
     */
    private Map<String, Set<Integer>> populateGenesAssociations( SQLQuery queryObject ) {
        Map<String, Set<Integer>> phenotypesGenesAssociations = new HashMap<>();
        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        while ( results.next() ) {

            Integer geneNcbiId = ( Integer ) results.get( 0 );
            String valueUri = ( String ) results.get( 1 );

            if ( phenotypesGenesAssociations.containsKey( valueUri ) ) {
                phenotypesGenesAssociations.get( valueUri ).add( geneNcbiId );
            } else {
                Set<Integer> genesNCBI = new HashSet<>();
                genesNCBI.add( geneNcbiId );
                phenotypesGenesAssociations.put( valueUri, genesNCBI );
            }
        }
        results.close();
        return phenotypesGenesAssociations;
    }

    /**
     * execute sqlQuery and populate phenotypesGenesAssociations is : phenotype --> genes
     * 
     * @param queryObject
     * @return
     */
    private Collection<GeneEvidenceValueObject> populateGenesWithPhenotypes( SQLQuery queryObject ) {
        StopWatch sw = new StopWatch();
        sw.start();

        // we accumulate the phenotypes for a gene in one VO
        Map<Long, GeneEvidenceValueObject> genesWithPhenotypes = new HashMap<>();

        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        while ( results.next() ) {
            /* 0: gene id 1: ncbi id 2: name 3: symbol 4: taxon id 5: taxon name 6: characteristic value URI */
            Long geneId = ( ( BigInteger ) results.get( 0 ) ).longValue();
            Integer nbciGeneId = ( Integer ) results.get( 1 );
            String officialName = ( String ) results.get( 2 );
            String officialSymbol = ( String ) results.get( 3 );
            Long taxonId = ( ( BigInteger ) results.get( 4 ) ).longValue();
            String taxonCommonName = ( String ) results.get( 5 );
            String valueUri = ( String ) results.get( 6 );

            if ( genesWithPhenotypes.get( geneId ) != null ) {
                genesWithPhenotypes.get( geneId ).getPhenotypesValueUri().add( valueUri );
            } else {
                GeneEvidenceValueObject g = new GeneEvidenceValueObject();
                g.setId( geneId );
                g.setNcbiId( nbciGeneId );
                g.setOfficialName( officialName );
                g.setOfficialSymbol( officialSymbol );
                g.setTaxonCommonName( taxonCommonName );
                g.setTaxonId( taxonId );
                g.getPhenotypesValueUri().add( valueUri );
                genesWithPhenotypes.put( geneId, g );
            }
        }
        results.close();

        if ( sw.getTime() > 500 ) {
            log.info( "Get " + genesWithPhenotypes.size() + " genes with phenotypes: " + sw.getTime() + "ms" );
        }

        return genesWithPhenotypes.values();
    }
}
