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
package ubic.gemma.persistence.service.expression.experiment;

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionMatrix;
import ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysis;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.util.BusinessKey;
import ubic.gemma.persistence.util.CommonQueries;
import ubic.gemma.persistence.util.QueryUtils;

import org.springframework.lang.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet</code>.
 * </p>
 *
 * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet
 */
@Repository
public class ExpressionExperimentSubSetDaoImpl extends AbstractDao<ExpressionExperimentSubSet>
        implements ExpressionExperimentSubSetDao {

    /**
     * Concrete bulk vector types, discovered from the metamodel so a newly mapped one is covered automatically.
     */
    private final Set<Class<? extends BulkExpressionDataVector>> bulkDataVectorTypes;

    @Autowired
    public ExpressionExperimentSubSetDaoImpl( SessionFactory sessionFactory ) {
        super( ExpressionExperimentSubSet.class, sessionFactory );
        //noinspection unchecked
        bulkDataVectorTypes = getSessionFactory().getMetamodel().getEntities().stream()
                .map( jakarta.persistence.metamodel.EntityType::getJavaType )
                .filter( BulkExpressionDataVector.class::isAssignableFrom )
                .map( clazz -> ( Class<? extends BulkExpressionDataVector> ) clazz )
                .collect( Collectors.toSet() );
    }

    @Override
    public ExpressionExperimentSubSet find( ExpressionExperimentSubSet entity ) {
        return BusinessKey.find( this.getSessionFactory().getCurrentSession(), entity );
    }

    @Nullable
    @Override
    public ExpressionExperimentSubSet loadWithBioAssays( Long id ) {
        ExpressionExperimentSubSet subSet = load( id );
        if ( subSet != null ) {
            Hibernate.initialize( subSet.getBioAssays() );
            Hibernate.initialize( subSet.getCharacteristics() );
            Hibernate.initialize( subSet.getSourceExperiment().getAccession() );
            Hibernate.initialize( subSet.getSourceExperiment().getCharacteristics() );
            Hibernate.initialize( subSet.getSourceExperiment().getPrimaryPublication() );
        }
        return subSet;
    }

    @Override
    public Collection<ExpressionExperimentSubSet> findByBioAssayIn( Collection<BioAssay> bioAssays ) {
        return new HashSet<>( QueryUtils.listByIdentifiableBatch( getSessionFactory().getCurrentSession()
                        .createQuery( "select eess from ExpressionExperimentSubSet eess "
                                + "join eess.bioAssays ba where ba in :bas group by eess" ),
                "bas", bioAssays, QueryUtils.MAX_PARAMETER_LIST_SIZE ) );
    }

    @Override
    public Collection<FactorValue> getFactorValuesUsed( ExpressionExperimentSubSet entity, ExperimentalFactor factor ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select fv from ExpressionExperimentSubSet es "
                        + "join es.bioAssays ba "
                        + "join ba.sampleUsed bm "
                        + "join bm.factorValues fv "
                        + "where es=:es and fv.experimentalFactor = :ef "
                        + "group by fv")
                .setParameter( "es", entity )
                .setParameter( "ef", factor )
                .list();
    }

    @Override
    public Collection<FactorValue> getFactorValuesUsed( Long subSetId, Long experimentalFactor ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select fv from ExpressionExperimentSubSet es "
                                + "join es.bioAssays ba "
                                + "join ba.sampleUsed bm "
                                + "join bm.factorValues fv "
                                + "where es.id=:es and fv.experimentalFactor.id = :ef "
                                + "group by fv")
                .setParameter( "es", subSetId )
                .setParameter( "ef", experimentalFactor )
                .list();
    }

    @Override
    public Collection<ArrayDesign> getArrayDesignsUsed( ExpressionExperimentSubSet subset ) {
        return CommonQueries.getArrayDesignsUsed( subset, this.getSessionFactory().getCurrentSession() );
    }

    @Override
    public void remove( ExpressionExperimentSubSet entity ) {
        Collection<FactorValue> factorValues = getFactorValueUsed( entity );
        Set<BioAssay> bioAssaysToRemove = new HashSet<>();
        Set<BioMaterial> samplesToRemove = new HashSet<>();

        // bioassays that are solely owned by this subset; this is currently the case for single-cell population
        // subsets. "Solely" is two conditions: the source experiment does not hold the assay, and no other subset
        // does either. The second is not hypothetical -- an assay can sit in a cell-type subset and in a further
        // subset of it -- and the join table restricts on delete, so missing it aborts the whole removal.
        Set<BioAssay> sharedWithOtherSubSets = getBioAssaysUsedByOtherSubSets( entity );
        List<BioAssay> ownedBioAssays = new ArrayList<>();
        for ( BioAssay ba : entity.getBioAssays() ) {
            if ( entity.getSourceExperiment().getBioAssays().contains( ba ) ) {
                continue;
            }
            if ( sharedWithOtherSubSets.contains( ba ) ) {
                log.warn( ba + " is also used by another ExpressionExperimentSubSet, it will not be deleted." );
                continue;
            }
            ownedBioAssays.add( ba );
        }

        // Pulling an assay out of a dimension that indexes data would misalign that data, so such assays are left
        // alone. A dimension that indexes nothing is itself garbage once its assays are gone: those assays are
        // detached from it below and the dimension is deleted as soon as it empties out. It commonly spans several
        // subsets (one per cell type), each removed by a separate call, so it only empties on the last of them.
        Map<BioAssay, Collection<BioAssayDimension>> dimensionsByBioAssay = new HashMap<>();
        Set<BioAssayDimension> dimensionsIndexingData = new HashSet<>();
        for ( BioAssay ba : ownedBioAssays ) {
            dimensionsByBioAssay.put( ba, getBioAssayDimensions( ba ) );
        }
        for ( BioAssayDimension dim : dimensionsByBioAssay.values().stream().flatMap( Collection::stream ).collect( Collectors.toSet() ) ) {
            if ( isIndexingData( dim ) ) {
                dimensionsIndexingData.add( dim );
            }
        }

        Set<BioAssayDimension> dimensionsToDetach = new HashSet<>();
        for ( BioAssay ba : ownedBioAssays ) {
            log.info( "Removing " + ba + " as it does not belong to the source experiment." );
            Collection<BioAssayDimension> dimensions = dimensionsByBioAssay.get( ba );
            if ( dimensions.stream().anyMatch( dimensionsIndexingData::contains ) ) {
                log.warn( ba + " is still attached to a BioAssayDimension that indexes data, it will not be deleted." );
                continue;
            }
            if ( !getSingleCellDimensions( ba ).isEmpty() ) {
                log.warn( ba + " is still attached to a SingleCellDimension, it will not be deleted." );
                continue;
            }
            ba.getSampleUsed().getFactorValues().removeAll( factorValues );
            ba.getSampleUsed().getBioAssaysUsedIn().removeAll( entity.getBioAssays() );
            if ( ba.getSampleUsed().getBioAssaysUsedIn().isEmpty() && ba.getSampleUsed().getFactorValues().isEmpty() ) {
                samplesToRemove.add( ba.getSampleUsed() );
            } else {
                log.warn( ba.getSampleUsed() + " is still attached to a BioAssay or FactorValue, it will not be deleted." );
            }
            dimensionsToDetach.addAll( dimensions );
            bioAssaysToRemove.add( ba );
        }

        super.remove( entity );

        // detach from the dimensions first: a completely detached dimension has no owner left and is removed
        Set<BioAssayDimension> dimensionsToRemove = new HashSet<>();
        for ( BioAssayDimension dim : dimensionsToDetach ) {
            dim.getBioAssays().removeAll( bioAssaysToRemove );
            if ( dim.getBioAssays().isEmpty() ) {
                dimensionsToRemove.add( dim );
            } else {
                log.debug( dim + " still holds BioAssays owned elsewhere, the dimension will not be deleted." );
            }
        }
        if ( !dimensionsToRemove.isEmpty() ) {
            log.info( "Removing " + dimensionsToRemove.size() + " BioAssayDimension that are no longer attached to any BioAssay." );
            for ( BioAssayDimension dim : dimensionsToRemove ) {
                getSessionFactory().getCurrentSession().delete( dim );
            }
        }

        if ( !bioAssaysToRemove.isEmpty() ) {
            log.info( "Removing " + bioAssaysToRemove.size() + " BioAssay that are owned by " + entity + " (i.e. they do not belong to the source experiment)." );
            for ( BioAssay ba : bioAssaysToRemove ) {
                getSessionFactory().getCurrentSession().delete( ba );
            }
        }
        if ( !samplesToRemove.isEmpty() ) {
            log.info( "Removing " + samplesToRemove.size() + " BioMaterial that are no longer attached to any BioAssay." );
            for ( BioMaterial bm : samplesToRemove ) {
                getSessionFactory().getCurrentSession().delete( bm );
            }
        }
    }

    /**
     * Obtain the {@link BioAssay} of a subset that are also used by at least one other subset.
     */
    private Set<BioAssay> getBioAssaysUsedByOtherSubSets( ExpressionExperimentSubSet subset ) {
        if ( subset.getBioAssays().isEmpty() ) {
            return Collections.emptySet();
        }
        return new HashSet<>( QueryUtils.listByIdentifiableBatch( getSessionFactory().getCurrentSession()
                        .createQuery( "select ba from ExpressionExperimentSubSet eess "
                                + "join eess.bioAssays ba "
                                + "where ba in :bas and eess <> :eess "
                                + "group by ba" )
                        .setParameter( "eess", subset ),
                "bas", subset.getBioAssays(), QueryUtils.MAX_PARAMETER_LIST_SIZE ) );
    }

    /**
     * Check whether anything indexes its values by the position of a {@link BioAssay} in the given dimension, which
     * makes the dimension's contents immutable in practice.
     */
    private boolean isIndexingData( BioAssayDimension dimension ) {
        for ( Class<? extends BulkExpressionDataVector> vectorType : bulkDataVectorTypes ) {
            if ( countReferencesTo( vectorType.getSimpleName(), dimension ) > 0 ) {
                return true;
            }
        }
        return countReferencesTo( PrincipalComponentAnalysis.class.getSimpleName(), dimension ) > 0
                || countReferencesTo( SampleCoexpressionMatrix.class.getSimpleName(), dimension ) > 0;
    }

    private long countReferencesTo( String entityName, BioAssayDimension dimension ) {
        return ( Long ) getSessionFactory().getCurrentSession()
                .createQuery( "select count(*) from " + entityName + " e where e.bioAssayDimension = :dim" )
                .setParameter( "dim", dimension )
                .uniqueResult();
    }

    private Collection<BioAssayDimension> getBioAssayDimensions( BioAssay ba ) {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                .createQuery( "select dim from BioAssayDimension dim join dim.bioAssays ba where ba = :ba group by dim" )
                .setParameter( "ba", ba )
                .list();
    }

    private Collection<SingleCellDimension> getSingleCellDimensions( BioAssay ba ) {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                .createQuery( "select dim from SingleCellDimension dim join dim.bioAssays ba where ba = :ba group by dim" )
                .setParameter( "ba", ba )
                .list();
    }

    /**
     * Obtain all {@link FactorValue} used by this subset.
     */
    private Collection<FactorValue> getFactorValueUsed( ExpressionExperimentSubSet subset ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select fv from ExpressionExperimentSubSet es "
                        + "join es.bioAssays ba "
                        + "join ba.sampleUsed bm "
                        + "join bm.factorValues fv "
                        + "where es=:es "
                        + "group by fv" )
                .setParameter( "es", subset )
                .list();
    }
}