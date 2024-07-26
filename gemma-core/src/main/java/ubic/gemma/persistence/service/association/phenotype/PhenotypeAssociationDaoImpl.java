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
package ubic.gemma.persistence.service.association.phenotype;

import gemma.gsec.util.SecurityUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.association.phenotype.DifferentialExpressionEvidence;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.association.phenotype.PhenotypeAssociationPublication;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ExternalDatabaseStatisticsValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GeneEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.PhenotypeValueObject;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailDao;
import ubic.gemma.persistence.util.EntityUtils;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.*;

/**
 * deals with all basic queries used by Neurocarta
 *
 * @author Nicolas
 */
@Repository
public class PhenotypeAssociationDaoImpl extends AbstractDao<PhenotypeAssociation> implements PhenotypeAssociationDao {
    public static final int DEFAULT_PA_LIMIT = 10000;
    private static final String QUERY_EV_CODE = " and phen.EVIDENCE_CODE != 'IEA'";
    private static final String DISCRIMINATOR_CLAUSE = "('ubic.gemma.model.association.phenotype.LiteratureEvidence',"
            + "'ubic.gemma.model.association.phenotype.GenericEvidence',"
            + "'ubic.gemma.model.association.phenotype.ExperimentalEvidence',"
            + "'ubic.gemma.model.association.phenotype.DifferentialExpressionEvidence') ";
    private static final Log log = LogFactory.getLog( PhenotypeAssociationDaoImpl.class );

    @Autowired
    private AuditTrailDao auditTrailDao;

    @Autowired
    public PhenotypeAssociationDaoImpl( SessionFactory sessionFactory ) {
        super( PhenotypeAssociation.class, sessionFactory );
    }

    @Override
    public PhenotypeAssociation load( Long id ) {
        return ( PhenotypeAssociation ) this.getSessionFactory().getCurrentSession()
                .createQuery( "from PhenotypeAssociation fetch all properties where id = :id" )
                .setParameter( "id", id )
                .uniqueResult();
    }

    /**
     * counts the evidence that from neurocarta that came from a specific MetaAnalysis
     */
    @Override
    public Long countEvidenceWithGeneDifferentialExpressionMetaAnalysis(
            Long geneDifferentialExpressionMetaAnalysisId ) {

        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery(
                        "select count (d) from DifferentialExpressionEvidence as d where d.geneDifferentialExpressionMetaAnalysisResult "
                                + "in (select r from GeneDifferentialExpressionMetaAnalysis as g join g.results as r where g.id= :gid)" )
                .setParameter( "gid", geneDifferentialExpressionMetaAnalysisId ).uniqueResult();
    }

    /**
     * find category terms currently used in the database by evidence
     */
    @Override
    public Collection<CharacteristicValueObject> findEvidenceCategoryTerms() {

        Collection<CharacteristicValueObject> mgedCategory = new TreeSet<>();

        String queryString = "SELECT DISTINCT CATEGORY_URI, category FROM PHENOTYPE_ASSOCIATION "
                + "JOIN INVESTIGATION ON PHENOTYPE_ASSOCIATION.EXPERIMENT_FK = INVESTIGATION.ID "
                + "JOIN CHARACTERISTIC ON CHARACTERISTIC.INVESTIGATION_FK= INVESTIGATION.ID";
        org.hibernate.SQLQuery queryObject = this.getSessionFactory().getCurrentSession().createSQLQuery( queryString );

        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        try {
            while ( results.next() ) {
                CharacteristicValueObject characteristicValueObject = new CharacteristicValueObject( -1L );
                characteristicValueObject.setCategoryUri( ( String ) results.get( 0 ) );
                characteristicValueObject.setCategory( ( String ) results.get( 1 ) );
                mgedCategory.add( characteristicValueObject );
            }
        } finally {
            results.close();
        }

        return mgedCategory;
    }

    /**
     * return the list of the owners that have evidence in the system
     */
    @Override
    public Collection<String> findEvidenceOwners() {

        Set<String> owners = new HashSet<>();

        String sqlQuery =
                "SELECT DISTINCT sid.PRINCIPAL FROM ACLOBJECTIDENTITY aoi JOIN ACLENTRY ace ON ace.OBJECTIDENTITY_FK = "
                        + "aoi.ID JOIN ACLSID sid ON sid.ID = aoi.OWNER_SID_FK WHERE aoi.OBJECT_CLASS " + "IN  "
                        + PhenotypeAssociationDaoImpl.DISCRIMINATOR_CLAUSE;

        SQLQuery queryObject = this.getSessionFactory().getCurrentSession().createSQLQuery( sqlQuery );

        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );

