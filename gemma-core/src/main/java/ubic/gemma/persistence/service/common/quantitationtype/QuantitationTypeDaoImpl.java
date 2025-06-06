/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.persistence.service.common.quantitationtype;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.NonUniqueResultException;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.expression.bioAssayData.*;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.hibernate.TypedResultTransformer;
import ubic.gemma.persistence.service.AbstractCriteriaFilteringVoEnabledDao;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static ubic.gemma.persistence.util.QueryUtils.optimizeParameterList;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * {@link QuantitationType}.
 * </p>
 *
 * @see ubic.gemma.model.common.quantitationtype.QuantitationType
 */
@Repository
public class QuantitationTypeDaoImpl extends AbstractCriteriaFilteringVoEnabledDao<QuantitationType, QuantitationTypeValueObject>
        implements QuantitationTypeDao {

    private final Set<Class<? extends DataVector>> dataVectorTypes;

    @Autowired
    public QuantitationTypeDaoImpl( SessionFactory sessionFactory ) {
        super( QuantitationType.class, sessionFactory );
        //noinspection unchecked
        dataVectorTypes = sessionFactory.getAllClassMetadata().values().stream()
                .filter( cm -> DataVector.class.isAssignableFrom( cm.getMappedClass() ) )
                .map( cm -> ( Class<? extends DataVector> ) cm.getMappedClass() )
                .collect( Collectors.toSet() );
    }

    @Override
    public Set<Class<? extends DataVector>> getVectorTypes() {
        return dataVectorTypes;
    }

    @Override
    public QuantitationType create( QuantitationType entity ) {
        Assert.isTrue( StringUtils.isNotBlank( entity.getName() ),
                "Quantitation type name cannot be blank." );
        return super.create( entity );
    }

    @Override
    public QuantitationType create( QuantitationType quantitationType, Class<? extends DataVector> dataVectorType ) {
        Assert.isTrue( !quantitationType.getIsPreferred() || RawExpressionDataVector.class.isAssignableFrom( dataVectorType ),
                "The isPreferred can only be set for RawExpressionDataVector." );
        Assert.isTrue( !quantitationType.getIsMaskedPreferred() || ProcessedExpressionDataVector.class.isAssignableFrom( dataVectorType ),
                "The isMaskedPreferred can only be set for ProcessedExpressionDataVector." );
        Assert.isTrue( !quantitationType.getIsSingleCellPreferred() || SingleCellExpressionDataVector.class.isAssignableFrom( dataVectorType ),
                "The isSingleCellPreferred can only be set for SingleCellExpressionDataVector." );
        return create( quantitationType );
    }

    @Override
    public QuantitationType find( QuantitationType quantitationType ) {
        // find all matching QTs; not necessarily for this experiment. This is lazy - we could go through the above to check each for a match.
        return ( QuantitationType ) this.getSessionFactory().getCurrentSession().createCriteria( QuantitationType.class )
                .add( createRestrictions( quantitationType ) )
                .uniqueResult();
    }

    @Override
    public QuantitationType find( QuantitationType entity, Class<? extends DataVector> dataVectorType ) {
        // find all matching QTs; not necessarily for this experiment. This is lazy - we could go through the above to check each for a match.
        return ( QuantitationType ) getSessionFactory().getCurrentSession()
                .createCriteria( dataVectorType )
                .createCriteria( "quantitationType" )
                .add( createRestrictions( entity ) )
                .setProjection( Projections.distinct( Projections.property( "id" ) ) )
                .setResultTransformer( new TypedResultTransformer<QuantitationType>() {
                    @Override
                    public QuantitationType transformTuple( Object[] tuple, String[] aliases ) {
                        Long id = ( Long ) tuple[0];
                        return ( QuantitationType ) getSessionFactory().getCurrentSession().load( QuantitationType.class, id );
                    }

                    @Override
                    public List<QuantitationType> transformListTyped( List<QuantitationType> collection ) {
                        return collection;
                    }
                } )
                .uniqueResult();
    }

    @Override
    public QuantitationType find( ExpressionExperiment ee, QuantitationType quantitationType, @Nullable Set<Class<? extends DataVector>> dataVectorTypes ) {
        Assert.isTrue( dataVectorTypes == null || !dataVectorTypes.isEmpty(), "At lease one type of data vector must be supplied." );

        // find all matching QTs; not necessarily for this experiment. This is lazy - we could go through the above to check each for a match.
        //noinspection unchecked
        Collection<QuantitationType> qts = this.getSessionFactory().getCurrentSession()
                .createCriteria( QuantitationType.class )
                .add( createRestrictions( quantitationType ) )
                .list();

        // find all QTs for the experiment
        //noinspection unchecked
        List<QuantitationType> list = this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct quantType from ExpressionExperiment ee "
                        + "inner join ee.quantitationTypes as quantType where ee  = :ee " )
                .setParameter( "ee", ee )
                .list();

        // intersect that with the ones the experiment has (again, this is the lazy way to do this)
        list.retainAll( qts );

        if ( dataVectorTypes != null ) {
            // find matching QTs in vectors
            Collection<QuantitationType> qtsFromVectors = new HashSet<>();
            for ( Class<? extends DataVector> dvt : dataVectorTypes ) {
                //noinspection unchecked
                qtsFromVectors.addAll( this.getSessionFactory().getCurrentSession()
                        .createQuery( "select distinct q from " + dvt.getName() + " v "
                                + "join v.quantitationType as q where v.expressionExperiment = :ee" )
                        .setParameter( "ee", ee ).list() );
            }
            list.retainAll( qtsFromVectors );
        }

        if ( list.size() > 1 ) {
            throw new NonUniqueResultException( list.size() );
        }

        return list.isEmpty() ? null : list.iterator().next();
    }

    @Override
    public QuantitationType findByNameAndVectorType( ExpressionExperiment ee, String name, Class<? extends DataVector> dataVectorType ) {
        String entityName = getSessionFactory().getClassMetadata( dataVectorType ).getEntityName();
        return ( QuantitationType ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select v.quantitationType from " + entityName + " v "
                        + "where v.expressionExperiment = :ee and v.quantitationType.name = :name "
                        + "group by v.quantitationType" )
                .setParameter( "ee", ee )
                .setParameter( "name", name )
                .uniqueResult();
    }

    @Override
    public <T extends DataVector> Collection<QuantitationType> findAllByNameAndVectorType( ExpressionExperiment ee, String name, Class<? extends T> vectorType ) {
        //noinspection unchecked
        List<Long> ids = getSessionFactory().getCurrentSession()
                .createCriteria( vectorType )
                .add( Restrictions.eq( "expressionExperiment", ee ) )
                .createCriteria( "quantitationType" )
                .add( Restrictions.eq( "name", name ) )
                .setProjection( Projections.distinct( Projections.id() ) )
                .list();
        return load( ids );
    }

    @Override
    public QuantitationType findOrCreate( QuantitationType quantitationType, Class<? extends DataVector> dataVectorType ) {
        QuantitationType found = find( quantitationType, dataVectorType );
        if ( found != null ) {
            return found;
        }
        return create( quantitationType, dataVectorType );
    }

    @Override
    public QuantitationType loadById( Long id, ExpressionExperiment ee ) {
        Set<QuantitationType> found = dataVectorTypes.stream()
                .map( vt -> loadByIdAndVectorType( id, ee, vt ) )
                .filter( Objects::nonNull )
                .collect( Collectors.toSet() );
        if ( found.size() == 1 ) {
            return found.iterator().next();
        } else if ( found.size() > 1 ) {
            throw new NonUniqueResultException( found.size() );
        } else {
            return null;
        }
    }

    @Override
    public QuantitationType loadByIdAndVectorType( Long id, ExpressionExperiment ee, Class<? extends DataVector> dataVectorType ) {
        String entityName = getSessionFactory().getClassMetadata( dataVectorType ).getEntityName();
        return ( QuantitationType ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select v.quantitationType from " + entityName + " v "
                        + "where v.expressionExperiment = :ee and v.quantitationType.id = :id "
                        + "group by v.quantitationType" )
                .setParameter( "ee", ee )
                .setParameter( "id", id )
                .uniqueResult();
    }

    @Override
    public Map<Class<? extends DataVector>, Set<QuantitationType>> findByExpressionExperiment( ExpressionExperiment ee ) {
        Map<Class<? extends DataVector>, Set<QuantitationType>> result = new HashMap<>();
        //noinspection unchecked
        Set<QuantitationType> qts = new HashSet<>( ( List<QuantitationType> ) getSessionFactory().getCurrentSession()
                .createQuery( "select qt from ExpressionExperiment ee join ee.quantitationTypes qt where ee = :ee" )
                .setParameter( "ee", ee )
                .list() );
        for ( Class<? extends DataVector> vectorType : dataVectorTypes ) {
            Collection<QuantitationType> q = findByExpressionExperiment( ee, vectorType );
            if ( !q.isEmpty() ) {
                result.computeIfAbsent( vectorType, k -> new HashSet<>() )
                        .addAll( q );
                qts.removeAll( q );
            }
        }
        if ( !qts.isEmpty() ) {
            log.warn( ee + " has " + qts.size() + " quantitation types that are not associated to any vectors." );
            result.computeIfAbsent( null, k -> new HashSet<>() )
                    .addAll( qts );
        }
        return result;
    }

    @Override
    public Collection<QuantitationType> findByExpressionExperiment( ExpressionExperiment ee, Class<? extends DataVector> vectorType ) {
        //noinspection unchecked
        return load( ( List<Long> ) getSessionFactory().getCurrentSession()
                .createCriteria( vectorType )
                .add( Restrictions.eq( "expressionExperiment", ee ) )
                .createCriteria( "quantitationType" )
                .setProjection( Projections.distinct( Projections.property( "id" ) ) )
                .list() );
    }

    @Override
    public Collection<QuantitationType> findByExpressionExperimentAndDimension( ExpressionExperiment expressionExperiment, BioAssayDimension dimension ) {
        Set<Class<? extends BulkExpressionDataVector>> bulkVectorTypes = dataVectorTypes.stream()
                .filter( BulkExpressionDataVector.class::isAssignableFrom )
                .map( vt -> ( Class<? extends BulkExpressionDataVector> ) vt )
                .collect( Collectors.toSet() );
        return findByExpressionExperimentAndDimension( expressionExperiment, dimension, bulkVectorTypes );
    }

    @Override
    public Collection<QuantitationType> findByExpressionExperimentAndDimension( ExpressionExperiment expressionExperiment, BioAssayDimension dimension, Collection<Class<? extends BulkExpressionDataVector>> vectorTypes ) {
        Set<QuantitationType> qts = new HashSet<>();
        for ( Class<? extends DataVector> vectorType : vectorTypes ) {
            //noinspection unchecked
            qts.addAll( load( ( List<Long> ) getSessionFactory().getCurrentSession()
                    .createCriteria( vectorType )
                    .add( Restrictions.eq( "expressionExperiment", expressionExperiment ) )
                    .add( Restrictions.eq( "bioAssayDimension", dimension ) )
                    .createCriteria( "quantitationType" )
                    .setProjection( Projections.distinct( Projections.property( "id" ) ) )
                    .list() ) );
        }
        return qts;
    }

    @Override
    protected QuantitationTypeValueObject doLoadValueObject( QuantitationType entity ) {
        return new QuantitationTypeValueObject( entity );
    }

    @Override
    public List<QuantitationTypeValueObject> loadValueObjectsWithExpressionExperiment( Collection<QuantitationType> qts, ExpressionExperiment ee ) {
        List<QuantitationTypeValueObject> vos = loadValueObjects( qts );
        populateVectorType( vos, ee );
        return vos;
    }

    @Override
    public Class<? extends DataVector> getDataVectorType( QuantitationType qt ) {
        for ( Class<? extends DataVector> vectorType : dataVectorTypes ) {
            if ( ( ( Long ) getSessionFactory().getCurrentSession()
                    .createCriteria( vectorType )
                    .add( Restrictions.eq( "quantitationType", qt ) )
                    .setProjection( Projections.rowCount() )
                    .uniqueResult() ) > 0 ) {
                return vectorType;
            }
        }
        return null;
    }

    @Override
    public <T extends DataVector> Collection<Class<? extends T>> getMappedDataVectorTypes( Class<T> vectorType ) {
        //noinspection unchecked
        return getSessionFactory().getAllClassMetadata().values().stream()
                .map( ClassMetadata::getMappedClass )
                .filter( vectorType::isAssignableFrom )
                .map( clazz -> ( Class<? extends T> ) clazz )
                .collect( Collectors.toSet() );
    }

    /**
     * Create a restriction that matches the fields of a QT as per {@link QuantitationType#equals(Object)}.
     */
    private Criterion createRestrictions( QuantitationType quantitationType ) {
        return Restrictions.and(
                Restrictions.eq( "name", quantitationType.getName() ),
                Restrictions.eq( "generalType", quantitationType.getGeneralType() ),
                Restrictions.eq( "type", quantitationType.getType() ),
                Restrictions.eq( "scale", quantitationType.getScale() ),
                Restrictions.eq( "representation", quantitationType.getRepresentation() ),
                Restrictions.eq( "isBackground", quantitationType.getIsBackground() ),
                Restrictions.eq( "isBackgroundSubtracted", quantitationType.getIsBackgroundSubtracted() ),
                Restrictions.eq( "isRatio", quantitationType.getIsRatio() ),
                Restrictions.eq( "isNormalized", quantitationType.getIsNormalized() ),
                Restrictions.eq( "isBatchCorrected", quantitationType.getIsBatchCorrected() ),
                Restrictions.eq( "isRecomputedFromRawData", quantitationType.getIsRecomputedFromRawData() ) );
    }

    private void populateVectorType( Collection<QuantitationTypeValueObject> quantitationTypeValueObjects, ExpressionExperiment ee ) {
        if ( quantitationTypeValueObjects.isEmpty() )
            return;

        Set<Long> ids = quantitationTypeValueObjects.stream()
                .map( QuantitationTypeValueObject::getId )
                .collect( Collectors.toSet() );

        MultiValueMap<Long, Class<? extends DataVector>> vectorTypeById = new LinkedMultiValueMap<>();
        for ( Class<? extends DataVector> vectorType : dataVectorTypes ) {
            //noinspection unchecked
            List<Long> qtIds = getSessionFactory().getCurrentSession().createCriteria( vectorType )
                    .add( Restrictions.eq( "expressionExperiment", ee ) )
                    .createCriteria( "quantitationType" )
                    .add( Restrictions.in( "id", optimizeParameterList( ids ) ) )
                    .setProjection( Projections.distinct( Projections.id() ) )
                    .list();
            qtIds.forEach( id -> vectorTypeById.add( id, vectorType ) );
        }

        for ( QuantitationTypeValueObject vo : quantitationTypeValueObjects ) {
            vo.setExpressionExperimentId( ee.getId() );
            List<Class<? extends DataVector>> vts = vectorTypeById.get( vo.getId() );
            if ( vts != null ) {
                if ( vts.size() > 1 ) {
                    log.warn( String.format( "%s is associated to multiple vector types in %s: %s.", vo, ee, vts ) );
                }
                vo.setVectorType( vts.iterator().next().getName() );
            } else {
                // this is generally not a problem since the QuantitationType might be generic (i.e. representing a data transformation)
                log.info( String.format( "%s is not associated to any process/raw vectors of %s.", vo, ee ) );
            }
        }
    }
}