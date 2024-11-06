/*
 * The Gemma project.
 *
 * Copyright (c) 2006 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.persistence.service.expression.experiment;

import gemma.gsec.acl.domain.AclObjectIdentity;
import gemma.gsec.acl.domain.AclSid;
import lombok.Value;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.*;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;
import ubic.gemma.core.profiling.StopWatchUtils;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.SampleRemovalEvent;
import ubic.gemma.model.common.description.*;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.*;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.hibernate.TypedResultTransformer;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.AbstractCuratableDao;
import ubic.gemma.persistence.service.common.description.CharacteristicDao;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignDao;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayDao;
import ubic.gemma.persistence.service.genome.taxon.TaxonDao;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.*;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingLong;
import static ubic.gemma.core.util.ListUtils.validateSparseRangeArray;
import static ubic.gemma.persistence.service.expression.biomaterial.BioMaterialUtils.visitBioMaterials;
import static ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil.*;
import static ubic.gemma.persistence.util.QueryUtils.*;

/**
 * @author pavlidis
 * @see ubic.gemma.model.expression.experiment.ExpressionExperiment
 */
@Repository
public class ExpressionExperimentDaoImpl
        extends AbstractCuratableDao<ExpressionExperiment, ExpressionExperimentValueObject>
        implements ExpressionExperimentDao {

    private static final String
            CHARACTERISTIC_ALIAS = CharacteristicDao.OBJECT_ALIAS,
            BIO_MATERIAL_CHARACTERISTIC_ALIAS = "bmc",
            FACTOR_VALUE_CHARACTERISTIC_ALIAS = "fvc",
            ALL_CHARACTERISTIC_ALIAS = "ac",
            BIO_ASSAY_ALIAS = BioAssayDao.OBJECT_ALIAS,
            TAXON_ALIAS = TaxonDao.OBJECT_ALIAS,
            ARRAY_DESIGN_ALIAS = ArrayDesignDao.OBJECT_ALIAS,
            EXTERNAL_DATABASE_ALIAS = "ED";

    /**
     * Aliases applicable for one-to-many relations.
     */
    private static final String[] ONE_TO_MANY_ALIASES = { CHARACTERISTIC_ALIAS, BIO_MATERIAL_CHARACTERISTIC_ALIAS,
            FACTOR_VALUE_CHARACTERISTIC_ALIAS, ALL_CHARACTERISTIC_ALIAS, BIO_ASSAY_ALIAS, ARRAY_DESIGN_ALIAS };

    @Autowired
    public ExpressionExperimentDaoImpl( SessionFactory sessionFactory ) {
        super( ExpressionExperimentDao.OBJECT_ALIAS, ExpressionExperiment.class, sessionFactory );
    }

    @Override
    public ExpressionExperiment load( Long id, CacheMode cacheMode ) {
        Session session = getSessionFactory().getCurrentSession();
        session.setCacheMode( cacheMode );
        return super.load( id );
    }

    @Override
    public List<ExpressionExperiment> browse( int start, int limit ) {
        Query query = this.getSessionFactory().getCurrentSession().createQuery( "from ExpressionExperiment" );
        query.setMaxResults( limit );
        query.setFirstResult( start );

        //noinspection unchecked
        return query.list();
    }

    @Override
    public List<ExpressionExperiment> browse( int start, int limit, String orderField, boolean descending ) {
        throw new NotImplementedException( "Browsing ExpressionExperiment in a specific order is not supported." );
    }

    @Override
    public BioAssaySet loadBioAssaySet( Long id ) {
        return ( BioAssaySet ) getSessionFactory().getCurrentSession().get( BioAssaySet.class, id );
    }

    @Override
    public Collection<Long> filterByTaxon( @Nullable Collection<Long> ids, Taxon taxon ) {
        if ( ids == null || ids.isEmpty() )
            return Collections.emptySet();
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct ee.id from ExpressionExperiment as ee "
                        + "join ee.bioAssays as ba "
                        + "join ba.sampleUsed as sample "
                        + "where sample.sourceTaxon = :taxon and ee.id in (:ids)" )
                .setParameter( "taxon", taxon )
                .setParameterList( "ids", optimizeParameterList( ids ) )
                .list();
    }

    @Override
    public ExpressionExperiment findByShortName( String shortName ) {
        return findOneByProperty( "shortName", shortName );
    }

    @Override
    public Collection<ExpressionExperiment> findByName( String name ) {
        return findByProperty( "name", name );
    }

    @Override
    public ExpressionExperiment findOneByName( String name ) {
        return findOneByProperty( "name", name );
    }

    @Override
    public ExpressionExperiment find( ExpressionExperiment entity ) {

        Criteria criteria = this.getSessionFactory().getCurrentSession().createCriteria( ExpressionExperiment.class );

        if ( entity.getAccession() != null ) {
            criteria.add( Restrictions.eq( "accession", entity.getAccession() ) );
        } else if ( entity.getShortName() != null ) {
            criteria.add( Restrictions.eq( "shortName", entity.getShortName() ) );
        } else if ( entity.getName() != null ) {
            criteria.add( Restrictions.eq( "name", entity.getName() ) );
        } else {
            throw new IllegalArgumentException( "At least one of accession, shortName or name must be non-null to find an ExpressionExperiment." );
        }

        return ( ExpressionExperiment ) criteria.uniqueResult();
    }

    @Override
    public Collection<ExpressionExperiment> findByAccession( DatabaseEntry accession ) {
        Criteria criteria = this.getSessionFactory().getCurrentSession().createCriteria( ExpressionExperiment.class );

        BusinessKey.checkKey( accession );
        BusinessKey.attachCriteria( criteria, accession, "accession" );

        //noinspection unchecked
        return criteria.list();
    }

    @Override
    public Collection<ExpressionExperiment> findByAccession( String accession ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select e from ExpressionExperiment e join e.accession a where a.accession = :accession" )
                .setParameter( "accession", accession )
                .list();
    }

    @Nullable
    @Override
    public ExpressionExperiment findOneByAccession( String accession ) {
        return ( ExpressionExperiment ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select e from ExpressionExperiment e join e.accession a where a.accession = :accession" )
                .setParameter( "accession", accession )
                .uniqueResult();
    }

    @Override
    public Collection<ExpressionExperiment> findByBibliographicReference( BibliographicReference bibRef ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct ee from ExpressionExperiment as ee "
                        + "left join ee.otherRelevantPublications as orp "
                        + "where ee.primaryPublication = :bibRef or orp = :bibRef" )
                .setParameter( "bibRef", bibRef )
                .list();
    }

    @Override
    public ExpressionExperiment findByBioAssay( BioAssay ba ) {
        return ( ExpressionExperiment ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct ee from ExpressionExperiment as ee "
                        + "join ee.bioAssays as ba "
                        + "where ba = :ba" )
                .setParameter( "ba", ba )
                .uniqueResult();
    }

    @Override
    public Collection<ExpressionExperiment> findByBioMaterial( BioMaterial bm ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct ee from ExpressionExperiment as ee "
                        + "join ee.bioAssays as ba join ba.sampleUsed as sample where sample = :bm" )
                .setParameter( "bm", bm )
                .list();
    }

    @Override
    public Map<ExpressionExperiment, Collection<BioMaterial>> findByBioMaterials( Collection<BioMaterial> bms ) {
        if ( bms.isEmpty() ) {
            return new HashMap<>();
        }
        //noinspection unchecked
        List<Object[]> r = this.getSessionFactory().getCurrentSession()
                .createQuery( "select ee, sample from ExpressionExperiment as ee "
                        + "join ee.bioAssays as ba join ba.sampleUsed as sample where sample in (:bms) "
                        + "group by ee, sample" )
                .setParameterList( "bms", optimizeIdentifiableParameterList( bms ) )
                .list();
        Map<ExpressionExperiment, Collection<BioMaterial>> results = new HashMap<>();
        for ( Object[] a : r ) {
            ExpressionExperiment e = ( ExpressionExperiment ) a[0];
            BioMaterial b = ( BioMaterial ) a[1];
            results.computeIfAbsent( e, k -> new HashSet<>() ).add( b );
        }
        return results;
    }

    @Override
    public Collection<ExpressionExperiment> findByExpressedGene( Gene gene, Double rank ) {
        //language=MySQL
        final String queryString = "SELECT DISTINCT ee.ID AS eeID FROM "
                + "GENE2CS g2s, COMPOSITE_SEQUENCE cs, PROCESSED_EXPRESSION_DATA_VECTOR dedv, INVESTIGATION ee "
                + "WHERE g2s.CS = cs.ID AND cs.ID = dedv.DESIGN_ELEMENT_FK AND dedv.EXPRESSION_EXPERIMENT_FK = ee.ID"
                + " AND g2s.gene = :geneID AND dedv.RANK_BY_MEAN >= :rank";

        Collection<Long> eeIds;

        Session session = this.getSessionFactory().getCurrentSession();
        org.hibernate.SQLQuery queryObject = session.createSQLQuery( queryString );
        queryObject.setLong( "geneID", gene.getId() );
        queryObject.setDouble( "rank", rank );
        queryObject.addScalar( "eeID", StandardBasicTypes.LONG );
        //noinspection unchecked
        List<Long> results = queryObject.list();

        eeIds = new HashSet<>( results );

        return this.load( eeIds );
    }

    @Override
    public ExpressionExperiment findByDesign( ExperimentalDesign ed ) {
        return findOneByProperty( "experimentalDesign", ed );
    }

    @Override
    public ExpressionExperiment findByFactor( ExperimentalFactor ef ) {
        return ( ExpressionExperiment ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct ee from ExpressionExperiment as ee "
                        + "join ee.experimentalDesign ed "
                        + "join ed.experimentalFactors ef "
                        + "where ef = :ef" )
                .setParameter( "ef", ef )
                .uniqueResult();
    }

    @Override
    public ExpressionExperiment findByFactorValue( FactorValue fv ) {
        return this.findByFactorValue( fv.getId() );
    }

    @Override
    public ExpressionExperiment findByFactorValue( Long factorValueId ) {
        return ( ExpressionExperiment ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct ee from ExpressionExperiment as ee "
                        + "join ee.experimentalDesign ed "
                        + "join ed.experimentalFactors ef "
                        + "join ef.factorValues fv "
                        + "where fv.id = :fvId" )
                .setParameter( "fvId", factorValueId )
                .uniqueResult();
    }

    @Override
    public Map<ExpressionExperiment, FactorValue> findByFactorValues( Collection<FactorValue> fvs ) {
        if ( fvs.isEmpty() )
            return new HashMap<>();
        Map<ExpressionExperiment, FactorValue> results = new HashMap<>();
        //noinspection unchecked
        List<Object[]> r2 = this.getSessionFactory().getCurrentSession()
                .createQuery( "select ee, f from ExpressionExperiment ee "
                        + "join ee.experimentalDesign ed join ed.experimentalFactors ef join ef.factorValues f "
                        + "where f in (:fvs) group by ee, f" )
                .setParameterList( "fvs", optimizeIdentifiableParameterList( fvs ) )
                .list();
        for ( Object[] row : r2 ) {
            results.put( ( ExpressionExperiment ) row[0], ( FactorValue ) row[1] );
        }
        return results;
    }

    @Override
    public Collection<ExpressionExperiment> findByGene( Gene gene ) {

        /*
         * uses GENE2CS table.
         */
        //language=MySQL
        final String queryString = "SELECT DISTINCT ee.ID AS eeID FROM "
                + "GENE2CS g2s, COMPOSITE_SEQUENCE cs, ARRAY_DESIGN ad, BIO_ASSAY ba, INVESTIGATION ee "
                + "WHERE g2s.CS = cs.ID AND ad.ID = cs.ARRAY_DESIGN_FK AND ba.ARRAY_DESIGN_USED_FK = ad.ID AND"
                + " ba.EXPRESSION_EXPERIMENT_FK = ee.ID AND g2s.GENE = :geneID";

        Collection<Long> eeIds;

        Session session = this.getSessionFactory().getCurrentSession();
        org.hibernate.SQLQuery queryObject = session.createSQLQuery( queryString );
        queryObject.setLong( "geneID", gene.getId() );
        queryObject.addScalar( "eeID", StandardBasicTypes.LONG );
        //noinspection unchecked
        List<Long> results = queryObject.list();

        eeIds = new HashSet<>( results );

        return this.load( eeIds );
    }

    @Override
    public ExpressionExperiment findByQuantitationType( QuantitationType quantitationType ) {
        return ( ExpressionExperiment ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select ee from ExpressionExperiment as ee join ee.quantitationTypes qt where qt = :qt" )
                .setParameter( "qt", quantitationType )
                .uniqueResult();
    }

    @Override
    public Collection<ExpressionExperiment> findByTaxon( Taxon taxon ) {
        return findByProperty( "taxon", taxon );
    }

    @Override
    public List<ExpressionExperiment> findByUpdatedLimit( Collection<Long> ids, int limit ) {
        if ( ids.isEmpty() || limit <= 0 )
            return Collections.emptyList();
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( "select e from ExpressionExperiment e "
                        + "join e.curationDetails s "
                        + "where e.id in (:ids) "
                        + "order by s.lastUpdated desc" )
                .setParameterList( "ids", optimizeParameterList( ids ) )
                .setMaxResults( limit )
                .list();
    }

    @Override
    public List<ExpressionExperiment> findByUpdatedLimit( int limit ) {
        if ( limit == 0 )
            return new ArrayList<>();
        Session s = this.getSessionFactory().getCurrentSession();
        String queryString = "select e from ExpressionExperiment e join e.curationDetails s order by s.lastUpdated " + (
                limit < 0 ?
                        "asc" :
                        "desc" );
        Query q = s.createQuery( queryString );
        q.setMaxResults( Math.abs( limit ) );

        //noinspection unchecked
        return q.list();
    }

    @Override
    public Collection<ExpressionExperiment> findUpdatedAfter( @Nullable Date date ) {
        if ( date == null )
            return Collections.emptyList();
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select e from ExpressionExperiment e join e.curationDetails cd where cd.lastUpdated >= :date" )
                .setParameter( "date", date )
                .list();
    }

    @Override
    public Map<Long, Long> getAnnotationCounts( Collection<Long> ids ) {
        if ( ids.isEmpty() ) {
            return Collections.emptyMap();
        }
        Map<Long, Long> results = new HashMap<>();
        for ( Long id : ids ) {
            results.put( id, 0L );
        }
        //noinspection unchecked
        List<Object[]> res = this.getSessionFactory().getCurrentSession()
                .createQuery( "select e.id, count(c.id) from ExpressionExperiment e "
                        + "join e.characteristics c "
                        + "where e.id in (:ids) "
                        + "group by e" )
                .setParameterList( "ids", optimizeParameterList( ids ) )
                .list();

        for ( Object[] ro : res ) {
            Long id = ( Long ) ro[0];
            Long count = ( Long ) ro[1];
            results.put( id, count );
        }

        return results;
    }

    @Override
    public Collection<Characteristic> getAnnotationsByBioMaterials( ExpressionExperiment ee ) {
        /*
         * Note we're not using 'distinct' here but the 'equals' for AnnotationValueObject should aggregate these. More
         * work to do.
         */
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select c from ExpressionExperiment e "
                        + "join e.bioAssays ba join ba.sampleUsed bm "
                        + "join bm.characteristics c where e = :ee" )
                .setParameter( "ee", ee )
                .list();
    }

    @Override
    public Collection<Statement> getAnnotationsByFactorValues( ExpressionExperiment ee ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select c from ExpressionExperiment e "
                        + "join e.experimentalDesign ed join ed.experimentalFactors ef join ef.factorValues fv "
                        + "join fv.characteristics c where e = :ee" )
                .setParameter( "ee", ee )
                .list();
    }

    @Override
    public Map<Class<? extends Identifiable>, List<Characteristic>> getAllAnnotations( ExpressionExperiment expressionExperiment ) {
        //noinspection unchecked
        List<Object[]> result = getSessionFactory().getCurrentSession()
                .createSQLQuery( "select T.`VALUE` as `VALUE`, T.VALUE_URI as VALUE_URI, T.CATEGORY as CATEGORY, T.CATEGORY_URI as CATEGORY_URI, T.EVIDENCE_CODE as EVIDENCE_CODE, T.LEVEL as LEVEL from EXPRESSION_EXPERIMENT2CHARACTERISTIC T "
                        + "where T.EXPRESSION_EXPERIMENT_FK = :eeId" )
                .addScalar( "VALUE", StandardBasicTypes.STRING )
                .addScalar( "VALUE_URI", StandardBasicTypes.STRING )
                .addScalar( "CATEGORY", StandardBasicTypes.STRING )
                .addScalar( "CATEGORY_URI", StandardBasicTypes.STRING )
                // FIXME: use an EnumType for converting
                .addScalar( "EVIDENCE_CODE", StandardBasicTypes.STRING )
                .addScalar( "LEVEL", StandardBasicTypes.CLASS )
                .setParameter( "eeId", expressionExperiment.getId() )
                .list();
        //noinspection unchecked
        return result.stream()
                .collect( Collectors.groupingBy( row -> ( Class<? extends Identifiable> ) row[5],
                        Collectors.mapping( this::convertRowToCharacteristic, Collectors.toList() ) ) );
    }

    @Override
    public List<Characteristic> getExperimentAnnotations( ExpressionExperiment expressionExperiment ) {
        return getAnnotationsByLevel( expressionExperiment, BioMaterial.class );
    }

    @Override
    public List<Characteristic> getBioMaterialAnnotations( ExpressionExperiment expressionExperiment ) {
        return getAnnotationsByLevel( expressionExperiment, BioMaterial.class );
    }

    @Override
    public List<Characteristic> getExperimentalDesignAnnotations( ExpressionExperiment expressionExperiment ) {
        return getAnnotationsByLevel( expressionExperiment, ExperimentalDesign.class );
    }

    private List<Characteristic> getAnnotationsByLevel( ExpressionExperiment expressionExperiment, Class<? extends Identifiable> level ) {
        //noinspection unchecked
        List<Object[]> result = getSessionFactory().getCurrentSession()
                .createSQLQuery( "select T.`VALUE` as `VALUE`, T.VALUE_URI as VALUE_URI, T.CATEGORY as CATEGORY, T.CATEGORY_URI as CATEGORY_URI, T.EVIDENCE_CODE as EVIDENCE_CODE from EXPRESSION_EXPERIMENT2CHARACTERISTIC T "
                        + "where T.LEVEL = :level and T.EXPRESSION_EXPERIMENT_FK = :eeId" )
                .addScalar( "VALUE", StandardBasicTypes.STRING )
                .addScalar( "VALUE_URI", StandardBasicTypes.STRING )
                .addScalar( "CATEGORY", StandardBasicTypes.STRING )
                .addScalar( "CATEGORY_URI", StandardBasicTypes.STRING )
                // FIXME: use an EnumType for converting
                .addScalar( "EVIDENCE_CODE", StandardBasicTypes.STRING )
                .setParameter( "level", level )
                .setParameter( "eeId", expressionExperiment.getId() )
                .list();
        return result.stream().map( this::convertRowToCharacteristic ).collect( Collectors.toList() );
    }

    @Override
    public Map<Characteristic, Long> getCategoriesUsageFrequency( @Nullable Collection<Long> eeIds, @Nullable Collection<String> excludedCategoryUris, @Nullable Collection<String> excludedTermUris, @Nullable Collection<String> retainedTermUris, int maxResults ) {
        boolean doAclFiltering = eeIds == null;
        boolean useRetainedTermUris = false;
        if ( eeIds != null && eeIds.isEmpty() ) {
            return Collections.emptyMap();
        }
        // never exclude terms that are explicitly retained
        if ( excludedTermUris != null && retainedTermUris != null ) {
            excludedTermUris = new HashSet<>( excludedTermUris );
            excludedTermUris.removeAll( retainedTermUris );
        }
        boolean excludeFreeTextCategories = false;
        boolean excludeUncategorized = false;
        if ( excludedCategoryUris != null ) {
            if ( excludedCategoryUris.contains( FREE_TEXT ) ) {
                excludeFreeTextCategories = true;
                excludedCategoryUris = excludedCategoryUris.stream().filter( Objects::nonNull ).collect( Collectors.toList() );
            }
            if ( excludedCategoryUris.contains( UNCATEGORIZED ) ) {
                excludeUncategorized = true;
                excludedCategoryUris = excludedCategoryUris.stream().filter( c -> !c.equals( UNCATEGORIZED ) ).collect( Collectors.toList() );
            }
        }
        boolean excludeFreeTextTerms = false;
        if ( excludedTermUris != null ) {
            if ( excludedTermUris.contains( null ) ) {
                excludeFreeTextTerms = true;
                excludedTermUris = excludedTermUris.stream().filter( Objects::nonNull ).collect( Collectors.toList() );
            }
        }
        String query = "select T.CATEGORY as CATEGORY, T.CATEGORY_URI as CATEGORY_URI, count(distinct T.EXPRESSION_EXPERIMENT_FK) as EE_COUNT from EXPRESSION_EXPERIMENT2CHARACTERISTIC T ";
        if ( doAclFiltering ) {
            query += EE2CAclQueryUtils.formNativeAclJoinClause( "T.EXPRESSION_EXPERIMENT_FK" ) + " ";
        }
        if ( eeIds != null ) {
            query += "where T.EXPRESSION_EXPERIMENT_FK in :eeIds";
        } else {
            query += "where T.EXPRESSION_EXPERIMENT_FK is not null";
        }
        String excludeUrisClause = getExcludeUrisClause( excludedCategoryUris, excludedTermUris, excludeFreeTextCategories, excludeFreeTextTerms, excludeUncategorized );
        if ( excludeUrisClause != null ) {
            query += " and (";
            query += "(" + excludeUrisClause + ")";
            if ( retainedTermUris != null && !retainedTermUris.isEmpty() ) {
                query += " or T.VALUE_URI in (:retainedTermUris)";
                useRetainedTermUris = true;
            }
            query += ")";
        }
        if ( doAclFiltering ) {
            query += EE2CAclQueryUtils.formNativeAclRestrictionClause( ( SessionFactoryImplementor ) getSessionFactory(), "T.ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK" );
            // troubled filtering
            query += formNativeNonTroubledClause( "T.EXPRESSION_EXPERIMENT_FK", ExpressionExperiment.class );
        }
        query += " group by COALESCE(T.CATEGORY_URI, T.CATEGORY)";
        if ( maxResults > 0 ) {
            query += " order by EE_COUNT desc";
        }
        Query q = getSessionFactory().getCurrentSession().createSQLQuery( query )
                .addScalar( "CATEGORY", StandardBasicTypes.STRING )
                .addScalar( "CATEGORY_URI", StandardBasicTypes.STRING )
                .addScalar( "EE_COUNT", StandardBasicTypes.LONG )
                .addSynchronizedQuerySpace( EE2C_QUERY_SPACE )
                .addSynchronizedEntityClass( ExpressionExperiment.class )
                .addSynchronizedEntityClass( Characteristic.class );
        if ( excludedCategoryUris != null && !excludedCategoryUris.isEmpty() ) {
            q.setParameterList( "excludedCategoryUris", optimizeParameterList( excludedCategoryUris ) );
        }
        if ( excludedTermUris != null && !excludedTermUris.isEmpty() ) {
            q.setParameterList( "excludedTermUris", optimizeParameterList( excludedTermUris ) );
        }
        if ( useRetainedTermUris ) {
            q.setParameterList( "retainedTermUris", optimizeParameterList( retainedTermUris ) );
        }
        if ( doAclFiltering ) {
            EE2CAclQueryUtils.addAclParameters( q, ExpressionExperiment.class );
        }
        q.setCacheable( true );
        List<Object[]> result;
        if ( eeIds != null ) {
            if ( eeIds.size() > MAX_PARAMETER_LIST_SIZE ) {
                result = listByBatch( q, "eeIds", eeIds, 2048 );
                if ( maxResults > 0 ) {
                    return aggregateByCategory( result ).entrySet().stream()
                            .sorted( Map.Entry.comparingByValue( Comparator.reverseOrder() ) )
                            .limit( maxResults )
                            .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) );
                }
            } else {
                //noinspection unchecked
                result = q
                        .setParameterList( "eeIds", optimizeParameterList( eeIds ) )
                        .setMaxResults( maxResults )
                        .list();
            }
        } else {
            //noinspection unchecked
            result = q.setMaxResults( maxResults ).list();
        }
        return aggregateByCategory( result );
    }

    private Map<Characteristic, Long> aggregateByCategory( List<Object[]> result ) {
        return result.stream().collect( Collectors.groupingBy( row -> Characteristic.Factory.newInstance( null, null, null, null, ( String ) row[0], ( String ) row[1], null ), Collectors.summingLong( row -> ( Long ) row[2] ) ) );
    }

    /**
     * We're making two assumptions: a dataset cannot have a characteristic more than once and a dataset cannot have
     * the same characteristic at multiple levels to make counting more efficient.
     */
    @Override
    public Map<Characteristic, Long> getAnnotationsUsageFrequency( @Nullable Collection<Long> eeIds, @Nullable Class<? extends Identifiable> level, int maxResults, int minFrequency, @Nullable String category, @Nullable Collection<String> excludedCategoryUris, @Nullable Collection<String> excludedTermUris, @Nullable Collection<String> retainedTermUris ) {
        boolean doAclFiltering = eeIds == null;
        boolean useRetainedTermUris = false;
        if ( eeIds != null && eeIds.isEmpty() ) {
            return Collections.emptyMap();
        }
        // never exclude terms that are explicitly retained
        if ( excludedTermUris != null && retainedTermUris != null ) {
            excludedTermUris = new HashSet<>( excludedTermUris );
            excludedTermUris.removeAll( retainedTermUris );
        }
        boolean excludeFreeTextCategories = false;
        boolean excludeUncategorized = false;
        if ( excludedCategoryUris != null ) {
            if ( excludedCategoryUris.contains( FREE_TEXT ) ) {
                excludeFreeTextCategories = true;
                excludedCategoryUris = excludedCategoryUris.stream().filter( Objects::nonNull ).collect( Collectors.toList() );
            }
            if ( excludedCategoryUris.contains( UNCATEGORIZED ) ) {
                excludeUncategorized = true;
                excludedCategoryUris = excludedCategoryUris.stream().filter( c -> !c.equals( UNCATEGORIZED ) ).collect( Collectors.toList() );
            }
        }
        boolean excludeFreeTextTerms = false;
        if ( excludedTermUris != null ) {
            if ( excludedTermUris.contains( null ) ) {
                excludeFreeTextTerms = true;
                excludedTermUris = excludedTermUris.stream().filter( Objects::nonNull ).collect( Collectors.toList() );
            }
        }
        String query = "select T.`VALUE` as `VALUE`, T.VALUE_URI as VALUE_URI, T.CATEGORY as CATEGORY, T.CATEGORY_URI as CATEGORY_URI, T.EVIDENCE_CODE as EVIDENCE_CODE, count(distinct T.EXPRESSION_EXPERIMENT_FK) as EE_COUNT from EXPRESSION_EXPERIMENT2CHARACTERISTIC T ";
        if ( doAclFiltering ) {
            query += EE2CAclQueryUtils.formNativeAclJoinClause( "T.EXPRESSION_EXPERIMENT_FK" ) + " ";
        }
        if ( eeIds != null ) {
            query += "where T.EXPRESSION_EXPERIMENT_FK in :eeIds";
        } else {
            query += "where T.EXPRESSION_EXPERIMENT_FK is not null"; // this is necessary for the clause building since there might be no clause
        }
        if ( level != null ) {
            query += " and T.LEVEL = :level";
        }
        String excludeUrisClause;
        if ( category != null ) {
            // a specific category is requested
            if ( category.equals( UNCATEGORIZED ) ) {
                query += " and COALESCE(T.CATEGORY_URI, T.CATEGORY) is NULL";
            }
            // using COALESCE(T.CATEGORY_URI, T.CATEGORY) = :category is very inefficient and we never use http:// in category labels
            else if ( category.startsWith( "http://" ) ) {
                query += " and T.CATEGORY_URI = :category";
            } else {
                query += " and T.CATEGORY = :category";
            }
            // no need to filter out excluded categories if a specific one is requested
            excludeUrisClause = getExcludeUrisClause( null, excludedTermUris, excludeFreeTextCategories, excludeFreeTextTerms, excludeUncategorized );
        } else {
            // all categories are requested, we may filter out excluded ones
            excludeUrisClause = getExcludeUrisClause( excludedCategoryUris, excludedTermUris, excludeFreeTextCategories, excludeFreeTextTerms, excludeUncategorized );
        }
        if ( excludeUrisClause != null ) {
            query += " and (";
            query += "(" + excludeUrisClause + ")";
            if ( retainedTermUris != null && !retainedTermUris.isEmpty() ) {
                query += " or T.VALUE_URI in (:retainedTermUris)";
                useRetainedTermUris = true;
            }
            query += ")";
        }
        if ( doAclFiltering ) {
            query += EE2CAclQueryUtils.formNativeAclRestrictionClause( ( SessionFactoryImplementor ) getSessionFactory(), "T.ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK" );
            query += formNativeNonTroubledClause( "T.EXPRESSION_EXPERIMENT_FK", ExpressionExperiment.class );
        }
        //language=HQL
        query += " group by "
                // no need to group by category if a specific one is requested
                + ( category == null ? "COALESCE(T.CATEGORY_URI, T.CATEGORY), " : "" )
                + "COALESCE(T.VALUE_URI, T.`VALUE`)";
        // if there are too many EE IDs, they will be retrieved by batch and filtered in-memory
        if ( minFrequency > 1 && ( eeIds == null || eeIds.size() <= MAX_PARAMETER_LIST_SIZE ) ) {
            query += " having EE_COUNT >= :minFrequency";
            if ( retainedTermUris != null && !retainedTermUris.isEmpty() ) {
                query += " or VALUE_URI in (:retainedTermUris)";
                useRetainedTermUris = true;
            }
        }
        if ( maxResults > 0 ) {
            query += " order by EE_COUNT desc";
        }
        Query q = getSessionFactory().getCurrentSession().createSQLQuery( query )
                .addScalar( "VALUE", StandardBasicTypes.STRING )
                .addScalar( "VALUE_URI", StandardBasicTypes.STRING )
                .addScalar( "CATEGORY", StandardBasicTypes.STRING )
                .addScalar( "CATEGORY_URI", StandardBasicTypes.STRING )
                // FIXME: use an EnumType for converting
                .addScalar( "EVIDENCE_CODE", StandardBasicTypes.STRING )
                .addScalar( "EE_COUNT", StandardBasicTypes.LONG )
                .addSynchronizedQuerySpace( EE2C_QUERY_SPACE )
                .addSynchronizedEntityClass( ExpressionExperiment.class )
                .addSynchronizedEntityClass( Characteristic.class ); // ensures that the cache is invalidated if characteristics are added or removed
        if ( category != null && !category.equals( UNCATEGORIZED ) ) {
            q.setParameter( "category", category );
        }
        if ( excludedCategoryUris != null && !excludedCategoryUris.isEmpty() ) {
            q.setParameterList( "excludedCategoryUris", optimizeParameterList( excludedCategoryUris ) );
        }
        if ( excludedTermUris != null && !excludedTermUris.isEmpty() ) {
            q.setParameterList( "excludedTermUris", optimizeParameterList( excludedTermUris ) );
        }
        if ( useRetainedTermUris ) {
            assert retainedTermUris != null;
            q.setParameterList( "retainedTermUris", optimizeParameterList( retainedTermUris ) );
        }
        if ( level != null ) {
            q.setParameter( "level", level );
        }
        if ( minFrequency > 1 && ( eeIds == null || eeIds.size() <= MAX_PARAMETER_LIST_SIZE ) ) {
            q.setParameter( "minFrequency", minFrequency );
        }
        if ( doAclFiltering ) {
            EE2CAclQueryUtils.addAclParameters( q, ExpressionExperiment.class );
        }
        q.setCacheable( true );
        List<Object[]> result;
        if ( eeIds != null ) {
            if ( eeIds.size() > MAX_PARAMETER_LIST_SIZE ) {
                result = listByBatch( q, "eeIds", eeIds, 2048 );
                if ( minFrequency > 1 || maxResults > 0 ) {
                    return aggregateByCategoryAndValue( result ).entrySet().stream()
                            .filter( e -> e.getValue() >= minFrequency || ( retainedTermUris != null && retainedTermUris.contains( e.getKey().getValueUri() ) ) )
                            .sorted( Map.Entry.comparingByValue( Comparator.reverseOrder() ) )
                            .limit( maxResults > 0 ? maxResults : Long.MAX_VALUE )
                            .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) );
                }
            } else {
                //noinspection unchecked
                result = q.setParameterList( "eeIds", optimizeParameterList( eeIds ) )
                        .setMaxResults( maxResults )
                        .list();
            }
        } else {
            //noinspection unchecked
            result = q.setMaxResults( maxResults ).list();
        }
        return aggregateByCategoryAndValue( result );
    }

    private Map<Characteristic, Long> aggregateByCategoryAndValue( List<Object[]> result ) {
        return result.stream().collect( Collectors.groupingBy( this::convertRowToCharacteristic, Collectors.summingLong( row -> ( Long ) row[5] ) ) );
    }

    private Characteristic convertRowToCharacteristic( Object[] row ) {
        GOEvidenceCode evidenceCode;
        try {
            evidenceCode = row[4] != null ? GOEvidenceCode.valueOf( ( String ) row[4] ) : null;
        } catch ( IllegalArgumentException e ) {
            evidenceCode = null;
        }
        return Characteristic.Factory.newInstance( null, null, ( String ) row[0], ( String ) row[1], ( String ) row[2], ( String ) row[3], evidenceCode );
    }

    /**
     * Produce a SQL clause for excluding various terms and categories.
     * <p>
     * FIXME: There's a bug in Hibernate that that prevents it from producing proper tuples the excluded URIs and
     *        retained term URIs
     * @param excludedCategoryUris      list of category URIs to exclude
     * @param excludedTermUris          list of URIs to exclude
     * @param excludeFreeTextCategories whether to exclude free-text categories
     * @param excludeFreeTextTerms      whether to exclude free-text terms
     * @param excludeUncategorized      whether to exclude uncategorized terms
     * @return a SQL clause for excluding terms and categories or null if no clause is necessary
     */
    @Nullable
    private String getExcludeUrisClause( @Nullable Collection<String> excludedCategoryUris, @Nullable Collection<String> excludedTermUris, boolean excludeFreeTextCategories, boolean excludeFreeTextTerms, boolean excludeUncategorized ) {
        List<String> clauses = new ArrayList<>( 5 );
        if ( excludedCategoryUris != null && !excludedCategoryUris.isEmpty() ) {
            clauses.add( "T.CATEGORY_URI is null or T.CATEGORY_URI not in (:excludedCategoryUris)" );
        }
        if ( excludedTermUris != null && !excludedTermUris.isEmpty() ) {
            clauses.add( "T.VALUE_URI is null or T.VALUE_URI not in (:excludedTermUris)" );
        }
        if ( excludeFreeTextCategories ) {
            // we don't want to exclude "uncategorized" terms when excluding free-text categories
            clauses.add( "T.CATEGORY_URI is not null or T.CATEGORY is null" );
        }
        if ( excludeFreeTextTerms ) {
            clauses.add( "T.VALUE_URI is not null" );
        }
        if ( excludeUncategorized ) {
            clauses.add( "COALESCE(T.CATEGORY_URI, T.CATEGORY) is not null" );
        }
        if ( !clauses.isEmpty() ) {
            return "(" + String.join( ") and (", clauses ) + ")";
        }
        return null;
    }

    @Override
    public Collection<ExpressionExperiment> getExperimentsLackingPublications() {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( "select e from ExpressionExperiment e where e.primaryPublication = null and e.shortName like 'GSE%'" ).list();
    }

    @Override
    public MeanVarianceRelation updateMeanVarianceRelation( ExpressionExperiment ee, MeanVarianceRelation mvr ) {
        if ( mvr.getId() == null ) {
            getSessionFactory().getCurrentSession().persist( mvr );
        }
        ee.setMeanVarianceRelation( mvr );
        update( ee );
        return mvr;
    }

    @Override
    public long countBioMaterials( @Nullable Filters filters ) {
        //language=HQL
        Query query = finishFilteringQuery( "select count(distinct bm) from ExpressionExperiment ee "
                        + "join ee.bioAssays ba "
                        + "join ba.sampleUsed bm",
                filters, null, null );
        return ( Long ) query.setCacheable( true ).uniqueResult();
    }

    @Override
    public Collection<ArrayDesign> getArrayDesignsUsed( BioAssaySet bas ) {
        return CommonQueries.getArrayDesignsUsed( bas, this.getSessionFactory().getCurrentSession() );
    }

    @Override
    public Collection<ArrayDesign> getArrayDesignsUsed( Collection<? extends BioAssaySet> ees ) {
        return CommonQueries.getArrayDesignsUsed( ees, this.getSessionFactory().getCurrentSession() );
    }

    @Override
    public Collection<ArrayDesign> getArrayDesignsUsed( ExpressionExperiment ee, QuantitationType qt, Class<? extends DataVector> dataVectorType ) {
        //noinspection unchecked
        List<Long> adIds = getSessionFactory().getCurrentSession()
                .createCriteria( dataVectorType )
                .add( Restrictions.eq( "expressionExperiment", ee ) )
                .add( Restrictions.eq( "quantitationType", qt ) )
                .createAlias( "designElement", "de" )
                .createAlias( "de.arrayDesign", "ad" )
                .setProjection( Projections.groupProperty( "ad.id" ) )
                .list();
        return adIds.stream()
                .map( id -> ( ArrayDesign ) getSessionFactory().getCurrentSession().get( ArrayDesign.class, id ) )
                .collect( Collectors.toList() );
    }

    @Override
    public Collection<Gene> getGenesUsedByPreferredVectors( ExpressionExperiment experimentConstraint ) {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                // using distinct for multi-mapping probes to prevent duplicated genes
                .createSQLQuery( "select distinct {G.*} from PROCESSED_EXPRESSION_DATA_VECTOR pedv "
                        + "join GENE2CS on pedv.DESIGN_ELEMENT_FK = GENE2CS.CS "
                        + "join CHROMOSOME_FEATURE G on GENE2CS.GENE = G.ID "
                        + "where pedv.EXPRESSION_EXPERIMENT_FK = :eeId" )
                .addEntity( "G", Gene.class )
                .addSynchronizedQuerySpace( GENE2CS_QUERY_SPACE )
                .addSynchronizedEntityClass( ArrayDesign.class )
                .addSynchronizedEntityClass( CompositeSequence.class )
                .addSynchronizedEntityClass( Gene.class )
                .setParameter( "eeId", experimentConstraint.getId() )
                .list();
    }

    @Override
    public Map<TechnologyType, Long> getTechnologyTypeUsageFrequency() {
        Query q = getSessionFactory().getCurrentSession()
                .createSQLQuery( "select AD.TECHNOLOGY_TYPE as TT, count(distinct EE2AD.EXPRESSION_EXPERIMENT_FK) as EE_COUNT from EXPRESSION_EXPERIMENT2ARRAY_DESIGN EE2AD "
                        + "join ARRAY_DESIGN AD on EE2AD.ARRAY_DESIGN_FK = AD.ID "
                        + EE2CAclQueryUtils.formNativeAclJoinClause( "EE2AD.EXPRESSION_EXPERIMENT_FK" ) + " "
                        + "where EE2AD.EXPRESSION_EXPERIMENT_FK is not NULL"
                        + EE2CAclQueryUtils.formNativeAclRestrictionClause( ( SessionFactoryImplementor ) getSessionFactory(), "EE2AD.ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK" )
                        + formNativeNonTroubledClause( "EE2AD.ARRAY_DESIGN_FK", ArrayDesign.class )
                        + formNativeNonTroubledClause( "EE2AD.EXPRESSION_EXPERIMENT_FK", ExpressionExperiment.class )
                        + " group by AD.TECHNOLOGY_TYPE" )
                .addScalar( "TT", StandardBasicTypes.STRING )
                .addScalar( "EE_COUNT", StandardBasicTypes.LONG )
                .addSynchronizedQuerySpace( EE2AD_QUERY_SPACE )
                .addSynchronizedEntityClass( ExpressionExperiment.class )
                .addSynchronizedEntityClass( ArrayDesign.class )
                .setCacheable( true );
        EE2CAclQueryUtils.addAclParameters( q, ExpressionExperiment.class );
        //noinspection unchecked
        List<Object[]> results = q.list();
        return results.stream().collect( Collectors.groupingBy( row -> TechnologyType.valueOf( ( String ) row[0] ), Collectors.summingLong( row -> ( Long ) row[1] ) ) );
    }

    @Override
    public Map<TechnologyType, Long> getTechnologyTypeUsageFrequency( Collection<Long> eeIds ) {
        if ( eeIds.isEmpty() ) {
            return Collections.emptyMap();
        }
        Query q = getSessionFactory().getCurrentSession()
                .createSQLQuery( "select AD.TECHNOLOGY_TYPE as TT, count(distinct EXPRESSION_EXPERIMENT_FK) as EE_COUNT from EXPRESSION_EXPERIMENT2ARRAY_DESIGN EE2AD "
                        + "join ARRAY_DESIGN AD on EE2AD.ARRAY_DESIGN_FK = AD.ID "
                        + "where EE2AD.EXPRESSION_EXPERIMENT_FK in (:ids) "
                        + "group by AD.TECHNOLOGY_TYPE" )
                .addScalar( "TT", StandardBasicTypes.STRING )
                .addScalar( "EE_COUNT", StandardBasicTypes.LONG )
                .addSynchronizedQuerySpace( EE2AD_QUERY_SPACE )
                .addSynchronizedEntityClass( ExpressionExperiment.class )
                .addSynchronizedEntityClass( ArrayDesign.class )
                .setCacheable( true );
        return streamByBatch( q, "ids", eeIds, getBatchSize(), Object[].class )
                .collect( Collectors.groupingBy( row -> TechnologyType.valueOf( ( String ) row[0] ), Collectors.summingLong( row -> ( Long ) row[1] ) ) );
    }

    @Override
    public Map<ArrayDesign, Long> getArrayDesignsUsageFrequency( int maxResults ) {
        return getPlatformsUsageFrequency( false, maxResults );
    }

    @Override
    public Map<ArrayDesign, Long> getArrayDesignsUsageFrequency( Collection<Long> eeIds, int maxResults ) {
        return getPlatformsUsageFrequency( eeIds, false, maxResults );
    }

    @Override
    public Map<ArrayDesign, Long> getOriginalPlatformsUsageFrequency( int maxResults ) {
        return getPlatformsUsageFrequency( true, maxResults );
    }

    @Override
    public Map<ArrayDesign, Long> getOriginalPlatformsUsageFrequency( Collection<Long> eeIds, int maxResults ) {
        return getPlatformsUsageFrequency( eeIds, true, maxResults );
    }

    private Map<ArrayDesign, Long> getPlatformsUsageFrequency( boolean original, int maxResults ) {
        Query query = getSessionFactory().getCurrentSession()
                .createSQLQuery( "select ad.*, count(distinct ee2ad.EXPRESSION_EXPERIMENT_FK) EE_COUNT from EXPRESSION_EXPERIMENT2ARRAY_DESIGN ee2ad "
                        + "join ARRAY_DESIGN ad on ee2ad.ARRAY_DESIGN_FK = ad.ID "
                        + EE2CAclQueryUtils.formNativeAclJoinClause( "ee2ad.EXPRESSION_EXPERIMENT_FK" ) + " "
                        + "where ee2ad.IS_ORIGINAL_PLATFORM = :original"
                        // exclude noop switch
                        + ( original ? " and ee2ad.ARRAY_DESIGN_FK not in (select ARRAY_DESIGN_FK from EXPRESSION_EXPERIMENT2ARRAY_DESIGN where EXPRESSION_EXPERIMENT_FK = ee2ad.EXPRESSION_EXPERIMENT_FK and ARRAY_DESIGN_FK = ee2ad.ARRAY_DESIGN_FK and not IS_ORIGINAL_PLATFORM)" : "" )
                        + EE2CAclQueryUtils.formNativeAclRestrictionClause( ( SessionFactoryImplementor ) getSessionFactory(), "ee2ad.ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK" ) + " "
                        // exclude troubled platforms or experiments for non-admins
                        + formNativeNonTroubledClause( "ee2ad.ARRAY_DESIGN_FK", ArrayDesign.class )
                        + formNativeNonTroubledClause( "ee2ad.EXPRESSION_EXPERIMENT_FK", ExpressionExperiment.class )
                        + " group by ad.ID "
                        // no need to sort results if limiting, we're collecting in a map
                        + ( maxResults > 0 ? "order by EE_COUNT desc" : "" ) )
                .addEntity( ArrayDesign.class )
                .addScalar( "EE_COUNT", StandardBasicTypes.LONG )
                // ensures that the cache is invalidated when the ee2ad table is regenerated
                .addSynchronizedQuerySpace( EE2AD_QUERY_SPACE )
                // ensures that the cache is invalidated when EEs or ADs are added/removed
                .addSynchronizedEntityClass( ExpressionExperiment.class )
                .addSynchronizedEntityClass( ArrayDesign.class );
        query.setParameter( "original", original );
        EE2CAclQueryUtils.addAclParameters( query, ExpressionExperiment.class );
        EE2CAclQueryUtils.addAclParameters( query, ExpressionExperiment.class );
        query.setCacheable( true );
        List<Object[]> result;
        //noinspection unchecked
        result = query
                .setMaxResults( maxResults )
                .list();
        return result.stream().collect( groupingBy( row -> ( ArrayDesign ) row[0], summingLong( row -> ( Long ) row[1] ) ) );
    }

    private Map<ArrayDesign, Long> getPlatformsUsageFrequency( Collection<Long> eeIds, boolean original, int maxResults ) {
        if ( eeIds.isEmpty() ) {
            return Collections.emptyMap();
        }
        // exclude noop switch
        // no need to sort results if limiting, we're collecting in a map
        Query query = getSessionFactory().getCurrentSession()
                .createSQLQuery( "select ad.*, count(distinct ee2ad.EXPRESSION_EXPERIMENT_FK) EE_COUNT from EXPRESSION_EXPERIMENT2ARRAY_DESIGN ee2ad "
                        + "join ARRAY_DESIGN ad on ee2ad.ARRAY_DESIGN_FK = ad.ID "
                        + "where ee2ad.IS_ORIGINAL_PLATFORM = :original"
                        // exclude noop switch
                        + ( original ? " and ee2ad.ARRAY_DESIGN_FK not in (select ARRAY_DESIGN_FK from EXPRESSION_EXPERIMENT2ARRAY_DESIGN where EXPRESSION_EXPERIMENT_FK = ee2ad.EXPRESSION_EXPERIMENT_FK and ARRAY_DESIGN_FK = ee2ad.ARRAY_DESIGN_FK and not IS_ORIGINAL_PLATFORM)" : "" )
                        + " and ee2ad.EXPRESSION_EXPERIMENT_FK in :ids "
                        + "group by ad.ID "
                        // no need to sort results if limiting, we're collecting in a map
                        + ( maxResults > 0 ? "order by EE_COUNT desc" : "" ) )
                .addEntity( ArrayDesign.class )
                .addScalar( "EE_COUNT", StandardBasicTypes.LONG )
                // ensures that the cache is invalidated when the ee2ad table is regenerated
                .addSynchronizedQuerySpace( EE2AD_QUERY_SPACE )
                // ensures that the cache is invalidated when EEs or ADs are added/removed
                .addSynchronizedEntityClass( ExpressionExperiment.class )
                .addSynchronizedEntityClass( ArrayDesign.class );
        query.setParameter( "original", original );
        query.setCacheable( true );
        Stream<Object[]> result;
        if ( eeIds.size() > MAX_PARAMETER_LIST_SIZE ) {
            result = streamByBatch( query, "ids", eeIds, 2048 );
            if ( maxResults > 0 ) {
                // results need to be aggregated and limited
                return result
                        .collect( groupingBy( row -> ( ArrayDesign ) row[0], summingLong( row -> ( Long ) row[1] ) ) )
                        .entrySet().stream()
                        .sorted( Map.Entry.comparingByValue( Comparator.reverseOrder() ) )
                        .limit( maxResults )
                        .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) );
            }
        } else {
            //noinspection unchecked
            result = query
                    .setParameterList( "ids", optimizeParameterList( eeIds ) )
                    .setMaxResults( maxResults )
                    .list()
                    .stream();
        }
        return result.collect( groupingBy( row -> ( ArrayDesign ) row[0], summingLong( row -> ( Long ) row[1] ) ) );
    }

    @Override
    public Map<Long, Collection<AuditEvent>> getAuditEvents( Collection<Long> ids ) {
        //noinspection unchecked
        List<Object[]> result = this.getSessionFactory().getCurrentSession()
                .createQuery( "select ee.id, auditEvent from ExpressionExperiment ee "
                        + "join ee.auditTrail as auditTrail "
                        + "join auditTrail.events as auditEvent "
                        + "where ee.id in (:ids) " )
                .setParameterList( "ids", optimizeParameterList( ids ) )
                .list();

        Map<Long, Collection<AuditEvent>> eventMap = new HashMap<>();

        for ( Object[] row : result ) {
            this.addEventsToMap( eventMap, ( Long ) row[0], ( AuditEvent ) row[1] );
        }
        // add in expression experiment ids that do not have events. Set
        // their values to null.
        for ( Long id : ids ) {
            if ( !eventMap.containsKey( id ) ) {
                eventMap.put( id, null );
            }
        }
        return eventMap;

    }

    @Override
    public Collection<BioAssayDimension> getBioAssayDimensions( ExpressionExperiment expressionExperiment ) {
        String queryString = "select distinct b from BioAssayDimension b, ExpressionExperiment e "
                + "inner join b.bioAssays bba inner join e.bioAssays eb where eb = bba and e = :ee ";
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "ee", expressionExperiment ).list();
    }

    @Override
    public Collection<BioAssayDimension> getBioAssayDimensions( ExpressionExperiment ee, QuantitationType qt, Class<? extends BulkExpressionDataVector> dataVectorType ) {
        //noinspection unchecked
        Collection<Long> ids = getSessionFactory().getCurrentSession()
                .createCriteria( dataVectorType )
                .add( Restrictions.eq( "expressionExperiment", ee ) )
                .add( Restrictions.eq( "quantitationType", qt ) )
                .createCriteria( "bioAssayDimension" )
                .setProjection( Projections.distinct( Projections.property( "id" ) ) )
                .list();
        return ids.stream()
                .map( id -> ( BioAssayDimension ) getSessionFactory().getCurrentSession().get( BioAssayDimension.class, id ) )
                .collect( Collectors.toSet() );
    }

    @Override
    public long getBioMaterialCount( ExpressionExperiment expressionExperiment ) {
        //language=HQL
        final String queryString =
                "select count(distinct sample) from ExpressionExperiment as ee "
                        + "inner join ee.bioAssays as ba "
                        + "inner join ba.sampleUsed as sample "
                        + "where ee = :ee";

        return ( Long ) this.getSessionFactory().getCurrentSession()
                .createQuery( queryString )
                .setParameter( "ee", expressionExperiment )
                .uniqueResult();
    }

    /**
     * @param ee the expression experiment
     * @return count of RAW vectors.
     */
    @Override
    public long getDesignElementDataVectorCount( ExpressionExperiment ee ) {
        //language=HQL
        final String queryString = "select count(distinct dedv) from ExpressionExperiment ee "
                + "inner join ee.rawExpressionDataVectors dedv where ee = :ee";
        return ( Long ) this.getSessionFactory().getCurrentSession()
                .createQuery( queryString )
                .setParameter( "ee", ee )
                .uniqueResult();
    }

    @Override
    public Collection<ExpressionExperiment> getExperimentsWithOutliers() {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                "select distinct e from ExpressionExperiment e join e.bioAssays b where b.isOutlier = true" ).list();
    }

    @Override
    public Map<Long, Date> getLastArrayDesignUpdate( Collection<ExpressionExperiment> expressionExperiments ) {
        if ( expressionExperiments.isEmpty() ) {
            return Collections.emptyMap();
        }
        //noinspection unchecked
        List<Object[]> res = this.getSessionFactory().getCurrentSession()
                .createQuery( "select ee.id, max(s.lastUpdated) from ExpressionExperiment as ee "
                        + "inner join ee.bioAssays b "
                        + "join b.arrayDesignUsed a "
                        + "join a.curationDetails s "
                        + "where ee in (:ees) "
                        + "group by ee.id" )
                .setParameterList( "ees", optimizeIdentifiableParameterList( expressionExperiments ) )
                .list();
        assert ( !res.isEmpty() );
        Map<Long, Date> result = new HashMap<>();
        for ( Object[] row : res ) {
            result.put( ( Long ) row[0], ( Date ) row[1] );
        }
        return result;
    }

    @Override
    public Date getLastArrayDesignUpdate( ExpressionExperiment ee ) {
        return ( Date ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select max(s.lastUpdated) from ExpressionExperiment as ee "
                        + "join ee.bioAssays b join b.arrayDesignUsed a join a.curationDetails s "
                        + "where ee = :ee" )
                .setParameter( "ee", ee )
                .uniqueResult();
    }

    @Override
    public Map<Taxon, Long> getPerTaxonCount() {
        //language=HQL
        String queryString = "select ee.taxon, count(distinct ee) as EE_COUNT from ExpressionExperiment ee "
                + AclQueryUtils.formAclRestrictionClause( "ee.id" )
                + formNonTroubledClause( "ee", ExpressionExperiment.class )
                + " group by ee.taxon";

        Query query = this.getSessionFactory().getCurrentSession().createQuery( queryString );

        AclQueryUtils.addAclParameters( query, ExpressionExperiment.class );

        // it is important to cache this, as it gets called on the home page. Though it's actually fast.
        //noinspection unchecked
        List<Object[]> list = query
                .setCacheable( true )
                .list();

        return list.stream()
                .collect( Collectors.toMap( row -> ( Taxon ) row[0], row -> ( Long ) row[1] ) );
    }

    @Override
    public Map<Taxon, Long> getPerTaxonCount( Collection<Long> ids ) {
        if ( ids.isEmpty() ) {
            return Collections.emptyMap();
        }
        Query query = this.getSessionFactory().getCurrentSession()
                .createQuery( "select ee.taxon, count(distinct ee) as EE_COUNT from ExpressionExperiment ee "
                        + "where ee.id in :eeIds "
                        + "group by ee.taxon" )
                .setCacheable( true );
        return streamByBatch( query, "eeIds", ids, getBatchSize(), Object[].class )
                .collect( Collectors.groupingBy( row -> ( Taxon ) row[0], Collectors.summingLong( row -> ( Long ) row[1] ) ) );
    }

    public Map<Long, Long> getPopulatedFactorCounts( Collection<Long> ids ) {
        if ( ids.isEmpty() ) {
            return Collections.emptyMap();
        }

        Map<Long, Long> results = new HashMap<>();
        for ( Long id : ids ) {
            results.put( id, 0L );
        }

        //noinspection unchecked
        List<Object[]> res = this.getSessionFactory().getCurrentSession()
                .createQuery( "select e.id,count(distinct ef.id) from ExpressionExperiment e "
                        + "join e.bioAssays ba join ba.sampleUsed bm join bm.factorValues fv join fv.experimentalFactor ef "
                        + "where e.id in (:ids) "
                        + "group by e.id" )
                .setParameterList( "ids", optimizeParameterList( ids ) )
                .list();

        for ( Object[] ro : res ) {
            Long id = ( Long ) ro[0];
            Long count = ( Long ) ro[1];
            results.put( id, count );
        }
        return results;
    }

    @Override
    public Map<Long, Long> getPopulatedFactorCountsExcludeBatch( Collection<Long> ids ) {
        if ( ids.isEmpty() ) {
            return Collections.emptyMap();
        }

        Map<Long, Long> results = new HashMap<>();
        for ( Long id : ids ) {
            results.put( id, 0L );
        }

        String queryString = "select e.id,count(distinct ef.id) from ExpressionExperiment e inner join e.bioAssays ba"
                + " inner join ba.sampleUsed bm inner join bm.factorValues fv inner join fv.experimentalFactor ef "
                + " inner join ef.category cat where e.id in (:ids) and cat.category != (:category) and ef.name != (:name) group by e.id";

        //noinspection unchecked
        List<Object[]> res = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ids", optimizeParameterList( ids ) ) // Set ids
                .setParameter( "category", ExperimentalFactorService.BATCH_FACTOR_CATEGORY_NAME ) // Set batch category
                .setParameter( "name", ExperimentalFactorService.BATCH_FACTOR_NAME ) // set batch name
                .list();

        for ( Object[] ro : res ) {
            Long id = ( Long ) ro[0];
            Long count = ( Long ) ro[1];
            results.put( id, count );
        }

        return results;
    }

    @Override
    public Map<QuantitationType, Long> getQuantitationTypeCount( ExpressionExperiment ee ) {
        //language=HQL
        final String queryString = "select quantType, count(distinct vectors) as count "
                + "from ubic.gemma.model.expression.experiment.ExpressionExperiment ee "
                + "join ee.rawExpressionDataVectors as vectors "
                + "join vectors.quantitationType as quantType "
                + "where ee = :ee "
                + "group by quantType";

        //noinspection unchecked
        List<Object[]> list = this.getSessionFactory().getCurrentSession()
                .createQuery( queryString )
                .setParameter( "ee", ee )
                .list();

        Map<QuantitationType, Long> qtCounts = new HashMap<>();

        for ( Object[] tuple : list ) {
            qtCounts.put( ( QuantitationType ) tuple[0], ( Long ) tuple[1] );
        }

        return qtCounts;
    }

    @Override
    public QuantitationType getPreferredSingleCellQuantitationType( ExpressionExperiment ee ) {
        return ( QuantitationType ) getSessionFactory().getCurrentSession()
                .createQuery( "select qt from ExpressionExperiment ee "
                        + "join ee.singleCellExpressionDataVectors v "
                        + "join v.quantitationType qt "
                        + "where qt.isSingleCellPreferred = true and ee = :ee "
                        + "group by qt" )
                .setParameter( "ee", ee )
                .uniqueResult();
    }

    @Override
    public QuantitationType getPreferredQuantitationType( ExpressionExperiment ee ) {
        return ( QuantitationType ) getSessionFactory().getCurrentSession()
                .createQuery( "select qt from ExpressionExperiment ee "
                        + "join ee.rawExpressionDataVectors rv "
                        + "join rv.quantitationType qt "
                        + "where qt.isPreferred = true and ee = :ee "
                        + "group by qt" )
                .setParameter( "ee", ee )
                .uniqueResult();
    }

    @Override
    public boolean hasProcessedExpressionData( ExpressionExperiment ee ) {
        return ( Boolean ) getSessionFactory().getCurrentSession()
                .createQuery( "select count(pedv) > 0 from ProcessedExpressionDataVector pedv "
                        + "where pedv.expressionExperiment = :ee" )
                .setParameter( "ee", ee )
                .uniqueResult();
    }

    @Override
    public Map<ExpressionExperiment, Collection<AuditEvent>> getSampleRemovalEvents( Collection<ExpressionExperiment> expressionExperiments ) {
        if ( expressionExperiments.isEmpty() ) {
            return Collections.emptyMap();
        }

        //noinspection unchecked
        List<Object[]> r = this.getSessionFactory().getCurrentSession()
                .createQuery( "select ee,ev from ExpressionExperiment ee "
                        + "join ee.auditTrail trail "
                        + "join trail.events ev join ev.eventType et join fetch ev.performer "
                        + "where ee in (:ees) and type(et) = :etClass" )
                .setParameterList( "ees", optimizeIdentifiableParameterList( expressionExperiments ) )
                .setParameter( "etClass", SampleRemovalEvent.class )
                .list();

        Map<ExpressionExperiment, Collection<AuditEvent>> result = new HashMap<>();
        for ( Object[] o : r ) {
            ExpressionExperiment e = ( ExpressionExperiment ) o[0];
            if ( !result.containsKey( e ) ) {
                result.put( e, new HashSet<>() );
            }
            AuditEvent ae = ( AuditEvent ) o[1];
            result.get( e ).add( ae );
        }
        return result;
    }

    @Override
    public Collection<ExpressionExperimentSubSet> getSubSets( ExpressionExperiment expressionExperiment ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select eess from ExpressionExperimentSubSet eess where eess.sourceExperiment = :ee" )
                .setParameter( "ee", expressionExperiment )
                .list();
    }

    @Override
    public <T extends BioAssaySet> Map<T, Taxon> getTaxa( Collection<T> bioAssaySets ) {
        if ( bioAssaySets.isEmpty() )
            return Collections.emptyMap();

        Collection<ExpressionExperiment> ees = new ArrayList<>();
        Collection<ExpressionExperimentSubSet> subsets = new ArrayList<>();
        for ( BioAssaySet bas : bioAssaySets ) {
            if ( bas instanceof ExpressionExperiment ) {
                ees.add( ( ExpressionExperiment ) bas );
            } else if ( bas instanceof ExpressionExperimentSubSet ) {
                subsets.add( ( ExpressionExperimentSubSet ) bas );
            } else {
                throw new UnsupportedOperationException(
                        "Can't get taxon of BioAssaySet of class " + bas.getClass().getName() );
            }
        }

        List<Object[]> list = new ArrayList<>();
        if ( !ees.isEmpty() ) {
            // FIXME: this query cannot be made cacheable because the taxon is not initialized when retrieved from the cache, defeating the purpose of caching altogether
            Query query = this.getSessionFactory().getCurrentSession()
                    .createQuery( "select EE, st from ExpressionExperiment as EE "
                            + "join EE.bioAssays as BA join BA.sampleUsed as SU join SU.sourceTaxon st where EE in (:ees) "
                            + "group by EE" );
            list.addAll( QueryUtils.listByIdentifiableBatch( query, "ees", ees, 2048 ) );
        }
        if ( !subsets.isEmpty() ) {
            Query query = this.getSessionFactory().getCurrentSession()
                    .createQuery( "select eess, st from ExpressionExperimentSubSet eess "
                            + "join eess.sourceExperiment ee join ee.bioAssays as BA join BA.sampleUsed as su "
                            + "join su.sourceTaxon as st where eess in (:ees) group by eess" );
            list.addAll( QueryUtils.listByIdentifiableBatch( query, "ees", ees, 2048 ) );
        }

        // collecting in a tree map in case BASs are proxies
        Map<T, Taxon> result = new TreeMap<>( Comparator.comparing( BioAssaySet::getId ) );
        for ( Object[] row : list ) {
            //noinspection unchecked
            result.put( ( T ) row[0], ( Taxon ) row[1] );
        }
        return result;
    }

    @Override
    public Taxon getTaxon( BioAssaySet ee ) {
        if ( ee instanceof ExpressionExperiment ) {
            if ( ( ( ExpressionExperiment ) ee ).getTaxon() != null ) {
                return ( ( ExpressionExperiment ) ee ).getTaxon();
            }
            return getTaxonFromSamples( ( ExpressionExperiment ) ee );
        } else if ( ee instanceof ExpressionExperimentSubSet ) {
            ExpressionExperiment sourceExperiment = ( ( ExpressionExperimentSubSet ) ee ).getSourceExperiment();
            if ( sourceExperiment.getTaxon() != null ) {
                return sourceExperiment.getTaxon();
            } else {
                return getTaxonFromSamples( sourceExperiment );
            }
        } else {
            throw new UnsupportedOperationException(
                    "Can't get taxon of BioAssaySet of class " + ee.getClass().getName() );
        }
    }

    private Taxon getTaxonFromSamples( ExpressionExperiment ee ) {
        String queryString = "select distinct SU.sourceTaxon from ExpressionExperiment as EE "
                + "inner join EE.bioAssays as BA inner join BA.sampleUsed as SU where EE = :ee";
        return ( Taxon ) this.getSessionFactory().getCurrentSession()
                .createQuery( queryString )
                .setParameter( "ee", ee )
                .uniqueResult();
    }

    @Value
    private static class ExpressionExperimentDetail {

        public static ExpressionExperimentDetail fromRow( Object[] row ) {
            return new ExpressionExperimentDetail( ( Long ) row[1], ( Long ) row[2], ( Long ) row[3], ( Integer ) row[4] );
        }

        @Nullable
        Long arrayDesignUsedId;
        @Nullable
        Long originalPlatformId;
        @Nullable
        Long otherPartId;
        Integer bioAssaysCount;
    }

    /**
     * Gather various EE details and group them by ID.
     */
    private Map<Long, List<ExpressionExperimentDetail>> getExpressionExperimentDetailsById( List<Long> expressionExperimentIds, boolean cacheable ) {
        if ( expressionExperimentIds.isEmpty() ) {
            return Collections.emptyMap();
        }
        //noinspection unchecked
        List<Object[]> results = getSessionFactory().getCurrentSession()
                .createQuery( "select ee.id, ad.id, op.id, oe.id, ee.bioAssays.size from ExpressionExperiment as ee "
                        + "left join ee.bioAssays ba "
                        + "left join ba.arrayDesignUsed ad "
                        + "left join ba.originalPlatform op " // not all bioAssays have an original platform
                        + "left join ee.otherParts as oe "    // not all experiments are splitted
                        + "where ee.id in :eeIds "
                        // FIXME: apply ACLs, other parts or platform might be private
                        + "group by ee, ad, op, oe" )
                .setParameterList( "eeIds", optimizeParameterList( expressionExperimentIds ) )
                .setCacheable( cacheable )
                .list();
        return results.stream().collect(
                groupingBy( row -> ( Long ) row[0],
                        Collectors.mapping( ExpressionExperimentDetail::fromRow, Collectors.toList() ) ) );
    }

    @Override
    public List<ExpressionExperiment> loadWithRelationsAndCache( List<Long> ids ) {
        if ( ids.isEmpty() ) {
            return Collections.emptyList();
        }
        //noinspection unchecked
        return ( List<ExpressionExperiment> ) getSessionFactory().getCurrentSession()
                .createQuery( "select ee from ExpressionExperiment ee "
                        + "left join ee.accession acc "
                        + "left join ee.experimentalDesign as EDES "
                        + "left join ee.curationDetails as s " /* needed for trouble status */
                        + "left join s.lastNeedsAttentionEvent as eAttn "
                        + "left join s.lastNoteUpdateEvent as eNote "
                        + "left join s.lastTroubledEvent as eTrbl "
                        + "left join ee.geeq as geeq "
                        + "where ee.id in :ids" )
                .setParameterList( "ids", optimizeParameterList( ids ) )
                .setCacheable( true )
                // this transformer performs initialization of cached results
                .setResultTransformer( getEntityTransformer() )
                .list();
    }

    @Override
    public Slice<ExpressionExperimentDetailsValueObject> loadDetailsValueObjects
            ( @Nullable Collection<Long> ids, @Nullable Taxon taxon, @Nullable Sort sort, int offset, int limit ) {
        if ( ids != null && ids.isEmpty() ) {
            return new Slice<>( Collections.emptyList(), sort, offset, limit, 0L );
        }
        return this.doLoadDetailsValueObjects( getFiltersForIdsAndTaxon( ids, taxon ), sort, offset, limit, false );
    }

    @Override
    public Slice<ExpressionExperimentDetailsValueObject> loadDetailsValueObjectsByIdsWithCache( @Nullable Collection<Long> ids, @Nullable Taxon taxon, @Nullable Sort sort, int offset, int limit ) {
        if ( ids != null && ids.isEmpty() ) {
            return new Slice<>( Collections.emptyList(), sort, offset, limit, 0L );
        }
        return this.doLoadDetailsValueObjects( getFiltersForIdsAndTaxon( ids, taxon ), sort, offset, limit, true );
    }

    @Override
    public List<ExpressionExperimentDetailsValueObject> loadDetailsValueObjectsByIds( Collection<Long> ids ) {
        if ( ids.isEmpty() ) {
            return Collections.emptyList();
        }
        return this.doLoadDetailsValueObjects( getFiltersForIdsAndTaxon( ids, null ), null, 0, 0, false );
    }

    @Override
    public List<ExpressionExperimentDetailsValueObject> loadDetailsValueObjectsByIdsWithCache( Collection<Long> ids ) {
        if ( ids.isEmpty() ) {
            return Collections.emptyList();
        }
        return this.doLoadDetailsValueObjects( getFiltersForIdsAndTaxon( ids, null ), null, 0, 0, true );
    }

    private Filters getFiltersForIdsAndTaxon( @Nullable Collection<Long> ids, @Nullable Taxon taxon ) {
        Filters filters = Filters.empty();

        if ( ids != null ) {
            List<Long> idList = new ArrayList<>( ids );
            Collections.sort( idList );
            filters.and( OBJECT_ALIAS, "id", Long.class, Filter.Operator.in, idList );
        }

        if ( taxon != null ) {
            filters.and( TaxonDao.OBJECT_ALIAS, "id", Long.class, Filter.Operator.eq, taxon.getId() );
        }

        return filters;
    }

    private Slice<ExpressionExperimentDetailsValueObject> doLoadDetailsValueObjects( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit, boolean cacheable ) {
        // Compose query
        Query query = this.getFilteringQuery( filters, sort );

        if ( offset > 0 ) {
            query.setFirstResult( offset );
        }
        if ( limit > 0 ) {
            query.setMaxResults( limit );
        }

        // overall timer
        StopWatch timer = StopWatch.createStarted();

        // timers for sub-steps
        StopWatch countingTimer = StopWatch.create();
        StopWatch postProcessingTimer = StopWatch.create();
        StopWatch detailsTimer = StopWatch.create();
        StopWatch analysisInformationTimer = StopWatch.create();

        query.setResultTransformer( getDetailedValueObjectTransformer( cacheable, postProcessingTimer, detailsTimer, analysisInformationTimer ) );

        //noinspection unchecked
        List<ExpressionExperimentDetailsValueObject> vos = query
                .setCacheable( cacheable )
                .list();

        countingTimer.start();
        Long totalElements;
        if ( limit > 0 && ( vos.isEmpty() || vos.size() == limit ) ) {
            totalElements = ( Long ) this.getFilteringCountQuery( filters )
                    .setCacheable( cacheable )
                    .uniqueResult();
        } else {
            totalElements = offset + ( long ) vos.size();
        }
        countingTimer.stop();

        timer.stop();

        if ( timer.getTime() > REPORT_SLOW_QUERY_AFTER_MS ) {
            log.warn( String.format( "EE details VO query + postprocessing: %d ms (query: %d ms, counting: %d ms, initializing VOs: %d ms, loading details (bioAssays + bioAssays.arrayDesignUsed + originalPlatforms + otherParts): %d ms, retrieving analysis information: %s ms)",
                    timer.getTime(),
                    timer.getTime() - postProcessingTimer.getTime(),
                    countingTimer.getTime(),
                    postProcessingTimer.getTime(),
                    detailsTimer.getTime(),
                    analysisInformationTimer.getTime() ) );
        }

        return new Slice<>( vos, sort, offset, limit, totalElements );
    }

    private TypedResultTransformer<ExpressionExperimentDetailsValueObject> getDetailedValueObjectTransformer( boolean cacheable, StopWatch postProcessingTimer, StopWatch detailsTimer, StopWatch analysisInformationTimer ) {
        return new TypedResultTransformer<ExpressionExperimentDetailsValueObject>() {
            @Override
            public ExpressionExperimentDetailsValueObject transformTuple( Object[] row, String[] aliases ) {
                ExpressionExperiment ee = ( ExpressionExperiment ) row[0];
                initializeCachedFilteringResult( ee );
                AclObjectIdentity aoi = ( AclObjectIdentity ) row[1];
                AclSid sid = ( AclSid ) row[2];
                return new ExpressionExperimentDetailsValueObject( ee, aoi, sid );
            }

            @Override
            public List<ExpressionExperimentDetailsValueObject> transformListTyped( List<ExpressionExperimentDetailsValueObject> vos ) {
                postProcessingTimer.start();

                // sort + distinct for cache consistency
                List<Long> expressionExperimentIds = vos.stream()
                        .map( Identifiable::getId )
                        .sorted()
                        .distinct()
                        .collect( Collectors.toList() );

                // fetch some extras details
                // we could make this a single query in getLoadValueObjectDetails, but performing a jointure with the bioAssays
                // and arrayDesignUsed is inefficient in the general case, so we only fetch what we need here
                detailsTimer.start();
                Map<Long, List<ExpressionExperimentDetail>> detailsByEE = getExpressionExperimentDetailsById( expressionExperimentIds, cacheable );
                detailsTimer.stop();

                for ( ExpressionExperimentDetailsValueObject vo : vos ) {
                    List<ExpressionExperimentDetail> details = detailsByEE.get( vo.getId() );

                    Set<Long> arrayDesignsUsedIds = details.stream()
                            .map( ExpressionExperimentDetail::getArrayDesignUsedId )
                            .filter( Objects::nonNull )
                            .collect( Collectors.toSet() );

                    // we need those later for computing original platforms
                    Collection<ArrayDesignValueObject> adVos = arrayDesignsUsedIds.stream()
                            .map( id -> ( ArrayDesign ) getSessionFactory().getCurrentSession().get( ArrayDesign.class, id ) )
                            .map( ArrayDesignValueObject::new )
                            .collect( Collectors.toSet() );
                    vo.setArrayDesigns( adVos ); // also sets taxon name, technology type, and number of ADs.

                    // original platforms
                    Collection<ArrayDesignValueObject> originalPlatformsVos = details.stream()
                            .map( ExpressionExperimentDetail::getOriginalPlatformId )
                            .filter( Objects::nonNull ) // on original platform for the bioAssay
                            .distinct()
                            .filter( op -> !arrayDesignsUsedIds.contains( op ) ) // omit noop switches
                            .map( id -> ( ArrayDesign ) getSessionFactory().getCurrentSession().get( ArrayDesign.class, id ) )
                            .map( ArrayDesignValueObject::new )
                            .collect( Collectors.toSet() );
                    vo.setOriginalPlatforms( originalPlatformsVos );

                    Integer bioAssayCount = details.stream()
                            .map( ExpressionExperimentDetail::getBioAssaysCount )
                            .findFirst()
                            .orElse( 0 );
                    vo.setNumberOfBioAssays( bioAssayCount );

                    Set<Long> otherPartsIds = details.stream()
                            .map( ExpressionExperimentDetail::getOtherPartId )
                            .filter( Objects::nonNull )
                            .collect( Collectors.toSet() );

                    List<ExpressionExperimentValueObject> otherPartsVos = loadValueObjectsByIds( otherPartsIds ).stream()
                            .sorted( Comparator.comparing( ExpressionExperimentValueObject::getShortName ) ).collect( Collectors.toList() );

                    // other parts (maybe fetch in details query?)
                    vo.setOtherParts( otherPartsVos );
                }

                try ( StopWatchUtils.StopWatchRegion ignored = StopWatchUtils.measuredRegion( analysisInformationTimer ) ) {
                    populateAnalysisInformation( vos, cacheable );
                }

                postProcessingTimer.stop();

                return vos;
            }
        };
    }

    @Override
    public Slice<ExpressionExperimentValueObject> loadBlacklistedValueObjects( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        //noinspection unchecked
        List<Object[]> result = getSessionFactory().getCurrentSession()
                .createQuery( "select be.shortName, ea.accession from BlacklistedExperiment be left join be.externalAccession ea" )
                .setCacheable( true )
                .list();
        if ( result.isEmpty() ) {
            return new Slice<>( Collections.emptyList(), sort, offset, limit, 0L );
        }
        if ( filters == null ) {
            filters = Filters.empty();
        } else {
            // we create a copy because we don't want to leak the lits of blacklisted EEs in the filter
            filters = Filters.by( filters );
        }
        Set<String> blacklistedShortNames = result.stream().map( row -> ( String ) row[0] ).filter( Objects::nonNull ).collect( Collectors.toSet() );
        Set<String> blacklistedAccessions = result.stream().map( row -> ( String ) row[1] ).filter( Objects::nonNull ).collect( Collectors.toSet() );
        Filters.FiltersClauseBuilder clause = filters.and();
        if ( !blacklistedShortNames.isEmpty() )
            clause = clause.or( "ee", "shortName", String.class, Filter.Operator.in, blacklistedShortNames );
        if ( !blacklistedAccessions.isEmpty() )
            clause = clause.or( "ee", "accession.accession", String.class, Filter.Operator.in, blacklistedAccessions );
        clause.build();
        return loadValueObjects( filters, sort, offset, limit );
    }

    @Override
    public Collection<ExpressionExperiment> loadLackingFactors() {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select e from ExpressionExperiment e join e.experimentalDesign d where d.experimentalFactors.size =  0" )
                .list();
    }

    @Override
    public Collection<ExpressionExperiment> loadLackingTags() {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select e from ExpressionExperiment e where e.characteristics.size = 0" ).list();
    }

    @Override
    protected ExpressionExperimentValueObject doLoadValueObject( ExpressionExperiment entity ) {
        return new ExpressionExperimentValueObject( entity );
    }

    @Override
    protected void postProcessValueObjects( List<ExpressionExperimentValueObject> results ) {
        populateArrayDesignCount( results );
    }

    @Override
    public List<ExpressionExperimentValueObject> loadValueObjects( @Nullable Filters
            filters, @Nullable Sort sort ) {
        if ( sort == null ) {
            sort = Sort.by( OBJECT_ALIAS, "id", null, Sort.NullMode.DEFAULT, "id" );
        }
        return super.loadValueObjects( filters, sort );
    }

    @Override
    public Slice<ExpressionExperimentValueObject> loadValueObjects( @Nullable Filters
            filters, @Nullable Sort sort, int offset, int limit ) {
        if ( sort == null ) {
            sort = Sort.by( OBJECT_ALIAS, "id", null, Sort.NullMode.DEFAULT, "id" );
        }
        return super.loadValueObjects( filters, sort, offset, limit );
    }

    @Override
    protected TypedResultTransformer<ExpressionExperimentValueObject> getValueObjectTransformer() {
        TypedResultTransformer<ExpressionExperimentValueObject> transformer = super.getValueObjectTransformer();
        return new TypedResultTransformer<ExpressionExperimentValueObject>() {
            @Override
            public ExpressionExperimentValueObject transformTuple( Object[] row, String[] aliases ) {
                ExpressionExperiment ee = ( ExpressionExperiment ) row[0];
                AclObjectIdentity aoi = ( AclObjectIdentity ) row[1];
                AclSid sid = ( AclSid ) row[2];
                initializeCachedFilteringResult( ee );
                return new ExpressionExperimentValueObject( ee, aoi, sid );
            }

            @Override
            public List<ExpressionExperimentValueObject> transformListTyped( List<ExpressionExperimentValueObject> collection ) {
                return transformer.transformListTyped( collection );
            }
        };
    }

    @Override
    public void remove( ExpressionExperiment ee ) {
        log.info( "Deleting " + ee + "..." );

        // Note that links and analyses are deleted separately - see the ExpressionExperimentService.

        // these are tied to the audit trail and will cause lock problems it we don't clear first (due to cascade=all on the curation details, but
        // this may be okay now with updated config - see CurationDetails.hbm.xml)
        ee.getCurationDetails().setLastNeedsAttentionEvent( null );
        ee.getCurationDetails().setLastNoteUpdateEvent( null );
        ee.getCurationDetails().setLastTroubledEvent( null );

        // dissociate this EE from other parts that refers to it
        // it's not reliable to check the otherParts collection because the relation is bi-directional and some dataset
        // might refer to this EE and not the other way around
        //noinspection unchecked
        List<ExpressionExperiment> otherParts = getSessionFactory().getCurrentSession()
                .createQuery( "select distinct ee from ExpressionExperiment ee join ee.otherParts op where op = :ee" )
                .setParameter( "ee", ee )
                .list();
        if ( !otherParts.isEmpty() ) {
            log.info( String.format( "Detaching split experiment from %d other parts", otherParts.size() ) );
            for ( ExpressionExperiment e : otherParts ) {
                log.debug( "Detaching from " + e );
                e.getOtherParts().remove( ee );
            }
        }

        // detach from BAs from dimensions, completely detached dimension will be removed later
        Set<BioAssayDimension> dimensionsToRemove = new HashSet<>();
        for ( BioAssayDimension dim : this.getBioAssayDimensions( ee ) ) {
            dim.getBioAssays().removeAll( ee.getBioAssays() );
            if ( dim.getBioAssays().isEmpty() ) {
                dimensionsToRemove.add( dim );
            } else {
                log.warn( dim + " is attached to more than one ExpressionExperiment, the dimension will not be deleted." );
            }
        }

        // find BMs attached to BAs
        Set<BioMaterial> bms = new HashSet<>();
        if ( !ee.getBioAssays().isEmpty() ) {
            bms.addAll( listByIdentifiableBatch( getSessionFactory().getCurrentSession()
                            .createQuery( "select distinct bm from BioMaterial bm join bm.bioAssaysUsedIn ba where ba in :bas" ),
                    "bas", ee.getBioAssays(), MAX_PARAMETER_LIST_SIZE ) );
        }

        // find BMs attached to FVs
        Set<FactorValue> fvs = new HashSet<>( getFactorValues( ee ) );
        if ( !fvs.isEmpty() ) {
            bms.addAll( listByIdentifiableBatch( getSessionFactory().getCurrentSession()
                            .createQuery( "select distinct bm from BioMaterial bm join bm.factorValues fv where fv in :fvs" ),
                    "fvs", fvs, MAX_PARAMETER_LIST_SIZE ) );
        }

        Set<BioMaterial> samplesToRemove = new HashSet<>();
        for ( BioMaterial bm : bms ) {
            // detach BAs and FVs from the samples, completely detached samples will be removed later
            bm.getFactorValues().removeAll( fvs );
            bm.getBioAssaysUsedIn().removeAll( ee.getBioAssays() );
            if ( bm.getBioAssaysUsedIn().isEmpty() && bm.getFactorValues().isEmpty() ) {
                samplesToRemove.add( bm );
            } else {
                log.warn( bm + " is attached to more than one ExpressionExperiment (via one or more BioAssay or FactorValue), the sample will not be deleted." );
            }
        }

        // remove vectors
        // those can also be removed in cascade, but it's much faster to use these instead
        removeAllRawDataVectors( ee );
        removeProcessedDataVectors( ee );

        if ( !dimensionsToRemove.isEmpty() ) {
            log.info( String.format( "Removing %d BioAssayDimension that are no longer attached to any BioAssay", dimensionsToRemove.size() ) );
            for ( BioAssayDimension dim : dimensionsToRemove ) {
                log.debug( "Removing " + dim + "..." );
                getSessionFactory().getCurrentSession().delete( dim );
            }
        }

        // unlike BioAssayDimension, SingleCellDimension are immutable and can never hold BAs from other experiments, so
        // we don't need to detach anything
        List<SingleCellDimension> singleCellDimensionsToRemove = getSingleCellDimensions( ee );

        removeAllSingleCellDataVectors( ee );

        // remove single-cell dimensions using any of the BAs
        // these are immutable, so we cannot simply detach
        for ( SingleCellDimension scd : singleCellDimensionsToRemove ) {
            log.info( "Removing " + scd + "..." );
            deleteSingleCellDimension( ee, scd );
            for ( BioAssay ba : scd.getBioAssays() ) {
                getSessionFactory().getCurrentSession().delete( ba );
                BioMaterial bm = ba.getSampleUsed();
                bm.getBioAssaysUsedIn().remove( ba );
                bm.getFactorValues().removeAll( fvs );
                if ( bm.getBioAssaysUsedIn().isEmpty() && bm.getFactorValues().isEmpty() ) {
                    samplesToRemove.add( ba.getSampleUsed() );
                } else {
                    log.warn( bm + " is attached to more than one ExpressionExperiment (via one or more BioAssay or FactorValue), the sample will not be deleted." );
                }
            }
        }

        super.remove( ee );

        if ( !samplesToRemove.isEmpty() ) {
            // those need to be removed afterward because otherwise the BioAssay.sampleUsed would become transient while
            // cascading and that is not allowed in the data model
            log.info( String.format( "Removing %d BioMaterial that are no longer attached to any BioAssay", samplesToRemove.size() ) );
            for ( BioMaterial bm : samplesToRemove ) {
                log.debug( "Removing " + bm + "..." );
                getSessionFactory().getCurrentSession().delete( bm );
            }
        }
    }

    private List<FactorValue> getFactorValues( ExpressionExperiment ee ) {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                .createQuery( "select distinct fv from ExpressionExperiment ee "
                        + "join ee.experimentalDesign ed "
                        + "join ed.experimentalFactors ef "
                        + "join ef.factorValues fv "
                        + "where ee = :ee" )
                .setParameter( "ee", ee )
                .list();
    }

    @Override
    public void thaw( final ExpressionExperiment expressionExperiment ) {
        thawLite( expressionExperiment );
        thawRawVectors( expressionExperiment );
        thawProcessedVectors( expressionExperiment );
    }

    // "thawLite"
    @Override
    public void thawLite( final ExpressionExperiment ee ) {
        thawLiter( ee );

        Hibernate.initialize( ee.getQuantitationTypes() );
        Hibernate.initialize( ee.getCharacteristics() );

        if ( ee.getAuditTrail() != null ) {
            Hibernate.initialize( ee.getAuditTrail().getEvents() );
        }

        Hibernate.initialize( ee.getBioAssays() );
        for ( BioAssay ba : ee.getBioAssays() ) {
            Hibernate.initialize( ba.getArrayDesignUsed() );
            Hibernate.initialize( ba.getArrayDesignUsed().getDesignProvider() );
            if ( ba.getOriginalPlatform() != null ) {
                Hibernate.initialize( ba.getOriginalPlatform() );
            }
            visitBioMaterials( ba.getSampleUsed(), bm -> {
                Hibernate.initialize( bm.getFactorValues() );
                for ( FactorValue fv : bm.getFactorValues() ) {
                    Hibernate.initialize( fv.getExperimentalFactor() );
                }
                Hibernate.initialize( bm.getTreatments() );
            } );
        }
    }

    @Override
    public void thawBioAssays( ExpressionExperiment expressionExperiment ) {
        thawLiter( expressionExperiment );

        Hibernate.initialize( expressionExperiment.getBioAssays() );

        for ( BioAssay ba : expressionExperiment.getBioAssays() ) {
            Hibernate.initialize( ba.getArrayDesignUsed() );
            Hibernate.initialize( ba.getOriginalPlatform() );
            thawBioMaterial( ba.getSampleUsed() );
        }
    }

    /**
     * @see ubic.gemma.persistence.service.expression.biomaterial.BioMaterialDao#thaw(BioMaterial)
     */
    private void thawBioMaterial( BioMaterial bm2 ) {
        visitBioMaterials( bm2, bm -> {
            Hibernate.initialize( bm.getSourceTaxon() );
            Hibernate.initialize( bm.getTreatments() );
            for ( FactorValue fv : bm.getFactorValues() ) {
                Hibernate.initialize( fv.getExperimentalFactor() );
            }
        } );
    }

    @Override
    public void thawLiter( final ExpressionExperiment expressionExperiment ) {
        if ( expressionExperiment.getAccession() != null ) {
            Hibernate.initialize( expressionExperiment.getAccession() );
            Hibernate.initialize( expressionExperiment.getAccession().getExternalDatabase() );
        }

        if ( expressionExperiment.getMeanVarianceRelation() != null ) {
            Hibernate.initialize( expressionExperiment.getMeanVarianceRelation() );
            Hibernate.initialize( expressionExperiment.getMeanVarianceRelation().getMeans() );
            Hibernate.initialize( expressionExperiment.getMeanVarianceRelation().getVariances() );
        }

        if ( expressionExperiment.getPrimaryPublication() != null ) {
            Hibernate.initialize( expressionExperiment.getPrimaryPublication() );
            if ( expressionExperiment.getPrimaryPublication().getPublication() != null ) {
                Hibernate.initialize( expressionExperiment.getPrimaryPublication().getPubAccession() );
                Hibernate.initialize( expressionExperiment.getPrimaryPublication().getPubAccession().getExternalDatabase() );
            }
        }

        Hibernate.initialize( expressionExperiment.getAuditTrail() );
        Hibernate.initialize( expressionExperiment.getGeeq() );
        Hibernate.initialize( expressionExperiment.getCurationDetails() );

        Hibernate.initialize( expressionExperiment.getOtherParts() );

        if ( expressionExperiment.getExperimentalDesign() != null ) {
            Hibernate.initialize( expressionExperiment.getExperimentalDesign() );
            Hibernate.initialize( expressionExperiment.getExperimentalDesign().getExperimentalFactors() );
            for ( ExperimentalFactor ef : expressionExperiment.getExperimentalDesign().getExperimentalFactors() ) {
                Hibernate.initialize( ef );
                for ( FactorValue fv : ef.getFactorValues() ) {
                    Hibernate.initialize( fv.getExperimentalFactor() ); // is it even necessary?
                }
            }
            Hibernate.initialize( expressionExperiment.getExperimentalDesign().getTypes() );
        }

        if ( expressionExperiment.getPrimaryPublication() != null ) {
            Hibernate.initialize( expressionExperiment.getPrimaryPublication().getMeshTerms() );
            Hibernate.initialize( expressionExperiment.getPrimaryPublication().getKeywords() );
            Hibernate.initialize( expressionExperiment.getPrimaryPublication().getChemicals() );
        }

        for ( BibliographicReference br : expressionExperiment.getOtherRelevantPublications() ) {
            Hibernate.initialize( br.getMeshTerms() );
            Hibernate.initialize( br.getKeywords() );
            Hibernate.initialize( br.getChemicals() );
        }
    }

    @Override
    public void thawRawVectors( ExpressionExperiment ee ) {
        StopWatch timer = StopWatch.createStarted();
        Hibernate.initialize( ee.getRawExpressionDataVectors() );
        if ( timer.getTime() > 1000 ) {
            log.info( String.format( "Initializing %d raw vectors took %d ms", ee.getRawExpressionDataVectors().size(), timer.getTime() ) );
        }
    }

    @Override
    public void thawProcessedVectors( ExpressionExperiment ee ) {
        StopWatch timer = StopWatch.createStarted();
        Hibernate.initialize( ee.getProcessedExpressionDataVectors() );
        if ( timer.getTime() > 1000 ) {
            log.info( String.format( "Initializing %d processed vectors took %d ms", ee.getProcessedExpressionDataVectors().size(), timer.getTime() ) );
        }
    }

    @Override
    public List<SingleCellDimension> getSingleCellDimensions( ExpressionExperiment ee ) {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                .createQuery( "select distinct scedv.singleCellDimension from SingleCellExpressionDataVector scedv "
                        + "where scedv.expressionExperiment = :ee" )
                .setParameter( "ee", ee )
                .list();
    }

    @Override
    public SingleCellDimension getSingleCellDimension( ExpressionExperiment ee, QuantitationType qt ) {
        return ( SingleCellDimension ) getSessionFactory().getCurrentSession()
                .createQuery( "select distinct scedv.singleCellDimension from SingleCellExpressionDataVector scedv "
                        + "where scedv.expressionExperiment = :ee and scedv.quantitationType = :qt" )
                .setParameter( "ee", ee )
                .setParameter( "qt", qt )
                .uniqueResult();
    }

    @Override
    public SingleCellDimension getPreferredSingleCellDimension( ExpressionExperiment ee ) {
        return ( SingleCellDimension ) getSessionFactory().getCurrentSession()
                .createQuery( "select distinct scedv.singleCellDimension from SingleCellExpressionDataVector scedv "
                        + "where scedv.quantitationType.isSingleCellPreferred = true and scedv.expressionExperiment = :ee" )
                .setParameter( "ee", ee )
                .uniqueResult();
    }

    @Override
    public void createSingleCellDimension( ExpressionExperiment ee, SingleCellDimension singleCellDimension ) {
        validateSingleCellDimension( ee, singleCellDimension );
        getSessionFactory().getCurrentSession().persist( singleCellDimension );
    }

    @Override
    public void updateSingleCellDimension( ExpressionExperiment ee, SingleCellDimension singleCellDimension ) {
        validateSingleCellDimension( ee, singleCellDimension );
        getSessionFactory().getCurrentSession().update( singleCellDimension );
    }

    /**
     * Validate single-cell dimension.
     */
    private void validateSingleCellDimension( ExpressionExperiment ee, SingleCellDimension scbad ) {
        Assert.isTrue( !scbad.getCellIds().isEmpty(), "There must be at least one cell ID." );
        for ( int i = 0; i < scbad.getBioAssays().size(); i++ ) {
            List<String> sampleCellIds = scbad.getCellIdsBySample( i );
            Assert.isTrue( sampleCellIds.stream().distinct().count() == sampleCellIds.size(),
                    "Cell IDs must be unique for each sample." );
        }
        Assert.isTrue( scbad.getCellIds().size() == scbad.getNumberOfCells(),
                "The number of cell IDs must match the number of cells." );
        Assert.isTrue( scbad.getCellTypeAssignments().stream().filter( CellTypeAssignment::isPreferred ).count() <= 1,
                "There must be at most one preferred cell type labelling." );
        validateCellTypeAssignments( scbad );
        validateCellLevelCharacteristics( scbad );
        Assert.isTrue( !scbad.getBioAssays().isEmpty(), "There must be at least one BioAssay." );
        Assert.isTrue( ee.getBioAssays().containsAll( scbad.getBioAssays() ), "Not all supplied BioAssays belong to " + ee );
        validateSparseRangeArray( scbad.getBioAssays(), scbad.getBioAssaysOffset(), scbad.getNumberOfCells() );
    }

    private void validateCellTypeAssignments( SingleCellDimension scbad ) {
        if ( !Hibernate.isInitialized( scbad.getCellTypeAssignments() ) ) {
            return; // no need to validate if not initialized
        }
        for ( CellTypeAssignment labelling : scbad.getCellTypeAssignments() ) {
            Assert.notNull( labelling.getCellTypes() );
            Assert.isTrue( labelling.getCellTypeIndices().length == scbad.getCellIds().size(),
                    "The number of cell types must match the number of cell IDs." );
            int numberOfCellTypeLabels = labelling.getCellTypes().size();
            Assert.isTrue( numberOfCellTypeLabels > 0,
                    "There must be at least one cell type label declared in the cellTypes collection." );
            Assert.isTrue( labelling.getCellTypes().stream().distinct().count() == labelling.getCellTypes().size(),
                    "Cell type labels must be unique." );
            Assert.isTrue( numberOfCellTypeLabels == labelling.getNumberOfCellTypes(),
                    "The number of cell types must match the number of values the cellTypes collection." );
            for ( int k : labelling.getCellTypeIndices() ) {
                Assert.isTrue( ( k >= 0 && k < numberOfCellTypeLabels ) || k == CellLevelCharacteristics.UNKNOWN_CHARACTERISTIC,
                        String.format( "Cell type vector values must be within the [%d, %d[ range or use %d as an unknown indicator.",
                                0, numberOfCellTypeLabels, CellLevelCharacteristics.UNKNOWN_CHARACTERISTIC ) );
            }
            for ( Characteristic c : labelling.getCellTypes() ) {
                Assert.isTrue( CharacteristicUtils.hasCategory( c, Categories.CELL_TYPE ), "All cell types must have the " + Categories.CELL_TYPE + " category." );
            }
        }
    }

    private void validateCellLevelCharacteristics( SingleCellDimension scbad ) {
        if ( !Hibernate.isInitialized( scbad.getCellLevelCharacteristics() ) ) {
            return; // no need to validate if not initialized
        }
        for ( CellLevelCharacteristics clc : scbad.getCellLevelCharacteristics() ) {
            Assert.isTrue( clc.getIndices().length == scbad.getCellIds().size(),
                    "The number of cell-level characteristics must match the number of cell IDs." );
            int numberOfCharacteristics = clc.getCharacteristics().size();
            Assert.isTrue( numberOfCharacteristics == clc.getNumberOfCharacteristics(),
                    "The number of cell-level characteristics must match the size of the characteristics collection." );
            for ( int k : clc.getIndices() ) {
                Assert.isTrue( ( k >= 0 && k < numberOfCharacteristics ) || k == CellTypeAssignment.UNKNOWN_CELL_TYPE,
                        String.format( "Cell-level characteristics vector values must be within the [%d, %d[ range or use %d as an unknown indicator.",
                                0, numberOfCharacteristics, CellTypeAssignment.UNKNOWN_CELL_TYPE ) );
            }
        }
    }

    @Override
    public void deleteSingleCellDimension( ExpressionExperiment ee, SingleCellDimension singleCellDimension ) {
        getSessionFactory().getCurrentSession().delete( singleCellDimension );
    }

    @Override
    public List<CellTypeAssignment> getCellTypeAssignments( ExpressionExperiment ee ) {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                .createQuery( "select distinct cta from SingleCellExpressionDataVector scedv "
                        + "join scedv.singleCellDimension scd "
                        + "join scd.cellTypeAssignments cta "
                        + "where scedv.expressionExperiment = :ee" )
                .setParameter( "ee", ee )
                .list();
    }

    @Override
    public CellTypeAssignment getPreferredCellTypeAssignment( ExpressionExperiment ee ) {
        return ( CellTypeAssignment ) getSessionFactory().getCurrentSession()
                .createQuery( "select distinct cta from SingleCellExpressionDataVector scedv "
                        + "join scedv.singleCellDimension scd "
                        + "join scd.cellTypeAssignments cta "
                        + "where scedv.quantitationType.isSingleCellPreferred = true and cta.preferred = true and scedv.expressionExperiment = :ee" )
                .setParameter( "ee", ee )
                .uniqueResult();
    }

    @Nullable
    @Override
    public CellTypeAssignment getCellTypeAssignment( ExpressionExperiment expressionExperiment, QuantitationType qt, Long ctaId ) {
        return ( CellTypeAssignment ) getSessionFactory().getCurrentSession()
                .createQuery( "select distinct cta from SingleCellExpressionDataVector scedv "
                        + "join scedv.singleCellDimension scd "
                        + "join scd.cellTypeAssignments cta "
                        + "where scedv.quantitationType = :qt and cta.id = :ctaId and scedv.expressionExperiment = :ee" )
                .setParameter( "ee", expressionExperiment )
                .setParameter( "qt", qt )
                .setParameter( "ctaId", ctaId )
                .uniqueResult();
    }

    @Nullable
    @Override
    public CellTypeAssignment getCellTypeAssignment( ExpressionExperiment expressionExperiment, QuantitationType qt, String ctaName ) {
        return ( CellTypeAssignment ) getSessionFactory().getCurrentSession()
                .createQuery( "select distinct cta from SingleCellExpressionDataVector scedv "
                        + "join scedv.singleCellDimension scd "
                        + "join scd.cellTypeAssignments cta "
                        + "where scedv.quantitationType = :qt and cta.name = :ctaName and scedv.expressionExperiment = :ee" )
                .setParameter( "ee", expressionExperiment )
                .setParameter( "qt", qt )
                .setParameter( "ctaName", ctaName )
                .uniqueResult();
    }

    @Override
    public List<CellLevelCharacteristics> getCellLevelCharacteristics( ExpressionExperiment ee ) {
        List<CellLevelCharacteristics> results = new ArrayList<>( getCellTypeAssignments( ee ) );
        //noinspection unchecked
        results.addAll( getSessionFactory().getCurrentSession()
                .createQuery( "select distinct clc from SingleCellExpressionDataVector scedv join scedv.singleCellDimension scd join scd.cellLevelCharacteristics clc where scedv.expressionExperiment = :ee" )
                .setParameter( "ee", ee )
                .list() );
        return results;
    }

    @Override
    public List<CellLevelCharacteristics> getCellLevelCharacteristics( ExpressionExperiment ee, Category category ) {
        List<CellLevelCharacteristics> results = new ArrayList<>();
        if ( category.equals( Categories.CELL_TYPE ) ) {
            results.addAll( getCellTypeAssignments( ee ) );
        }
        //noinspection unchecked
        results.addAll( getSessionFactory().getCurrentSession()
                .createQuery( "select distinct clc from SingleCellExpressionDataVector scedv join scedv.singleCellDimension scd join scd.cellLevelCharacteristics clc join clc.characteristics c where scedv.expressionExperiment = :ee and coalesce(c.categoryUri, c.category) = :c" )
                .setParameter( "ee", ee )
                .setParameter( "c", category.getCategoryUri() != null ? category.getCategoryUri() : category.getCategory() )
                .list() );
        return results;
    }

    @Override
    public List<Characteristic> getCellTypes( ExpressionExperiment ee ) {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                .createQuery( "select distinct ct from SingleCellExpressionDataVector scedv "
                        + "join scedv.singleCellDimension scd "
                        + "join scd.cellTypeAssignments cta "
                        + "join cta.cellTypes ct "
                        + "where scedv.expressionExperiment = :ee and scedv.quantitationType.isSingleCellPreferred = true and cta.preferred = true" )
                .setParameter( "ee", ee )
                .list();
    }

    @Override
    public List<QuantitationType> getSingleCellQuantitationTypes( ExpressionExperiment ee ) {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                .createQuery( "select distinct scedv.quantitationType from SingleCellExpressionDataVector scedv "
                        + "where scedv.expressionExperiment = :ee" )
                .setParameter( "ee", ee )
                .list();
    }

    @Override
    public List<SingleCellExpressionDataVector> getSingleCellDataVectors( ExpressionExperiment expressionExperiment, QuantitationType quantitationType ) {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                .createQuery( "select scedv from SingleCellExpressionDataVector scedv "
                        + "where scedv.expressionExperiment = :ee and scedv.quantitationType = :qt" )
                .setParameter( "ee", expressionExperiment )
                .setParameter( "qt", quantitationType )
                .list();
    }

    @Override
    public Stream<SingleCellExpressionDataVector> streamSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType, int fetchSize ) {
        Session session = getSessionFactory().openSession();
        // prevent any changes to the database from entities originating from this session, this should be a read-only
        // thing unless stated otherwise
        session.setDefaultReadOnly( true );
        Query query = session.createQuery( "select scedv from SingleCellExpressionDataVector scedv "
                        + "where scedv.expressionExperiment = :ee and scedv.quantitationType = :qt" )
                .setParameter( "ee", ee )
                .setParameter( "qt", quantitationType );
        return QueryUtils.<SingleCellExpressionDataVector>stream( query, fetchSize ).onClose( session::close );
    }

    @Override
    public long getNumberOfSingleCellDataVectors( ExpressionExperiment ee, QuantitationType qt ) {
        return ( Long ) getSessionFactory().getCurrentSession().createQuery( "select count(scedv) from SingleCellExpressionDataVector scedv "
                        + "where scedv.expressionExperiment = :ee and scedv.quantitationType = :qt" )
                .setParameter( "ee", ee )
                .setParameter( "qt", qt )
                .uniqueResult();
    }

    @Override
    public long getNumberOfNonZeroes( ExpressionExperiment ee, QuantitationType qt ) {
        return ( Long ) getSessionFactory().getCurrentSession()
                .createQuery( "select sum(length(scedv.dataIndices)) from SingleCellExpressionDataVector scedv "
                        + "where scedv.expressionExperiment = :ee and scedv.quantitationType = :qt" )
                .setParameter( "ee", ee )
                .setParameter( "qt", qt )
                .uniqueResult() / 4;
    }

    @Override
    public Map<BioAssay, Long> getNumberOfNonZeroesBySample( ExpressionExperiment ee, QuantitationType qt, int fetchSize ) {
        SingleCellDimension dimension = getSingleCellDimension( ee, qt );
        if ( dimension == null ) {
            throw new IllegalStateException( qt + " from " + ee + " does not have an associated single-cell dimension." );
        }
        long numVecs = getNumberOfSingleCellDataVectors( ee, qt );
        try ( Stream<int[]> stream = QueryUtils.stream( getSessionFactory().getCurrentSession()
                .createQuery( "select scedv.dataIndices from SingleCellExpressionDataVector scedv "
                        + "where scedv.expressionExperiment = :ee and scedv.quantitationType = :qt" )
                .setParameter( "ee", ee )
                .setParameter( "qt", qt ), fetchSize ) ) {
            long[] nnzs = new long[dimension.getBioAssays().size()];
            Iterator<int[]> it = stream.iterator();
            StopWatch timer = StopWatch.createStarted();
            int done = 0;
            while ( it.hasNext() ) {
                int[] row = it.next();
                // the first sample starts at zero, the use the end as the start of the next one
                int start = 0;
                for ( int i = 0; i < dimension.getBioAssays().size(); i++ ) {
                    int end = SingleCellExpressionDataVectorUtils.getSampleEnd( dimension, row, i, start );
                    nnzs[i] += end - start;
                    start = end;
                }
                if ( ++done % 1000 == 0 ) {
                    log.info( String.format( "Processed %d/%d vectors at %.2f vectors/sec", done, numVecs, 1000.0 * done / timer.getTime() ) );
                }
            }
            Map<BioAssay, Long> result = new HashMap<>();
            for ( int i = 0; i < dimension.getBioAssays().size(); i++ ) {
                result.put( dimension.getBioAssays().get( i ), nnzs[i] );
            }
            return result;
        }
    }

    @Override
    public int removeSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType, boolean deleteQt ) {
        Assert.notNull( quantitationType.getId(), "The quantitation type must be persistent." );
        Assert.isTrue( ee.getQuantitationTypes().contains( quantitationType ) || ee.getSingleCellExpressionDataVectors().stream().anyMatch( v -> v.getQuantitationType().equals( quantitationType ) ),
                "The quantitation must belong to at least one single-cell vector from experiment." );
        ee.getSingleCellExpressionDataVectors()
                .removeIf( v -> v.getQuantitationType().equals( quantitationType ) );
        int deletedVectors = getSessionFactory().getCurrentSession()
                .createQuery( "delete from SingleCellExpressionDataVector v where v.expressionExperiment = :ee and v.quantitationType = :qt" )
                .setParameter( "ee", ee )
                .setParameter( "qt", quantitationType )
                .executeUpdate();
        if ( deleteQt ) {
            log.info( "Deleting " + quantitationType + "..." );
            if ( !ee.getQuantitationTypes().remove( quantitationType ) ) {
                log.warn( quantitationType + " was not attached to " + ee + ", but was attached to at least one of its single-cell data vectors, it will be removed." );
            }
            getSessionFactory().getCurrentSession().delete( quantitationType );
        }
        log.info( "Removed " + deletedVectors + " single-cell data vectors from " + ee + " for " + quantitationType );
        return deletedVectors;
    }

    @Override
    public int removeAllSingleCellDataVectors( ExpressionExperiment ee ) {
        Set<QuantitationType> qtsToRemove = ee.getSingleCellExpressionDataVectors().stream()
                .map( SingleCellExpressionDataVector::getQuantitationType )
                .collect( Collectors.toSet() );
        ee.getSingleCellExpressionDataVectors().clear();
        int deletedVectors = getSessionFactory().getCurrentSession()
                .createQuery( "delete from SingleCellExpressionDataVector v where v.expressionExperiment = :ee" )
                .setParameter( "ee", ee )
                .executeUpdate();
        for ( QuantitationType qt : qtsToRemove ) {
            if ( !ee.getQuantitationTypes().remove( qt ) ) {
                log.warn( qt + " was not attached to " + ee + ", but was attached to at least one of its single-cell data vectors, it will be removed." );
            }
            getSessionFactory().getCurrentSession().delete( qt );
        }
        log.info( "Removed " + deletedVectors + " single-cell data vectors from " + ee );
        return deletedVectors;
    }

    @Override
    protected Query getFilteringQuery( @Nullable Filters filters, @Nullable Sort sort ) {
        // the constants for aliases are messing with the inspector
        //language=HQL
        return finishFilteringQuery( "select ee, " + AclQueryUtils.AOI_ALIAS + ", " + AclQueryUtils.SID_ALIAS + " "
                + "from ExpressionExperiment as ee "
                + "left join fetch ee.accession acc "
                + "left join fetch ee.experimentalDesign as EDES "
                + "left join fetch ee.curationDetails as s " /* needed for trouble status */
                + "left join fetch s.lastNeedsAttentionEvent as eAttn "
                + "left join fetch eAttn.eventType "
                + "left join fetch s.lastNoteUpdateEvent as eNote "
                + "left join fetch eNote.eventType "
                + "left join fetch s.lastTroubledEvent as eTrbl "
                + "left join fetch eTrbl.eventType "
                + "left join fetch ee.geeq as geeq", filters, sort, groupByIfNecessary( sort, ONE_TO_MANY_ALIASES ) );
    }

    @Override
    protected void initializeCachedFilteringResult( ExpressionExperiment ee ) {
        Hibernate.initialize( ee.getAccession() );
        Hibernate.initialize( ee.getExperimentalDesign() );
        Hibernate.initialize( ee.getCurationDetails() );
        Hibernate.initialize( ee.getGeeq() );
        Hibernate.initialize( ee.getCharacteristics() );
    }

    @Override
    protected Query getFilteringIdQuery( @Nullable Filters filters, @Nullable Sort sort ) {
        //language=HQL
        return finishFilteringQuery( "select ee.id "
                + "from ExpressionExperiment as ee "
                + "left join ee.accession acc "
                + "left join ee.experimentalDesign as EDES "
                + "left join ee.curationDetails as s " /* needed for trouble status */
                + "left join s.lastNeedsAttentionEvent as eAttn "
                + "left join s.lastNoteUpdateEvent as eNote "
                + "left join s.lastTroubledEvent as eTrbl "
                + "left join ee.geeq as geeq", filters, sort, groupByIfNecessary( sort, ONE_TO_MANY_ALIASES ) );
    }

    @Override
    protected Query getFilteringCountQuery( @Nullable Filters filters ) {
        //language=HQL
        return finishFilteringQuery( "select count(" + distinctIfNecessary() + "ee) "
                + "from ExpressionExperiment as ee "
                + "left join ee.accession acc "
                + "left join ee.experimentalDesign as EDES "
                + "left join ee.curationDetails as s " /* needed for trouble status */
                + "left join s.lastNeedsAttentionEvent as eAttn "
                + "left join s.lastNoteUpdateEvent as eNote "
                + "left join s.lastTroubledEvent as eTrbl "
                + "left join ee.geeq as geeq", filters, null, null );
    }

    private Query finishFilteringQuery( String queryString, @Nullable Filters filters, @Nullable Sort sort, @Nullable String groupBy ) {
        if ( filters == null ) {
            filters = Filters.empty();
        } else {
            filters = Filters.by( filters );
        }

        // Restrict to non-troubled EEs for non-administrators
        addNonTroubledFilter( filters, OBJECT_ALIAS );
        // Filtering by AD is costly because we need two jointures, so the results might in some rare cases return EEs
        // from troubled platforms that have not been mark themselves as troubled. Thus, we do it only if it is present
        // in the query
        if ( FiltersUtils.containsAnyAlias( filters, sort, ARRAY_DESIGN_ALIAS ) ) {
            addNonTroubledFilter( filters, ARRAY_DESIGN_ALIAS );
        }

        // The following two associations are retrieved eagerly via select, which is far more efficient since they are
        // commonly shared across EEs and may benefit from the second-level cache
        if ( FiltersUtils.containsAnyAlias( filters, sort, EXTERNAL_DATABASE_ALIAS ) ) {
            queryString += " left join acc.externalDatabase as " + EXTERNAL_DATABASE_ALIAS;  // this one will be fetched in a subsequent select clause since it's eagerly fetched from DatabaseEntry
        }
        if ( FiltersUtils.containsAnyAlias( filters, sort, TAXON_ALIAS ) ) {
            queryString += " left join ee.taxon as " + TAXON_ALIAS;
        }

        // fetching characteristics, bioAssays and arrayDesignUsed is costly, so we reserve these operations only if it
        // is mentioned in the filters

        if ( FiltersUtils.containsAnyAlias( null, sort, CHARACTERISTIC_ALIAS ) ) {
            queryString += " left join ee.characteristics as " + CHARACTERISTIC_ALIAS;
        }

        if ( FiltersUtils.containsAnyAlias( null, sort, FACTOR_VALUE_CHARACTERISTIC_ALIAS ) ) {
            queryString += " left join ee.experimentalDesign.experimentalFactors as ef";
            queryString += " left join ef.factorValues as fv";
            queryString += " left join fv.characteristics as " + FACTOR_VALUE_CHARACTERISTIC_ALIAS;
        }

        if ( FiltersUtils.containsAnyAlias( null, sort, BIO_ASSAY_ALIAS, BIO_MATERIAL_CHARACTERISTIC_ALIAS, ARRAY_DESIGN_ALIAS ) ) {
            queryString += " left join ee.bioAssays as " + BIO_ASSAY_ALIAS;
        }

        // this is a shorthand for all characteristics (direct + biomaterial + experimental design)
        if ( FiltersUtils.containsAnyAlias( null, sort, ALL_CHARACTERISTIC_ALIAS ) ) {
            queryString += " left join ee.allCharacteristics as " + ALL_CHARACTERISTIC_ALIAS;
        }

        if ( FiltersUtils.containsAnyAlias( null, sort, BIO_MATERIAL_CHARACTERISTIC_ALIAS ) ) {
            queryString += " left join " + BIO_ASSAY_ALIAS + ".sampleUsed.characteristics as " + BIO_MATERIAL_CHARACTERISTIC_ALIAS;
        }

        if ( FiltersUtils.containsAnyAlias( null, sort, ArrayDesignDao.OBJECT_ALIAS ) ) {
            queryString += " left join " + BIO_ASSAY_ALIAS + ".arrayDesignUsed as " + ARRAY_DESIGN_ALIAS;
        }

        // parts of this query (above) are only needed for administrators: the notes, so it could theoretically be sped up even more
        queryString += AclQueryUtils.formAclRestrictionClause( OBJECT_ALIAS + ".id" );

        queryString += FilterQueryUtils.formRestrictionClause( filters );

        if ( groupBy != null ) {
            queryString += " group by " + groupBy;
        }

        queryString += FilterQueryUtils.formOrderByClause( sort );

        Query query = this.getSessionFactory().getCurrentSession().createQuery( queryString );

        AclQueryUtils.addAclParameters( query, ExpressionExperiment.class );
        FilterQueryUtils.addRestrictionParameters( query, filters );

        return query;
    }

    @Override
    protected void configureFilterableProperties( FilterablePropertiesConfigurer configurer ) {
        super.configureFilterableProperties( configurer );

        configurer.registerProperties( "taxon", "bioAssayCount" );

        // irrelevant
        configurer.unregisterProperty( "accession.Uri" );
        configurer.unregisterProperty( "geeq.id" );
        configurer.unregisterProperty( "source" );
        configurer.unregisterProperty( "otherParts.size" );
        configurer.unregisterProperty( "otherRelevantPublications.size" );

        configurer.unregisterProperties( p -> p.endsWith( "externalDatabases.size" ) );

        // reserved for curators
        configurer.unregisterProperty( "curationDetails.curationNote" );

        // only expose selected fields for GEEQ
        configurer.unregisterEntity( "geeq.", Geeq.class );
        configurer.registerProperty( "geeq.publicQualityScore" );
        configurer.registerProperty( "geeq.publicSuitabilityScore" );

        // the primary publication is not very useful, but its attached database entry is
        configurer.unregisterEntity( "primaryPublication.", BibliographicReference.class );
        configurer.registerEntity( "primaryPublication.pubAccession.", DatabaseEntry.class, 2 );
        configurer.unregisterProperty( "primaryPublication.pubAccession.Uri" );

        // attached terms
        configurer.registerAlias( "characteristics.", CHARACTERISTIC_ALIAS, Characteristic.class, null, 1, true );
        configurer.unregisterProperty( "characteristics.originalValue" );
        configurer.unregisterProperty( "characteristics.migratedToStatement" );
        configurer.registerAlias( "experimentalDesign.experimentalFactors.factorValues.characteristics.", FACTOR_VALUE_CHARACTERISTIC_ALIAS, Characteristic.class, null, 1, true );
        configurer.unregisterProperty( "experimentalDesign.experimentalFactors.factorValues.characteristics.originalValue" );
        configurer.unregisterProperty( "experimentalDesign.experimentalFactors.factorValues.characteristics.migratedToStatement" );
        configurer.registerAlias( "bioAssays.sampleUsed.characteristics.", BIO_MATERIAL_CHARACTERISTIC_ALIAS, Characteristic.class, null, 1, true );
        configurer.unregisterProperty( "bioAssays.sampleUsed.characteristics.migratedToStatement" );
        configurer.unregisterProperty( "bioAssays.sampleUsed.characteristics.originalValue" );
        configurer.registerAlias( "allCharacteristics.", ALL_CHARACTERISTIC_ALIAS, Characteristic.class, null, 1, true );
        configurer.unregisterProperty( "allCharacteristics.originalValue" );
        configurer.unregisterProperty( "allCharacteristics.migratedToStatement" );

        configurer.registerAlias( "bioAssays.", BIO_ASSAY_ALIAS, BioAssay.class, null, 2, true );
        configurer.unregisterProperty( "bioAssays.accession.Uri" );
        configurer.unregisterProperty( "bioAssays.sampleUsed.factorValues.size" );
        configurer.unregisterProperty( "bioAssays.sampleUsed.treatments.size" );

        // this is not useful, unless we add an alias to the alternate names
        configurer.unregisterProperties( p -> p.endsWith( "alternateNames.size" ) );
    }

    /**
     * Checks for special properties that are allowed to be referenced on certain objects. E.g. characteristics on EEs.
     * {@inheritDoc}
     */
    @Override
    protected FilterablePropertyMeta getFilterablePropertyMeta( String propertyName ) {
        switch ( propertyName ) {
            case "taxon":
                return getFilterablePropertyMeta( TAXON_ALIAS, "id", Taxon.class )
                        .withDescription( "alias for taxon.id" );
            case "bioAssayCount":
                return super.getFilterablePropertyMeta( "bioAssays.size" )
                        .withDescription( "alias for bioAssays.size" );
            case "geeq.publicQualityScore":
                return new FilterablePropertyMeta( null, "(case when geeq.manualQualityOverride = true then geeq.manualQualityScore else geeq.detectedQualityScore end)", Double.class, null, null );
            case "geeq.publicSuitabilityScore":
                return new FilterablePropertyMeta( null, "(case when geeq.manualSuitabilityOverride = true then geeq.manualSuitabilityScore else geeq.detectedSuitabilityScore end)", Double.class, null, null );
            default:
                return super.getFilterablePropertyMeta( propertyName );
        }
    }

    /**
     * Filling 'hasDifferentialExpressionAnalysis' and 'hasCoexpressionAnalysis'
     */
    private void populateAnalysisInformation( Collection<ExpressionExperimentDetailsValueObject> vos, boolean cacheable ) {
        if ( vos.isEmpty() ) {
            return;
        }

        // these are cached queries (thus super-fast)
        Set<Long> withCoexpression = new HashSet<>( getExpressionExperimentIdsWithCoexpression( cacheable ) );
        Set<Long> withDiffEx = new HashSet<>( getExpressionExperimentIdsWithDifferentialExpressionAnalysis( cacheable ) );

        for ( ExpressionExperimentDetailsValueObject vo : vos ) {
            vo.setHasCoexpressionAnalysis( withCoexpression.contains( vo.getId() ) );
            vo.setHasDifferentialExpressionAnalysis( withDiffEx.contains( vo.getId() ) );
        }
    }

    private List<Long> getExpressionExperimentIdsWithCoexpression( boolean cacheable ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select experimentAnalyzed.id from CoexpressionAnalysis" )
                .setCacheable( cacheable )
                .list();
    }

    private List<Long> getExpressionExperimentIdsWithDifferentialExpressionAnalysis( boolean cacheable ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select experimentAnalyzed.id from DifferentialExpressionAnalysis" )
                .setCacheable( cacheable )
                .list();
    }

    private void populateArrayDesignCount( Collection<ExpressionExperimentValueObject> eevos ) {
        if ( eevos.isEmpty() ) {
            return;
        }
        Query q = getSessionFactory().getCurrentSession()
                .createQuery( "select ee.id, count(distinct ba.arrayDesignUsed) from ExpressionExperiment ee "
                        + "join ee.bioAssays as ba "
                        + "where ee.id in (:ids) "
                        + "group by ee" )
                .setCacheable( true );
        Map<Long, Long> adCountById = streamByBatch( q, "ids", IdentifiableUtils.getIds( eevos ), 2048, Object[].class )
                .collect( Collectors.toMap( row -> ( Long ) row[0], row -> ( Long ) row[1] ) );
        for ( ExpressionExperimentValueObject eevo : eevos ) {
            eevo.setArrayDesignCount( adCountById.getOrDefault( eevo.getId(), 0L ) );
        }
    }

    @Override
    public Collection<RawExpressionDataVector> getRawDataVectors( ExpressionExperiment ee, QuantitationType qt ) {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                .createQuery( "select v from RawExpressionDataVector v where v.expressionExperiment = :ee and v.quantitationType = :qt" )
                .setParameter( "ee", ee )
                .setParameter( "qt", qt )
                .list();
    }

    @Override
    public int addRawDataVectors( ExpressionExperiment ee, QuantitationType newQt, Collection<RawExpressionDataVector> newVectors ) {
        Assert.notNull( ee.getId(), "ExpressionExperiment must be persistent." );
        Assert.isTrue( !newVectors.isEmpty(), "At least one vectors must be provided, use removeAllRawDataVectors() to delete vectors instead." );
        // each set of raw vectors must have a *distinct* QT
        Assert.isTrue( !ee.getQuantitationTypes().contains( newQt ),
                "ExpressionExperiment already has a quantitation like " + newQt );
        checkVectors( ee, newQt, newVectors );
        if ( newQt.getId() == null ) {
            log.info( "Creating " + newQt + "..." );
            getSessionFactory().getCurrentSession().persist( newQt );
        }
        if ( newQt.getIsPreferred() ) {
            for ( QuantitationType qt : ee.getQuantitationTypes() ) {
                if ( !qt.equals( newQt ) ) {
                    log.info( "Unmarking " + qt + " as preferred for " + ee + ", a new set of preferred raw vectors will be added." );
                    qt.setIsPreferred( false );
                    // we could break here since there is at most one, but we might as well check all of them and fix
                    // incorrect QTs
                }
            }
        }
        // this is a denormalization; easy to forget to update this.
        ee.getQuantitationTypes().add( newQt );
        ee.getRawExpressionDataVectors().addAll( newVectors );
        update( ee );
        log.info( "Added " + newVectors.size() + " raw vectors to " + ee + " for " + newQt );
        return newVectors.size();
    }

    @Override
    public int removeAllRawDataVectors( ExpressionExperiment ee ) {
        Assert.notNull( ee.getId(), "ExpressionExperiment must be persistent." );
        // ensures the EE is in the session before we retrieve QTs to delete
        update( ee );
        //noinspection unchecked
        List<QuantitationType> qtsToRemove = this.getSessionFactory().getCurrentSession().createQuery(
                        "select q from ExpressionExperiment e "
                                + "join e.rawExpressionDataVectors p "
                                + "join p.quantitationType q "
                                + "where e = :ee "
                                + "group by q" )
                .setParameter( "ee", ee )
                .list();
        ee.getRawExpressionDataVectors().clear();
        int deletedVectors = getSessionFactory().getCurrentSession()
                .createQuery( "delete from RawExpressionDataVector v where v.expressionExperiment = :ee" )
                .setParameter( "ee", ee )
                .executeUpdate();
        // remove QTs
        for ( QuantitationType qt : qtsToRemove ) {
            if ( !ee.getQuantitationTypes().remove( qt ) ) {
                log.warn( qt + " was not attached to " + ee + ", but was associated to at least one of its raw vector, it will be removed directly." );
                getSessionFactory().getCurrentSession().delete( qt );
            }
        }
        if ( deletedVectors > 0 ) {
            log.info( "Deleted all " + deletedVectors + " raw data vectors from " + ee + " for " + qtsToRemove.size() + " quantitation types." );
        }
        return deletedVectors;
    }

    @Override
    public int removeRawDataVectors( ExpressionExperiment ee, QuantitationType qt ) {
        Assert.notNull( ee.getId(), "ExpressionExperiment must be persistent." );
        Assert.notNull( qt.getId(), "Quantitation type must be persistent" );
        Assert.isTrue( ee.getQuantitationTypes().contains( qt ) || ee.getRawExpressionDataVectors().stream().anyMatch( v -> v.getQuantitationType().equals( qt ) ),
                "The provided quantitation type must belong to at least one raw vector of the experiment." );
        ee.getRawExpressionDataVectors().clear();
        int deletedVectors = getSessionFactory().getCurrentSession()
                .createQuery( "delete from RawExpressionDataVector v where v.expressionExperiment = :ee and v.quantitationType = :qt" )
                .setParameter( "ee", ee )
                .setParameter( "qt", qt )
                .executeUpdate();
        if ( !ee.getQuantitationTypes().remove( qt ) ) {
            log.warn( qt + " was not attached to " + ee + ", but was associated to at least one of its raw vectors, it will be removed directly." );
            getSessionFactory().getCurrentSession().delete( qt );
        }
        update( ee );
        if ( deletedVectors > 0 ) {
            log.info( "Deleted " + deletedVectors + " raw data vectors from " + ee + " for " + qt );
        }
        return deletedVectors;
    }

    @Override
    public int replaceRawDataVectors( ExpressionExperiment ee, QuantitationType qt, Collection<RawExpressionDataVector> vectors ) {
        Assert.notNull( ee.getId(), "ExpressionExperiment must be persistent." );
        Assert.notNull( qt.getId(), "Quantitation type must be persistent" );
        Assert.isTrue( ee.getQuantitationTypes().contains( qt ) || ee.getRawExpressionDataVectors().stream().anyMatch( v -> v.getQuantitationType().equals( qt ) ),
                "The provided quantitation type must belong to at least one vector of the experiment." );
        checkVectors( ee, qt, vectors );
        ee.getRawExpressionDataVectors().removeIf( v -> v.getQuantitationType().equals( qt ) );
        int deletedVectors = getSessionFactory().getCurrentSession()
                .createQuery( "delete from RawExpressionDataVector v where v.expressionExperiment = :ee and v.quantitationType = :qt" )
                .setParameter( "ee", ee )
                .setParameter( "qt", qt )
                .executeUpdate();
        // in case it was attached to the previous vectors, but not the EE QTs
        if ( ee.getQuantitationTypes().add( qt ) ) {
            log.warn( qt + " was not attached to " + ee + ", but was associated to at least one of its replaced raw vectors, it will be added directly." );
        }
        ee.getRawExpressionDataVectors().addAll( vectors );
        update( ee );
        if ( deletedVectors > 0 ) {
            log.info( "Replaced " + deletedVectors + " raw data vectors from " + ee + " for " + qt );
        }
        return deletedVectors;
    }

    @Override
    public int createProcessedDataVectors( ExpressionExperiment ee, Collection<ProcessedExpressionDataVector> vectors ) {
        Assert.notNull( ee.getId(), "ExpressionExperiment must be persistent." );
        Assert.isTrue( ee.getProcessedExpressionDataVectors().isEmpty(), "ExpressionExperiment already has processed vectors, remove them before creating new ones or use replaceProcessedDataVectors()." );
        Assert.isTrue( !vectors.isEmpty(), "At least one vector must be provided." );
        QuantitationType qt = vectors.iterator().next().getQuantitationType();
        Assert.isTrue( qt.getIsMaskedPreferred(), "QuantitationType must be marked as masked preferred." );
        checkVectors( ee, qt, vectors );
        if ( qt.getId() == null ) {
            log.info( "Creating " + qt + "..." );
            getSessionFactory().getCurrentSession().persist( qt );
        }
        ee.getQuantitationTypes().add( qt );
        ee.getProcessedExpressionDataVectors().addAll( vectors );
        ee.setNumberOfDataVectors( vectors.size() );
        update( ee );
        return vectors.size();
    }

    @Override
    public int removeProcessedDataVectors( ExpressionExperiment ee ) {
        Assert.notNull( ee.getId(), "ExpressionExperiment must be persistent." );

        // this is only necessary if EE is detached, in this case the QTs from the experiment will conflict with freshly
        // retrieved QTs from the database
        update( ee );

        // obtain QTs to remove directly from the vectors
        //noinspection unchecked
        List<QuantitationType> qtsToRemove = this.getSessionFactory().getCurrentSession().createQuery(
                        "select q from ExpressionExperiment e "
                                + "join e.processedExpressionDataVectors p "
                                + "join p.quantitationType q "
                                + "where e = :ee "
                                + "group by q" )
                .setParameter( "ee", ee )
                .list();

        // this is not really allowed, but it might happen
        if ( qtsToRemove.size() > 1 ) {
            log.warn( ee + " has more than one QT associated to its processed vectors, they will all be removed." );
        }

        ee.getProcessedExpressionDataVectors().clear();
        ee.setNumberOfDataVectors( 0 );

        int deletedVectors = this.getSessionFactory().getCurrentSession()
                .createQuery( "delete from ProcessedExpressionDataVector pv where pv.expressionExperiment = :ee" )
                .setParameter( "ee", ee )
                .executeUpdate();

        // remove QTs
        for ( QuantitationType qt : qtsToRemove ) {
            if ( !ee.getQuantitationTypes().remove( qt ) ) {
                log.warn( qt + " was not attached to " + ee + ", but was associated to at least one of its processed vector, it will be removed directly." );
                getSessionFactory().getCurrentSession().delete( qt );
            }
        }

        if ( deletedVectors > 0 ) {
            log.info( "Deleted " + deletedVectors + " processed data vectors from " + ee + " for " + qtsToRemove.size() + " quantitation types." );
        }

        return deletedVectors;
    }

    @Override
    public int replaceProcessedDataVectors( ExpressionExperiment ee, Collection<ProcessedExpressionDataVector> vectors ) {
        Assert.notNull( ee.getId(), "ExpressionExperiment must be persistent." );
        Assert.isTrue( !vectors.isEmpty(), "At least one vector must be provided when replacing data, use removeProcessedDataVectors() for removing vectors." );
        QuantitationType newQt = vectors.iterator().next().getQuantitationType();
        Assert.isTrue( newQt.getIsMaskedPreferred(), "QuantitationType must be marked as masked preferred." );
        checkVectors( ee, newQt, vectors );
        if ( newQt.getId() == null ) {
            log.info( "Creating " + newQt + "..." );
            getSessionFactory().getCurrentSession().persist( newQt );
        }
        // this is only necessary if EE is detached, in this case the QTs from the experiment will conflict with freshly
        // retrieved QTs from the database
        update( ee );
        //noinspection unchecked
        List<QuantitationType> qtsToRemove = this.getSessionFactory().getCurrentSession()
                .createQuery( "select q from ExpressionExperiment e "
                        + "join e.processedExpressionDataVectors p "
                        + "join p.quantitationType q "
                        + "where e = :ee "
                        + "group by q" )
                .setParameter( "ee", ee )
                .list();
        if ( qtsToRemove.remove( newQt ) ) {
            log.info( newQt + " is being reused, will not remove it." );
        }
        ee.getProcessedExpressionDataVectors().clear();
        int deletedVectors = this.getSessionFactory().getCurrentSession()
                .createQuery( "delete from ProcessedExpressionDataVector pv where pv.expressionExperiment = :ee" )
                .setParameter( "ee", ee )
                .executeUpdate();
        ee.getProcessedExpressionDataVectors().addAll( vectors );
        ee.setNumberOfDataVectors( vectors.size() );
        ee.getQuantitationTypes().add( newQt );
        // remove QTs
        for ( QuantitationType qt : qtsToRemove ) {
            if ( !ee.getQuantitationTypes().remove( qt ) ) {
                log.warn( qt + " was not attached to " + ee + ", but was associated to at least one of its processed vector, it will be removed." );
                getSessionFactory().getCurrentSession().delete( qt );
            }
        }
        if ( deletedVectors > 0 ) {
            log.info( "Replaced " + deletedVectors + " from " + ee + " for " + newQt );
        }
        return deletedVectors;
    }

    /**
     * Perform a few sanity checks on a collection of vectors to create.
     * <p>
     * In particular, we ensure that the followings are true:
     * <ul>
     *     <li>all vectors are transient</li>
     *     <li>all vectors are linked to the expression experiment</li>
     *     <li>all vectors share the same bioassay dimension</li>
     *     <li>all vectors share the same quantitation type</li>
     *     <li>all vectors have expected size as per the bioassay dimension and storage requirement</li>
     * </ul>
     */
    private void checkVectors( ExpressionExperiment ee, QuantitationType qt, Collection<? extends BulkExpressionDataVector> vectors ) {
        BioAssayDimension bad = vectors.iterator().next().getBioAssayDimension();
        Assert.notNull( bad.getId(), "Vectors must have a persistent dimension." );
        // a BA might belong to a subset of the experiment for single-cell experiments, so we only check direct BAs that
        // are not derived
        Set<BioAssay> directBas = bad.getBioAssays().stream()
                .filter( ba -> ba.getSampleUsed().getSourceBioMaterial() == null )
                .collect( Collectors.toSet() );
        Assert.isTrue( ee.getBioAssays().containsAll( directBas ), "The BioAssayDimension must be a subset of the experiment's BioAssays." );
        Assert.isTrue( vectors.stream().allMatch( v -> v.getId() == null ), "All vectors must be transient" );
        Assert.isTrue( vectors.stream().map( DataVector::getExpressionExperiment ).allMatch( e -> e == ee ), "All vectors must belong to " + ee );
        Assert.isTrue( vectors.stream().map( DesignElementDataVector::getQuantitationType ).allMatch( q -> q == qt ), "All vectors must use " + qt );
        Assert.isTrue( vectors.stream().map( BulkExpressionDataVector::getBioAssayDimension ).allMatch( b -> b == bad ), "All vectors must use " + bad );
        if ( qt.getRepresentation().getSizeInBytes() != -1 ) {
            int expectedVectorSizeInBytes = bad.getBioAssays().size() * qt.getRepresentation().getSizeInBytes();
            Assert.isTrue( vectors.stream().map( DesignElementDataVector::getData ).allMatch( b -> b.length == expectedVectorSizeInBytes ),
                    "All vectors must contain " + bad.getBioAssays().size() + " values, expected size is " + expectedVectorSizeInBytes + " B." );
        }
    }
}
