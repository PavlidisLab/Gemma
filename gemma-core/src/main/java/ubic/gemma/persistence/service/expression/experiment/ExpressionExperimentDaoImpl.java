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
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.stream.Streams;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.*;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.CustomType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;
import ubic.gemma.core.analysis.singleCell.SingleCellMaskUtils;
import ubic.gemma.core.profiling.StopWatchUtils;
import ubic.gemma.core.util.ListUtils;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.SampleRemovalEvent;
import ubic.gemma.model.common.description.*;
import ubic.gemma.model.common.protocol.Protocol;
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
import ubic.gemma.model.util.UninitializedList;
import ubic.gemma.model.util.UninitializedSet;
import ubic.gemma.persistence.hibernate.CompressedStringListType;
import ubic.gemma.persistence.hibernate.TypedResultTransformer;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.AbstractCuratableDao;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignDao;
import ubic.gemma.persistence.service.genome.taxon.TaxonDao;
import ubic.gemma.persistence.util.*;
import ubic.gemma.persistence.util.Filter;

import javax.annotation.Nullable;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingLong;
import static org.hibernate.transform.Transformers.aliasToBean;
import static ubic.gemma.core.datastructure.sparse.SparseListUtils.validateSparseRangeArray;
import static ubic.gemma.core.util.NetUtils.bytePerSecondToDisplaySize;
import static ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil.*;
import static ubic.gemma.persistence.util.QueryUtils.*;
import static ubic.gemma.persistence.util.Thaws.thawBibliographicReference;
import static ubic.gemma.persistence.util.Thaws.thawDatabaseEntry;

/**
 * @author pavlidis
 * @see ubic.gemma.model.expression.experiment.ExpressionExperiment
 */
