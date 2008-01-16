/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.apps;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.time.StopWatch;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix2DNamed;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.gui.ColorMap;
import ubic.basecode.gui.ColorMatrix;
import ubic.basecode.gui.JMatrixDisplay;
import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.math.CorrelationStats;
import ubic.gemma.analysis.coexpression.GeneEffectSizeCoExpressionAnalyzer;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import cern.colt.list.DoubleArrayList;

/**
 * @author raymond,xwan
 * @version $Id$
 */
public class CorrelationDistCli extends ExpressionExperimentManipulatingCLI {

    private ArrayDesignService adService = null;

    private int binNum = 100;
    private int[][] histogram = null;
    private Map<ExpressionExperiment, Integer> eeIndexMap = null;
    private Collection<ExpressionExperiment> noLinkEEs = null;
    private ByteArrayConverter bac = new ByteArrayConverter();

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {

        Option binNumOption = OptionBuilder.hasArg().withArgName( "Bin Num for Histogram" ).withDescription(
                "Bin Num for Histogram" ).withLongOpt( "binNum" ).create( 'b' );
        addOption( binNumOption );
    }

    @Override
    protected void processOptions() {
        super.processOptions();

        if ( hasOption( 'b' ) ) {
            this.binNum = Integer.valueOf( getOptionValue( 'b' ) );
        }
        adService = ( ArrayDesignService ) this.getBean( "arrayDesignService" );
        noLinkEEs = new HashSet<ExpressionExperiment>();
    }

    /**
     * @param ee
     * @return
     */
    private QuantitationType getPreferredQT( ExpressionExperiment ee ) {
        Collection<QuantitationType> qts = eeService.getQuantitationTypes( ee );
        for ( QuantitationType qt : qts ) {
            if ( qt.getIsPreferred() ) return qt;
        }
        return null;
    }

    /**
     * @param needles
     * @param geneIds
     * @return
     */
    boolean known( Collection<Long> needles, Collection<Long> geneIds ) {
        boolean res = false;
        for ( Long id : needles ) {
            if ( geneIds.contains( id ) ) return true;
        }
        return res;
    }

    /**
     * @param source
     * @param target
     * @return
     */
    private double medianEPCorrelation( Collection<ExpressionProfile> source, Collection<ExpressionProfile> target ) {
        DoubleArrayList data = new DoubleArrayList();
        for ( ExpressionProfile ep1 : source ) {
            for ( ExpressionProfile ep2 : target ) {
                if ( ep1.val.length == ep2.val.length
                        && ep1.val.length > GeneEffectSizeCoExpressionAnalyzer.MINIMUM_SAMPLE ) {
                    data.add( CorrelationStats.correl( ep1.val, ep2.val ) );
                }
            }
        }
        data.sort();
        return ( data.size() > 0 ) ? data.get( data.size() / 2 ) : 0.0;
    }

    /**
     * @param genes
     * @return
     */
    private Object[] shuffling( Object[] genes ) {
        Object[] shuffledGenes = new Object[genes.length];
        System.arraycopy( genes, 0, shuffledGenes, 0, genes.length );
        Random random = new Random();
        for ( int i = genes.length - 1; i >= 0; i-- ) {
            int pos = random.nextInt( i + 1 );
            Object tmp = shuffledGenes[pos];
            shuffledGenes[pos] = shuffledGenes[i];
            shuffledGenes[i] = tmp;
        }
        return shuffledGenes;
    }

    /**
     * @param ee
     * @param cs2knowngenes
     * @return
     */
    @SuppressWarnings("unchecked")
    Collection<Double> calculateCorrs( ExpressionExperiment ee, Map<Long, Collection<Long>> cs2knowngenes ) {
        ArrayList<Double> corrs = new ArrayList<Double>();
        QuantitationType qt = getPreferredQT( ee );
        Map<DesignElementDataVector, Collection<Long>> dedv2genes = eeService.getDesignElementDataVectors(
                cs2knowngenes, qt );
        Map<Long, Collection<ExpressionProfile>> geneID2EPs = new HashMap<Long, Collection<ExpressionProfile>>();
        for ( DesignElementDataVector dedv : dedv2genes.keySet() ) {
            Collection<Long> geneIds = dedv2genes.get( dedv );
            for ( Long id : geneIds ) {
                Collection<ExpressionProfile> eps = geneID2EPs.get( id );
                if ( eps == null ) {
                    eps = new HashSet<ExpressionProfile>();
                    geneID2EPs.put( id, eps );
                }
                eps.add( new ExpressionProfile( dedv ) );
            }
        }
        Object[] geneIds = geneID2EPs.keySet().toArray();
        for ( int i = 0; i < 100; i++ ) {
            Object[] shuffledGeneIds = shuffling( geneIds );
            for ( int j = 0; j < shuffledGeneIds.length; j++ ) {
                Collection<ExpressionProfile> source = geneID2EPs.get( geneIds[j] );
                Collection<ExpressionProfile> target = geneID2EPs.get( shuffledGeneIds[j] );
                if ( source != null && target != null ) {
                    double corr = medianEPCorrelation( source, target );
                    corrs.add( corr );
                }
            }
        }
        return corrs;
    }