        try {
            while ( results.next() ) {
                String owner = ( String ) results.get( 0 );
                owners.add( owner );
            }
        } finally {
            results.close();
        }

        return owners;
    }

    /**
     * loads all evidences from a specific external database
     */
    @Override
    public Collection<PhenotypeAssociation> findEvidencesWithExternalDatabaseName( String externalDatabaseName,
            int limit, int start ) {

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select p from PhenotypeAssociation as p fetch all properties where "
                                + "lower(p.evidenceSource.externalDatabase.name)=:name" )
                .setParameter( "name", externalDatabaseName.toLowerCase() )
                .setFirstResult( start )
                .setMaxResults( limit )
                .list();
    }

    /**
     * find all evidence that doesn't come from an external course
     */
    @Override
    public Collection<PhenotypeAssociation> findEvidencesWithoutExternalDatabaseName() {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                "select p from PhenotypeAssociation as p fetch all properties where p.evidenceSource is null" ).list();
    }

    /**
     * Gets all External Databases that are used with evidence
     */
    @Override
    public Collection<ExternalDatabase> findExternalDatabasesWithEvidence() {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct p.evidenceSource.externalDatabase from PhenotypeAssociation as p" )
                .list();
    }

    @Override
    public Map<GeneValueObject, OntologyTerm> findGenesForPhenotype( OntologyTerm term, Long taxon,
            boolean includeIEA ) {

        Collection<OntologyTerm> children = term.getChildren( false );
        Map<String, OntologyTerm> uris = new HashMap<>();
        uris.put( term.getUri(), term );
        for ( OntologyTerm c : children ) {
            uris.put( c.getUri(), c );
        }

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

            if ( !taxon.equals( g.getTaxon().getId() ) )
                continue;

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
     */
    @Override
    public Collection<GeneEvidenceValueObject> findGenesWithPhenotypes( Set<String> phenotypeUris, @Nullable Taxon taxon,
            boolean showOnlyEditable, @Nullable Collection<Long> externalDatabaseIds ) {

        if ( phenotypeUris.isEmpty() ) {
            return new HashSet<>();
        }

        // build query.
        // base query; 0: gene id 1: ncbi id 2: name 3: symbol 4: taxon id 5: taxon name 6: characteristic value URI
        String sqlSelectQuery = "select distinct gene.ID as gid, gene.NCBI_GENE_ID, gene.OFFICIAL_NAME, "
                + "gene.OFFICIAL_SYMBOL, tax.ID as taxonid, tax.COMMON_NAME, charac.VALUE_URI ";

        String sqlQuery = sqlSelectQuery + this.getPhenotypesGenesAssociationsBeginQuery( false );

        sqlQuery += this.addValuesUriToQuery( SecurityUtil.isUserAdmin() ? " where " : " and ", phenotypeUris );

        if ( !SecurityUtil.isUserAdmin() ) {
            if ( !sqlQuery.trim().endsWith( "where" ) ) {
                sqlQuery += " and ";
            }
            sqlQuery += EntityUtils.addGroupAndUserNameRestriction( showOnlyEditable, true );
        }
        sqlQuery += this.addTaxonToQuery( taxon, !showOnlyEditable );
        sqlQuery += this.addExternalDatabaseQuery( externalDatabaseIds );

        // create query and set parameters.
        SQLQuery queryObject = this.getSessionFactory().getCurrentSession().createSQLQuery( sqlQuery );
        queryObject.setParameterList( "valueUris", phenotypeUris );

        if ( taxon != null ) {
            queryObject.setParameter( "taxonId", taxon.getId() );
        }
        EntityUtils.addUserAndGroupParameters( queryObject, this.getSessionFactory() );

        return this.populateGenesWithPhenotypes( queryObject );

    }

    /**
     * find all PhenotypeAssociation for a specific gene id
     */
    @Override
    public Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneId( Long geneId ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select distinct p from PhenotypeAssociation as p fetch all properties where p.gene.id = :gid" )
                .setParameter( "gid", geneId ).list();
    }

    /**
     * find all PhenotypeAssociation for a specific gene id and external Databases ids
     */
    @Override
    public Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneIdAndDatabases( Long geneId,
            @Nullable Collection<Long> externalDatabaseIds ) {

        boolean excludeManualCuration = false;
        boolean excludeExternalDatabase = false;
        Collection<Long> excludeExternalIds = new HashSet<>();
        Collection<PhenotypeAssociation> manualCuration = new HashSet<>();
        Collection<PhenotypeAssociation> evidenceWithSource;

        if ( externalDatabaseIds != null && !externalDatabaseIds.isEmpty() ) {

            for ( Long id : externalDatabaseIds ) {
                // 1 is Manual curation excluded
                if ( id.equals( 1L ) ) {
                    excludeManualCuration = true;
                } else {
                    excludeExternalDatabase = true;
                    excludeExternalIds.add( id );
                }
            }

            if ( !excludeManualCuration ) {
                // get all manual curated evidence (the ones with no external source)
                //noinspection unchecked
                manualCuration = this.getSessionFactory().getCurrentSession().createQuery(
                                "select distinct p from PhenotypeAssociation as p fetch all properties "
                                        + "where p.gene.id=:gid and p.evidenceSource is null" ).setParameter( "gid", geneId )
                        .list();
            }
        }

        String queryString = "select distinct p from PhenotypeAssociation as p fetch all properties where p.gene.id=:gid ";

        if ( excludeExternalDatabase ) {
            queryString += " and p.evidenceSource.externalDatabase.id not in (:eids)";
        }

        Query query = this.getSessionFactory().getCurrentSession().createQuery( queryString );
        query.setParameter( "gid", geneId );

        if ( excludeExternalDatabase ) {
            query.setParameterList( "eids", excludeExternalIds );
        }

        //noinspection unchecked
        evidenceWithSource = query.list();
        evidenceWithSource.addAll( manualCuration );

        return evidenceWithSource;
    }

    /**
     * find all PhenotypeAssociation for a specific NCBI id
     */
    @Override
    public Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneNCBI( Integer geneNCBI ) {

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select p from PhenotypeAssociation as p fetch all properties " + "where p.gene.ncbiGeneId=:n" )
                .setParameter( "n", geneNCBI ).list();

    }

    /**
     * find all PhenotypeAssociation for a specific NCBI id and phenotypes valueUri
     */
    @Override
    public Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneNCBI( Integer geneNCBI,
            Set<String> phenotype ) {

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select p from PhenotypeAssociation as p join p.phenotypes as phe join p.gene as g "
                                + "where phe.valueUri in (:p) and g.ncbiGeneId=:n " ).setParameterList( "p", phenotype )
                .setParameter( "n", geneNCBI ).list();
    }

    /**
     * find PhenotypeAssociation satisfying the given filters: paIds, taxonId and limit
     */
    @Override
    public Collection<PhenotypeAssociation> findPhenotypeAssociationWithIds( Collection<Long> paIds ) {

        if ( paIds.isEmpty() ) {
            return new HashSet<>();
        }

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select p from PhenotypeAssociation p fetch all properties where p.id in (:paIds) " )
                .setParameterList( "paIds", paIds ).list();
    }

    /**
     * find PhenotypeAssociations associated with a BibliographicReference
     */
    @Override
    public Collection<PhenotypeAssociation> findPhenotypesForBibliographicReference( String pubMedID ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select phe from PhenotypeAssociation as phe join phe.phenotypeAssociationPublications as pub "
                        + "join pub.citation as c join c.pubAccession as acc where acc.accession=:pubMedID" )
                .setParameter( "pubMedID", pubMedID )
                .list();
    }

    @Override
    public Set<Long> findPrivateEvidenceId( @Nullable Long taxonId, int limit ) {

        String limitAbs;
        String orderBy;

        if ( limit < 0 ) {
            limitAbs = "limit " + limit * -1;
            orderBy = "order by LAST_UPDATED asc ";
        } else {
            orderBy = "order by LAST_UPDATED desc ";
            limitAbs = "limit " + limit;
        }

        Set<Long> ids = new HashSet<>();

        String sqlQuery = "select distinct phen.ID ";
        sqlQuery += this.getPhenotypesGenesAssociationsBeginQuery( false );

        if ( !SecurityUtil.isUserAdmin() ) { // admins have no restrictions.
            if ( !sqlQuery.trim().endsWith( "where" ) ) {
                sqlQuery += " AND ";
            }
            sqlQuery += EntityUtils.addGroupAndUserNameRestriction( true, false );
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

        EntityUtils.addUserAndGroupParameters( queryObject, this.getSessionFactory() );

        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );

        try {
            while ( results.next() ) {
                Long phenotypeId = ( ( BigInteger ) results.get( 0 ) ).longValue();
                ids.add( phenotypeId );
            }
        } finally {
            results.close();
        }

        return ids;
    }

    /**
     * find all private phenotypes associated with genes on a specific taxon and containing the valuesUri
     */
    @Override
    public Map<String, Set<Integer>> findPrivatePhenotypesGenesAssociations( @Nullable Taxon taxon, @Nullable Set<String> valuesUri,
            boolean showOnlyEditable, @Nullable Collection<Long> externalDatabaseIds, boolean noElectronicAnnotation ) {

        /*
         * At this level of the application, we can't access acls. The reason for this so we don't get uneven page
         * numbers. ACESID 4 is anonymous; MASK=1 is read.
         */
        String sqlQuery = "select gene.NCBI_GENE_ID, charac.VALUE_URI ";
        sqlQuery += this.getPhenotypesGenesAssociationsBeginQuery( true );
        if ( !sqlQuery.trim().endsWith( "where" ) ) {
            sqlQuery += " and ";
        }
        sqlQuery += EntityUtils.addGroupAndUserNameRestriction( showOnlyEditable, false );
        sqlQuery += "and phen.ID not in " + "(select phen.ID from CHARACTERISTIC as charac "
                + " inner join PHENOTYPE_ASSOCIATION as phen on charac.PHENOTYPE_ASSOCIATION_FK = phen.ID "
                + "inner join CHROMOSOME_FEATURE as gene on gene.ID = phen.GENE_FK "
                + "inner join TAXON tax on tax.ID = gene.TAXON_FK "
                // ACL
                + "inner join ACLOBJECTIDENTITY aoi on phen.ID = aoi.OBJECT_ID "
                + "inner join ACLENTRY ace on ace.OBJECTIDENTITY_FK = aoi.ID "
                + "inner join ACLSID sid on sid.ID = aoi.OWNER_SID_FK where ace.MASK = 1 and ace.SID_FK = 4 "
                + "and aoi.OBJECT_CLASS IN " + PhenotypeAssociationDaoImpl.DISCRIMINATOR_CLAUSE + ") ";
        sqlQuery += this.addTaxonToQuery( taxon );
        sqlQuery += this.addValuesUriToQuery( "and", valuesUri );
        sqlQuery += this.addExternalDatabaseQuery( externalDatabaseIds );

        if ( noElectronicAnnotation ) {
            sqlQuery += PhenotypeAssociationDaoImpl.QUERY_EV_CODE;
        }

        SQLQuery queryObject = this.getSessionFactory().getCurrentSession().createSQLQuery( sqlQuery );

        if ( sqlQuery.contains( ":valueUris" ) ) {
            queryObject.setParameterList( "valueUris", valuesUri );
        }

        if ( taxon != null ) {
            queryObject.setParameter( "taxonId", taxon.getId() );
        }

        EntityUtils.addUserAndGroupParameters( queryObject, this.getSessionFactory() );
        return this.populateGenesAssociations( queryObject );

    }

    /**
     * find all public phenotypes associated with genes on a specific taxon and containing the valuesUri
     */
    @Override
    public Map<String, Set<Integer>> findPublicPhenotypesGenesAssociations( @Nullable Taxon taxon, @Nullable Set<String> valuesUri,
            boolean showOnlyEditable, @Nullable Collection<Long> externalDatabaseIds, boolean noElectronicAnnotation ) {

        String sqlQuery = "select gene.NCBI_GENE_ID, charac.VALUE_URI ";
        sqlQuery += this.getPhenotypesGenesAssociationsBeginQuery( true );

        // rule to find public: anonymous, READ.
        if ( !sqlQuery.trim().endsWith( "where" ) ) {
            sqlQuery += " and ";
        }
        sqlQuery += " ace.MASK = 1 and ace.SID_FK = 4 ";
        sqlQuery += this.addTaxonToQuery( taxon );
        sqlQuery += this.addValuesUriToQuery( "and", valuesUri );
        sqlQuery += this.addExternalDatabaseQuery( externalDatabaseIds );

        if ( noElectronicAnnotation ) {
            sqlQuery += PhenotypeAssociationDaoImpl.QUERY_EV_CODE;
        }

        if ( showOnlyEditable ) {
            sqlQuery += "and phen.ID in ( select phen.ID ";
            sqlQuery += this.getPhenotypesGenesAssociationsBeginQuery( false );
            if ( !sqlQuery.trim().endsWith( "where" ) ) {
                sqlQuery += " and ";
            }
            sqlQuery += EntityUtils.addGroupAndUserNameRestriction( true, false );
            sqlQuery += ") ";
        }

        SQLQuery queryObject = this.getSessionFactory().getCurrentSession().createSQLQuery( sqlQuery );

        if ( sqlQuery.contains( ":valueUris" ) ) {
            queryObject.setParameterList( "valueUris", valuesUri );
        }

        if ( taxon != null ) {
            queryObject.setParameter( "taxonId", taxon.getId() );
        }

        EntityUtils.addUserAndGroupParameters( queryObject, this.getSessionFactory() );
        return this.populateGenesAssociations( queryObject );

    }

    /**
     * find all phenotypes in Neurocarta, this was requested by AspireBD
     */
    @Override
    public Collection<PhenotypeValueObject> loadAllNeurocartaPhenotypes() {

        Collection<PhenotypeValueObject> phenotypeValueObjects = new HashSet<>();

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

    /**
     * load all valueURI of Phenotype in the database
     */
    @Override
    public Set<String> loadAllPhenotypesUri() {
        // noinspection unchecked
        return new HashSet<>( ( List<String> ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct c.valueUri from PhenotypeAssociation p join p.phenotypes c" )
                .setCacheable( true ).setCacheRegion( null ).list() );
    }

    /**
     * @param geneDifferentialExpressionMetaAnalysisId id
     * @param maxResults                               max results
     * @return a Collection for a geneDifferentialExpressionMetaAnalysisId if one exists
     * (can be used to find the threshold and phenotypes for a GeneDifferentialExpressionMetaAnalysis)
     */
    @Override
    public Collection<DifferentialExpressionEvidence> loadEvidenceWithGeneDifferentialExpressionMetaAnalysis(
            Long geneDifferentialExpressionMetaAnalysisId, int maxResults ) {

        //noinspection unchecked
        return ( List<DifferentialExpressionEvidence> ) this.getSessionFactory().getCurrentSession().createQuery(
                        "select d from DifferentialExpressionEvidence as d where d.geneDifferentialExpressionMetaAnalysisResult "
                                + "in (select r from GeneDifferentialExpressionMetaAnalysis as g join g.results as r where g.id=:gid)" )
                .setParameter( "gid", geneDifferentialExpressionMetaAnalysisId ).setMaxResults( maxResults ).list();
    }

    @Override
    public ExternalDatabaseStatisticsValueObject loadStatisticsOnAllEvidence( String downloadFile ) {

        Long numEvidence = ( Long ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select count (p) from PhenotypeAssociation as p" )
                .uniqueResult();

        Long numGenes = ( Long ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select count (distinct g) from Gene as g inner join g.phenotypeAssociations as p" )
                .uniqueResult();

        Long numPhenotypes = ( Long ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select count (distinct c.valueUri) from PhenotypeAssociation as p inner join p.phenotypes as c" )
                .uniqueResult();

        //noinspection unchecked
        Collection<String> publications = this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct phe.citation.pubAccession.accession from PhenotypeAssociation as p"
                        + " join p.phenotypeAssociationPublications as phe" )
                .list();

        Long numPublications = ( long ) publications.size();

        return new ExternalDatabaseStatisticsValueObject( "Total (unique)", "", "", numEvidence, numGenes,
                numPhenotypes, numPublications, null, downloadFile );
    }

    @Override
    public Collection<ExternalDatabaseStatisticsValueObject> loadStatisticsOnExternalDatabases( String downloadPath ) {

        HashMap<String, ExternalDatabaseStatisticsValueObject> externalDatabasesStatistics = new HashMap<>();

        //noinspection unchecked
        List<Object[]> numEvidence = this.getSessionFactory().getCurrentSession()
                .createQuery( "select p.evidenceSource.externalDatabase, count (*), p.lastUpdated from PhenotypeAssociation "
                        + "as p group by p.evidenceSource.externalDatabase order by p.lastUpdated desc" )
                .list();

        for ( Object[] o : numEvidence ) {

            ExternalDatabase externalDatabase = ( ExternalDatabase ) o[0];
            Long count = ( Long ) o[1];

            ExternalDatabaseStatisticsValueObject externalDatabaseStatistics = new ExternalDatabaseStatisticsValueObject();
            externalDatabaseStatistics.setDescription( externalDatabase.getDescription() );
            externalDatabaseStatistics.setName( externalDatabase.getName() );
            externalDatabaseStatistics
                    .setPathToDownloadFile( downloadPath + externalDatabase.getName().replaceAll( " ", "" ) + ".tsv" );
            externalDatabaseStatistics.setLastUpdateDate( ( Date ) o[2] );
            externalDatabaseStatistics.setWebUri( externalDatabase.getWebUri() );
            externalDatabaseStatistics.setNumEvidence( count );
            externalDatabasesStatistics.put( externalDatabase.getName(), externalDatabaseStatistics );
        }

        //noinspection unchecked
        List<Object[]> numGenes = this.getSessionFactory().getCurrentSession()
                .createQuery( "select p.evidenceSource.externalDatabase.name, count (distinct g) from Gene as g join g.phenotypeAssociations "
                        + "as p group by p.evidenceSource.externalDatabase" )
                .list();

        for ( Object[] o : numGenes ) {
            String externalDatabaseName = ( String ) o[0];
            externalDatabasesStatistics.get( externalDatabaseName ).setNumGenes( ( Long ) o[1] );
        }

        //noinspection unchecked
        List<Object[]> numPhenotypes = this.getSessionFactory().getCurrentSession()
                .createQuery( "select p.evidenceSource.externalDatabase.name, count (distinct c.valueUri) "
                        + "from PhenotypeAssociation as p join p.phenotypes as c "
                        + "group by p.evidenceSource.externalDatabase" )
                .list();

        for ( Object[] o : numPhenotypes ) {
            String externalDatabaseName = ( String ) o[0];
            externalDatabasesStatistics.get( externalDatabaseName ).setNumPhenotypes( ( Long ) o[1] );
        }

        //noinspection unchecked
        List<Object[]> numPublications = this.getSessionFactory().getCurrentSession()
                .createQuery( "select p.evidenceSource.externalDatabase.name, count (distinct pub.citation.pubAccession.accession) "
                        + "from PhenotypeAssociation as p join p.phenotypeAssociationPublications as pub"
                        + " group by p.evidenceSource.externalDatabase" )
                .list();

        for ( Object[] o : numPublications ) {
            String externalDatabaseName = ( String ) o[0];
            externalDatabasesStatistics.get( externalDatabaseName ).addNumPublications( ( Long ) o[1] );
        }

        return externalDatabasesStatistics.values();
    }

    /**
     * @param downloadFile file
     * @return find statistics for manual curation (numGene, numPhenotypes, etc.)
     */
    @Override
    public ExternalDatabaseStatisticsValueObject loadStatisticsOnManualCuration( String downloadFile ) {

        Long numEvidence = ( Long ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select count (p) from PhenotypeAssociation as p where p.evidenceSource is null" )
                .uniqueResult();

        Long numGenes = ( Long ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select count (distinct g) from Gene as g inner join g.phenotypeAssociations as p where p.evidenceSource is null" )
                .uniqueResult();

        Long numPhenotypes = ( Long ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select count (distinct c.valueUri) from PhenotypeAssociation as p inner join p.phenotypes as c where p.evidenceSource is null" )
                .uniqueResult();

        List<?> result = getSessionFactory().getCurrentSession()
                .createQuery( "select p.lastUpdated from PhenotypeAssociation as p "
                        + "where p.evidenceSource is null order by p.lastUpdated desc" )
                .setMaxResults( 1 )
                .list();

        Date lastUpdatedDate = null;
        if ( !result.isEmpty() ) {
            lastUpdatedDate = ( Timestamp ) result.get( 0 );
        }

        // find all secondary pubmed for ExperimentalEvidence
        //noinspection unchecked
        Collection<String> publications = this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct pub.citation.pubAccession.accession from PhenotypeAssociation as p "
                        + "join p.phenotypeAssociationPublications as pub where p.evidenceSource is null" )
                .list();

        Long numPublications = ( long ) publications.size();

        return new ExternalDatabaseStatisticsValueObject( "Manual curation",
                "Evidence curated manually through literature review", "", numEvidence, numGenes, numPhenotypes,
                numPublications, lastUpdatedDate, downloadFile );
    }

    /**
     * @param phenotypeAssociationPublicationId remove a PhenotypeAssociationPublication
     */
    @Override
    public void removePhenotypePublication( PhenotypeAssociationPublication phenotypeAssociationPublication ) {
        this.getSessionFactory().getCurrentSession()
                .createQuery( "delete from PhenotypeAssociationPublication p where p = :p" )
                .setParameter( "p", phenotypeAssociationPublication )
                .executeUpdate();
    }

    @Override
    public int removeAll() {
        //noinspection unchecked
        List<Long> atIds = getSessionFactory().getCurrentSession()
                .createQuery( "select at.id from PhenotypeAssociation pa join pa.auditTrail at" )
                .list();
        getSessionFactory().getCurrentSession()
                .createSQLQuery( "delete from CHARACTERISTIC where PHENOTYPE_ASSOCIATION_FK is not null" )
                .executeUpdate();
        getSessionFactory().getCurrentSession()
                .createSQLQuery( "delete from PHENOTYPE_ASSOCIATION_PUBLICATIONS where PHENOTYPE_ASSOCIATION_FK is not null" )
                .executeUpdate();
        int associationsRemoved = this.getSessionFactory().getCurrentSession()
                .createQuery( "delete from PhenotypeAssociation" )
                .executeUpdate();
        auditTrailDao.removeByIds( atIds );
        return associationsRemoved;
    }

    @SuppressWarnings("ConstantConditions") // Better readability
    private String addExternalDatabaseQuery( @Nullable Collection<Long> externalDatabaseIds ) {

        String externalDatabaseSqlQuery = "";
        StringBuilder listIds = new StringBuilder();
        boolean excludeManualCuration = false;
        boolean excludeExternalDatabase = false;

        if ( externalDatabaseIds != null && !externalDatabaseIds.isEmpty() ) {

            for ( Long id : externalDatabaseIds ) {

                if ( id.equals( 1L ) ) {
                    excludeManualCuration = true;
                } else {
                    listIds.append( id ).append( "," );
                    excludeExternalDatabase = true;
                }
            }

            listIds = new StringBuilder( StringUtils.removeEnd( listIds.toString(), "," ) );
            // SLIGHTLY UNSAFE USE PARAMETER
            if ( excludeManualCuration && excludeExternalDatabase ) {
                //language=MySQL
                externalDatabaseSqlQuery = "and"
                        + " phen.EVIDENCE_SOURCE_FK in (SELECT id FROM DATABASE_ENTRY dbe where dbe.EXTERNAL_DATABASE_FK not in ("
                        + listIds + ")) ";
            } else if ( excludeExternalDatabase ) {
                //language=MySQL
                externalDatabaseSqlQuery = "and" + " (phen.EVIDENCE_SOURCE_FK is null or phen.EVIDENCE_SOURCE_FK "
                        + "not in (SELECT id FROM DATABASE_ENTRY dbe where dbe.EXTERNAL_DATABASE_FK in (" + listIds
                        + "))) ";
            } else if ( excludeManualCuration ) {
                externalDatabaseSqlQuery = "and" + " phen.EVIDENCE_SOURCE_FK is not null";
            }

        }
        return externalDatabaseSqlQuery;

    }

    private String addTaxonToQuery( @Nullable Taxon taxon ) {
        return this.addTaxonToQuery( taxon, true );
    }

    private String addTaxonToQuery( @Nullable Taxon taxon, boolean useAnd ) {
        String taxonSqlQuery = "";
        if ( taxon != null && taxon.getId() != null && !taxon.getId().equals( 0L ) ) {
            taxonSqlQuery = ( useAnd ? "and" : "" ) + " tax.ID = :taxonId ";
        }
        return taxonSqlQuery;
    }

    /**
     * Add IN clause for contstraint on valueuris.
     *
     * @param keyWord    either 'and' or '' depending on whether this is the first clause...
     * @param valuesUris uris
     * @return complete string
     */
    private String addValuesUriToQuery( String keyWord, @Nullable Set<String> valuesUris ) {

        String query = "";
        if ( valuesUris != null && !valuesUris.isEmpty() ) {
            query = keyWord + " charac.VALUE_URI in (:valueUris) ";
        }
        return query;
    }

    /**
     * @param force force
     * @return basic sql command to deal with security; adds the where clause; delcare aliases charac, phen and gene; ace, aoi,
     * sid
     */
    private String getPhenotypesGenesAssociationsBeginQuery( boolean force ) {
        String queryString = "";

        queryString += "from CHARACTERISTIC as charac ";
        queryString += "join PHENOTYPE_ASSOCIATION as phen on charac.PHENOTYPE_ASSOCIATION_FK = phen.ID ";
        queryString += "join CHROMOSOME_FEATURE as gene on gene.id = phen.GENE_FK ";
        queryString += "join TAXON tax on tax.ID = gene.TAXON_FK ";

        if ( SecurityUtil.isUserAdmin() && !force ) {
            // no constraint needed. A 'where' will be added later if we need it.
            queryString += " ";
        } else {
            // See entityutils for a generalization of this.
            // non-admin user, need to add constraint on permissions. Adds the beginning of the WHERE clause.
            queryString += "join ACLOBJECTIDENTITY aoi on phen.ID = aoi.OBJECT_ID ";
            queryString += "join ACLENTRY ace on ace.OBJECTIDENTITY_FK = aoi.ID ";
            queryString += "join ACLSID sid on sid.ID = aoi.OWNER_SID_FK ";
            queryString += "where aoi.OBJECT_CLASS IN " + PhenotypeAssociationDaoImpl.DISCRIMINATOR_CLAUSE;
        }

        return queryString;
    }

    /**
     * @param queryObject execute sqlQuery and populate phenotypesGenesAssociations is : phenotype --&gt; genes
     * @return map
     */
    private Map<String, Set<Integer>> populateGenesAssociations( SQLQuery queryObject ) {
        Map<String, Set<Integer>> phenotypesGenesAssociations = new HashMap<>();
        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        try {
            while ( results.next() ) {
                Integer geneNcbiId = ( Integer ) results.get( 0 );
                String valueUri = ( String ) results.get( 1 );
                EntityUtils.populateMapSet( phenotypesGenesAssociations, valueUri, geneNcbiId );
            }
        } finally {
            results.close();
        }
        return phenotypesGenesAssociations;
    }

    /**
     * @param queryObject execute sqlQuery and populate phenotypesGenesAssociations is : phenotype --&gt; genes
     * @return collection
     */
    private Collection<GeneEvidenceValueObject> populateGenesWithPhenotypes( SQLQuery queryObject ) {
        StopWatch sw = new StopWatch();
        sw.start();

        // we accumulate the phenotypes for a gene in one VO
        Map<Long, GeneEvidenceValueObject> genesWithPhenotypes = new HashMap<>();

        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        try {
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
                    GeneEvidenceValueObject g = new GeneEvidenceValueObject( geneId );
                    g.setNcbiId( nbciGeneId );
                    g.setOfficialName( officialName );
                    g.setOfficialSymbol( officialSymbol );
                    g.setTaxon( new TaxonValueObject( taxonId, taxonCommonName ) );
                    g.getPhenotypesValueUri().add( valueUri );
                    genesWithPhenotypes.put( geneId, g );
                }
            }
        } finally {
            results.close();
        }

        if ( sw.getTime() > 500 ) {
            PhenotypeAssociationDaoImpl.log
                    .info( "Get " + genesWithPhenotypes.size() + " genes with phenotypes: " + sw.getTime() + "ms" );
        }

        return genesWithPhenotypes.values();
    }

    @Override
    public void remove( PhenotypeAssociation entity ) {
        // detach the PA from the gene to prevent re-save by cascade
        entity.getGene().getPhenotypeAssociations().remove( entity );
        super.remove( entity );
    }
}
