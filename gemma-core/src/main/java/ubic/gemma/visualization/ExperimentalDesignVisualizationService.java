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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrixFactory;
import ubic.basecode.graphics.ColorMap;
import ubic.basecode.graphics.ColorMatrix;
import ubic.basecode.graphics.MatrixDisplay;
import ubic.gemma.datastructure.matrix.EmptyExpressionMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrixColumnSort;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * Tools for visualizing experimental designs. The idea is to generate a overview of the design that can be put over
 * heatmaps or line graphs.
 * 
 * @author paul
 * @version $Id$
 */
@Service
public class ExperimentalDesignVisualizationService {

    protected Log log = LogFactory.getLog( getClass().getName() );

    @Autowired
    ExpressionExperimentService expressionExperimentService;

    @Autowired
    BioAssayService bioAssayService;

    /**
     * Cache. TODO: use ehcache so we can manage this.
     */
    private Map<ExpressionExperiment, LinkedHashMap<BioAssay, Map<ExperimentalFactor, Double>>> layouts = new HashMap<ExpressionExperiment, LinkedHashMap<BioAssay, Map<ExperimentalFactor, Double>>>();

    @Autowired
    private BioAssayDimensionService bioAssayDimensionService;

    /**
     * Cache. TODO: use ehcache so we can manage this.
     */
    private Map<Long, BioAssayDimension> thawedBads = new HashMap<Long, BioAssayDimension>();

    /**
     * For an experiment, spit out
     * 
     * @param e, experiment; should be lightly thawed.
     * @return Map of bioassays to factors to values for plotting. If there are no Factors, a dummy value is returned.
     */
    public LinkedHashMap<BioAssay, Map<ExperimentalFactor, Double>> getExperimentalDesignLayout( ExpressionExperiment e ) {

        if ( layouts.containsKey( e ) ) {
            return layouts.get( e );
        }

        Collection<BioAssayDimension> bds = expressionExperimentService.getBioAssayDimensions( e );

        /*
         * FIXME if there are multiple bioassay dimensions...they had better match up. This should be the case, but
         * mightF tnot be if curation is incomplete.
         */

        BioAssayDimension bd = bds.iterator().next();
        assert bd != null;
        bd = this.thawedBads.get( bd.getId() );
        if ( bd == null ) {
            bd = this.bioAssayDimensionService.thaw( bd );
            this.thawedBads.put( bd.getId(), bd );
        }

        // needed?
        ExpressionExperiment tee = this.expressionExperimentService.thawLite( e );

        LinkedHashMap<BioAssay, Map<ExperimentalFactor, Double>> result = getExperimentalDesignLayout( tee, bd );

        layouts.put( tee, result );

        return result;
    }

