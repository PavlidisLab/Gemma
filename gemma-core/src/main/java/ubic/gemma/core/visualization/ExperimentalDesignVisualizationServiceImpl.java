/*
 * The Gemma project
 *
 * Copyright (c) 2008-2009 University of British Columbia
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

package ubic.gemma.core.visualization;

import lombok.Value;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrixFactory;
import ubic.basecode.graphics.ColorMap;
import ubic.basecode.graphics.ColorMatrix;
import ubic.basecode.graphics.MatrixDisplay;
import ubic.gemma.core.datastructure.matrix.BulkExpressionDataMatrix;
import ubic.gemma.core.datastructure.matrix.EmptyExpressionMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrixColumnSort;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimensionValueObject;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.bioAssayData.SlicedDoubleVectorValueObject;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.IdentifiableUtils;

import javax.annotation.Nullable;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;

import static ubic.gemma.model.expression.experiment.ExperimentalDesignUtils.measurement2double;

/**
 * Tools for visualizing experimental designs. The idea is to generate an overview of the design that can be put over
 * heat maps or line graphs.
 *
 * @author paul
 */
@Service
public class ExperimentalDesignVisualizationServiceImpl implements ExperimentalDesignVisualizationService {

    @Value
    private static class LayoutSelection {
        Long experimentId;
        @Nullable
        Long factorId;
    }