    /**
     * @param ee
     * @param geneIds
     */
    @SuppressWarnings("unchecked")
    void fillHistogram( ExpressionExperiment ee, Collection<Long> geneIds ) {
        int halfBin = binNum / 2;
        Collection<Long> csIds = new HashSet<Long>();
        Collection<DesignElement> allCSs = new HashSet<DesignElement>();
        Collection<ArrayDesign> ads = eeService.getArrayDesignsUsed( ee );
        for ( ArrayDesign ad : ads ) {
            allCSs.addAll( adService.loadCompositeSequences( ad ) );
        }
        for ( DesignElement cs : allCSs ) {
            csIds.add( cs.getId() );
        }

        Map<Long, Collection<Long>> cs2genes = geneService.getCS2GeneMap( csIds );
        Map<Long, Collection<Long>> cs2knowngenes = new HashMap<Long, Collection<Long>>();
        for ( Long csId : cs2genes.keySet() ) {
            Collection<Long> mappedGeneIds = cs2genes.get( csId );
            if ( known( mappedGeneIds, geneIds ) ) cs2knowngenes.put( csId, mappedGeneIds );
        }
        Collection<Double> corrs = calculateCorrs( ee, cs2knowngenes );
        int eeIndex = eeIndexMap.get( ee );
        for ( Double corr : corrs ) {
            int bin = Math.min( ( int ) ( ( 1.0 + corr ) * halfBin ), binNum - 1 );
            histogram[eeIndex][bin]++;
        }
    }

    /**
     * 
     */
    void saveHistogram() {
        try {
            FileWriter out = new FileWriter( new File( "correlationDist.txt" ) );
            List<String> rowLabels = new ArrayList<String>();
            List<String> colLabels = new ArrayList<String>();
            for ( int i = 0; i < binNum; i++ ) {
                out.write( "\t" + i );
                colLabels.add( Integer.toString( i ) );
            }
            out.write( "\n" );
            // double culmulative = 0.0;
            int culmulatives[] = new int[histogram.length];
            for ( int i = 0; i < histogram.length; i++ ) {
                for ( int j = 0; j < binNum; j++ ) {
                    culmulatives[i] = culmulatives[i] + histogram[i][j];
                }
            }
            for ( ExpressionExperiment ee : eeIndexMap.keySet() ) {
                if ( noLinkEEs.contains( ee ) ) continue;
                rowLabels.add( ee.getShortName() );
            }
            double data[][] = new double[histogram.length - noLinkEEs.size()][binNum];
            int dataIndex = 0;
            for ( ExpressionExperiment ee : eeIndexMap.keySet() ) {
                if ( noLinkEEs.contains( ee ) ) continue;
                out.write( eeService.getTaxon( ee.getId() ).getCommonName() + ee.getShortName() );
                int eeIndex = eeIndexMap.get( ee );
                for ( int j = 0; j < binNum; j++ ) {
                    data[dataIndex][j] = ( double ) histogram[eeIndex][j] / ( double ) culmulatives[eeIndex];
                    out.write( "\t" + data[dataIndex][j] );
                }
                out.write( "\n" );
                log.info( ee.getShortName() + "---->" + culmulatives[eeIndex] );
                dataIndex++;
            }
            DoubleMatrixNamed dataMatrix = new DenseDoubleMatrix2DNamed( data );
            dataMatrix.setRowNames( rowLabels );
            dataMatrix.setColumnNames( colLabels );

            ColorMatrix dataColorMatrix = new ColorMatrix( dataMatrix );
            // dataColorMatrix.setColorMap( ColorMap.GREENRED_COLORMAP );
            dataColorMatrix.setColorMap( ColorMap.BLACKBODY_COLORMAP );
            JMatrixDisplay dataMatrixDisplay = new JMatrixDisplay( dataColorMatrix );
            dataMatrixDisplay.saveImage( "correlationDist.png", true );

            out.write( "\n" );
            out.close();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "Correlation Distribution ", args );
        if ( err != null ) {
            return err;
        }

        try {

            histogram = new int[expressionExperiments.size()][binNum];
            eeIndexMap = new HashMap<ExpressionExperiment, Integer>();
            int index = 0;
            for ( ExpressionExperiment ee : expressionExperiments ) {
                eeIndexMap.put( ee, index );
                index++;
            }
            Collection<Gene> genes = geneService.loadKnownGenes( taxon );
            Collection<Long> geneIds = new HashSet<Long>();
            for ( Gene gene : genes )
                geneIds.add( gene.getId() );
            for ( ExpressionExperiment ee : expressionExperiments ) {
                fillHistogram( ee, geneIds );
            }
            saveHistogram();
            return null;
        } catch ( Exception e ) {
            return e;
        }
    }

    /**
     */
    public class ExpressionProfile {
        DesignElementDataVector dedv = null;

        double[] val = null;

        long id;

        /**
         * Construct an ExpressionProfile from the specified DesignElementDataVector
         * 
         * @param dedv - vector to convert
         */
        public ExpressionProfile( DesignElementDataVector dedv ) {
            this.dedv = dedv;
            this.id = dedv.getId();
            byte[] bytes = dedv.getData();
            val = bac.byteArrayToDoubles( bytes );
        }

        /**
         * Get the ID of the vector
         * 
         * @return the vector ID
         */
        public long getId() {
            return this.id;
        }
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        CorrelationDistCli corrDist = new CorrelationDistCli();
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Exception ex = corrDist.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
            watch.stop();
            log.info( watch.getTime() / 1000 );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

}