    /**
     * @param experiment assumed thawed
     * @param bd assumed thawed
     * @return
     */
    @SuppressWarnings("unchecked")
    public LinkedHashMap<BioAssay, Map<ExperimentalFactor, Double>> getExperimentalDesignLayout(
            ExpressionExperiment experiment, BioAssayDimension bd ) {
        LinkedHashMap<BioAssay, Map<ExperimentalFactor, Double>> result = new LinkedHashMap<BioAssay, Map<ExperimentalFactor, Double>>();

        /*
         * This is FAST - 2 ms
         */
        ExpressionDataMatrix mat = new EmptyExpressionMatrix( bd );

        List<BioMaterial> bms = ExpressionDataMatrixColumnSort.orderByExperimentalDesign( mat );

        Map<FactorValue, Double> fvV = new HashMap<FactorValue, Double>();

        if ( experiment.getExperimentalDesign().getExperimentalFactors().size() == 0 ) {

            ExperimentalFactor dummyFactor = ExperimentalFactor.Factory.newInstance();
            dummyFactor.setName( "No factors" );
            for ( BioMaterial bm : bms ) {
                int j = mat.getColumnIndex( bm );

                Collection<BioAssay> bas = mat.getBioAssaysForColumn( j );

                for ( BioAssay ba : bas ) {
                    result.put( ba, new HashMap<ExperimentalFactor, Double>() );
                    result.get( ba ).put( dummyFactor, 0.0 );
                }

            }

            return result;
        }

        for ( ExperimentalFactor ef : experiment.getExperimentalDesign().getExperimentalFactors() ) {
            Double i = 0.0;
            for ( FactorValue fv : ef.getFactorValues() ) {
                i = i + 1.0;
                fvV.put( fv, i ); // just for now, a placeholder value.
            }
        }
        for ( BioMaterial bm : bms ) {
            int j = mat.getColumnIndex( bm );

            Collection<BioAssay> bas = mat.getBioAssaysForColumn( j );

            Collection<FactorValue> fvs = bm.getFactorValues();

            for ( BioAssay ba : bas ) {
                result.put( ba, new HashMap<ExperimentalFactor, Double>() );
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

    /**
     * Test method for now, shows how this can be used.
     * 
     * @param e
     */
    public void plotExperimentalDesign( ExpressionExperiment e ) {
        LinkedHashMap<BioAssay, Map<ExperimentalFactor, Double>> layout = getExperimentalDesignLayout( e );

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

    /**
     * @param bioAssayDimensionService the bioAssayDimensionService to set
     */
    public void setBioAssayDimensionService( BioAssayDimensionService bioAssayDimensionService ) {
        this.bioAssayDimensionService = bioAssayDimensionService;
    }

    /**
     * @param expressionExperimentService the expressionExperimentService to set
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * Put data vectors in the order you'd want to display the experimental design.
     * 
     * @param dedvs
     */
    public Map<ExpressionExperiment, LinkedHashMap<BioAssay, Map<ExperimentalFactor, Double>>> sortVectorDataByDesign(
            Collection<DoubleVectorValueObject> dedvs ) {

        // thawedBads.clear(); // FIXME TEMPORARY FOR DEBUGGING.
        // layouts.clear(); // FIXME TEMPORARY FOR DEBUGGING.

        Map<ExpressionExperiment, LinkedHashMap<BioAssay, Map<ExperimentalFactor, Double>>> returnedLayouts = new HashMap<ExpressionExperiment, LinkedHashMap<BioAssay, Map<ExperimentalFactor, Double>>>();

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
            LinkedHashMap<BioAssay, Map<ExperimentalFactor, Double>> layout = layouts.get( vec
                    .getExpressionExperiment() );
            returnedLayouts.put( vec.getExpressionExperiment(), layout );
            BioAssayDimension bad = vec.getBioAssayDimension();

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

            if ( !thawedBads.containsKey( bad.getId() ) ) {
                log.warn( "Missing bioassaydimension: " + bad.getId() ); // WHY?
                continue;
            }

            Collection<BioAssay> oldOrdering = thawedBads.get( bad.getId() ).getBioAssays();
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
            vec.setBioAssayDimension( null );

        }

        if ( timer.getTime() > 1500 ) {
            // log.info( "Sort vectors by design: " + timer.getTime() + "ms" );
        }

        return returnedLayouts;

    }

    /**
     * Get the order that bioassays need to be in for the 'real' data.
     * 
     * @param layout
     * @return
     */
    private Map<BioAssay, Integer> getOrdering( LinkedHashMap<BioAssay, Map<ExperimentalFactor, Double>> layout ) {
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
     * @param dedvs
     */
    private void prepare( Collection<DoubleVectorValueObject> dedvs ) {

        for ( DoubleVectorValueObject vec : dedvs ) {
            ExpressionExperiment ee = vec.getExpressionExperiment();

            if ( layouts.containsKey( ee ) ) {
                continue;
            }
            BioAssayDimension bioAssayDimension = vec.getBioAssayDimension();

            if ( bioAssayDimension == null ) continue;

            if ( bioAssayDimension.getId() == null ) {
                /*
                 * Happens if vectors are associated with a temporary bioassaydimension -- subsets, and it will always
                 * be thawed already. See ProcessedExpressionDataVectorDaoImpl.sliceSubSet. We should do this a better
                 * way, like allowing the BAD to have a reference to the original source and a "isSubset" flag.
                 */
            } else {
                if ( thawedBads.containsKey( bioAssayDimension.getId() ) ) {
                    log.debug( "Already got" );
                    bioAssayDimension = thawedBads.get( bioAssayDimension.getId() );
                } else {
                    log.debug( "Thawing" );
                    bioAssayDimension = bioAssayDimensionService.thaw( bioAssayDimension );
                    thawedBads.put( bioAssayDimension.getId(), bioAssayDimension );
                }
                vec.setBioAssayDimension( bioAssayDimension );
            }

            /*
             * The following is the really slow part if we don't use a cache.
             */
            ee = expressionExperimentService.thawLite( ee );
            // plotExperimentalDesign( ee ); // debugging/testing
            LinkedHashMap<BioAssay, Map<ExperimentalFactor, Double>> experimentalDesignLayout = getExperimentalDesignLayout(
                    ee, bioAssayDimension );
            layouts.put( ee, experimentalDesignLayout );

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