    private final Log log = LogFactory.getLog( this.getClass().getName() );

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Override
    public Map<Long, LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>>> sortVectorDataByDesign(
            Collection<DoubleVectorValueObject> dedVs, @Nullable ExperimentalFactor primaryFactor ) {
        if ( dedVs == null || dedVs.isEmpty() ) {
            return new HashMap<>( 0 );
        }

        StopWatch timer = new StopWatch();
        timer.start();

        // caches so we don't repeatedly query the database for the same BADs and EEs for each vector
        Map<Long, ExpressionExperiment> eeCache = new HashMap<>();
        Map<ExpressionExperiment, Collection<BioAssayDimension>> bdsCache = new HashMap<>();
        Map<LayoutSelection, LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>>> cachedLayouts = new HashMap<>();

        /*
         * This is shared across experiments that might show up in the dedVs; this should be okay...saves computation.
         * This is the only slow part. FIXME: if primaryFactor is non-null we can't use the cache as it stands.
         */
        this.prepare( dedVs, primaryFactor, eeCache, bdsCache, cachedLayouts );

        Map<Long, LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>>> returnedLayouts = new HashMap<>(
                dedVs.size() );
        Map<DoubleVectorValueObject, List<BioAssayValueObject>> newOrderingsForBioAssayDimensions = new HashMap<>();
        for ( DoubleVectorValueObject vec : dedVs ) {

            if ( vec.isReorganized() ) {
                continue;
            }

            assert !vec.getBioAssays().isEmpty();

            // compute the cache key without loading the EE
            Long eeId = getExperimentIdForVector( vec );
            LayoutSelection cacheKey = new LayoutSelection( eeId, primaryFactor != null ? primaryFactor.getId() : null );
            LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> layout = cachedLayouts.get( cacheKey );
            if ( layout == null ) {
                log.debug( "Did not find cached layout for " + vec.getId() + ( primaryFactor != null ? " PrimaryFactor=" + primaryFactor.getName() : "" ) );
                continue;
            }

            ExpressionExperiment ee = this.getExperimentForVector( vec, eeCache );

            List<BioAssayValueObject> newOrdering = new ArrayList<>( layout.keySet() );

            newOrdering.retainAll( vec.getBioAssays() );

            /*
             * This can happen if the vectors are out of whack with the bioassays - e.g. two platforms were used but
             * merging is not done. See bug 3775. Skipping the ordering is not the right thing to do.
             */
            if ( newOrdering.isEmpty() ) {

                boolean allNaN = this.allNaN( vec );

                if ( allNaN ) {
                    // reordering will have no effect.
                    continue;
                }

                Collection<BioAssayDimension> bds = getBioAssayDimensionsForExperiment( ee, bdsCache );

                /*
                 * Add to the layout.
                 */
                this.extendLayout( layout, vec, ee, bds );
                newOrdering = new ArrayList<>( layout.keySet() );
                newOrdering.retainAll( vec.getBioAssays() );
                assert !newOrdering.isEmpty();
            }

            newOrderingsForBioAssayDimensions.put( vec, newOrdering );

            Map<BioAssayValueObject, Integer> ordering = this.getOrdering( newOrdering );

            eeId = vec.getExpressionExperiment().getId(); // might be subset id.

            if ( !returnedLayouts.containsKey( eeId ) ) {
                if ( vec instanceof SlicedDoubleVectorValueObject ) {
                    LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> trimmedLayout = new LinkedHashMap<>();

                    for ( BioAssayValueObject baVo : newOrdering ) {
                        trimmedLayout.put( baVo, layout.get( baVo ) );
                    }

                    returnedLayouts.put( eeId, trimmedLayout );

                } else {
                    returnedLayouts.put( eeId, layout );
                }
            }

            /*
             * Might be a faster way.
             */
            double[] data = vec.getData();
            double[] dol = ArrayUtils.clone( data );

            // assert ordering.size() == data.length : "got " + ordering.size() + " expected " + data.length;

            List<BioAssayValueObject> oldOrdering = vec.getBioAssayDimension().getBioAssays();
            int j = 0;
            if ( log.isTraceEnabled() )
                log.trace( "Old order: " + StringUtils.join( ArrayUtils.toObject( data ), "," ) );
            for ( BioAssayValueObject ba : oldOrdering ) {

                if ( ordering.get( ba ) == null ) {
                    assert Double.isNaN( dol[j] );
                    j++;
                    continue;
                }

                assert ordering.containsKey( ba );
                assert ordering.get( ba ) != null;

                Integer targetIndex = ordering.get( ba );

                data[targetIndex] = dol[j++];

            }
            if ( log.isTraceEnabled() )
                log.trace( "New order: " + StringUtils.join( ArrayUtils.toObject( data ), "," ) );

            vec.setReorganized( true );

        }

        for ( DoubleVectorValueObject vec : dedVs ) {
            if ( vec.getBioAssayDimension().isReordered() )
                continue;
            List<BioAssayValueObject> newOrdering = newOrderingsForBioAssayDimensions.get( vec );
            if ( newOrdering == null )
                continue; // data was empty, etc.
            vec.getBioAssayDimension().reorder( newOrdering );
        }

        if ( timer.getTime() > 1500 ) {
            log.info( "Sort vectors by design: " + timer.getTime() + "ms" );
        }

        return returnedLayouts;
    }

    /**
     * Test method for now, shows how this can be used.
     *
     * @param e ee
     */
    @SuppressWarnings("unused") // Test method for now, shows how this can be used.
    private void plotExperimentalDesign( ExpressionExperiment e, Map<LayoutSelection, LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>>> cachedLayouts ) {
        LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> layout = this
                .getExperimentalDesignLayout( e, cachedLayouts );

        List<String> efStrings = new ArrayList<>();
        List<String> baStrings = new ArrayList<>();
        List<double[]> rows = new ArrayList<>();
        boolean first = true;
        int i = 0;
        for ( BioAssayValueObject ba : layout.keySet() ) {
            baStrings.add( ba.getName() );

            int j = 0;
            for ( ExperimentalFactor ef : layout.get( ba ).keySet() ) {
                if ( first ) {
                    double[] nextRow = new double[layout.size()];
                    rows.add( nextRow );
                    efStrings.add( ef.getName() + " ( id=" + ef.getId() + ")" ); // make sure they are unique.
                }
                double d = layout.get( ba ).get( ef );

                rows.get( j )[i] = d;
                j++;
            }
            i++;
            first = false;
        }

        double[][] mat = rows.toArray( new double[][] {} );

        DoubleMatrix<String, String> data = DoubleMatrixFactory.dense( mat );
        data.setRowNames( efStrings );
        data.setColumnNames( baStrings );

        ColorMatrix<String, String> cm = new ColorMatrix<>( data, ColorMap.GREENRED_COLORMAP, Color.GRAY );

        try {
            this.writeImage( cm, File.createTempFile( e.getShortName() + "_", ".png" ) );
        } catch ( IOException e1 ) {
            throw new RuntimeException( e1 );
        }
    }

