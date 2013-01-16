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

package ubic.gemma.visualization;

import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrixFactory;
import ubic.basecode.graphics.ColorMap;
import ubic.basecode.graphics.ColorMatrix;
import ubic.basecode.graphics.MatrixDisplay;
import ubic.gemma.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.datastructure.matrix.EmptyExpressionMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrixColumnSort;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * Tools for visualizing experimental designs. The idea is to generate a overview of the design that can be put over
 * heatmaps or line graphs.
 * 
 * @author paul
 * @version $Id$
 */
@Component
public class ExperimentalDesignVisualizationServiceImpl implements ExperimentalDesignVisualizationService {

    protected Log log = LogFactory.getLog( getClass().getName() );

    @Autowired
    private BioAssayDimensionService bioAssayDimensionService;

    /**
     * Cache. TODO: use ehcache so we can manage this.
     */
    private Map<Long, LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>>> cachedLayouts = new ConcurrentHashMap<Long, LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>>>();

    /**
     * Cache. TODO: use ehcache so we can manage this.
     */
    private Map<Long, BioAssayDimension> cachedThawedBioAssayDimensions = new ConcurrentHashMap<Long, BioAssayDimension>();

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Override
    public void clearCaches() {
        this.cachedLayouts.clear();
        this.cachedThawedBioAssayDimensions.clear();
    }

    @Override
    public void clearCaches( BioAssaySet ee ) {
        this.clearCachedLayouts( ee.getId() );
        this.clearCachedBioAssayDimensions( ee );
    }

    @Override
    public void clearCaches( Long eeId ) {
        this.clearCachedLayouts( eeId );
        this.clearCachedBioAssayDimensions( eeId );
    }

    /*
     * (non-Javadoc) Only used in tests
     * 
     * @see
     * ubic.gemma.visualization.ExperimentalDesignVisualizationService#getExperimentalDesignLayout(ubic.gemma.model.
     * expression.experiment.ExpressionExperiment)
     */
    @Override
    public LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>> getExperimentalDesignLayout(
            ExpressionExperiment e ) {

        if ( cachedLayouts.containsKey( e.getId() ) ) {
            return cachedLayouts.get( e.getId() );
        }

        Collection<BioAssayDimension> bds = expressionExperimentService.getBioAssayDimensions( e );

        /*
         * FIXME if there are multiple bioassay dimensions...they had better match up. This should be the case, but
         * might not be if curation is incomplete.
         */

        if ( bds.size() > 1 ) {
            log.debug( "More than oneo bioassaydimension for visualization, only using the first one" );
        }

        BioAssayDimension bd = bds.iterator().next();
        assert bd != null;
        BioAssayDimension tbd = this.cachedThawedBioAssayDimensions.get( bd.getId() );
        if ( tbd == null ) {
            tbd = this.bioAssayDimensionService.thaw( bd );
            assert tbd.getId().equals( bd.getId() );
            this.cachedThawedBioAssayDimensions.put( tbd.getId(), tbd );
        }

        // needed?
        ExpressionExperiment tee = this.expressionExperimentService.thawLite( e );

        LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>> result = this.getExperimentalDesignLayout(
                tee, bd );

        cachedLayouts.put( tee.getId(), result );

        return result;
    }