@Repository
public class ExpressionExperimentDaoImpl
        extends AbstractCuratableDao<ExpressionExperiment, ExpressionExperimentValueObject>
        implements ExpressionExperimentDao {

    private static final String
            CHARACTERISTIC_ALIAS = "ch",
            BIO_MATERIAL_CHARACTERISTIC_ALIAS = "bmc",
            FACTOR_VALUE_CHARACTERISTIC_ALIAS = "fvc",
            ALL_CHARACTERISTIC_ALIAS = "ac",
            BIO_ASSAY_ALIAS = "ba",
            TAXON_ALIAS = TaxonDao.OBJECT_ALIAS,
            ARRAY_DESIGN_ALIAS = ArrayDesignDao.OBJECT_ALIAS,
            EXTERNAL_DATABASE_ALIAS = "ED";

    /**
     * Aliases applicable for one-to-many relations.
     */
    private static final String[] ONE_TO_MANY_ALIASES = { CHARACTERISTIC_ALIAS, BIO_MATERIAL_CHARACTERISTIC_ALIAS,
            FACTOR_VALUE_CHARACTERISTIC_ALIAS, ALL_CHARACTERISTIC_ALIAS, BIO_ASSAY_ALIAS, ARRAY_DESIGN_ALIAS };

    /**
     * A set of all vectors type that are considered bulk.
     * <p>
     * Those will possess a {@link BioAssayDimension}.
     */
    private final Set<Class<? extends BulkExpressionDataVector>> bulkDataVectorTypes;

    private final CompressedStringListType cellIdsUserType;

    @Autowired
    public ExpressionExperimentDaoImpl( SessionFactory sessionFactory ) {
        super( ExpressionExperimentDao.OBJECT_ALIAS, ExpressionExperiment.class, sessionFactory );
        //noinspection unchecked
        bulkDataVectorTypes = getSessionFactory().getAllClassMetadata().values().stream()
                .map( ClassMetadata::getMappedClass )
                .filter( BulkExpressionDataVector.class::isAssignableFrom )
                .map( clazz -> ( Class<? extends BulkExpressionDataVector> ) clazz )
                .collect( Collectors.toSet() );
        Type type = getSessionFactory().getClassMetadata( SingleCellDimension.class )
                .getPropertyType( "cellIds" );
        cellIdsUserType = ( CompressedStringListType ) ( ( CustomType ) type ).getUserType();
    }

    @Override
    public ExpressionExperiment load( Long id, CacheMode cacheMode ) {
        Session session = getSessionFactory().getCurrentSession();
        session.setCacheMode( cacheMode );
        return super.load( id );
    }

    @Override
    public void evictCharacteristicsCache( ExpressionExperiment ee ) {
        getSessionFactory().getCache().evictCollection( Investigation.class.getName() + ".characteristics", ee.getId() );
    }

    @Override
    public void evictBioAssaysCache( ExpressionExperiment ee ) {
        getSessionFactory().getCache().evictCollection( getEntityName() + ".bioAssays", ee.getId() );
    }

    @Override
    public void evictOtherPartsCache( ExpressionExperiment ee ) {
        getSessionFactory().getCache().evictCollection( getEntityName() + ".otherParts", ee.getId() );
    }

    @Override
    public void evictQuantitationTypesCache( ExpressionExperiment ee ) {
        getSessionFactory().getCache().evictCollection( getEntityName() + ".quantitationTypes", ee.getId() );
    }

    @Override
    public List<Identifiers> loadAllIdentifiers() {
        Query query = getSessionFactory().getCurrentSession()
                .createQuery( "select ee.id as id, ee.shortName as shortName, ee.name as name, accession.accession as accession from ExpressionExperiment ee "
                        + "left join ee.accession accession "
                        + AclQueryUtils.formAclRestrictionClause( "ee.id" ) );
        AclQueryUtils.addAclParameters( query, ExpressionExperiment.class );
        //noinspection unchecked
        return query.setResultTransformer( aliasToBean( Identifiers.class ) ).list();
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
    public Collection<Long> filterByTaxon( @Nullable Collection<Long> ids, Taxon taxon ) {
        if ( ids == null || ids.isEmpty() )
            return Collections.emptySet();
        return QueryUtils.listByBatch( this.getSessionFactory().getCurrentSession()
                .createQuery( "select ee.id from ExpressionExperiment as ee "
                        + "join ee.bioAssays as ba "
                        + "join ba.sampleUsed as sample "
                        + "where sample.sourceTaxon = :taxon and ee.id in (:ids) "
                        + "group by ee" )
                .setParameter( "taxon", taxon ), "ids", ids, MAX_PARAMETER_LIST_SIZE );
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
                .createQuery( "select ee from ExpressionExperiment as ee "
                        + "left join ee.otherRelevantPublications as orp "
                        + "where ee.primaryPublication = :bibRef or orp = :bibRef "
                        + "group by ee" )
                .setParameter( "bibRef", bibRef )
                .list();
    }

    @Override
    public ExpressionExperiment findByBioAssay( BioAssay ba ) {
        return ( ExpressionExperiment ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select ee from ExpressionExperiment as ee "
                        + "join ee.bioAssays as ba "
                        + "where ba = :ba "
                        + "group by ee" )
                .setParameter( "ba", ba )
                .uniqueResult();
    }

    @Override
    public Collection<ExpressionExperiment> findByBioMaterial( BioMaterial bm ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select ee from ExpressionExperiment as ee "
                        + "join ee.bioAssays as ba join ba.sampleUsed as sample where sample = :bm "
                        + "group by ee" )
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
        //noinspection unchecked
        List<Long> eeIds = this.getSessionFactory().getCurrentSession().
                createSQLQuery( "SELECT ee.ID AS eeID FROM INVESTIGATION ee "
                        + "join PROCESSED_EXPRESSION_DATA_VECTOR dedv on dedv.EXPRESSION_EXPERIMENT_FK = ee.ID "
                        + "join COMPOSITE_SEQUENCE cs on dedv.DESIGN_ELEMENT_FK = cs.ID "
                        + "join GENE2CS g2s on g2s.CS = cs.ID "
                        + "where g2s.GENE = :geneID and dedv.RANK_BY_MEAN >= :rank "
                        + "group by ee.ID" )
                .addScalar( "eeID", StandardBasicTypes.LONG )
                .setLong( "geneID", gene.getId() )
                .setDouble( "rank", rank )
                .list();
        return this.load( eeIds );
    }

    @Override
    public ExpressionExperiment findByDesign( ExperimentalDesign ed ) {
        return findOneByProperty( "experimentalDesign", ed );
    }

    @Override
    public ExpressionExperiment findByDesignId( Long designId ) {
        return findOneByProperty( "experimentalDesign.id", designId );
    }

    @Override
    public ExpressionExperiment findByFactor( ExperimentalFactor ef ) {
        return ( ExpressionExperiment ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select ee from ExpressionExperiment as ee "
                        + "join ee.experimentalDesign ed "
                        + "join ed.experimentalFactors ef "
                        + "where ef = :ef "
                        + "group by ee" )
                .setParameter( "ef", ef )
                .uniqueResult();
    }

    @Override
    public Collection<ExpressionExperiment> findByFactors( Collection<ExperimentalFactor> factors ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select ee from ExpressionExperiment as ee "
                        + "join ee.experimentalDesign ed "
                        + "join ed.experimentalFactors ef "
                        + "where ef in :factors "
                        + "group by ee" )
                .setParameterList( "factors", optimizeIdentifiableParameterList( factors ) )
                .list();
    }

    @Override
    public ExpressionExperiment findByFactorValue( FactorValue fv ) {
        return this.findByFactorValue( fv.getId() );
    }

    @Override
    public ExpressionExperiment findByFactorValue( Long factorValueId ) {
        return ( ExpressionExperiment ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select ee from ExpressionExperiment as ee "
                        + "join ee.experimentalDesign ed "
                        + "join ed.experimentalFactors ef "
                        + "join ef.factorValues fv "
                        + "where fv.id = :fvId "
                        + "group by ee" )
                .setParameter( "fvId", factorValueId )
                .uniqueResult();
    }

    @Override
    public Collection<ExpressionExperiment> findByFactorValues( Collection<FactorValue> fvs ) {
        if ( fvs.isEmpty() ) {
            return Collections.emptyList();
        }
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select ee from ExpressionExperiment ee "
                        + "join ee.experimentalDesign ed join ed.experimentalFactors ef join ef.factorValues f "
                        + "where f in (:fvs) "
                        + "group by ee" )
                .setParameterList( "fvs", optimizeIdentifiableParameterList( fvs ) )
                .list();
    }

    @Override
    public Collection<ExpressionExperiment> findByFactorValueIds( Collection<Long> factorValueIds ) {
        if ( factorValueIds.isEmpty() ) {
            return Collections.emptyList();
        }
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select ee from ExpressionExperiment ee "
                        + "join ee.experimentalDesign ed join ed.experimentalFactors ef join ef.factorValues f "
                        + "where f.id in (:fvIds) "
                        + "group by ee" )
                .setParameterList( "fvIds", optimizeParameterList( factorValueIds ) )
                .list();
    }

    /**
     * uses GENE2CS table.
     */
    @Override
    public Collection<ExpressionExperiment> findByGene( Gene gene ) {
        //noinspection unchecked
        Collection<Long> eeIds = this.getSessionFactory().getCurrentSession()
                .createSQLQuery( "select ee.ID as eeID from INVESTIGATION ee "
                        + "join BIO_ASSAY ba on ba.EXPRESSION_EXPERIMENT_FK = ee.ID "
                        + "join ARRAY_DESIGN on ba.ARRAY_DESIGN_USED_FK = ARRAY_DESIGN.ID "
                        + "join gemd.COMPOSITE_SEQUENCE cs on cs.ARRAY_DESIGN_FK = ARRAY_DESIGN.ID "
                        + "join GENE2CS g2s on g2s.CS = cs.ID "
                        + "where g2s.GENE = :geneID" )
                .addScalar( "eeID", StandardBasicTypes.LONG )
                .setLong( "geneID", gene.getId() )
                .list();
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
    public Map<Class<? extends Identifiable>, List<Characteristic>> getAllAnnotations( ExpressionExperiment expressionExperiment, boolean useEe2c ) {
        if ( useEe2c ) {
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
        } else {
            Map<Class<? extends Identifiable>, List<Characteristic>> result = new HashMap<>();
            result.put( ExpressionExperiment.class, getExperimentAnnotations( expressionExperiment, false ) );
            result.put( ExperimentalDesign.class, getExperimentalDesignAnnotations( expressionExperiment, false ) );
            result.put( BioMaterial.class, getBioMaterialAnnotations( expressionExperiment, false ) );
            return result;
        }
    }

    @Override
    public List<Characteristic> getExperimentAnnotations( ExpressionExperiment expressionExperiment, boolean useEe2c ) {
        if ( useEe2c ) {
            return getAnnotationsByLevel( expressionExperiment, ExpressionExperiment.class );
        } else {
            //noinspection unchecked
            return getSessionFactory().getCurrentSession()
                    .createQuery( "select c from ExpressionExperiment ee "
                            + "join ee.characteristics c "
                            + "where ee = :ee" )
                    .setParameter( "ee", expressionExperiment )
                    .setCacheable( true )
                    .list();
        }
    }

    @Override
    public Collection<Characteristic> getExperimentSubSetAnnotations( ExpressionExperiment ee ) {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                .createQuery( "select c from ExpressionExperimentSubSet subset "
                        + "join subset.characteristics c "
                        + "where subset.sourceExperiment = :ee" )
                .setParameter( "ee", ee )
                .setCacheable( true )
                .list();
    }

    @Override
    public List<Characteristic> getBioMaterialAnnotations( ExpressionExperiment expressionExperiment, boolean useEe2c ) {
        if ( useEe2c ) {
            return getAnnotationsByLevel( expressionExperiment, BioMaterial.class );
        } else {
            /*
             * Note we're not using 'distinct' here but the 'equals' for AnnotationValueObject should aggregate these. More
             * work to do.
             */
            //noinspection unchecked
            return this.getSessionFactory().getCurrentSession()
                    .createQuery( "select c from ExpressionExperiment e "
                            + "join e.bioAssays ba join ba.sampleUsed bm "
                            + "join bm.characteristics c where e = :ee" )
                    .setParameter( "ee", expressionExperiment )
                    .setCacheable( true )
                    .list();
        }
    }

    @Override
    public List<Characteristic> getBioMaterialAnnotations( ExpressionExperimentSubSet subset ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select c from ExpressionExperimentSubSet subset "
                        + "join subset.bioAssays ba join ba.sampleUsed bm "
                        + "join bm.characteristics c "
                        + "where subset = :subset" )
                .setParameter( "subset", subset )
                .setCacheable( true )
                .list();
    }

    @Override
    public List<Characteristic> getExperimentalDesignAnnotations( ExpressionExperiment expressionExperiment, boolean useEe2c ) {
        if ( useEe2c ) {
            return getAnnotationsByLevel( expressionExperiment, ExperimentalDesign.class );
        } else {
            return new ArrayList<>( getFactorValueAnnotations( expressionExperiment ) );
        }
    }

    @Override
    public List<Statement> getFactorValueAnnotations( ExpressionExperiment ee ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select c from ExpressionExperiment e "
                        + "join e.experimentalDesign ed join ed.experimentalFactors ef join ef.factorValues fv "
                        + "join fv.characteristics c where e = :ee " )
                .setParameter( "ee", ee )
                .setCacheable( true )
                .list();
    }

    @Override
    public List<Statement> getFactorValueAnnotations( ExpressionExperimentSubSet subset ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select c from ExpressionExperimentSubSet subset "
                        + "join subset.bioAssays ba join ba.sampleUsed bm "
                        + "join bm.factorValues fv "
                        + "join fv.characteristics c "
                        + "where subset = :subset "
                        + "group by c" )
                .setParameter( "subset", subset )
                .setCacheable( true )
                .list();
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
        String excludeUrisClause = getExcludeUrisClause( "VALUE_URI", "`VALUE`", excludedCategoryUris, excludedTermUris, excludeFreeTextCategories, excludeFreeTextTerms, excludeUncategorized, false );
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
    public Map<Characteristic, Long> getAnnotationsUsageFrequency( @Nullable Collection<Long> eeIds, @Nullable Class<? extends Identifiable> level, int maxResults, int minFrequency, @Nullable String category, @Nullable Collection<String> excludedCategoryUris, @Nullable Collection<String> excludedTermUris, @Nullable Collection<String> retainedTermUris, boolean includePredicates, boolean includeObjects ) {
        // we only include category-only terms once, because otherwise they would be repeated for each subsequent queries
        Map<Characteristic, Long> result = getAnnotationsUsageFrequencyInternal( eeIds, level, maxResults, minFrequency, category, excludedCategoryUris,
                excludedTermUris, false, retainedTermUris, "`VALUE`", "VALUE_URI", true );

        // predicates and objects do not have their own categories and should be treated as uncategorized, thus we strip
        // the category constraint and only check if uncategorized terms are excluded
        boolean excludeUncategorized = excludedCategoryUris != null && excludedCategoryUris.contains( UNCATEGORIZED );
        if ( includePredicates && !excludeUncategorized ) {
            mergeUsageFrequencies( result, getAnnotationsUsageFrequencyInternal( eeIds, level, maxResults, minFrequency, null, null,
                    excludedTermUris, true, retainedTermUris, "PREDICATE", "PREDICATE_URI", false ) );
            mergeUsageFrequencies( result, getAnnotationsUsageFrequencyInternal( eeIds, level, maxResults, minFrequency, null, null,
                    excludedTermUris, true, retainedTermUris, "SECOND_PREDICATE", "SECOND_PREDICATE_URI", false ) );
        }
        if ( includeObjects && !excludeUncategorized ) {
            mergeUsageFrequencies( result, getAnnotationsUsageFrequencyInternal( eeIds, level, maxResults, minFrequency, null, null,
                    excludedTermUris, true, retainedTermUris, "OBJECT", "OBJECT_URI", false ) );
            mergeUsageFrequencies( result, getAnnotationsUsageFrequencyInternal( eeIds, level, maxResults, minFrequency, null, null,
                    excludedTermUris, true, retainedTermUris, "SECOND_OBJECT", "SECOND_OBJECT_URI", false ) );
        }
        if ( includePredicates || includeObjects ) {
            // re-filter results to satisfy the maxResults requirements
            if ( maxResults > 0 && result.size() > maxResults ) {
                result = result.entrySet().stream()
                        .sorted( Map.Entry.comparingByValue( Comparator.reverseOrder() ) )
                        .limit( maxResults )
                        .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) );
            }
        }
        return result;
    }

    /**
     * Merge the usage frequencies from two different queries.
     * <p>
     * We take the sum with the assumption that a dataset would not use the same characteristic as a subject, predicate
     * or object.
     */
    private void mergeUsageFrequencies( Map<Characteristic, Long> result, Map<Characteristic, Long> annotationsUsageFrequencyInternal ) {
        for ( Map.Entry<Characteristic, Long> entry : annotationsUsageFrequencyInternal.entrySet() ) {
            result.merge( entry.getKey(), entry.getValue(), Long::sum );
        }
    }

    public Map<Characteristic, Long> getAnnotationsUsageFrequencyInternal(
            @Nullable Collection<Long> eeIds, @Nullable Class<? extends Identifiable> level, int maxResults,
            int minFrequency, @Nullable String category, @Nullable Collection<String> excludedCategoryUris,
            @Nullable Collection<String> excludedTermUris,
            boolean excludeCategoryOnly,
            @Nullable Collection<String> retainedTermUris,
            String valueColumn, String valueUriColumn,
            boolean isCategorized ) {
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
        String query = "select T." + valueColumn + " as `VALUE`, T." + valueUriColumn + " as VALUE_URI, " + ( isCategorized ? "T.CATEGORY" : "NULL" ) + " as CATEGORY, " + ( isCategorized ? "T.CATEGORY_URI" : "NULL" ) + " as CATEGORY_URI, T.EVIDENCE_CODE as EVIDENCE_CODE, count(distinct T.EXPRESSION_EXPERIMENT_FK) as EE_COUNT from EXPRESSION_EXPERIMENT2CHARACTERISTIC T ";
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
            // no need to filter out excluded categories if a specific one is requested or if the requested annotations
            // are intrinsically uncategorized
            excludeUrisClause = getExcludeUrisClause( valueUriColumn, valueColumn, null, excludedTermUris, excludeFreeTextCategories, excludeFreeTextTerms, excludeUncategorized, excludeCategoryOnly );
        } else {
            // all categories are requested, we may filter out excluded ones
            excludeUrisClause = getExcludeUrisClause( valueUriColumn, valueColumn, excludedCategoryUris, excludedTermUris, excludeFreeTextCategories, excludeFreeTextTerms, excludeUncategorized, excludeCategoryOnly );
        }
        if ( excludeUrisClause != null ) {
            query += " and (";
            query += "(" + excludeUrisClause + ")";
            if ( retainedTermUris != null && !retainedTermUris.isEmpty() ) {
                query += " or T." + valueUriColumn + " in (:retainedTermUris)";
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
                + ( category == null && isCategorized ? "COALESCE(T.CATEGORY_URI, T.CATEGORY), " : "" )
                + "COALESCE(T." + valueUriColumn + ", T." + valueColumn + ")";
        // if there are too many EE IDs, they will be retrieved by batch and filtered in-memory
        if ( minFrequency > 1 && ( eeIds == null || eeIds.size() <= MAX_PARAMETER_LIST_SIZE ) ) {
            query += " having EE_COUNT >= :minFrequency";
            if ( retainedTermUris != null && !retainedTermUris.isEmpty() ) {
                query += " or " + valueUriColumn + " in (:retainedTermUris)";
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
     *
     * @param excludedCategoryUris      list of category URIs to exclude
     * @param excludedTermUris          list of URIs to exclude
     * @param excludeFreeTextCategories whether to exclude free-text categories
     * @param excludeFreeTextTerms      whether to exclude free-text terms
     * @param excludeUncategorized      whether to exclude uncategorized terms
     * @param excludeCategoryOnly       whether to exclude terms that are only categories (i.e. no value nor value URI)
     * @return a SQL clause for excluding terms and categories or null if no clause is necessary
     */
    @Nullable
    private String getExcludeUrisClause( String valueUriColumn, String valueColumn,
            @Nullable Collection<String> excludedCategoryUris,
            @Nullable Collection<String> excludedTermUris, boolean excludeFreeTextCategories,
            boolean excludeFreeTextTerms, boolean excludeUncategorized,
            boolean excludeCategoryOnly ) {
        List<String> clauses = new ArrayList<>( 6 );
        if ( excludedCategoryUris != null && !excludedCategoryUris.isEmpty() ) {
            clauses.add( "T.CATEGORY_URI is null or T.CATEGORY_URI not in (:excludedCategoryUris)" );
        }
        if ( excludedTermUris != null && !excludedTermUris.isEmpty() ) {
            clauses.add( "T." + valueUriColumn + " is null or T." + valueUriColumn + " not in (:excludedTermUris)" );
        }
        if ( excludeFreeTextCategories ) {
            // we don't want to exclude "uncategorized" terms when excluding free-text categories
            clauses.add( "T.CATEGORY_URI is not null or T.CATEGORY is null" );
        }
        if ( excludeFreeTextTerms ) {
            clauses.add( "T." + valueUriColumn + " is not null" );
        }
        if ( excludeUncategorized ) {
            clauses.add( "COALESCE(T.CATEGORY_URI, T.CATEGORY) is not null" );
        }
        if ( excludeCategoryOnly ) {
            clauses.add( "COALESCE(T." + valueUriColumn + ", T." + valueColumn + ") is not null" );
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
            Assert.isTrue( mvr.getMeans().length == mvr.getVariances().length,
                    "The number of means and variances must correspond." );
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
    public Collection<ArrayDesign> getArrayDesignsUsed( ExpressionExperiment bas ) {
        return CommonQueries.getArrayDesignsUsed( bas, this.getSessionFactory().getCurrentSession() );
    }

    @Override
    public Collection<ArrayDesign> getArrayDesignsUsed( Collection<ExpressionExperiment> ees ) {
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
                .createSQLQuery( "select {G.*} from PROCESSED_EXPRESSION_DATA_VECTOR pedv "
                        + "join GENE2CS on pedv.DESIGN_ELEMENT_FK = GENE2CS.CS "
                        + "join CHROMOSOME_FEATURE G on GENE2CS.GENE = G.ID "
                        + "where pedv.EXPRESSION_EXPERIMENT_FK = :eeId "
                        + "group by G.ID" )
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
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select b from BioAssayDimension b, ExpressionExperiment e "
                        + "join b.bioAssays bba join e.bioAssays eb "
                        + "where eb = bba and e = :ee "
                        + "group by b" )
                .setParameter( "ee", expressionExperiment )
                .list();
    }

    @Override
    public Collection<BioAssayDimension> getBioAssayDimensionsFromSubSets( ExpressionExperiment expressionExperiment ) {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession().createQuery( "select b from BioAssayDimension b, ExpressionExperimentSubSet eess "
                        + "join b.bioAssays bba join eess.bioAssays eb "
                        + "where eb = bba and eess.sourceExperiment = :ee "
                        + "group by b" )
                .setParameter( "ee", expressionExperiment )
                .list();
    }

    @Override
    public Collection<BioAssayDimension> getBioAssayDimensions( ExpressionExperiment ee, QuantitationType qt ) {
        Set<Collection<BioAssayDimension>> dimensions = bulkDataVectorTypes.stream()
                .map( vectorType -> getBioAssayDimensions( ee, qt, vectorType ) )
                .filter( c -> !c.isEmpty() )
                .collect( Collectors.toSet() );
        if ( dimensions.size() == 1 ) {
            return dimensions.iterator().next();
        } else if ( dimensions.size() > 1 ) {
            throw new NonUniqueResultException( dimensions.size() );
        } else {
            return null;
        }
    }

    @Override
    public Collection<BioAssayDimension> getBioAssayDimensions( ExpressionExperiment ee, QuantitationType qt, Class<? extends BulkExpressionDataVector> dataVectorType ) {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                .createCriteria( dataVectorType )
                .add( Restrictions.eq( "expressionExperiment", ee ) )
                .add( Restrictions.eq( "quantitationType", qt ) )
                .setProjection( Projections.groupProperty( "bioAssayDimension" ) )
                .list();
    }

    @Override
    public BioAssayDimension getBioAssayDimension( ExpressionExperiment ee, QuantitationType qt, Class<? extends BulkExpressionDataVector> dataVectorType ) {
        return ( BioAssayDimension ) getSessionFactory().getCurrentSession()
                .createCriteria( dataVectorType )
                .add( Restrictions.eq( "expressionExperiment", ee ) )
                .add( Restrictions.eq( "quantitationType", qt ) )
                .setProjection( Projections.groupProperty( "bioAssayDimension" ) )
                .uniqueResult();
    }

    @Override
    public BioAssayDimension getBioAssayDimension( ExpressionExperiment ee, QuantitationType qt ) {
        Set<BioAssayDimension> dimensions = bulkDataVectorTypes.stream()
                .map( vectorType -> this.getBioAssayDimension( ee, qt, vectorType ) )
                .filter( Objects::nonNull )
                .collect( Collectors.toSet() );
        if ( dimensions.size() == 1 ) {
            return dimensions.iterator().next();
        } else if ( dimensions.size() > 1 ) {
            throw new NonUniqueResultException( dimensions.size() );
        } else {
            return null;
        }
    }

    @Override
    public BioAssayDimension getBioAssayDimensionById( ExpressionExperiment ee, Long dimensionId, Class<? extends BulkExpressionDataVector> dataVectorType ) {
        return ( BioAssayDimension ) getSessionFactory().getCurrentSession()
                .createCriteria( dataVectorType )
                .add( Restrictions.eq( "expressionExperiment", ee ) )
                .add( Restrictions.eq( "bioAssayDimension.id", dimensionId ) )
                .setProjection( Projections.groupProperty( "bioAssayDimension" ) )
                .uniqueResult();
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
    public long getRawDataVectorCount( ExpressionExperiment ee ) {
        return ( Long ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select count(*) from RawExpressionDataVector dedv "
                        + "where dedv.expressionExperiment = :ee" )
                .setParameter( "ee", ee )
                .uniqueResult();
    }

    @Override
    public Collection<ExpressionExperiment> getExperimentsWithOutliers() {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select e from ExpressionExperiment e "
                        + "join e.bioAssays b "
                        + "where b.isOutlier = true "
                        + "group by e" )
                .list();
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
                + " inner join ef.category cat where e.id in (:ids) and cat.category != (:category) and cat.categoryUri != (:categoryUri) and ef.name != (:name) group by e.id";

        //noinspection unchecked
        List<Object[]> res = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ids", optimizeParameterList( ids ) ) // Set ids
                .setParameter( "category", Categories.BLOCK.getCategory() ) // Set batch category
                .setParameter( "categoryUri", Categories.BLOCK.getCategoryUri() ) // Set batch category
                .setParameter( "name", ExperimentFactorUtils.BATCH_FACTOR_NAME ) // set batch name
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
        final String queryString = "select v.quantitationType, count(distinct v) as count "
                + "from RawExpressionDataVector v "
                + "where v.expressionExperiment = :ee "
                + "group by v.quantitationType";

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
                .createQuery( "select v.quantitationType from SingleCellExpressionDataVector v "
                        + "where v.quantitationType.isSingleCellPreferred = true and v.expressionExperiment = :ee "
                        + "group by v.quantitationType" )
                .setParameter( "ee", ee )
                .uniqueResult();
    }

    @Override
    public QuantitationType getPreferredQuantitationType( ExpressionExperiment ee ) {
        return ( QuantitationType ) getSessionFactory().getCurrentSession()
                .createQuery( "select rv.quantitationType from RawExpressionDataVector rv "
                        + "where rv.quantitationType.isPreferred = true and rv.expressionExperiment = :ee "
                        + "group by rv.quantitationType" )
                .setParameter( "ee", ee )
                .uniqueResult();
    }

    @Override
    public QuantitationType getProcessedQuantitationType( ExpressionExperiment ee ) {
        return ( QuantitationType ) getSessionFactory().getCurrentSession()
                .createQuery( "select rv.quantitationType from ProcessedExpressionDataVector rv "
                        + "where rv.expressionExperiment = :ee "
                        + "group by rv.quantitationType" )
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
    public Collection<ExpressionExperimentSubSet> getSubSets( ExpressionExperiment expressionExperiment, BioAssayDimension bad ) {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                .createQuery( "select eess from ExpressionExperimentSubSet eess join eess.bioAssays ba, "
                        + "BioAssayDimension bad join bad.bioAssays ba2 "
                        + "where eess.sourceExperiment = :ee and bad = :bad and ba = ba2 "
                        + "group by eess, bad "
                        // require all the subset's assays to be matched
                        + "having size(eess.bioAssays) = count(ba)" )
                .setParameter( "ee", expressionExperiment )
                .setParameter( "bad", bad )
                .list();
    }

    @Override
    public Map<BioAssayDimension, Set<ExpressionExperimentSubSet>> getSubSetsByDimension( ExpressionExperiment expressionExperiment ) {
        //noinspection unchecked
        List<Object[]> results = getSessionFactory().getCurrentSession()
                .createQuery( "select eess, bad from ExpressionExperimentSubSet eess join eess.bioAssays ba, "
                        + "BioAssayDimension bad join bad.bioAssays ba2 "
                        + "where eess.sourceExperiment = :ee and ba = ba2 "
                        + "group by eess, bad "
                        // require all the subset's assays to be matched
                        + "having size(eess.bioAssays) = count(ba)" )
                .setParameter( "ee", expressionExperiment )
                .list();
        return results.stream()
                .collect( Collectors.groupingBy( row -> ( BioAssayDimension ) row[1], Collectors.mapping( row -> ( ExpressionExperimentSubSet ) row[0], Collectors.toSet() ) ) );
    }

    @Override
    public ExpressionExperimentSubSet getSubSetById( ExpressionExperiment ee, Long subSetId ) {
        return ( ExpressionExperimentSubSet ) getSessionFactory().getCurrentSession()
                .createQuery( "select eess from ExpressionExperimentSubSet eess where eess.sourceExperiment = :ee and eess.id = :id" )
                .setParameter( "ee", ee )
                .setParameter( "id", subSetId )
                .uniqueResult();
    }

    @Override
    public Map<ExpressionExperiment, Taxon> getTaxa( Collection<ExpressionExperiment> ees ) {
        if ( ees.isEmpty() )
            return Collections.emptyMap();

        // FIXME: this query cannot be made cacheable because the taxon is not initialized when retrieved from the cache, defeating the purpose of caching altogether
        Query query = this.getSessionFactory().getCurrentSession()
                .createQuery( "select EE, st from ExpressionExperiment as EE "
                        + "join EE.bioAssays as BA join BA.sampleUsed as SU join SU.sourceTaxon st where EE in (:ees) "
                        + "group by EE" );
        List<Object[]> list = QueryUtils.listByIdentifiableBatch( query, "ees", ees, 2048 );

        // collecting in a tree map in case BASs are proxies
        Map<ExpressionExperiment, Taxon> result = new TreeMap<>( Comparator.comparing( ExpressionExperiment::getId ) );
        for ( Object[] row : list ) {
            result.put( ( ExpressionExperiment ) row[0], ( Taxon ) row[1] );
        }
        return result;
    }

    @Override
    public Taxon getTaxon( ExpressionExperiment ee ) {
        if ( ee.getTaxon() != null ) {
            return ee.getTaxon();
        }
        return getTaxonFromSamples( ee );
    }

    private Taxon getTaxonFromSamples( ExpressionExperiment ee ) {
        return ( Taxon ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select SU.sourceTaxon from ExpressionExperiment as EE "
                        + "join EE.bioAssays as BA join BA.sampleUsed as SU "
                        + "where EE = :ee "
                        + "group by SU.sourceTaxon" )
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
                .createQuery( "select ee from ExpressionExperiment ee join ee.otherParts op where op = :ee group by ee" )
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
                            .createQuery( "select bm from BioMaterial bm join bm.bioAssaysUsedIn ba where ba in :bas group by bm" ),
                    "bas", ee.getBioAssays(), MAX_PARAMETER_LIST_SIZE ) );
        }

        // find BMs attached to FVs
        Set<FactorValue> fvs = new HashSet<>( getFactorValues( ee ) );
        if ( !fvs.isEmpty() ) {
            bms.addAll( listByIdentifiableBatch( getSessionFactory().getCurrentSession()
                            .createQuery( "select bm from BioMaterial bm join bm.factorValues fv where fv in :fvs group by bm" ),
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
        // keep the dimensions around as we will perform additional cleanups on them since we're removing the EE as a
        // whole
        removeAllRawDataVectors( ee, true );
        removeProcessedDataVectors( ee, true );

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

        removeAllSingleCellDataVectors( ee, true );

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
                .createQuery( "select fv from ExpressionExperiment ee "
                        + "join ee.experimentalDesign ed "
                        + "join ed.experimentalFactors ef "
                        + "join ef.factorValues fv "
                        + "where ee = :ee "
                        + "group by fv" )
                .setParameter( "ee", ee )
                .list();
    }

    @Override
    public void thaw( final ExpressionExperiment expressionExperiment ) {
        thawLite( expressionExperiment );
        thawRawVectors( expressionExperiment );
        thawProcessedVectors( expressionExperiment );
    }

    @Override
    public void thawLite( final ExpressionExperiment ee ) {
        thawLiter( ee );
        ee.getBioAssays().forEach( Thaws::thawBioAssay );
    }

    @Override
    public void thawLiter( final ExpressionExperiment expressionExperiment ) {
        Hibernate.initialize( expressionExperiment.getQuantitationTypes() );
        Hibernate.initialize( expressionExperiment.getCharacteristics() );

        if ( expressionExperiment.getAccession() != null ) {
            thawDatabaseEntry( expressionExperiment.getAccession() );
        }

        if ( expressionExperiment.getMeanVarianceRelation() != null ) {
            Hibernate.initialize( expressionExperiment.getMeanVarianceRelation() );
        }

        Hibernate.initialize( expressionExperiment.getAuditTrail() );
        Hibernate.initialize( expressionExperiment.getGeeq() );
        Hibernate.initialize( expressionExperiment.getCurationDetails() );

        Hibernate.initialize( expressionExperiment.getOtherParts() );

        if ( expressionExperiment.getExperimentalDesign() != null ) {
            for ( ExperimentalFactor ef : expressionExperiment.getExperimentalDesign().getExperimentalFactors() ) {
                Hibernate.initialize( ef );
                ef.getFactorValues().forEach( Hibernate::initialize );
            }
            Hibernate.initialize( expressionExperiment.getExperimentalDesign().getTypes() );
        }

        if ( expressionExperiment.getPrimaryPublication() != null ) {
            thawBibliographicReference( expressionExperiment.getPrimaryPublication() );
        }

        expressionExperiment.getOtherRelevantPublications().forEach( Thaws::thawBibliographicReference );
    }

    private void thawRawVectors( ExpressionExperiment ee ) {
        StopWatch timer = StopWatch.createStarted();
        Hibernate.initialize( ee.getRawExpressionDataVectors() );
        if ( timer.getTime() > 1000 ) {
            log.info( String.format( "Initializing %d raw vectors took %d ms", ee.getRawExpressionDataVectors().size(), timer.getTime() ) );
        }
    }

    private void thawProcessedVectors( ExpressionExperiment ee ) {
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
                .createQuery( "select scedv.singleCellDimension from SingleCellExpressionDataVector scedv "
                        + "where scedv.expressionExperiment = :ee "
                        + "group by scedv.singleCellDimension" )
                .setParameter( "ee", ee )
                .list();
    }

    @Override
    public List<SingleCellDimension> getSingleCellDimensionsWithoutCellIds( ExpressionExperiment ee ) {
        return getSingleCellDimensionsWithoutCellIds( ee, true, true, true, true, true, true );
    }

    @Override
    public List<SingleCellDimension> getSingleCellDimensionsWithoutCellIds( ExpressionExperiment ee, boolean includeBioAssays, boolean includeCtas, boolean includeClcs, boolean includeProtocol, boolean includeCharacteristics, boolean includeIndices ) {
        //noinspection unchecked
        return ( List<SingleCellDimension> ) getSessionFactory().getCurrentSession()
                .createQuery( "select dimension.id as id, dimension.numberOfCellIds as numberOfCellIds, dimension.bioAssaysOffset as bioAssaysOffset from SingleCellExpressionDataVector scedv "
                        + "join scedv.singleCellDimension dimension "
                        + "where scedv.expressionExperiment = :ee "
                        + "group by dimension" )
                .setParameter( "ee", ee )
                .setResultTransformer( new SingleCellDimensionWithoutCellIdsInitializer( includeBioAssays, includeCtas, includeClcs, includeProtocol, includeCharacteristics, includeIndices ) )
                .list();
    }

    @Override
    public SingleCellDimension getSingleCellDimension( ExpressionExperiment ee, QuantitationType qt ) {
        return getSingleCellDimension( ee, qt, getSessionFactory().getCurrentSession() );
    }

    private SingleCellDimension getSingleCellDimension( ExpressionExperiment ee, QuantitationType qt, Session session ) {
        return ( SingleCellDimension ) session
                .createQuery( "select scedv.singleCellDimension from SingleCellExpressionDataVector scedv "
                        + "where scedv.expressionExperiment = :ee and scedv.quantitationType = :qt "
                        + "group by scedv.singleCellDimension" )
                .setParameter( "ee", ee )
                .setParameter( "qt", qt )
                .uniqueResult();
    }

    @Override
    public SingleCellDimension getSingleCellDimensionWithoutCellIds( ExpressionExperiment ee, QuantitationType qt ) {
        return getSingleCellDimensionWithoutCellIds( ee, qt, true, true, true, true, true, true );
    }

    @Override
    public SingleCellDimension getSingleCellDimensionWithoutCellIds( ExpressionExperiment ee, QuantitationType qt, boolean includeBioAssays, boolean includeCtas, boolean includeClcs, boolean includeProtocol, boolean includeCharacteristics, boolean includeIndices ) {
        return ( SingleCellDimension ) getSessionFactory().getCurrentSession()
                .createQuery( "select dimension.id as id, dimension.numberOfCellIds as numberOfCellIds, dimension.bioAssaysOffset as bioAssaysOffset from SingleCellExpressionDataVector scedv "
                        + "join scedv.singleCellDimension dimension "
                        + "where scedv.expressionExperiment = :ee and scedv.quantitationType = :qt "
                        + "group by dimension" )
                .setParameter( "ee", ee )
                .setParameter( "qt", qt )
                .setResultTransformer( new SingleCellDimensionWithoutCellIdsInitializer( includeBioAssays, includeCtas, includeClcs, includeProtocol, includeCharacteristics, includeIndices ) )
                .uniqueResult();
    }

    @Override
    public SingleCellDimension getPreferredSingleCellDimension( ExpressionExperiment ee ) {
        return ( SingleCellDimension ) getSessionFactory().getCurrentSession()
                .createQuery( "select scedv.singleCellDimension from SingleCellExpressionDataVector scedv "
                        + "where scedv.quantitationType.isSingleCellPreferred = true and scedv.expressionExperiment = :ee "
                        + "group by scedv.singleCellDimension" )
                .setParameter( "ee", ee )
                .uniqueResult();
    }

    @Override
    public SingleCellDimension getPreferredSingleCellDimensionWithoutCellIds( ExpressionExperiment ee ) {
        return getPreferredSingleCellDimensionsWithoutCellIds( ee, true, true, true, true, true, true );
    }

    @Override
    public SingleCellDimension getPreferredSingleCellDimensionsWithoutCellIds( ExpressionExperiment ee, boolean includeBioAssays, boolean includeCtas, boolean includeClcs, boolean includeProtocol, boolean includeCharacteristics, boolean includeIndices ) {
        return ( SingleCellDimension ) getSessionFactory().getCurrentSession()
                .createQuery( "select dimension.id as id, dimension.numberOfCellIds as numberOfCellIds, dimension.bioAssaysOffset as bioAssaysOffset from SingleCellExpressionDataVector scedv "
                        + "join scedv.singleCellDimension dimension "
                        + "where scedv.quantitationType.isSingleCellPreferred = true and scedv.expressionExperiment = :ee "
                        + "group by dimension" )
                .setParameter( "ee", ee )
                .setResultTransformer( new SingleCellDimensionWithoutCellIdsInitializer( includeBioAssays, includeCtas, includeClcs, includeProtocol, includeCharacteristics, includeIndices ) )
                .uniqueResult();
    }

    private class SingleCellDimensionWithoutCellIdsInitializer implements TypedResultTransformer<SingleCellDimension> {

        private final boolean includeBioAssays;
        private final boolean includeCtas;
        private final boolean includeClcs;
        private final boolean includeProtocol;
        private final boolean includeCharacteristics;
        private final boolean includeIndices;

        private SingleCellDimensionWithoutCellIdsInitializer( boolean includeBioAssays, boolean includeCtas, boolean includeClcs, boolean includeProtocol, boolean includeCharacteristics, boolean includeIndices ) {
            this.includeBioAssays = includeBioAssays;
            this.includeCtas = includeCtas;
            this.includeClcs = includeClcs;
            this.includeProtocol = includeProtocol;
            this.includeCharacteristics = includeCharacteristics;
            this.includeIndices = includeIndices;
        }

        @Override
        public SingleCellDimension transformTuple( Object[] tuple, String[] aliases ) {
            SingleCellDimension result = ( SingleCellDimension ) aliasToBean( SingleCellDimension.class ).transformTuple( tuple, aliases );
            result.setCellIds( new UninitializedList<>( result.getNumberOfCellIds() ) );
            if ( includeBioAssays ) {
                //noinspection unchecked
                List<BioAssay> bas = getSessionFactory().getCurrentSession()
                        .createQuery( "select ba from SingleCellDimension dimension join dimension.bioAssays ba where dimension = :scd order by index(ba)" )
                        .setParameter( "scd", result )
                        .list();
                result.setBioAssays( bas );
            } else {
                result.setBioAssays( new UninitializedList<>( result.getBioAssaysOffset().length ) );
            }
            // FIXME: fix lazy initialization and use Hibernate.initialize() instead
            if ( includeCtas ) {
                if ( includeCharacteristics && includeIndices ) {
                    //noinspection unchecked
                    List<CellTypeAssignment> ctas = getSessionFactory().getCurrentSession()
                            .createQuery( "select cta from SingleCellDimension scd join scd.cellTypeAssignments cta where scd = :scd" )
                            .setParameter( "scd", result )
                            .list();
                    if ( includeProtocol ) {
                        ctas.forEach( cta -> {
                            Hibernate.initialize( cta.getProtocol() );
                        } );
                    }
                    result.setCellTypeAssignments( new HashSet<>( ctas ) );
                } else {
                    //noinspection unchecked
                    List<CellTypeAssignment> ctas = getSessionFactory().getCurrentSession()
                            .createQuery( "select cta.id as id, cta.name as name, cta.description as description, cta.numberOfCellTypes as numberOfCellTypes, cta.numberOfAssignedCells as numberOfAssignedCells, cta.preferred as preferred"
                                    + ( includeIndices ? ", cta.cellTypeIndices as indices" : "" ) + " "
                                    + "from SingleCellDimension scd "
                                    + "join scd.cellTypeAssignments cta where scd = :scd" )
                            .setParameter( "scd", result )
                            .setResultTransformer( new CtaInitializer( includeProtocol, includeCharacteristics ) )
                            .list();
                    result.setCellTypeAssignments( new HashSet<>( ctas ) );
                }
            } else {
                result.setCellTypeAssignments( new UninitializedSet<>() );
            }
            if ( includeClcs ) {
                if ( includeCharacteristics && includeIndices ) {
                    //noinspection unchecked
                    List<CellLevelCharacteristics> clcs = getSessionFactory().getCurrentSession()
                            .createQuery( "select clc from SingleCellDimension scd join scd.cellLevelCharacteristics clc where scd = :scd" )
                            .setParameter( "scd", result )
                            .list();
                    result.setCellLevelCharacteristics( new HashSet<>( clcs ) );
                } else {
                    //noinspection unchecked
                    List<CellLevelCharacteristics> clcs = getSessionFactory().getCurrentSession()
                            .createQuery( "select clc.id as id, clc.numberOfCharacteristics as numberOfCharacteristics, clc.numberOfAssignedCells as numberOfAssignedCells"
                                    + ( includeIndices ? ", clc.indices as indices" : "" ) + " "
                                    + "from SingleCellDimension scd "
                                    + "join scd.cellLevelCharacteristics clc where scd = :scd" )
                            .setParameter( "scd", result )
                            .setResultTransformer( new ClcInitializer( includeCharacteristics ) )
                            .list();
                    result.setCellLevelCharacteristics( new HashSet<>( clcs ) );
                }
            } else {
                result.setCellLevelCharacteristics( new UninitializedSet<>() );
            }
            return result;
        }

        @Override
        public List<SingleCellDimension> transformListTyped( List<SingleCellDimension> collection ) {
            return collection;
        }
    }

    private class CtaInitializer implements TypedResultTransformer<CellTypeAssignment> {

        private final boolean includeProtocol;
        private final boolean includeCharacteristics;

        private CtaInitializer( boolean includeProtocol, boolean includeCharacteristics ) {
            this.includeProtocol = includeProtocol;
            this.includeCharacteristics = includeCharacteristics;
        }

        @Override
        public CellTypeAssignment transformTuple( Object[] tuple, String[] aliases ) {
            CellTypeAssignment result = ( CellTypeAssignment ) aliasToBean( CellTypeAssignment.class ).transformTuple( tuple, aliases );
            if ( includeProtocol ) {
                Protocol protocol = ( Protocol ) getSessionFactory().getCurrentSession()
                        .createQuery( "select cta.protocol from CellTypeAssignment cta where cta = :cta" )
                        .setParameter( "cta", result )
                        .uniqueResult();
                result.setProtocol( protocol );
            }
            if ( includeCharacteristics ) {
                //noinspection unchecked
                List<Characteristic> cellTypes = getSessionFactory().getCurrentSession()
                        .createQuery( "select c from CellTypeAssignment cta join cta.cellTypes c where cta = :cta order by index(c)" )
                        .setParameter( "cta", result )
                        .list();
                result.setCellTypes( cellTypes );
            } else {
                result.setCellTypes( new UninitializedList<>() );
            }
            return result;
        }

        @Override
        public List<CellTypeAssignment> transformListTyped( List<CellTypeAssignment> collection ) {
            return collection;
        }
    }

    private class ClcInitializer implements TypedResultTransformer<GenericCellLevelCharacteristics> {

        private final boolean includeCharacteristics;

        private ClcInitializer( boolean includeCharacteristics ) {
            this.includeCharacteristics = includeCharacteristics;
        }

        @Override
        public GenericCellLevelCharacteristics transformTuple( Object[] tuple, String[] aliases ) {
            GenericCellLevelCharacteristics result = ( GenericCellLevelCharacteristics ) aliasToBean( GenericCellLevelCharacteristics.class ).transformTuple( tuple, aliases );
            if ( includeCharacteristics ) {
                //noinspection unchecked
                List<Characteristic> characteristics = getSessionFactory().getCurrentSession()
                        .createQuery( "select c from GenericCellLevelCharacteristics clc join clc.characteristics c where clc = :clc order by index(c)" )
                        .setParameter( "clc", result )
                        .list();
                result.setCharacteristics( characteristics );
            } else {
                result.setCharacteristics( new UninitializedList<>() );
            }
            return result;
        }

        @Override
        public List<GenericCellLevelCharacteristics> transformListTyped( List<GenericCellLevelCharacteristics> collection ) {
            return collection;
        }
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
        Assert.notNull( scbad.getCellIds() );
        Assert.isTrue( !scbad.getCellIds().isEmpty(), "There must be at least one cell ID." );
        for ( int i = 0; i < scbad.getBioAssays().size(); i++ ) {
            List<String> sampleCellIds = scbad.getCellIdsBySample( i );
            Assert.isTrue( sampleCellIds.stream().distinct().count() == sampleCellIds.size(),
                    "Cell IDs must be unique for each sample." );
        }
        Assert.isTrue( scbad.getCellIds().size() == scbad.getNumberOfCellIds(),
                "The number of cell IDs must match the number of cells." );
        Assert.isTrue( scbad.getCellTypeAssignments().stream().filter( CellTypeAssignment::isPreferred ).count() <= 1,
                "There must be at most one preferred cell type labelling." );
        validateCellTypeAssignments( scbad );
        validateCellLevelCharacteristics( scbad );
        Assert.isTrue( !scbad.getBioAssays().isEmpty(), "There must be at least one BioAssay." );
        Assert.isTrue( ee.getBioAssays().containsAll( scbad.getBioAssays() ), "Not all supplied BioAssays belong to " + ee );
        validateSparseRangeArray( scbad.getBioAssays(), scbad.getBioAssaysOffset(), scbad.getNumberOfCellIds() );
    }

    private void validateCellTypeAssignments( SingleCellDimension scbad ) {
        if ( !Hibernate.isInitialized( scbad.getCellTypeAssignments() ) ) {
            return; // no need to validate if not initialized
        }
        for ( CellTypeAssignment labelling : scbad.getCellTypeAssignments() ) {
            Assert.notNull( labelling.getCellTypes() );
            Assert.isTrue( labelling.getCellTypeIndices().length == scbad.getNumberOfCellIds(),
                    "The number of cell type assignments (" + labelling.getCellTypeIndices().length + ") must match the number of cell IDs (" + scbad.getNumberOfCellIds() + ")." );
            int numberOfCellTypeLabels = labelling.getCellTypes().size();
            Assert.isTrue( numberOfCellTypeLabels > 0,
                    "There must be at least one cell type label declared in the cellTypes collection." );
            Assert.isTrue( labelling.getCellTypes().stream().distinct().count() == labelling.getCellTypes().size(),
                    "Cell type labels must be unique." );
            Assert.isTrue( numberOfCellTypeLabels == labelling.getNumberOfCellTypes(),
                    "The number of cell types must match the number of values the cellTypes collection." );
            int N = 0;
            for ( int k : labelling.getCellTypeIndices() ) {
                Assert.isTrue( ( k >= 0 && k < numberOfCellTypeLabels ) || k == CellTypeAssignment.UNKNOWN_CHARACTERISTIC,
                        String.format( "Cell type vector values must be within the [%d, %d[ range or use %d as an unknown indicator.",
                                0, numberOfCellTypeLabels, CellTypeAssignment.UNKNOWN_CHARACTERISTIC ) );
                if ( k != CellTypeAssignment.UNKNOWN_CELL_TYPE ) {
                    N++;
                }
            }
            Assert.isTrue( labelling.getNumberOfAssignedCells() == null || labelling.getNumberOfAssignedCells() == N,
                    "The number of assigned cells must match the number of assigned values in cellTypeIndices." );
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
            Assert.isTrue( clc.getIndices().length == scbad.getNumberOfCellIds(),
                    "The number of cell-level characteristic assignments (" + clc.getIndices().length + ") must match the number of cell IDs (" + scbad.getNumberOfCellIds() + ")." );
            int numberOfCharacteristics = clc.getCharacteristics().size();
            Assert.isTrue( numberOfCharacteristics > 0, "There must be at least one cell-level characteristic." );
            Assert.isTrue( numberOfCharacteristics == clc.getNumberOfCharacteristics(),
                    "The number of cell-level characteristics must match the size of the characteristics collection." );
            int N = 0;
            for ( int k : clc.getIndices() ) {
                Assert.isTrue( ( k >= 0 && k < numberOfCharacteristics ) || k == CellTypeAssignment.UNKNOWN_CELL_TYPE,
                        String.format( "Cell-level characteristics vector values must be within the [%d, %d[ range or use %d as an unknown indicator.",
                                0, numberOfCharacteristics, CellTypeAssignment.UNKNOWN_CELL_TYPE ) );
                if ( k != CellTypeAssignment.UNKNOWN_CHARACTERISTIC ) {
                    N++;
                }
            }
            Assert.isTrue( clc.getNumberOfAssignedCells() == null || clc.getNumberOfAssignedCells() == N,
                    "The number of assigned cells must match the number of assigned values in indices." );
            Category category = CharacteristicUtils.getCategory( clc.getCharacteristics().iterator().next() );
            for ( Characteristic c : clc.getCharacteristics() ) {
                Assert.isTrue( CharacteristicUtils.hasCategory( c, category ), "All characteristics must have the " + category + " category." );
            }
            if ( category.equals( Categories.MASK ) ) {
                SingleCellMaskUtils.validateMask( clc );
            }
        }
    }

    @Override
    public void deleteSingleCellDimension( ExpressionExperiment ee, SingleCellDimension singleCellDimension ) {
        log.info( "Removing " + singleCellDimension + " from " + ee + "..." );
        getSessionFactory().getCurrentSession().delete( singleCellDimension );
    }

    @Override
    public Stream<String> streamCellIds( SingleCellDimension dimension, boolean createNewSession ) {
        return QueryUtils.createStream( getSessionFactory(), session -> {
            return session.doReturningWork( work -> {
                PreparedStatement stmt = work.prepareStatement( "select CELL_IDS from SINGLE_CELL_DIMENSION where ID = ?" );
                stmt.setLong( 1, dimension.getId() );
                ResultSet rs = stmt.executeQuery();
                if ( rs.next() ) {
                    return cellIdsUserType.decompressToStream( rs.getBinaryStream( 1 ) );
                } else {
                    return null;
                }
            } );
        }, createNewSession );
    }

    @Override
    public Stream<Characteristic> streamCellTypes( CellTypeAssignment cta, boolean createNewSession ) {
        return streamCellLevelCharacteristics( cta, "ANALYSIS", "CELL_TYPE_INDICES", "CELL_TYPE_ASSIGNMENT_FK", "CELL_TYPE_ASSIGNMENT_ORDERING", createNewSession );
    }

    @Override
    public Category getCellLevelCharacteristicsCategory( CellLevelCharacteristics clc ) {
        Characteristic result;
        if ( clc.getCharacteristics() != null ) {
            result = clc.getCharacteristics().stream().findFirst().orElse( null );
        } else {
            result = ( Characteristic ) getSessionFactory().getCurrentSession()
                    .createQuery( "select c from GenericCellLevelCharacteristics clc join clc.characteristics c "
                            + "where clc = :clc" )
                    .setParameter( "clc", clc )
                    .setMaxResults( 1 )
                    .uniqueResult();
        }
        return result != null ? new Category( result.getCategory(), result.getCategoryUri() ) : null;
    }

    @Override
    public Characteristic getCellTypeAt( CellTypeAssignment cta, int cellIndex ) {
        return getCellLevelCharacteristicAt( cta, cellIndex, "ANALYSIS", "CELL_TYPE_INDICES" );
    }

    @Override
    public Characteristic[] getCellTypeAt( CellTypeAssignment cta, int startIndex, int endIndexExclusive ) {
        return getCellLevelCharacteristicAt( cta, startIndex, endIndexExclusive, "ANALYSIS", "CELL_TYPE_INDICES" );
    }

    @Override
    public Characteristic getCellLevelCharacteristicAt( CellLevelCharacteristics clc, int cellIndex ) {
        return getCellLevelCharacteristicAt( clc, cellIndex, "CELL_LEVEL_CHARACTERISTICS", "INDICES" );
    }

    @Override
    public Characteristic[] getCellLevelCharacteristicAt( CellLevelCharacteristics clc, int startIndex, int endIndexExclusive ) {
        return getCellLevelCharacteristicAt( clc, startIndex, endIndexExclusive, "CELL_LEVEL_CHARACTERISTICS", "INDICES" );
    }

    @Nullable
    private Characteristic getCellLevelCharacteristicAt( CellLevelCharacteristics clc, int cellIndex, String tableName, String indicesColumn ) {
        if ( clc.getIndices() != null ) {
            return clc.getCharacteristic( cellIndex );
        }
        byte[] result = ( byte[] ) getSessionFactory().getCurrentSession()
                .createSQLQuery( "select substring(" + indicesColumn + ", (4 * :offset) + 1, 4) from " + tableName + " cta where cta.ID = :id" )
                .setParameter( "id", clc.getId() )
                .setParameter( "offset", cellIndex )
                .uniqueResult();
        if ( result == null ) {
            return null;
        }
        int resultI = ByteBuffer.wrap( result ).asIntBuffer().get( 0 );
        return resultI != CellLevelCharacteristics.UNKNOWN_CHARACTERISTIC ? clc.getCharacteristics().get( resultI ) : null;
    }

    @Nullable
    private Characteristic[] getCellLevelCharacteristicAt( CellLevelCharacteristics clc, int startIndex, int endIndexExclusive, String tableName, String indicesColumn ) {
        if ( clc.getIndices() != null ) {
            Characteristic[] result = new Characteristic[endIndexExclusive - startIndex];
            int j = 0;
            for ( int i = startIndex; i < endIndexExclusive; i++ ) {
                result[j++] = clc.getCharacteristic( i );
            }
            return result;
        }
        byte[] result = ( byte[] ) getSessionFactory().getCurrentSession()
                .createSQLQuery( "select substring(" + indicesColumn + ", (4 * :offset) + 1, 4 * :len) from " + tableName + " cta where cta.ID = :id" )
                .setParameter( "id", clc.getId() )
                .setParameter( "offset", startIndex )
                .setParameter( "len", endIndexExclusive - startIndex )
                .uniqueResult();
        if ( result == null ) {
            return null;
        }
        if ( result.length != 4 * ( endIndexExclusive - startIndex ) ) {
            throw new IndexOutOfBoundsException( "Unexpected length of result: " + result.length );
        }
        IntBuffer bufferI = ByteBuffer.wrap( result ).asIntBuffer();
        Characteristic[] resultC = new Characteristic[endIndexExclusive - startIndex];
        for ( int i = 0; i < endIndexExclusive - startIndex; i++ ) {
            int resultI = bufferI.get( i );
            resultC[i] = resultI != CellLevelCharacteristics.UNKNOWN_CHARACTERISTIC ? clc.getCharacteristics().get( resultI ) : null;
        }
        return resultC;
    }

    @Override
    public Stream<Characteristic> streamCellLevelCharacteristics( CellLevelCharacteristics clc, boolean createNewSession ) {
        return streamCellLevelCharacteristics( clc, "CELL_LEVEL_CHARACTERISTICS", "INDICES", "CELL_LEVEL_CHARACTERISTICS_FK", "CELL_LEVEL_CHARACTERISTICS_ORDERING", createNewSession );
    }

    /**
     * Stream the characteristics of a given CLC.
     *
     * @param clc                    CLC to stream characteristics from
     * @param tableName              name of the table that stored the CLC
     * @param indicesColumn          column that contain indices
     * @param characteristicFk       foreign key in the CHARACTERISTICS table for the CLC
     * @param characteristicOrdering column in the CHARACTERISTICS table that orders characteristics
     * @param createNewSession       whether to create a new session tied to the returned stream or simply use the
     *                               current session
     * @return a stream over the characteristics or null if not found
     */
    @Nullable
    private Stream<Characteristic> streamCellLevelCharacteristics( CellLevelCharacteristics clc, String tableName, String indicesColumn, String characteristicFk, String characteristicOrdering, boolean createNewSession ) {
        return QueryUtils.createStream( getSessionFactory(), session -> {
            return session.doReturningWork( work -> {
                PreparedStatement stmt = work.prepareStatement( "select " + indicesColumn + " from " + tableName + " where ID = ?" );
                stmt.setLong( 1, clc.getId() );
                ResultSet rs = stmt.executeQuery();
                if ( rs.next() ) {
                    Map<Integer, Characteristic> cache = new HashMap<>();
                    return fromDataStream( new DataInputStream( rs.getBinaryStream( 1 ) ) )
                            .mapToObj( i -> getCharacteristicAt( clc, characteristicFk, characteristicOrdering, i, session, cache ) );
                } else {
                    return null;
                }
            } );
        }, createNewSession );
    }

    @Nullable
    private Characteristic getCharacteristicAt( CellLevelCharacteristics clc, String cfk, String cfo, int i, Session session, Map<Integer, Characteristic> cache ) {
        if ( i == -1 ) {
            return null;
        }
        if ( clc.getCharacteristics() != null ) {
            return clc.getCharacteristics().get( i );
        }
        return cache.computeIfAbsent( i, j -> ( Characteristic ) session
                .createSQLQuery( "select * from CHARACTERISTIC where " + cfk + " = :id and " + cfo + " = :i" )
                .addEntity( Characteristic.class )
                .setParameter( "id", clc.getId() )
                .setParameter( "i", i )
                .uniqueResult() );
    }

    private IntStream fromDataStream( DataInputStream dis ) {
        return Streams.of( new Iterator<Integer>() {

            private Integer _next;

            @Override
            public boolean hasNext() {
                fetchNextIfNecessary();
                return _next != null;
            }

            @Override
            public Integer next() {
                fetchNextIfNecessary();
                if ( _next == null ) {
                    throw new NoSuchElementException();
                }
                try {
                    return _next;
                } finally {
                    _next = null;
                }
            }

            private void fetchNextIfNecessary() {
                if ( _next == null ) {
                    try {
                        _next = dis.readInt();
                    } catch ( EOFException e ) {
                        _next = null;
                    } catch ( IOException e ) {
                        throw new RuntimeException( e );
                    }
                }
            }
        } ).mapToInt( i -> i );
    }

    @Override
    public List<CellTypeAssignment> getCellTypeAssignments( ExpressionExperiment ee ) {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                .createQuery( "select cta from SingleCellExpressionDataVector scedv "
                        + "join scedv.singleCellDimension scd "
                        + "join scd.cellTypeAssignments cta "
                        + "where scedv.expressionExperiment = :ee "
                        + "group by cta" )
                .setParameter( "ee", ee )
                .list();
    }

    @Override
    public List<CellTypeAssignment> getCellTypeAssignments( ExpressionExperiment expressionExperiment, QuantitationType qt ) {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                .createQuery( "select cta from SingleCellExpressionDataVector scedv "
                        + "join scedv.singleCellDimension scd "
                        + "join scd.cellTypeAssignments cta "
                        + "where scedv.expressionExperiment = :ee and scedv.quantitationType = :qt "
                        + "group by cta" )
                .setParameter( "ee", expressionExperiment )
                .setParameter( "qt", qt )
                .list();
    }

    @Override
    public Collection<CellTypeAssignment> getCellTypeAssignmentsWithoutIndices( ExpressionExperiment ee, QuantitationType qt ) {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                .createQuery( "select cta.id as id, cta.name as name, cta.description as description, cta.preferred as preferred, cta.numberOfCellTypes as numberOfCellTypes from SingleCellExpressionDataVector scedv "
                        + "join scedv.singleCellDimension scd "
                        + "join scd.cellTypeAssignments cta "
                        + "where scedv.quantitationType = :qt and scedv.expressionExperiment = :ee "
                        + "group by cta" )
                .setParameter( "ee", ee )
                .setParameter( "qt", qt )
                .setResultTransformer( new CtaInitializer( true, true ) )
                .list();
    }

    @Override
    public CellTypeAssignment getPreferredCellTypeAssignment( ExpressionExperiment ee ) {
        return ( CellTypeAssignment ) getSessionFactory().getCurrentSession()
                .createQuery( "select cta from SingleCellExpressionDataVector scedv "
                        + "join scedv.singleCellDimension scd "
                        + "join scd.cellTypeAssignments cta "
                        + "where scedv.quantitationType.isSingleCellPreferred = true and cta.preferred = true and scedv.expressionExperiment = :ee "
                        + "group by cta" )
                .setParameter( "ee", ee )
                .uniqueResult();
    }

    @Override
    public CellTypeAssignment getPreferredCellTypeAssignment( ExpressionExperiment ee, QuantitationType qt ) throws NonUniqueResultException {
        return ( CellTypeAssignment ) getSessionFactory().getCurrentSession()
                .createQuery( "select cta from SingleCellExpressionDataVector scedv "
                        + "join scedv.singleCellDimension scd "
                        + "join scd.cellTypeAssignments cta "
                        + "where scedv.quantitationType = :qt and cta.preferred = true and scedv.expressionExperiment = :ee "
                        + "group by cta" )
                .setParameter( "ee", ee )
                .setParameter( "qt", qt )
                .uniqueResult();
    }

    @Override
    public CellTypeAssignment getPreferredCellTypeAssignmentWithoutIndices( ExpressionExperiment ee, QuantitationType qt ) throws NonUniqueResultException {
        return ( CellTypeAssignment ) getSessionFactory().getCurrentSession()
                .createQuery( "select cta.id as id, cta.name as name, cta.description as description, cta.preferred as preferred, cta.numberOfCellTypes as numberOfCellTypes from SingleCellExpressionDataVector scedv "
                        + "join scedv.singleCellDimension scd "
                        + "join scd.cellTypeAssignments cta "
                        + "where scedv.quantitationType = :qt and cta.preferred = true and scedv.expressionExperiment = :ee "
                        + "group by cta" )
                .setParameter( "ee", ee )
                .setParameter( "qt", qt )
                .setResultTransformer( new CtaInitializer( true, true ) )
                .uniqueResult();
    }

    @Override
    public CellTypeAssignment getCellTypeAssignment( ExpressionExperiment expressionExperiment, QuantitationType qt, Long ctaId ) {
        return ( CellTypeAssignment ) getSessionFactory().getCurrentSession()
                .createQuery( "select cta from SingleCellExpressionDataVector scedv "
                        + "join scedv.singleCellDimension scd "
                        + "join scd.cellTypeAssignments cta "
                        + "where scedv.quantitationType = :qt and cta.id = :ctaId and scedv.expressionExperiment = :ee "
                        + "group by cta" )
                .setParameter( "ee", expressionExperiment )
                .setParameter( "qt", qt )
                .setParameter( "ctaId", ctaId )
                .uniqueResult();
    }

    @Override
    public CellTypeAssignment getCellTypeAssignmentWithoutIndices( ExpressionExperiment expressionExperiment, QuantitationType qt, Long ctaId ) {
        return ( CellTypeAssignment ) getSessionFactory().getCurrentSession()
                .createQuery( "select cta.id as id, cta.name as name, cta.description as description, cta.preferred as preferred, cta.numberOfCellTypes as numberOfCellTypes from SingleCellExpressionDataVector scedv "
                        + "join scedv.singleCellDimension scd "
                        + "join scd.cellTypeAssignments cta "
                        + "where scedv.quantitationType = :qt and cta.id = :ctaId and scedv.expressionExperiment = :ee "
                        + "group by cta" )
                .setParameter( "ee", expressionExperiment )
                .setParameter( "qt", qt )
                .setParameter( "ctaId", ctaId )
                .setResultTransformer( new CtaInitializer( true, true ) )
                .uniqueResult();
    }

    @Override
    public CellTypeAssignment getCellTypeAssignment( ExpressionExperiment expressionExperiment, QuantitationType qt, String ctaName ) {
        return ( CellTypeAssignment ) getSessionFactory().getCurrentSession()
                .createQuery( "select cta from SingleCellExpressionDataVector scedv "
                        + "join scedv.singleCellDimension scd "
                        + "join scd.cellTypeAssignments cta "
                        + "where scedv.quantitationType = :qt and cta.name = :ctaName and scedv.expressionExperiment = :ee "
                        + "group by cta" )
                .setParameter( "ee", expressionExperiment )
                .setParameter( "qt", qt )
                .setParameter( "ctaName", ctaName )
                .uniqueResult();
    }

    @Override
    public Collection<Protocol> getCellTypeAssignmentProtocols() {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                .createQuery( "select cta.protocol from CellTypeAssignment cta group by cta.protocol" )
                .list();
    }

    @Nullable
    @Override
    public Collection<CellTypeAssignment> getCellTypeAssignmentByProtocol( ExpressionExperiment ee, QuantitationType qt, String protocolName ) {
        //noinspection unchecked
        return ( List<CellTypeAssignment> ) getSessionFactory().getCurrentSession()
                .createQuery( "select cta from SingleCellExpressionDataVector scedv "
                        + "join scedv.singleCellDimension scd "
                        + "join scd.cellTypeAssignments cta "
                        + "join cta.protocol protocol "
                        + "where scedv.quantitationType = :qt and protocol.name = :protocolName and scedv.expressionExperiment = :ee "
                        + "group by cta" )
                .setParameter( "ee", ee )
                .setParameter( "qt", qt )
                .setParameter( "protocolName", protocolName )
                .list();
    }

    @Override
    public CellTypeAssignment getCellTypeAssignmentWithoutIndices( ExpressionExperiment expressionExperiment, QuantitationType qt, String ctaName ) {
        return ( CellTypeAssignment ) getSessionFactory().getCurrentSession()
                .createQuery( "select cta.id as id, cta.name as name, cta.description as description, cta.preferred as preferred, cta.numberOfCellTypes as numberOfCellTypes from SingleCellExpressionDataVector scedv "
                        + "join scedv.singleCellDimension scd "
                        + "join scd.cellTypeAssignments cta "
                        + "where scedv.quantitationType = :qt and cta.name = :ctaName and scedv.expressionExperiment = :ee "
                        + "group by cta" )
                .setParameter( "ee", expressionExperiment )
                .setParameter( "qt", qt )
                .setParameter( "ctaName", ctaName )
                .setResultTransformer( new CtaInitializer( true, true ) )
                .uniqueResult();
    }

    @Override
    public List<CellLevelCharacteristics> getCellLevelCharacteristics( ExpressionExperiment ee ) {
        List<CellLevelCharacteristics> results = new ArrayList<>( getCellTypeAssignments( ee ) );
        //noinspection unchecked
        results.addAll( getSessionFactory().getCurrentSession()
                .createQuery( "select clc from SingleCellExpressionDataVector scedv "
                        + "join scedv.singleCellDimension scd join scd.cellLevelCharacteristics clc "
                        + "where scedv.expressionExperiment = :ee "
                        + "group by clc" )
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
                .createQuery( "select clc from SingleCellExpressionDataVector scedv join scedv.singleCellDimension scd join scd.cellLevelCharacteristics clc join clc.characteristics c where scedv.expressionExperiment = :ee and coalesce(c.categoryUri, c.category) = :c group by clc" )
                .setParameter( "ee", ee )
                .setParameter( "c", category.getCategoryUri() != null ? category.getCategoryUri() : category.getCategory() )
                .list() );
        return results;
    }

    @Override
    public CellLevelCharacteristics getCellLevelCharacteristics( ExpressionExperiment ee, QuantitationType qt, Long clcId ) {
        return ( CellLevelCharacteristics ) getSessionFactory().getCurrentSession()
                .createQuery( "select clc from SingleCellExpressionDataVector scedv "
                        + "join scedv.singleCellDimension scd "
                        + "join scd.cellLevelCharacteristics clc join clc.characteristics c "
                        + "where scedv.expressionExperiment = :ee and c.id = :clcId "
                        + "group by clc" )
                .setParameter( "ee", ee )
                .setParameter( "clcId", clcId )
                .uniqueResult();
    }

    @Nullable
    @Override
    public CellLevelCharacteristics getCellLevelCharacteristics( ExpressionExperiment ee, QuantitationType qt, String clcName ) {
        return ( CellLevelCharacteristics ) getSessionFactory().getCurrentSession()
                .createQuery( "select clc from SingleCellExpressionDataVector scedv "
                        + "join scedv.singleCellDimension scd "
                        + "join scd.cellLevelCharacteristics clc join clc.characteristics c "
                        + "where scedv.expressionExperiment = :ee and c.name = :clcName "
                        + "group by clc" )
                .setParameter( "ee", ee )
                .setParameter( "clcName", clcName )
                .uniqueResult();
    }

    @Override
    public List<CellLevelCharacteristics> getCellLevelCharacteristics( ExpressionExperiment expressionExperiment, QuantitationType qt ) {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                .createQuery( "select clc from SingleCellExpressionDataVector scedv "
                        + "join scedv.singleCellDimension scd "
                        + "join scd.cellLevelCharacteristics clc join clc.characteristics c "
                        + "where scedv.expressionExperiment = :ee "
                        + "group by clc" )
                .setParameter( "ee", expressionExperiment )
                .list();
    }

    @Override
    public List<CellLevelCharacteristics> getCellLevelCharacteristics( ExpressionExperiment expressionExperiment, QuantitationType qt, Category category ) {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                .createQuery( "select clc from SingleCellExpressionDataVector scedv "
                        + "join scedv.singleCellDimension scd "
                        + "join scd.cellLevelCharacteristics clc join clc.characteristics c "
                        + "where scedv.expressionExperiment = :ee "
                        + "and scedv.quantitationType = :qt "
                        + "and coalesce(c.categoryUri, c.category) = :c "
                        + "group by clc" )
                .setParameter( "ee", expressionExperiment )
                .setParameter( "qt", qt )
                .setParameter( "c", category.getCategoryUri() != null ? category.getCategoryUri() : category.getCategory() )
                .list();
    }

    @Override
    public CellLevelCharacteristics getCellLevelCharacteristicsWithoutIndices( ExpressionExperiment ee, QuantitationType qt, Long clcId ) {
        return ( CellLevelCharacteristics ) getSessionFactory().getCurrentSession()
                .createQuery( "select clc.id as id, clc.numberOfCharacteristics as numberOfCharacteristics, clc.numberOfAssignedCells as numberOfAssignedCells "
                        + "from SingleCellExpressionDataVector scedv "
                        + "join scedv.singleCellDimension scd "
                        + "join scd.cellLevelCharacteristics clc "
                        + "where scedv.expressionExperiment = :ee and scedv.quantitationType = :qt and clc.id = :clcId "
                        + "group by clc" )
                .setParameter( "ee", ee )
                .setParameter( "qt", qt )
                .setParameter( "clcId", clcId )
                .setResultTransformer( new ClcInitializer( true ) )
                .uniqueResult();
    }

    @Override
    public CellLevelCharacteristics getCellLevelCharacteristicsWithoutIndices( ExpressionExperiment ee, QuantitationType qt, String clcName ) {
        return ( CellLevelCharacteristics ) getSessionFactory().getCurrentSession()
                .createQuery( "select clc.id as id, clc.numberOfCharacteristics as numberOfCharacteristics, clc.numberOfAssignedCells as numberOfAssignedCells "
                        + "from SingleCellExpressionDataVector scedv "
                        + "join scedv.singleCellDimension scd "
                        + "join scd.cellLevelCharacteristics clc "
                        + "where scedv.expressionExperiment = :ee and scedv.quantitationType = :qt and clc.name = :clcName "
                        + "group by clc" )
                .setParameter( "ee", ee )
                .setParameter( "qt", qt )
                .setParameter( "clcName", clcName )
                .setResultTransformer( new ClcInitializer( true ) )
                .uniqueResult();
    }

    @Override
    public Collection<CellLevelCharacteristics> getCellLevelCharacteristicsWithoutIndices( ExpressionExperiment ee, QuantitationType qt ) {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                .createQuery( "select clc.id as id, clc.numberOfCharacteristics as numberOfCharacteristics, clc.numberOfAssignedCells as numberOfAssignedCells "
                        + "from SingleCellExpressionDataVector scedv "
                        + "join scedv.singleCellDimension scd "
                        + "join scd.cellLevelCharacteristics clc "
                        + "where scedv.expressionExperiment = :ee and scedv.quantitationType = :qt "
                        + "group by clc" )
                .setParameter( "ee", ee )
                .setParameter( "qt", qt )
                .setResultTransformer( new ClcInitializer( true ) )
                .list();
    }

    @Override
    public List<Characteristic> getCellTypes( ExpressionExperiment ee ) {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                .createQuery( "select ct from SingleCellExpressionDataVector scedv "
                        + "join scedv.singleCellDimension scd "
                        + "join scd.cellTypeAssignments cta "
                        + "join cta.cellTypes ct "
                        + "where scedv.expressionExperiment = :ee and scedv.quantitationType.isSingleCellPreferred = true and cta.preferred = true "
                        + "group by ct" )
                .setParameter( "ee", ee )
                .list();
    }

    @Override
    public List<QuantitationType> getSingleCellQuantitationTypes( ExpressionExperiment ee ) {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                .createQuery( "select scedv.quantitationType from SingleCellExpressionDataVector scedv "
                        + "where scedv.expressionExperiment = :ee "
                        + "group by scedv.quantitationType" )
                .setParameter( "ee", ee )
                .list();
    }

    @Override
    public boolean hasSingleCellQuantitationTypes( ExpressionExperiment ee ) {
        return ( Boolean ) getSessionFactory().getCurrentSession()
                .createQuery( "select count(*) > 0 from SingleCellExpressionDataVector scedv "
                        + "where scedv.expressionExperiment = :ee" )
                .setParameter( "ee", ee )
                .uniqueResult();
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
    public List<SingleCellExpressionDataVector> getSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType, boolean includeBiologicalCharacteristics, boolean includeCellIds, boolean includeData, boolean includeDataIndices ) {
        SingleCellDimension dimension = includeCellIds ? getSingleCellDimension( ee, quantitationType ) : getSingleCellDimensionWithoutCellIds( ee, quantitationType );
        if ( dimension == null ) {
            throw new IllegalStateException( quantitationType + " from " + ee + " does not have an associated single-cell dimension." );
        }
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                .createQuery( createSelectSingleCellDataVectorQuery( true, includeData, includeDataIndices ) + " "
                        + "where scedv.expressionExperiment = :ee and scedv.quantitationType = :qt" )
                .setParameter( "ee", ee )
                .setParameter( "qt", quantitationType )
                .setResultTransformer( new SingleCellDataVectorInitializer( ee, null, quantitationType, dimension, includeBiologicalCharacteristics, includeData, includeDataIndices ) )
                .list();
    }

    @Override
    public Stream<SingleCellExpressionDataVector> streamSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType, int fetchSize, boolean useCursorFetchIfSupported, boolean createNewSession ) {
        return streamQuery( session -> {
            // prefetch all the entities to make the query effectively stateless
            log.info( "Prefetching related entities and design elements for " + ee + " and " + quantitationType + "..." );
            ExpressionExperiment prefetchedEe = ( ExpressionExperiment ) requireNonNull( session.get( ExpressionExperiment.class, ee.getId() ) );
            QuantitationType prefetchedQt = ( QuantitationType ) requireNonNull( session.get( QuantitationType.class, quantitationType.getId() ) );
            SingleCellDimension prefetchedDimension = getSingleCellDimension( prefetchedEe, prefetchedQt, session );
            if ( prefetchedDimension == null ) {
                throw new IllegalStateException( quantitationType + " from " + ee + " does not have an associated single-cell dimension." );
            }
            prefetchDesignElements( ee, quantitationType, session );
            log.info( String.format( "Querying single-cell vectors for %s and %s with a fetch size of %d%s...",
                    ee, quantitationType, fetchSize,
                    useCursorFetchIfSupported ? " and cursor fetching" : "" ) );
            return session.createQuery( "select scedv from SingleCellExpressionDataVector scedv "
                            + "where scedv.expressionExperiment = :ee and scedv.quantitationType = :qt" )
                    .setParameter( "ee", ee )
                    .setParameter( "qt", quantitationType );
        }, SingleCellExpressionDataVector.class, fetchSize, useCursorFetchIfSupported, true, createNewSession );
    }

    @Override
    public Stream<SingleCellExpressionDataVector> streamSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType, int fetchSize, boolean useCursorFetchIfSupported, boolean createNewSession, boolean includeBiologicalCharacteristics, boolean includeCellIds, boolean includeData, boolean includeDataIndices ) {
        SingleCellDimension dimension = includeCellIds ? getSingleCellDimension( ee, quantitationType ) : getSingleCellDimensionWithoutCellIds( ee, quantitationType );
        if ( dimension == null ) {
            throw new IllegalStateException( quantitationType + " from " + ee + " does not have an associated single-cell dimension." );
        }
        return streamQuery( session -> {
                    log.info( "Prefetching design elements for " + ee + " and " + quantitationType + "..." );
                    prefetchDesignElements( ee, quantitationType, session );
                    log.info( String.format( "Querying single-cell vectors for %s and %s with a fetch size of %d%s...",
                            ee, quantitationType, fetchSize,
                            useCursorFetchIfSupported ? " and cursor fetching" : "" ) );
                    return session.createQuery( createSelectSingleCellDataVectorQuery( true, includeData, includeDataIndices ) + " "
                                    + "where scedv.expressionExperiment = :ee and scedv.quantitationType = :qt" )
                            .setParameter( "ee", ee )
                            .setParameter( "qt", quantitationType )
                            .setResultTransformer( new SingleCellDataVectorInitializer( ee, null, quantitationType, dimension, includeBiologicalCharacteristics, includeData, includeDataIndices ) );
                },
                SingleCellExpressionDataVector.class,
                fetchSize,
                useCursorFetchIfSupported, true, createNewSession );
    }

    /**
     * Prefetch design elements in a given session such that streaming single-cell vectors becomes effectively stateless.
     */
    private void prefetchDesignElements( ExpressionExperiment ee, QuantitationType quantitationType, Session session ) {
        session.createQuery( "select vector.designElement from SingleCellExpressionDataVector vector where vector.expressionExperiment = :ee and vector.quantitationType = :qt" )
                .setParameter( "ee", ee )
                .setParameter( "qt", quantitationType )
                .list();
    }

    @Override
    public SingleCellExpressionDataVector getSingleCellDataVectorWithoutCellIds( ExpressionExperiment ee, QuantitationType quantitationType, CompositeSequence designElement ) {
        SingleCellDimension dimension = getSingleCellDimensionWithoutCellIds( ee, quantitationType );
        if ( dimension == null ) {
            throw new IllegalStateException( quantitationType + " from " + ee + " does not have an associated single-cell dimension." );
        }
        return ( SingleCellExpressionDataVector ) getSessionFactory().getCurrentSession()
                .createQuery( createSelectSingleCellDataVectorQuery( false, true, true ) + " "
                        + "where scedv.expressionExperiment = :ee and scedv.quantitationType = :qt and scedv.designElement = :de" )
                .setParameter( "ee", ee )
                .setParameter( "qt", quantitationType )
                .setParameter( "de", designElement )
                .setResultTransformer( new SingleCellDataVectorInitializer( ee, designElement, quantitationType, dimension, false, true, true ) )
                .uniqueResult();
    }

    private String createSelectSingleCellDataVectorQuery( boolean includeDesignElement, boolean includeData, boolean includeDataIndices ) {
        return "select scedv.id as id, "
                + ( includeDesignElement ? "scedv.designElement as designElement, " : "" )
                + ( includeData ? "scedv.data as data, " : "" )
                + ( includeDataIndices ? "scedv.dataIndices as dataIndices, " : "" )
                + "scedv.originalDesignElement as originalDesignElement from SingleCellExpressionDataVector scedv";
    }

    private static class SingleCellDataVectorInitializer implements TypedResultTransformer<SingleCellExpressionDataVector> {

        private final ExpressionExperiment ee;
        @Nullable
        private final CompositeSequence designElement;
        private final QuantitationType qt;
        private final SingleCellDimension dimension;
        private final boolean includeBiologicalCharacteristics;
        private final boolean includeData;
        private final boolean includeDataIndices;

        public SingleCellDataVectorInitializer( ExpressionExperiment ee, @Nullable CompositeSequence designElement, QuantitationType qt, SingleCellDimension dimension, boolean includeBiologicalCharacteristics, boolean includeData, boolean includeDataIndices ) {
            this.ee = ee;
            this.designElement = designElement;
            this.qt = qt;
            this.dimension = dimension;
            this.includeBiologicalCharacteristics = includeBiologicalCharacteristics;
            this.includeData = includeData;
            this.includeDataIndices = includeDataIndices;
        }

        @Override
        public SingleCellExpressionDataVector transformTuple( Object[] tuple, String[] aliases ) {
            SingleCellExpressionDataVector vector = ( SingleCellExpressionDataVector ) aliasToBean( SingleCellExpressionDataVector.class ).transformTuple( tuple, aliases );
            vector.setExpressionExperiment( ee );
            if ( designElement != null ) {
                vector.setDesignElement( designElement );
            }
            if ( includeBiologicalCharacteristics ) {
                Hibernate.initialize( vector.getDesignElement().getBiologicalCharacteristic() );
            }
            vector.setQuantitationType( qt );
            vector.setSingleCellDimension( dimension );
            if ( !includeData ) {
                vector.setData( null );
            }
            if ( !includeDataIndices ) {
                vector.setDataIndices( null );
            }
            return vector;
        }

        @Override
        public List<SingleCellExpressionDataVector> transformListTyped( List<SingleCellExpressionDataVector> collection ) {
            return collection;
        }
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
    public Map<BioAssay, Long> getNumberOfNonZeroesBySample( ExpressionExperiment ee, QuantitationType qt, int fetchSize, boolean useCursorFetchIfSupported ) {
        SingleCellDimension dimension = getSingleCellDimensionWithoutCellIds( ee, qt );
        if ( dimension == null ) {
            throw new IllegalStateException( qt + " from " + ee + " does not have an associated single-cell dimension." );
        }
        long numVecs = getNumberOfSingleCellDataVectors( ee, qt );
        try ( Stream<Object[]> stream = QueryUtils.stream( getSessionFactory().getCurrentSession()
                // FIXME: there's a bug in Hibernate scroll() ScrollableResults implementation that causes the native
                //        int[] array to be cast to Object[], so we need to add a dummy column to avoid this.
                .createQuery( "select scedv.dataIndices, 1 from SingleCellExpressionDataVector scedv "
                        + "where scedv.expressionExperiment = :ee and scedv.quantitationType = :qt" )
                .setParameter( "ee", ee )
                .setParameter( "qt", qt ), Object[].class, fetchSize, useCursorFetchIfSupported, true ) ) {
            long[] nnzs = new long[dimension.getBioAssays().size()];
            Iterator<Object[]> it = stream.iterator();
            StopWatch timer = StopWatch.createStarted();
            int done = 0;
            long bytesRetrieved = 0;
            while ( it.hasNext() ) {
                int[] row = ( int[] ) it.next()[0];
                // the first sample starts at zero, the use the end as the start of the next one
                int start = 0;
                for ( int i = 0; i < dimension.getBioAssays().size(); i++ ) {
                    int end = SingleCellExpressionDataVectorUtils.getSampleEnd( dimension, row, i, start );
                    nnzs[i] += end - start;
                    start = end;
                }
                bytesRetrieved += 4L * row.length;
                if ( ++done % 100 == 0 ) {
                    log.info( String.format( "Retrieving data indices of %s in %s [%d/%d] @ %.2f vectors/sec and @ ~%s",
                            qt.getName(), ee.getShortName(),
                            done, numVecs,
                            1000.0 * done / timer.getTime(),
                            bytePerSecondToDisplaySize( 1000.0 * bytesRetrieved / timer.getTime() ) ) );
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
        return removeAllSingleCellDataVectors( ee, false );
    }

    private int removeAllSingleCellDataVectors( ExpressionExperiment ee, boolean keepDimensions ) {
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
        if ( !keepDimensions ) {
            removeUnusedSingleCellDimensions( ee );
        }
        if ( deletedVectors > 0 ) {
            log.info( "Removed " + deletedVectors + " single-cell data vectors from " + ee );
        }
        return deletedVectors;
    }

    /**
     * Remove all unused single-cell dimensions.
     */
    private void removeUnusedSingleCellDimensions( ExpressionExperiment ee ) {
        Collection<SingleCellDimension> dimensions = getSingleCellDimensions( ee );
        for ( SingleCellDimension scd : dimensions ) {
            List<QuantitationType> otherUsers = list( getSessionFactory().getCurrentSession()
                    .createQuery( "select vec.quantitationType from SingleCellExpressionDataVector vec "
                            + "where vec.expressionExperiment = :ee and vec.singleCellDimension = :dim "
                            + "group by vec.quantitationType" )
                    .setParameter( "dim", scd ) );
            if ( !otherUsers.isEmpty() ) {
                log.warn( scd + " is used by " + otherUsers.size() + " sets of vectors, it will not be deleted." );
                continue;
            }
            deleteSingleCellDimension( ee, scd );
        }
    }

    @Override
    public Map<BioAssay, Long> getNumberOfDesignElementsPerSample( ExpressionExperiment expressionExperiment ) {
        //noinspection unchecked
        List<Object[]> result = getSessionFactory().getCurrentSession()
                .createQuery( "select ba, count(*) from ExpressionExperiment ee "
                        + "join ee.bioAssays ba join ba.arrayDesignUsed ad "
                        + "join ad.compositeSequences cs where ee = :ee "
                        + "group by ba" )
                .setParameter( "ee", expressionExperiment )
                .setCacheable( true )
                .list();
        return result.stream()
                .collect( Collectors.toMap( o -> ( BioAssay ) ( ( Object[] ) o )[0], o -> ( Long ) ( ( Object[] ) o )[1] ) );
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
        configurer.registerObjectAlias( "characteristics.", CHARACTERISTIC_ALIAS, Characteristic.class, null, 1, true );
        configurer.unregisterProperty( "characteristics.originalValue" );
        configurer.unregisterProperty( "characteristics.migratedToStatement" );
        configurer.registerObjectAlias( "experimentalDesign.experimentalFactors.factorValues.characteristics.", FACTOR_VALUE_CHARACTERISTIC_ALIAS, Statement.class, null, 1, true );
        configurer.registerProperty( "experimentalDesign.experimentalFactors.factorValues.characteristics.subject", true );
        configurer.registerProperty( "experimentalDesign.experimentalFactors.factorValues.characteristics.subjectUri", true );
        configurer.deprecateProperty( "experimentalDesign.experimentalFactors.factorValues.characteristics.value" );
        configurer.describeProperty( "experimentalDesign.experimentalFactors.factorValues.characteristics.value",
                "use experimentalDesign.experimentalFactors.factorValues.characteristics.subject instead" );
        configurer.deprecateProperty( "experimentalDesign.experimentalFactors.factorValues.characteristics.valueUri" );
        configurer.describeProperty( "experimentalDesign.experimentalFactors.factorValues.characteristics.valueUri",
                "use experimentalDesign.experimentalFactors.factorValues.characteristics.subjectUri instead" );
        configurer.unregisterProperty( "experimentalDesign.experimentalFactors.factorValues.characteristics.secondPredicate" );
        configurer.unregisterProperty( "experimentalDesign.experimentalFactors.factorValues.characteristics.secondPredicateUri" );
        configurer.unregisterProperty( "experimentalDesign.experimentalFactors.factorValues.characteristics.secondObject" );
        configurer.unregisterProperty( "experimentalDesign.experimentalFactors.factorValues.characteristics.secondObjectUri" );
        configurer.unregisterProperty( "experimentalDesign.experimentalFactors.factorValues.characteristics.originalValue" );
        configurer.unregisterProperty( "experimentalDesign.experimentalFactors.factorValues.characteristics.migratedToStatement" );
        configurer.registerObjectAlias( "bioAssays.sampleUsed.characteristics.", BIO_MATERIAL_CHARACTERISTIC_ALIAS, Characteristic.class, null, 1, true );
        configurer.unregisterProperty( "bioAssays.sampleUsed.characteristics.migratedToStatement" );
        configurer.unregisterProperty( "bioAssays.sampleUsed.characteristics.originalValue" );
        configurer.registerObjectAlias( "allCharacteristics.", ALL_CHARACTERISTIC_ALIAS, Statement.class, null, 1, true );
        configurer.registerProperty( "allCharacteristics.subject", true );
        configurer.registerProperty( "allCharacteristics.subjectUri", true );
        configurer.unregisterProperty( "allCharacteristics.secondPredicate" );
        configurer.unregisterProperty( "allCharacteristics.secondPredicateUri" );
        configurer.unregisterProperty( "allCharacteristics.secondObject" );
        configurer.unregisterProperty( "allCharacteristics.secondObjectUri" );
        configurer.unregisterProperty( "allCharacteristics.originalValue" );
        configurer.unregisterProperty( "allCharacteristics.migratedToStatement" );

        configurer.registerObjectAlias( "bioAssays.", BIO_ASSAY_ALIAS, BioAssay.class, null, 2, true );
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
    protected FilterablePropertyMeta.FilterablePropertyMetaBuilder resolveFilterablePropertyMeta( String propertyName ) {
        switch ( propertyName ) {
            // allCharacteristics contains a mixture of statements and characteristics, so we need to clarify which ones
            // are statement-specific
            case "allCharacteristics.subject":
                return resolveFilterablePropertyMeta( ALL_CHARACTERISTIC_ALIAS, Statement.class, "value" )
                        .description( "only applicable to statements" );
            case "allCharacteristics.subjectUri":
                return resolveFilterablePropertyMeta( ALL_CHARACTERISTIC_ALIAS, Statement.class, "valueUri" )
                        .description( "only applicable to statements" );
            case "allCharacteristics.predicate":
            case "allCharacteristics.predicateUri":
            case "allCharacteristics.object":
            case "allCharacteristics.objectUri":
                return super.resolveFilterablePropertyMeta( propertyName )
                        .description( "only applicable to statements" );

            // expose statements subject/subjectUri as aliases for value/valueUri
            case "experimentalDesign.experimentalFactors.factorValues.characteristics.subject":
                return resolveFilterablePropertyMeta( FACTOR_VALUE_CHARACTERISTIC_ALIAS, Statement.class, "value" );
            case "experimentalDesign.experimentalFactors.factorValues.characteristics.subjectUri":
                return resolveFilterablePropertyMeta( FACTOR_VALUE_CHARACTERISTIC_ALIAS, Statement.class, "valueUri" );

            // pretend that value/valueUri are aliases for subject/subjectUri even if it's not really the case in the
            // data model
            case "experimentalDesign.experimentalFactors.factorValues.characteristics.value":
                return super.resolveFilterablePropertyMeta( propertyName )
                        .description( "alias for experimentalDesign.experimentalFactors.factorValues.characteristics.subject" );
            case "experimentalDesign.experimentalFactors.factorValues.characteristics.valueUri":
                return super.resolveFilterablePropertyMeta( propertyName )
                        .description( "alias for experimentalDesign.experimentalFactors.factorValues.characteristics.subjectUri" );

            case "taxon":
                return resolveFilterablePropertyMeta( TAXON_ALIAS, Taxon.class, "id" )
                        .description( "alias for taxon.id" );
            case "bioAssayCount":
                return super.resolveFilterablePropertyMeta( "bioAssays.size" )
                        .description( "alias for bioAssays.size" );
            case "geeq.publicQualityScore":
                return FilterablePropertyMeta.builder()
                        .propertyName( "(case when geeq.manualQualityOverride = true then geeq.manualQualityScore else geeq.detectedQualityScore end)" )
                        .propertyType( Double.class );
            case "geeq.publicSuitabilityScore":
                return FilterablePropertyMeta.builder()
                        .propertyName( "(case when geeq.manualSuitabilityOverride = true then geeq.manualSuitabilityScore else geeq.detectedSuitabilityScore end)" )
                        .propertyType( Double.class );
            default:
                return super.resolveFilterablePropertyMeta( propertyName );
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
    public Collection<RawExpressionDataVector> getRawDataVectors( ExpressionExperiment ee, List<BioAssay> assays, QuantitationType qt ) {
        Assert.isTrue( !assays.isEmpty(), "At least one assay must be requested." );
        Assert.isTrue( new HashSet<>( assays ).size() == assays.size() );
        int sizeInBytes = qt.getRepresentation().getSizeInBytes();
        Assert.isTrue( sizeInBytes != -1, "Variable-length representation " + qt.getRepresentation() + " cannot be sliced." );
        BioAssayDimension bad = requireNonNull( getBioAssayDimension( ee, qt ), "Could not find a BAD for " + qt + " in " + ee + "." );
        if ( bad.getBioAssays().equals( assays ) ) {
            log.info( "Requesting all assays for " + ee + " and " + qt + ", returning the original, unsliced vectors." );
            return getRawDataVectors( ee, qt );
        }
        Map<BioAssay, Integer> assay2index = ListUtils.indexOfElements( bad.getBioAssays() );
        int[] columns = new int[assays.size()];
        for ( int i = 0; i < assays.size(); i++ ) {
            Integer ix = assay2index.get( assays.get( i ) );
            if ( ix == null ) {
                throw new IllegalArgumentException( assays.get( i ) + " does not appear in " + bad + "." );
            }
            columns[i] = ix;
        }
        String[] stuffToConcat = new String[columns.length];
        for ( int i = 0; i < columns.length; i++ ) {
            stuffToConcat[i] = "substring(v.data, " + ( sizeInBytes * columns[i] + 1 ) + ", " + sizeInBytes + ")";
        }
        //noinspection unchecked
        List<Object[]> result = getSessionFactory().getCurrentSession()
                .createQuery( "select v.designElement, cast(concat(" + String.join( ", ", stuffToConcat ) + ") as binary) from RawExpressionDataVector v "
                        + "where v.expressionExperiment = :ee and v.quantitationType = :qt" )
                .setParameter( "ee", ee )
                .setParameter( "qt", qt )
                .list();
        BioAssayDimension newBad = BioAssayDimension.Factory.newInstance( assays );
        List<RawExpressionDataVector> vectors = new ArrayList<>( result.size() );
        for ( Object[] row : result ) {
            CompositeSequence designElement = ( CompositeSequence ) row[0];
            byte[] data = Arrays.copyOfRange( ( byte[] ) row[1], 0, columns.length * sizeInBytes );
            if ( data.length != columns.length * sizeInBytes ) {
                throw new IndexOutOfBoundsException();
            }
            RawExpressionDataVector vector = new RawExpressionDataVector();
            vector.setExpressionExperiment( ee );
            vector.setQuantitationType( qt );
            vector.setBioAssayDimension( newBad );
            vector.setDesignElement( designElement );
            vector.setData( data );
            vectors.add( vector );
        }
        return vectors;
    }

    @Override
    public Collection<RawExpressionDataVector> getPreferredRawDataVectors( ExpressionExperiment ee ) {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession().createQuery(
                        "select dedv from RawExpressionDataVector dedv "
                                + "join dedv.quantitationType q "
                                + "where q.isPreferred = true and dedv.expressionExperiment = :ee" )
                .setParameter( "ee", ee )
                .list();
    }

    @Override
    public Map<QuantitationType, Collection<RawExpressionDataVector>> getMissingValueVectors( ExpressionExperiment ee ) {
        //noinspection unchecked
        return ( ( List<RawExpressionDataVector> ) getSessionFactory().getCurrentSession().createQuery(
                        "select dedv from RawExpressionDataVector dedv "
                                + "join dedv.quantitationType q "
                                + "where q.type = 'PRESENTABSENT' and dedv.expressionExperiment  = :ee " )
                .setParameter( "ee", ee )
                .list() ).stream()
                .collect( Collectors.groupingBy( RawExpressionDataVector::getQuantitationType, Collectors.toCollection( ArrayList::new ) ) );
    }

    @Override
    public int addRawDataVectors( ExpressionExperiment ee, QuantitationType newQt, Collection<RawExpressionDataVector> newVectors ) {
        Assert.notNull( ee.getId(), "ExpressionExperiment must be persistent." );
        Assert.notNull( newQt.getId(), "Quantitation type must be persistent." );
        Assert.isTrue( !newVectors.isEmpty(), "At least one vectors must be provided, use removeAllRawDataVectors() to delete vectors instead." );
        // each set of raw vectors must have a *distinct* QT
        Assert.isTrue( !ee.getQuantitationTypes().contains( newQt ),
                "ExpressionExperiment already has a quantitation like " + newQt );
        checkVectors( ee, newQt, newVectors );
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
        return removeAllRawDataVectors( ee, false );
    }

    private int removeAllRawDataVectors( ExpressionExperiment ee, boolean keepDimensions ) {
        Assert.notNull( ee.getId(), "ExpressionExperiment must be persistent." );
        Set<QuantitationType> qtsToRemove = ee.getRawExpressionDataVectors().stream()
                .map( DataVector::getQuantitationType )
                .collect( Collectors.toSet() );
        Set<BioAssayDimension> dimensions = ee.getRawExpressionDataVectors().stream()
                .map( BulkExpressionDataVector::getBioAssayDimension )
                .collect( Collectors.toSet() );
        ee.getRawExpressionDataVectors().clear();
        int deletedVectors = getSessionFactory().getCurrentSession()
                .createQuery( "delete from RawExpressionDataVector v where v.expressionExperiment = :ee" )
                .setParameter( "ee", ee )
                .executeUpdate();
        // remove QTs and unused dimensions
        removeQts( ee, qtsToRemove );
        update( ee );
        if ( !keepDimensions ) {
            removeUnusedDimensions( ee, dimensions );
        }
        if ( deletedVectors > 0 ) {
            log.info( "Deleted all " + deletedVectors + " raw data vectors from " + ee + " for " + qtsToRemove.size() + " quantitation types." );
        }
        return deletedVectors;
    }

    @Override
    public int removeRawDataVectors( ExpressionExperiment ee, QuantitationType qt, boolean keepDimension ) {
        Assert.notNull( ee.getId(), "ExpressionExperiment must be persistent." );
        Assert.notNull( qt.getId(), "Quantitation type must be persistent" );
        Assert.isTrue( ee.getQuantitationTypes().contains( qt ) || ee.getRawExpressionDataVectors().stream().anyMatch( v -> v.getQuantitationType().equals( qt ) ),
                "The provided quantitation type must belong to at least one raw vector of the experiment." );
        Set<BioAssayDimension> dimensions = ee.getRawExpressionDataVectors().stream()
                .filter( v -> v.getQuantitationType().equals( qt ) )
                .map( BulkExpressionDataVector::getBioAssayDimension )
                .collect( Collectors.toSet() );
        ee.getRawExpressionDataVectors().clear();
        int deletedVectors = getSessionFactory().getCurrentSession()
                .createQuery( "delete from RawExpressionDataVector v where v.expressionExperiment = :ee and v.quantitationType = :qt" )
                .setParameter( "ee", ee )
                .setParameter( "qt", qt )
                .executeUpdate();
        removeQts( ee, Collections.singleton( qt ) );
        update( ee );
        if ( !keepDimension ) {
            removeUnusedDimensions( ee, dimensions );
        }
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
        Set<BioAssayDimension> dimensions = ee.getRawExpressionDataVectors().stream()
                .filter( v -> v.getQuantitationType().equals( qt ) )
                .map( BulkExpressionDataVector::getBioAssayDimension )
                .collect( Collectors.toSet() );
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
        removeUnusedDimensions( ee, dimensions );
        if ( deletedVectors > 0 ) {
            log.info( "Replaced " + deletedVectors + " raw data vectors from " + ee + " for " + qt );
        }
        return deletedVectors;
    }

    @Override
    public Collection<ProcessedExpressionDataVector> getProcessedDataVectors( ExpressionExperiment ee ) {
        QuantitationType qt = getProcessedQuantitationType( ee );
        if ( qt == null ) {
            return null;
        }
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                .createQuery( "select vec from ProcessedExpressionDataVector vec where vec.expressionExperiment = :ee and vec.quantitationType = :qt" )
                .setParameter( "ee", ee )
                .setParameter( "qt", qt )
                .list();
    }

    @Override
    public Collection<ProcessedExpressionDataVector> getProcessedDataVectors( ExpressionExperiment ee, List<BioAssay> assays ) {
        Assert.isTrue( !assays.isEmpty(), "At least one assay must be requested." );
        Assert.isTrue( new HashSet<>( assays ).size() == assays.size() );
        QuantitationType qt = getProcessedQuantitationType( ee );
        if ( qt == null ) {
            return null;
        }
        int sizeInBytes = qt.getRepresentation().getSizeInBytes();
        Assert.isTrue( sizeInBytes != -1, "Variable-length representation " + qt.getRepresentation() + " cannot be sliced." );
        BioAssayDimension bad = requireNonNull( getBioAssayDimension( ee, qt ), "Could not find a BAD for " + qt + " in " + ee + "." );
        if ( bad.getBioAssays().equals( assays ) ) {
            log.info( "Requesting all assays for " + ee + " and " + qt + ", returning the original, unsliced vectors." );
            return getProcessedDataVectors( ee );
        }
        Map<BioAssay, Integer> assay2index = ListUtils.indexOfElements( bad.getBioAssays() );
        int[] columns = new int[assays.size()];
        for ( int i = 0; i < assays.size(); i++ ) {
            Integer ix = assay2index.get( assays.get( i ) );
            if ( ix == null ) {
                throw new IllegalArgumentException( assays.get( i ) + " does not appear in " + bad + "." );
            }
            columns[i] = ix;
        }
        String[] stuffToConcat = new String[columns.length];
        for ( int i = 0; i < columns.length; i++ ) {
            stuffToConcat[i] = "substring(v.data, " + ( sizeInBytes * columns[i] + 1 ) + ", " + sizeInBytes + ")";
        }
        //noinspection unchecked
        List<Object[]> result = getSessionFactory().getCurrentSession()
                .createQuery( "select v.designElement, cast(concat(" + String.join( ", ", stuffToConcat ) + ") as binary), v.rankByMax, v.rankByMean from ProcessedExpressionDataVector v "
                        + "where v.expressionExperiment = :ee and v.quantitationType = :qt" )
                .setParameter( "ee", ee )
                .setParameter( "qt", qt )
                .list();
        BioAssayDimension newBad = BioAssayDimension.Factory.newInstance( assays );
        List<ProcessedExpressionDataVector> vectors = new ArrayList<>( result.size() );
        for ( Object[] row : result ) {
            CompositeSequence designElement = ( CompositeSequence ) row[0];
            byte[] data = ( byte[] ) row[1];
            Double rankByMax = ( Double ) row[2];
            Double rankByMean = ( Double ) row[3];
            ProcessedExpressionDataVector vector = new ProcessedExpressionDataVector();
            vector.setExpressionExperiment( ee );
            vector.setQuantitationType( qt );
            vector.setBioAssayDimension( newBad );
            vector.setDesignElement( designElement );
            vector.setData( ArrayUtils.subarray( data, 0, columns.length * sizeInBytes ) );
            vector.setRankByMax( rankByMax );
            vector.setRankByMean( rankByMean );
            vectors.add( vector );
        }
        return vectors;
    }

    @Override
    public int createProcessedDataVectors( ExpressionExperiment ee, Collection<ProcessedExpressionDataVector> vectors ) {
        Assert.notNull( ee.getId(), "ExpressionExperiment must be persistent." );
        Assert.isTrue( ee.getProcessedExpressionDataVectors().isEmpty(), "ExpressionExperiment already has processed vectors, remove them before creating new ones or use replaceProcessedDataVectors()." );
        Assert.isTrue( !vectors.isEmpty(), "At least one vector must be provided." );
        QuantitationType qt = vectors.iterator().next().getQuantitationType();
        Assert.notNull( qt.getId(), "Quantitation type must be persistent." );
        Assert.isTrue( qt.getIsMaskedPreferred(), "QuantitationType must be marked as masked preferred." );
        checkVectors( ee, qt, vectors );
        ee.getQuantitationTypes().add( qt );
        ee.getProcessedExpressionDataVectors().addAll( vectors );
        ee.setNumberOfDataVectors( vectors.size() );
        update( ee );
        return vectors.size();
    }

    @Override
    public int removeProcessedDataVectors( ExpressionExperiment ee ) {
        return removeProcessedDataVectors( ee, false );
    }

    private int removeProcessedDataVectors( ExpressionExperiment ee, boolean keepDimensions ) {
        Assert.notNull( ee.getId(), "ExpressionExperiment must be persistent." );

        Set<BioAssayDimension> dimensions = ee.getProcessedExpressionDataVectors().stream()
                .map( BulkExpressionDataVector::getBioAssayDimension )
                .collect( Collectors.toSet() );

        // obtain QTs to remove directly from the vectors
        Set<QuantitationType> qtsToRemove = ee.getProcessedExpressionDataVectors().stream()
                .map( BulkExpressionDataVector::getQuantitationType )
                .collect( Collectors.toSet() );

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

        // remove QTs and unused dimensions
        removeQts( ee, qtsToRemove );

        update( ee );

        if ( !keepDimensions ) {
            removeUnusedDimensions( ee, dimensions );
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
        Assert.notNull( newQt.getId(), "Quantitation type must be persistent." );
        Assert.isTrue( newQt.getIsMaskedPreferred(), "QuantitationType must be marked as masked preferred." );
        checkVectors( ee, newQt, vectors );
        Set<BioAssayDimension> dimensions = ee.getProcessedExpressionDataVectors().stream()
                .map( BulkExpressionDataVector::getBioAssayDimension )
                .collect( Collectors.toSet() );

        // obtain QTs to remove directly from the vectors
        Set<QuantitationType> qtsToRemove = ee.getProcessedExpressionDataVectors().stream()
                .map( BulkExpressionDataVector::getQuantitationType )
                .collect( Collectors.toSet() );
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
        removeQts( ee, qtsToRemove );
        update( ee );
        // remove unused dimensions, if the dimension is reused for the replaced vectors, nothing will happen
        removeUnusedDimensions( ee, dimensions );
        if ( deletedVectors > 0 ) {
            log.info( "Replaced " + deletedVectors + " from " + ee + " for " + newQt );
        }
        return deletedVectors;
    }

    private void removeQts( ExpressionExperiment ee, Collection<QuantitationType> qts ) {
        // remove QTs
        for ( QuantitationType qt : qts ) {
            if ( !ee.getQuantitationTypes().remove( qt ) ) {
                log.warn( qt + " was not attached to " + ee + ", but was associated to at least one of its vectors, it will be removed." );
            }
            getSessionFactory().getCurrentSession().delete( qt );
        }
    }

    private void removeUnusedDimensions( ExpressionExperiment ee, Collection<BioAssayDimension> dimensions ) {
        for ( BioAssayDimension dim : dimensions ) {
            long otherUsers = 0;
            for ( Class<? extends BulkExpressionDataVector> clazz : bulkDataVectorTypes ) {
                String entityName = getSessionFactory().getClassMetadata( clazz ).getEntityName();
                otherUsers += ( Long ) getSessionFactory().getCurrentSession()
                        .createQuery( "select count(*) from " + entityName + " v join v.quantitationType qt where v.bioAssayDimension = :dim" )
                        .setParameter( "dim", dim )
                        .uniqueResult();
            }
            if ( otherUsers > 0 ) {
                log.info( dim + " has " + otherUsers + " other vectors using it, it will not be deleted." );
                continue;
            }
            // check if the BAD is used by a co-expression matrix
            Long matrices = ( Long ) getSessionFactory().getCurrentSession()
                    .createQuery( "select count(*) from SampleCoexpressionMatrix m where m.bioAssayDimension = :dim" )
                    .setParameter( "dim", dim )
                    .uniqueResult();
            if ( matrices > 0 ) {
                log.info( dim + " has " + matrices + " co-expression matrices using it, it will not be deleted." );
                continue;
            }
            // check if the BAD is used by a PCA
            Long pcas = ( Long ) getSessionFactory().getCurrentSession()
                    .createQuery( "select count(*) from PrincipalComponentAnalysis m where m.bioAssayDimension = :dim" )
                    .setParameter( "dim", dim )
                    .uniqueResult();
            if ( pcas > 0 ) {
                log.info( dim + " has " + pcas + " PCAs using it, it will not be deleted." );
                continue;
            }
            getSessionFactory().getCurrentSession().delete( dim );
            log.info( "Removed unused dimension " + dim + " from " + ee + "." );
        }
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