    private boolean allNaN( DoubleVectorValueObject vec ) {
        boolean allNaN = true;
        for ( double d : vec.getData() ) {
            if ( !Double.isNaN( d ) ) {
                allNaN = false;
                break;
            }
        }
        return allNaN;
    }

    /**
     * See bug 3775. For experiments which have more than one bioassay dimension, we typically have to "extend" the
     * layout to include more bioassays. Because the ordering is defined by the factor values associated with the
     * underlying biomaterials, this is going to be okay.
     * @param ee  the experiment associated to the vector
     * @param bds the {@link BioAssayDimension}s for the experiment
     */
    private void extendLayout( LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> layout, DoubleVectorValueObject vec, ExpressionExperiment ee, Collection<BioAssayDimension> bds ) {
        BioAssayDimensionValueObject bioAssayDimension = this.getBioAssayDimensionForVector( vec );

        LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> extension = this.getExperimentalDesignLayout( ee, bds, null );

        for ( BioAssayValueObject vbaVo : bioAssayDimension.getBioAssays() ) {
            assert extension.containsKey( vbaVo );
        }

        for ( BioAssayValueObject vbaVo : vec.getBioAssays() ) {
            assert extension.containsKey( vbaVo );
        }

        layout.putAll( extension );
    }

    private BioAssayDimensionValueObject getBioAssayDimensionForVector( DoubleVectorValueObject vec ) {
        BioAssayDimensionValueObject bioAssayDimension = vec.getBioAssayDimension();

        if ( vec.getBioAssayDimension().getSourceBioAssayDimension() != null ) {
            bioAssayDimension = vec.getBioAssayDimension().getSourceBioAssayDimension();
        }

        assert bioAssayDimension.getId() != null;

        // this actually doesn't really matter, but we're wasting time redoing it.
        assert !bioAssayDimension.isReordered();
        assert !bioAssayDimension.getIsSubset();
        return bioAssayDimension;
    }

    private LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> getExperimentalDesignLayout( ExpressionExperiment e, Map<LayoutSelection, LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>>> cachedLayouts ) {
        LayoutSelection cacheKey = new LayoutSelection( e.getId(), null );
        LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> layout = cachedLayouts.get( cacheKey );
        if ( layout != null ) {
            return layout;
        }

        Collection<BioAssayDimension> bds = expressionExperimentService.getBioAssayDimensions( e );

        LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> result = this
                .getExperimentalDesignLayout( e, bds, null );

        cachedLayouts.put( cacheKey, result );

        return result;
    }