    private LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>> getExperimentalDesignLayout(
            ExpressionExperiment experiment, BioAssayDimension bd ) {
        LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>> result = new LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>>();

        /*
         * This is FAST - 2 ms
         */
        ExpressionDataMatrix<Object> mat = new EmptyExpressionMatrix( bd );

        List<BioMaterial> bms = ExpressionDataMatrixColumnSort.orderByExperimentalDesign( mat );

        Map<FactorValue, Double> fvV = new HashMap<FactorValue, Double>();

        assert experiment != null;
        assert experiment.getExperimentalDesign() != null;
        if ( experiment.getExperimentalDesign().getExperimentalFactors().size() == 0 ) {

            ExperimentalFactor dummyFactor = ExperimentalFactor.Factory.newInstance();
            dummyFactor.setName( "No factors" );
            for ( BioMaterial bm : bms ) {
                int j = mat.getColumnIndex( bm );

                Collection<BioAssay> bas = mat.getBioAssaysForColumn( j );

                for ( BioAssay ba : bas ) {
                    result.put( ba, new LinkedHashMap<ExperimentalFactor, Double>() );
                    result.get( ba ).put( dummyFactor, 0.0 );
                }

            }

            return result;
        }

        for ( ExperimentalFactor ef : experiment.getExperimentalDesign().getExperimentalFactors() ) {
            Double i = 0.0;
            for ( FactorValue fv : ef.getFactorValues() ) {
                i = i + 1.0;
                // fvV.put( fv, i ); // just for now, a placeholder value.
                if ( fv.getId() == null ) {
                    log.warn( "FactorValue has null id, this shouldn't happen!" + fv.toString() );
                }
                fvV.put( fv, new Double( fv.getId() ) ); // try using the factorValue id
            }
        }
        for ( BioMaterial bm : bms ) {
            int j = mat.getColumnIndex( bm );

            Collection<BioAssay> bas = mat.getBioAssaysForColumn( j );

            Collection<FactorValue> fvs = bm.getFactorValues();

            for ( BioAssay ba : bas ) {
                result.put( ba, new LinkedHashMap<ExperimentalFactor, Double>( fvs.size() ) );
                for ( FactorValue fv : fvs ) {
                    ExperimentalFactor ef = fv.getExperimentalFactor();

                    Double value;
                    if ( fv.getMeasurement() != null ) {
                        try {
                            value = Double.parseDouble( fv.getMeasurement().getValue() );
                        } catch ( NumberFormatException e ) {
                            value = fvV.get( fv );
                        }
                    } else {
                        value = fvV.get( fv );
                    }
                    result.get( ba ).put( ef, value );

                }
            }

        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.visualization.ExperimentalDesignVisualizationService#sortLayoutSamplesByFactor(java.util.Map)
     */
    @Override
    public Map<Long, LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>>> sortLayoutSamplesByFactor(
            final Map<Long, LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>>> layouts ) {

        Map<Long, LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>>> sortedLayouts = new HashMap<Long, LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>>>();
        StopWatch timer = new StopWatch();
        timer.start();
        for ( Long bioAssaySet : layouts.keySet() ) {

            final LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>> layout = layouts.get( bioAssaySet );

            if ( layout == null || layout.size() == 0 ) {
                log.warn( "Null or empty layout for ee: " + bioAssaySet ); // does this happen?
                continue;
            }
            LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>> sortedLayout = new LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>>();

            Collection<ExperimentalFactor> filteredFactors = extractFactors( layout, false );

            if ( filteredFactors.isEmpty() ) {
                if ( sortedLayouts.containsKey( bioAssaySet ) ) {
                    log.warn( "sortedLayouts already contained ee with ID = " + bioAssaySet
                            + ". Value was map with # keys = " + sortedLayouts.get( bioAssaySet ).keySet().size() );
                }
                sortedLayouts.put( bioAssaySet, sortedLayout );
                continue; // batch was the only factor.
            }

            List<BioMaterial> bmList = new ArrayList<BioMaterial>();
            Map<BioMaterial, BioAssay> BMtoBA = new HashMap<BioMaterial, BioAssay>();

            for ( BioAssay ba : layout.keySet() ) {
                Collection<BioMaterial> samplesUsed = ba.getSamplesUsed();
                assert !samplesUsed.isEmpty();
                for ( BioMaterial bm : samplesUsed ) {
                    BMtoBA.put( bm, ba ); // is one biomaterial per bioassay true??
                    bmList.add( bm );
                }
            }

            // sort factors within layout by number of values
            LinkedList<ExperimentalFactor> sortedFactors = ( LinkedList<ExperimentalFactor> ) ExpressionDataMatrixColumnSort
                    .orderFactorsByExperimentalDesign( bmList, filteredFactors );

            // this isn't necessary, because we can have factors get dropped if we are looking at a subset.
            // assert sortedFactors.size() == filteredFactors.size();

            List<BioMaterial> sortedBMList = ExpressionDataMatrixColumnSort.orderBiomaterialsBySortedFactors( bmList,
                    sortedFactors );

            assert sortedBMList.size() == bmList.size();

            // sort layout entries according to sorted ba list
            List<BioAssay> sortedBAList = new ArrayList<BioAssay>();
            for ( BioMaterial bm : sortedBMList ) {
                BioAssay ba = BMtoBA.get( bm );
                assert ba != null;

                sortedBAList.add( ba );

                // sort factor-value pairs for each biomaterial
                LinkedHashMap<ExperimentalFactor, Double> facs = layout.get( ba );

                LinkedHashMap<ExperimentalFactor, Double> sortedFacs = new LinkedHashMap<ExperimentalFactor, Double>();
                for ( ExperimentalFactor fac : sortedFactors ) {
                    sortedFacs.put( fac, facs.get( fac ) );
                }

                // assert facs.size() == sortedFacs.size() : "Expected " + facs.size() + " factors, got "
                // + sortedFacs.size();
                sortedLayout.put( ba, sortedFacs );
            }
            sortedLayouts.put( bioAssaySet, sortedLayout );

        }

        if ( timer.getTime() > 1000 ) {
            log.info( "Sorting layout samples by factor: " + timer.getTime() + "ms" );
        }

        return sortedLayouts;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.visualization.ExperimentalDesignVisualizationService#sortVectorDataByDesign(java.util.Collection)
     */
    @Override
    public Map<Long, LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>>> sortVectorDataByDesign(
            Collection<DoubleVectorValueObject> dedvs ) {

        // cachedThawedBioAssayDimensions.clear(); // TEMPORARY FOR DEBUGGING.
        // cachedLayouts.clear(); // TEMPORARY FOR DEBUGGING.

        if ( dedvs == null )
            return new HashMap<Long, LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>>>( 0 );

        Map<Long, LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>>> returnedLayouts = new HashMap<Long, LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>>>(
                dedvs.size() );

        StopWatch timer = new StopWatch();
        timer.start();

        /*
         * This is shared across experiments that might show up in the dedvs; this should be okay...saves computation.
         * This is the only slow part.
         */
        prepare( dedvs );
        if ( timer.getTime() > 1000 ) {
            log.info( "Prepare for sorting with thaws: " + timer.getTime() + "ms" ); // this is almos tall the time
        }

        /*
         * This loop is not a performance issue.
         */
        for ( DoubleVectorValueObject vec : dedvs ) {
            LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>> layout = cachedLayouts.get( vec
                    .getExpressionExperiment().getId() );
            returnedLayouts.put( vec.getExpressionExperiment().getId(), layout );

            BioAssayDimension bad = vec.getBioAssayDimension();

            /*
             * FIXME this might be a 'gapped' dimension that has a null id.
             */

            if ( bad == null || vec.isReorganized() ) {
                /*
                 * We've already done this vector, probably - from the cache. If the experimental design changed in the
                 * meantime ... bad
                 */
                continue;
            }

            Map<BioAssay, Integer> ordering = getOrdering( layout );

            /*
             * Might be a faster way.
             */
            double[] data = vec.getData();
            double[] dol = ArrayUtils.clone( data );

            if ( bad.getId() != null && !cachedThawedBioAssayDimensions.containsKey( bad.getId() ) ) {
                // log.warn( "Missing bioassaydimension: " + bad.getId() ); // WHY?
                continue;
            }

            if ( bad.getId() == null ) {
                // / if the bad was replaced with a pseudo one.
                // log.warn( "bioassaydimension has no id" );
                continue;
            }

            Collection<BioAssay> oldOrdering = cachedThawedBioAssayDimensions.get( bad.getId() ).getBioAssays();
            assert oldOrdering instanceof List<?>;
            int j = 0;
            for ( BioAssay ba : oldOrdering ) {

                if ( !ordering.containsKey( ba ) ) {
                    log.warn( "Order for vector didn't contain " + ba );
                    continue;
                }

                int targetIndex = ordering.get( ba );

                data[targetIndex] = dol[j++];

            }
            /*
             * Invalidate the bioassaydimension, it's in the wrong order compared to the bioasays.
             */
            vec.setReorganized( true );

        }

        if ( timer.getTime() > 1500 ) {
            log.info( "Sort vectors by design: " + timer.getTime() + "ms" );
        }

        return returnedLayouts;

    }

    /**
     * Test method for now, shows how this can be used.
     * 
     * @param e
     */
    protected void plotExperimentalDesign( ExpressionExperiment e ) {
        LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>> layout = getExperimentalDesignLayout( e );

        List<String> efStrings = new ArrayList<String>();
        List<String> baStrings = new ArrayList<String>();
        List<double[]> rows = new ArrayList<double[]>();
        boolean first = true;
        int i = 0;
        for ( BioAssay ba : layout.keySet() ) {
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

        ColorMatrix<String, String> cm = new ColorMatrix<String, String>( data, ColorMap.GREENRED_COLORMAP, Color.GRAY );

        try {
            writeImage( cm, File.createTempFile( e.getShortName() + "_", ".png" ) );
        } catch ( IOException e1 ) {
            throw new RuntimeException( e1 );
        }
    }

    private void clearCachedBioAssayDimensions( BioAssaySet ee ) {

        if ( ee instanceof ExpressionExperimentSubSet ) {
            // shouldn't cache?
        } else {
            assert ee instanceof ExpressionExperiment;
            Collection<BioAssayDimension> bds = expressionExperimentService
                    .getBioAssayDimensions( ( ExpressionExperiment ) ee );
            for ( BioAssayDimension bad : bds ) {
                cachedThawedBioAssayDimensions.remove( bad.getId() );
            }
        }

    }

    private void clearCachedBioAssayDimensions( Long eeId ) {

        this.clearCachedBioAssayDimensions( expressionExperimentService.load( eeId ) );

    }

    private void clearCachedLayouts( Long eeId ) {
        this.cachedLayouts.remove( eeId );
    }

    /**
     * @param layout
     * @param skipBatchFactors don't return the 'batch' factor if there is one.
     * @return
     */
    private Collection<ExperimentalFactor> extractFactors(
            LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>> layout, boolean skipBatchFactors ) {
        Collection<ExperimentalFactor> factors = new HashSet<ExperimentalFactor>();
        for ( BioAssay bioAssay : layout.keySet() ) {
            if ( layout.get( bioAssay ) != null && layout.get( bioAssay ).keySet() != null ) {
                for ( ExperimentalFactor ef : layout.get( bioAssay ).keySet() ) {
                    factors.add( ef );
                    if ( skipBatchFactors && ExperimentalDesignUtils.isBatch( ef ) ) {
                        layout.get( bioAssay ).remove( ef );
                        break;
                    }
                }
            }
        }
        return factors;
    }

    /**
     * Get the order that bioassays need to be in for the 'real' data.
     * 
     * @param layout
     * @return
     */
    private Map<BioAssay, Integer> getOrdering(
            LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>> layout ) {
        Map<BioAssay, Integer> ordering = new HashMap<BioAssay, Integer>();

        int i = 0;
        for ( BioAssay bbb : layout.keySet() ) {
            ordering.put( bbb, i++ );
        }
        return ordering;
    }

    /**
     * Does a lot of thawing.
     * 
     * @param dvvos
     */
    private void prepare( Collection<DoubleVectorValueObject> dvvos ) {

        if ( dvvos == null ) return;

        for ( DoubleVectorValueObject vec : dvvos ) {
            if ( vec == null ) {
                log.debug( "DoubleVectorValueObject is null" );
                continue;
            }
            ExpressionExperimentValueObject ee = vec.getExpressionExperiment();

            if ( vec.isSliced() ) {

            }

            if ( cachedLayouts.containsKey( ee.getId() ) ) {
                continue;
            }
            BioAssayDimension bioAssayDimension = vec.getBioAssayDimension();

            if ( bioAssayDimension == null ) {// need bioAssayDimension to create layouts
                /*
                 * shouldn't be null (used to be nullified after vector data was sorted with sortVectorDataByDesign but
                 * now only reorganized flag is set)
                 */
                log.debug( "bioAssayDimension is null" );
                continue;
            }

            if ( bioAssayDimension.getId() == null ) {
                /*
                 * Happens if vectors are associated with a temporary bioassaydimension -- subsets, and it will always
                 * be thawed already. See ProcessedExpressionDataVectorDaoImpl.sliceSubSet. We should do this a better
                 * way, like allowing the BAD to have a reference to the original source and a "isSubset" flag.
                 */
            } else {
                if ( cachedThawedBioAssayDimensions.containsKey( bioAssayDimension.getId() ) ) {
                    log.debug( "Already got" );
                    bioAssayDimension = cachedThawedBioAssayDimensions.get( bioAssayDimension.getId() );
                } else {
                    // log.debug( "Thawing" );
                    // bioAssayDimension = bioAssayDimensionService.thaw( bioAssayDimension );
                    cachedThawedBioAssayDimensions.put( bioAssayDimension.getId(), bioAssayDimension );
                }
                vec.setBioAssayDimension( bioAssayDimension );
            }

            /*
             * The following is the really slow part if we don't use a cache.
             */
            ExpressionExperiment actualee = null;
            if ( vec.isSliced() ) {
                /*
                 * Then we are looking at a subset.
                 */
                actualee = expressionExperimentService.thawLiter( expressionExperimentService.loadBySubsetId( ee
                        .getId() ) );
            } else {

                actualee = expressionExperimentService.thawLiter( expressionExperimentService.load( ee.getId() ) );
                // plotExperimentalDesign( ee ); // debugging/testing
            }

            LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>> experimentalDesignLayout = getExperimentalDesignLayout(
                    actualee, bioAssayDimension );

            cachedLayouts.put( ee.getId(), experimentalDesignLayout );

        }

    }

    /**
     * @param matrix
     * @param location
     * @param fileName
     * @throws IOException
     */
    private void writeImage( ColorMatrix<String, String> matrix, File outputfile ) throws IOException {
        log.info( outputfile );
        MatrixDisplay<String, String> writer = new MatrixDisplay<String, String>( matrix );
        writer.setCellSize( new Dimension( 18, 18 ) );
        writer.saveImage( matrix, outputfile.getAbsolutePath(), true, false, true );
    }

}
