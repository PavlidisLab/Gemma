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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrixFactory;
import ubic.basecode.graphics.ColorMap;
import ubic.basecode.graphics.ColorMatrix;
import ubic.basecode.graphics.MatrixDisplay;
import ubic.gemma.core.datastructure.matrix.EmptyExpressionMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrix;
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
import ubic.gemma.persistence.util.EntityUtils;

import javax.annotation.Nullable;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tools for visualizing experimental designs. The idea is to generate a overview of the design that can be put over
 * heat maps or line graphs.
 *
 * @author paul
 */
@Component
public class ExperimentalDesignVisualizationServiceImpl implements ExperimentalDesignVisualizationService {

    class LayoutSelection {
        private Long experimentId;
        private Long factorId; // nullable.

        public LayoutSelection( Long experimentId, @Nullable ExperimentalFactor primaryFactor ) {
            this.experimentId = experimentId;

            if ( primaryFactor != null )
                this.factorId = primaryFactor.getId();
        }

        public LayoutSelection( Long experimentId, Long primaryFactorId ) {
            this.experimentId = experimentId;
            this.factorId = primaryFactorId;
        }

        public LayoutSelection( Long experimentId ) {
            this.experimentId = experimentId;
            this.factorId = null;
        }

        public int hashCode() {
            return Objects.hash( experimentId, factorId );
        }

        public boolean equals( Object obj ) {
            if ( this == obj ) {
                return true;
            }
            if ( obj == null ) {
                return false;
            }
            if ( getClass() != obj.getClass() ) {
                return false;
            }
            LayoutSelection other = ( LayoutSelection ) obj;
            return Objects.equals( experimentId, other.experimentId ) && Objects.equals( factorId, other.factorId );
        }
    }

    /**
     * Cache of layouts for experiments, keyed by experiment ID.
     */
    private final Map<LayoutSelection, LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>>> cachedLayouts = new ConcurrentHashMap<>();
    private final Log log = LogFactory.getLog( this.getClass().getName() );
    private final ExpressionExperimentService expressionExperimentService;