    /**
     * @param  ee experiment ID
     * @param  bds          a BioAssayDimension that represents the BioAssayDimensionValueObject. This is only needed to
     *                      avoid making ExpressionMatrix use value objects, otherwise we could use the
     *                      BioAssayDimensionValueObject
     * @param primaryFactor an optional primary factor to use for ordering before others
     * @return A "Layout": a map of bioassays to map of factors to doubles that represent the position in the
     *                    layout.
     */
    private LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> getExperimentalDesignLayout(
            ExpressionExperiment ee, Collection<BioAssayDimension> bds, @Nullable ExperimentalFactor primaryFactor ) {

        LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> result = new LinkedHashMap<>();

        BulkExpressionDataMatrix<Object> mat = new EmptyExpressionMatrix( bds );

        // This is the place the actual sort order is determined.
        List<BioMaterial> bms = ExpressionDataMatrixColumnSort.orderByExperimentalDesign( mat, primaryFactor );

        Map<FactorValue, Double> fvV = new HashMap<>();

        if ( ee.getExperimentalDesign() == null || ee.getExperimentalDesign().getExperimentalFactors().isEmpty() ) {
            // Case of no experimental design; just put in a dummy factor.
            ExperimentalFactor dummyFactor = ExperimentalFactor.Factory.newInstance();
            dummyFactor.setName( "No factors" );
            for ( BioMaterial bm : bms ) {
                int j = mat.getColumnIndex( bm );

                Collection<BioAssay> bas = mat.getBioAssaysForColumn( j );

                for ( BioAssay ba : bas ) {
                    BioAssayValueObject baVo = new BioAssayValueObject( ba, false );
                    result.put( baVo, new LinkedHashMap<>() );
                    result.get( baVo ).put( dummyFactor, 0.0 );
                }
            }
            return result;
        }

        /*
         * Choose values to use as placeholders.
         */
        for ( ExperimentalFactor ef : ee.getExperimentalDesign().getExperimentalFactors() ) {
            if ( ef.getFactorValues().isEmpty() ) {
                // this can happen if the design isn't complete.
                continue;
            }
            if ( ef.getType().equals( FactorType.CONTINUOUS ) ) {
                for ( FactorValue fv : ef.getFactorValues() ) {
                    double value;
                    if ( fv.getMeasurement() != null ) {
                        value = measurement2double( fv.getMeasurement() );
                    } else {
                        log.warn( fv + " is continuous, but lacking a measurement, will use NaN." );
                        value = Double.NaN;
                    }
                    fvV.put( fv, value );
                }
            } else {
                for ( FactorValue fv : ef.getFactorValues() ) {
                    fvV.put( fv, fv.getId().doubleValue() );
                }
            }
        }

        assert !fvV.isEmpty();
        assert !bms.isEmpty();

        for ( BioMaterial bm : bms ) { // this will be for all samples in the experiment, we don't know about subsets here
            int j = mat.getColumnIndex( bm );

            BioAssay ba = mat.getBioAssayForColumn( j );

            Collection<FactorValue> fvs = bm.getAllFactorValues();


            BioAssayValueObject baVo = new BioAssayValueObject( ba, false );
            result.put( baVo, new LinkedHashMap<>( fvs.size() ) );
            for ( FactorValue fv : fvs ) {
                ExperimentalFactor ef = fv.getExperimentalFactor();
                Double value = fvV.get( fv ); // we use IDs to stratify the groups.
                result.get( baVo ).put( ef, value );
            }
        }
        return result;
    }

    /**
     * @return the experiment ID; if the vector is for a subset, we return the source experiment ID
     */
    private ExpressionExperiment getExperimentForVector( DoubleVectorValueObject vec, Map<Long, ExpressionExperiment> eeCache ) {
        Long eeId = getExperimentIdForVector( vec );
        return eeCache.computeIfAbsent( eeId, eeId2 -> expressionExperimentService.loadAndThawLiteOrFail( eeId2, NullPointerException::new, "No ExpressionExperiment with ID " + eeId + "." ) );
    }

    private Long getExperimentIdForVector( DoubleVectorValueObject vec ) {
        Long eeId;
        if ( vec.getExpressionExperiment() instanceof ExpressionExperimentValueObject ) {
            eeId = vec.getExpressionExperiment().getId();
        } else if ( vec.getExpressionExperiment() instanceof ExpressionExperimentSubsetValueObject ) {
            ExpressionExperimentSubsetValueObject eesvo = ( ExpressionExperimentSubsetValueObject ) vec
                    .getExpressionExperiment();
            eeId = eesvo.getSourceExperiment();
        } else {
            throw new UnsupportedOperationException();
        }
        return eeId;
    }