    @Autowired
    public ExperimentalDesignVisualizationServiceImpl( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    @Override
    public Map<Long, LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>>> sortVectorDataByDesign(
            Collection<DoubleVectorValueObject> dedVs ) {
        return this.sortVectorDataByDesign( dedVs, null );
    }

    @Override
    public Map<Long, LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>>> sortVectorDataByDesign(
            Collection<DoubleVectorValueObject> dedVs, ExperimentalFactor primaryFactor ) {

        //cachedLayouts.clear(); // uncomment FOR DEBUGGING.

        if ( dedVs == null ) {
            return new HashMap<>( 0 );
        }

        Map<Long, LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>>> returnedLayouts = new HashMap<>(
                dedVs.size() );

        StopWatch timer = new StopWatch();
        timer.start();

        /*
         * This is shared across experiments that might show up in the dedVs; this should be okay...saves computation.
         * This is the only slow part. FIXME: if primaryFactor is non-null we can't use the cache as it stands.
         */
        this.prepare( dedVs, primaryFactor );

        Map<DoubleVectorValueObject, List<BioAssayValueObject>> newOrderingsForBioAssayDimensions = new HashMap<>();
        for ( DoubleVectorValueObject vec : dedVs ) {

            if ( vec.isReorganized() ) {
                continue;
            }

            assert !vec.getBioAssays().isEmpty();

            LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> layout = null;


            LayoutSelection cacheKey = null;

            if ( vec.getExpressionExperiment().getClass()
                    .isInstance( ExpressionExperimentSubsetValueObject.class ) ) {
                cacheKey = new LayoutSelection( ( ( ExpressionExperimentSubsetValueObject ) vec.getExpressionExperiment() ).getSourceExperiment(), primaryFactor );
            } else {
                cacheKey = new LayoutSelection( vec.getExpressionExperiment().getId(), primaryFactor );
            }


            layout = cachedLayouts.get( cacheKey );


            if ( layout == null || layout.isEmpty() ) {
                log.debug( "Did not find cached layout for " + vec.getId() + ( primaryFactor != null ? " PrimaryFactor=" + primaryFactor.getName() : "" ) );
                continue;
            }

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

                /*
                 * Add to the layout.
                 */
                layout = this.extendLayout( vec, vec.getExpressionExperiment().getId() );
                newOrdering = new ArrayList<>( layout.keySet() );
                newOrdering.retainAll( vec.getBioAssays() );
                assert !newOrdering.isEmpty();
            }

            newOrderingsForBioAssayDimensions.put( vec, newOrdering );

            Map<BioAssayValueObject, Integer> ordering = this.getOrdering( newOrdering );

            Long eeId;
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

    @Override
    public void clearCaches( Long eeId ) {
        this.clearCachedLayouts( eeId, null );
    }

    /**
     * Test method for now, shows how this can be used.
     *
     * @param e ee
     */
    @SuppressWarnings("unused") // Test method for now, shows how this can be used.
    protected void plotExperimentalDesign( ExpressionExperiment e ) {
        LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> layout = this
                .getExperimentalDesignLayout( e );

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

    private void clearCachedLayouts( Long eeId, Long factorId ) {
        this.cachedLayouts.remove( new LayoutSelection( eeId, factorId ) );
    }

    /**
     * See bug 3775. For experiments which have more than one bioassay dimension, we typically have to "extend" the
     * layout to include more bioassays. Because the ordering is defined by the factor values associated with the
     * underlying biomaterials, this is going to be okay.
     *
     * @param eeId - could be a subset?
     */
    private LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> extendLayout(
            DoubleVectorValueObject vec, Long eeId ) {
        BioAssayDimensionValueObject bioAssayDimension = this.getBioAssayDimensionForVector( vec );

        ExpressionExperimentValueObject ee = vec.getExpressionExperiment();
        ExpressionExperiment actualEe = this.getExperimentForVector( vec, ee );

        LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> extension = this
                .getExperimentalDesignLayout( actualEe, expressionExperimentService.getBioAssayDimensions( actualEe ), null );

        for ( BioAssayValueObject vbaVo : bioAssayDimension.getBioAssays() ) {
            assert extension.containsKey( vbaVo );
        }

        for ( BioAssayValueObject vbaVo : vec.getBioAssays() ) {
            assert extension.containsKey( vbaVo );
        }

        cachedLayouts.get( new LayoutSelection( eeId ) ).putAll( extension );

        return cachedLayouts.get( new LayoutSelection( eeId ) );
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

    private LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> getExperimentalDesignLayout(
            ExpressionExperiment e ) {

        if ( cachedLayouts.containsKey( new LayoutSelection( e.getId() ) ) ) {
            return cachedLayouts.get( new LayoutSelection( e.getId() ) );
        }

        Collection<BioAssayDimension> bds = expressionExperimentService.getBioAssayDimensions( e );
        e = this.expressionExperimentService.thawLite( e );

        LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> result = this
                .getExperimentalDesignLayout( e, bds, null );

        cachedLayouts.put( new LayoutSelection( e.getId() ), result );

        return result;
    }

    /**
     * @param  experiment Experiment or Subset
     * @param  bds        a BioAssayDimension that represents the BioAssayDimensionValueObject. This is only needed to
     *                    avoid
     *                    making ExpressionMatrix use value objects, otherwise we could use the
     *                    BioAssayDimensionValueObject
     * @param primaryFactor an optional primary factor to use for ordering before others
     * @return A "Layout": a map of bioassays to map of factors to doubles that represent the position in the
     *                    layout.
     */
    private LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> getExperimentalDesignLayout(
            BioAssaySet experiment, Collection<BioAssayDimension> bds, ExperimentalFactor primaryFactor ) {

        LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> result = new LinkedHashMap<>();

        ExpressionDataMatrix<Object> mat = new EmptyExpressionMatrix( bds );

        ExpressionExperiment sourceExperiment = null;
        if ( experiment instanceof ExpressionExperimentSubSet ) {
            sourceExperiment = ( ( ExpressionExperimentSubSet ) experiment ).getSourceExperiment();
        } else {
            sourceExperiment = ( ExpressionExperiment ) experiment;
        }

        // This is the place the actual sort order is determined.
        List<BioMaterial> bms = ExpressionDataMatrixColumnSort.orderByExperimentalDesign( mat, primaryFactor );

        Map<Long, Double> fvV = new HashMap<>();

        if ( sourceExperiment.getExperimentalDesign().getExperimentalFactors().isEmpty() ) {
            // Case of no experimental design; just put in a dummy factor.
            ExperimentalFactor dummyFactor = ExperimentalFactor.Factory.newInstance();
            dummyFactor.setName( "No factors" );
            for ( BioMaterial bm : bms ) {
                int j = mat.getColumnIndex( bm );

                Collection<BioAssay> bas = mat.getBioAssaysForColumn( j );

                for ( BioAssay ba : bas ) {
                    BioAssayValueObject baVo = new BioAssayValueObject( ba, false );
                    result.put( baVo, new LinkedHashMap<ExperimentalFactor, Double>() );
                    result.get( baVo ).put( dummyFactor, 0.0 );
                }
            }
            return result;
        }

        /*
         * Choose values to use as placeholders.
         */
        for ( ExperimentalFactor ef : sourceExperiment.getExperimentalDesign().getExperimentalFactors() ) {
            if ( ef.getFactorValues().isEmpty() ) {
                // this can happen if the design isn't complete.
                continue;
            }
            for ( FactorValue fv : ef.getFactorValues() ) {
                // the id is just used as a convenience.
                fvV.put( fv.getId(), new Double( fv.getId() ) );
            }
        }

        assert !fvV.isEmpty();
        assert !bms.isEmpty();

        for ( BioMaterial bm : bms ) { // this will be for all samples in the experiment, we don't know about subsets here
            int j = mat.getColumnIndex( bm );

            Collection<BioAssay> bas = mat.getBioAssaysForColumn( j );

            if ( bas.size() > 1 ) {
                throw new UnsupportedOperationException( "Can't have more than one bioassay per sample" );
            }

            BioAssay ba = bas.iterator().next();

            Collection<FactorValue> fvs = bm.getFactorValues();


            BioAssayValueObject baVo = new BioAssayValueObject( ba, false );
            result.put( baVo, new LinkedHashMap<ExperimentalFactor, Double>( fvs.size() ) );
            for ( FactorValue fv : fvs ) {
                ExperimentalFactor ef = fv.getExperimentalFactor();
                Double value;
                if ( ef.getType().equals( FactorType.CONTINUOUS ) ) {
                    if ( fv.getMeasurement() != null && fv.getMeasurement().getValue() != null ) {
                        try {
                            value = Double.parseDouble( fv.getMeasurement().getValue() );
                        } catch ( NumberFormatException e ) {
                            value = Double.NaN;
                        }
                    } else {
                        value = Double.NaN;
                    }
                } else {
                    value = fvV.get( fv.getId() ); // we use IDs to stratify the groups.
                }
                result.get( baVo ).put( ef, value );
            }
        }
        return result;
    }

    /**
     * @return the experiment; if the vector is for a subset, we return the source experiment
     */
    private ExpressionExperiment getExperimentForVector( DoubleVectorValueObject vec,
            ExpressionExperimentValueObject ee ) {
        /*
         * The following is the really slow part if we don't use a cache.
         */
        ExpressionExperiment actualEe;
        if ( vec instanceof SlicedDoubleVectorValueObject ) {
            /*
             * Then we are looking at a subset, so associate it with the original experiment.
             */
            if ( !vec.getClass().isInstance( ExpressionExperimentSubsetValueObject.class ) ) {
                log.warn( "Vector is sliced, but the experiment is not a subset! " + ee.getShortName() + " element=" + vec.getDesignElement() );
            }
            ExpressionExperimentSubsetValueObject eesvo = ( ExpressionExperimentSubsetValueObject ) vec
                    .getExpressionExperiment();

            if ( eesvo.getSourceExperiment() == null ) {
                log.warn( "Vector is sliced, but the source experiment is null!" );
            }

            actualEe = expressionExperimentService.loadOrFail( eesvo.getSourceExperiment() );
            actualEe = expressionExperimentService.thawLiter( actualEe );
        } else {
            actualEe = expressionExperimentService.loadOrFail( ee.getId() );
            actualEe = expressionExperimentService.thawLiter( actualEe );
        }
        return actualEe;
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
     * @param dvvOs datavector objects.
     */
    private void prepare( Collection<DoubleVectorValueObject> dvvOs, ExperimentalFactor primaryFactor ) {

        if ( dvvOs == null )
            return;

        for ( DoubleVectorValueObject vec : dvvOs ) {
            if ( vec == null ) {
                log.debug( "DoubleVectorValueObject is null" );
                continue;
            }

            if ( vec.isReorganized() ) {
                log.warn( "Vector already reorganized, this shouldn't happen" );
                continue;
            }

            ExpressionExperimentValueObject ee = vec.getExpressionExperiment();
            boolean isSubset = vec.getExpressionExperiment() instanceof ExpressionExperimentSubsetValueObject;

            LayoutSelection cacheKey = new LayoutSelection( ee.getId(), primaryFactor );
            // this also probably shouldn't happen?
            if ( cachedLayouts.containsKey( cacheKey ) && !cachedLayouts.get( cacheKey ).isEmpty() ) {
                continue;
            }

            if ( isSubset ) {
                ExpressionExperimentSubsetValueObject eesvo = ( ExpressionExperimentSubsetValueObject ) vec
                        .getExpressionExperiment();
                if ( eesvo.getSourceExperiment() != null && cachedLayouts.containsKey( new LayoutSelection( eesvo.getSourceExperiment(), primaryFactor ) ) ) {
                    continue;
                }
            }

            ExpressionExperiment actualEe = this.getExperimentForVector( vec, ee );
            Collection<BioAssayDimension> bioAssayDimensions = expressionExperimentService.getBioAssayDimensions( actualEe );

            if ( isSubset ) {
                BioAssayDimensionValueObject badvo = vec.getBioAssayDimension();
                Collection<Long> badids = EntityUtils.getIds( badvo.getBioAssays() );
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

            LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> experimentalDesignLayout = this
                    .getExperimentalDesignLayout( actualEe, bioAssayDimensions, primaryFactor );

            cachedLayouts.put( new LayoutSelection( ee.getId()  /* could be a subset */, primaryFactor ), experimentalDesignLayout );

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