    /**
     * Retrieve all the BADs for the experiment, including those from subsets.
     */
    private Collection<BioAssayDimension> getBioAssayDimensionsForExperiment( ExpressionExperiment ee, Map<ExpressionExperiment, Collection<BioAssayDimension>> bdsCache ) {
        return bdsCache.computeIfAbsent( ee, expressionExperiment -> {
            HashSet<BioAssayDimension> dimensions = new HashSet<>( expressionExperimentService.getBioAssayDimensions( expressionExperiment ) );
            dimensions.addAll( expressionExperimentService.getBioAssayDimensionsFromSubSets( expressionExperiment ) );
            return dimensions;
        } );
    }

    /**
     * Get the order that bioassays need to be in for the 'real' data.
     */
    private Map<BioAssayValueObject, Integer> getOrdering( List<BioAssayValueObject> bioAssaysInOrder ) {
        assert !bioAssaysInOrder.isEmpty();
        Map<BioAssayValueObject, Integer> ordering = new HashMap<>();

        int i = 0;
        for ( BioAssayValueObject bbb : bioAssaysInOrder ) {
            ordering.put( bbb, i++ );
        }
        return ordering;
    }

    /**
     * Gets the bioassay dimensions for the experiments (or subsets) associated with the given vectors. These are cached
     * for later re-use.
     *
     * @param dvvOs    vectors to prepare
     * @param eeCache  a cache of {@link ExpressionExperiment}
     * @param bdsCache a cache of {@link BioAssayDimension}s by {@link ExpressionExperiment}
     */
    private void prepare( Collection<DoubleVectorValueObject> dvvOs, @Nullable ExperimentalFactor primaryFactor,
            Map<Long, ExpressionExperiment> eeCache, Map<ExpressionExperiment, Collection<BioAssayDimension>> bdsCache, Map<LayoutSelection, LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>>> cachedLayouts ) {
        for ( DoubleVectorValueObject vec : dvvOs ) {
            if ( vec == null ) {
                log.debug( "DoubleVectorValueObject is null" );
                continue;
            }

            if ( vec.isReorganized() ) {
                log.warn( "Vector already reorganized, this shouldn't happen" );
                continue;
            }

            // compute the cache key without retrieving the EE
            LayoutSelection cacheKey = new LayoutSelection( getExperimentIdForVector( vec ), primaryFactor != null ? primaryFactor.getId() : null );
            if ( cachedLayouts.get( cacheKey ) != null ) {
                continue;
            }

            ExpressionExperiment ee = this.getExperimentForVector( vec, eeCache );
            Collection<BioAssayDimension> bioAssayDimensions = getBioAssayDimensionsForExperiment( ee, bdsCache );

            if ( bioAssayDimensions.isEmpty() ) {
                throw new IllegalStateException( "There are no BADs for " + ee );
            }

            boolean isSubset = vec.getExpressionExperiment() instanceof ExpressionExperimentSubsetValueObject;

            if ( isSubset ) {
                BioAssayDimensionValueObject badvo = vec.getBioAssayDimension();
                Collection<Long> badids = IdentifiableUtils.getIds( badvo.getBioAssays() );
                for ( BioAssayDimension bad : bioAssayDimensions ) {
                    // trim the bads for relevant samples. This is unpleasant - it would be better to work with VOs
                    // but it's easier said than done
                    List<BioAssay> revisedBioAssayList = new ArrayList<>();
                    for ( BioAssay ba : bad.getBioAssays() ) {
                        if ( badids.contains( ba.getId() ) ) {
                            revisedBioAssayList.add( ba );
                        }
                    }

                    bad.setBioAssays( revisedBioAssayList );

                }
            }

            cachedLayouts.put( cacheKey, getExperimentalDesignLayout( ee, bioAssayDimensions, primaryFactor ) );
        }
    }

    /**
     * Test method.
     */
    private void writeImage( ColorMatrix<String, String> matrix, File outputFile ) throws IOException {
        MatrixDisplay<String, String> writer = new MatrixDisplay<>( matrix );
        writer.setCellSize( new Dimension( 18, 18 ) );
        writer.saveImage( matrix, outputFile.getAbsolutePath(), true, false, true );
    }
}
