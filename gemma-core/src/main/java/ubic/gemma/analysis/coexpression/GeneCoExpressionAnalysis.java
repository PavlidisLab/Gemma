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
package ubic.gemma.analysis.coexpression;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix2DNamed;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.dataStructure.matrix.ObjectMatrix2DNamed;
import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.math.CorrelationStats;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;

/**
 * Compute the pairwise correlations for a bunch of datavectors for a bunch of genes.
 * 
 * @author xwan
 * @version $Id$
 */
public class GeneCoExpressionAnalysis {
    private class ExpressedData {
        public DesignElementDataVector query = null;
        public DesignElementDataVector coexpressed = null;

        public ExpressedData( DesignElementDataVector query, DesignElementDataVector coexpressed ) {
            this.query = query;
            this.coexpressed = coexpressed;
        }
    }

    private Map<Long, Map<Long, Collection<DesignElementDataVector>>> ee_Gene_DataVectors = new HashMap<Long, Map<Long,Collection<DesignElementDataVector>>>();;

    private Map<DesignElementDataVector, Collection<Gene>> dataVector_Genes = new HashMap<DesignElementDataVector, Collection<Gene>>();;

    //Cached the mean value and STD value for designElementDataVector.
    private Map<Long, Double> dataVector_CachedMeanValue = new HashMap<Long, Double>();
    private Map<Long, Double> dataVector_CachedSTDValue = new HashMap<Long, Double>();
    
    //The next two NamedMatrix: coExpressedGenes X expression_experiments
    //The following map save the correlation data between query gene and each coExpressedGene in all EEs
    private Map<Long, DenseDoubleMatrix2DNamed> queryGene_correlationData = new HashMap<Long, DenseDoubleMatrix2DNamed>();
    //The following map save the designElementDataVectors (Encapulated in ExpressedData) involved in the correlation caculation
    //between query gene and each coExpressedGene in all EEs, which will be used to get the rank matrix.
    //The reason for using ExpressedData object is that there are many DEVs for each gene and different correlation calculation
    //may use different DEV.
    private Map<Long, ObjectMatrix2DNamed> queryGene_coExpressedData = new HashMap<Long, ObjectMatrix2DNamed>();
    
    //The next NamedMatrix: queryGenes X expression_experiments
    //Each object is a designElementVector which is specifically associated with the query gene in a expression experiment 
    private ObjectMatrix2DNamed queryGenesData = null;

    //The following two maps are only used for the output.
    private Map<Long, String> geneNames = new HashMap<Long, String>();
    private Map<Long, String> eeNames = new HashMap<Long, String>();
    
    private static Log log = LogFactory.getLog( GeneCoExpressionAnalysis.class.getName() );
    private ExpressionExperimentService eeService = null;
    private static int MINIMUM_SAMPLE = 5;

    public GeneCoExpressionAnalysis( Collection<Gene> queryGenes, Collection<Gene> coExpressedGenes,
            Collection<ExpressionExperiment> ees ) {
    	queryGenesData = new ObjectMatrix2DNamed(queryGenes.size(), ees.size());
        for(Gene queryGene:queryGenes){
        	queryGenesData.addRowName(queryGene.getId());
        }
        for(ExpressionExperiment ee:ees){
        	queryGenesData.addColumnName(ee.getId());
        }
        for(int i = 0; i < queryGenesData.rows(); i++){
        	for(int j = 0; j < queryGenesData.columns(); j++){
        		queryGenesData.setQuick(i, j, null);
        	}
        }

    	
        for(Gene queryGene:queryGenes){
        	DenseDoubleMatrix2DNamed correlationData = new DenseDoubleMatrix2DNamed(coExpressedGenes.size(), ees.size());
        	ObjectMatrix2DNamed coExpressedData = new ObjectMatrix2DNamed(coExpressedGenes.size(), ees.size());
            for ( int i = 0; i < correlationData.rows(); i++ ) {
                for ( int j = 0; j < correlationData.columns(); j++ ) {
                    correlationData.setQuick( i, j, Double.NaN );
                    coExpressedData.setQuick( i, j, null );
                }
            }
            for ( Gene coExpressedGene:coExpressedGenes ) {
                correlationData.addRowName( coExpressedGene.getId() );
                coExpressedData.addRowName(coExpressedGene.getId());
            }
            for ( ExpressionExperiment ee:ees ) {
                correlationData.addColumnName( ee.getId() );
                coExpressedData.addColumnName(ee.getId());
            }
            queryGene_correlationData.put(queryGene.getId(), correlationData);
            queryGene_coExpressedData.put(queryGene.getId(), coExpressedData);
        }
        
        for(Gene queryGene:queryGenes){
        	geneNames.put(queryGene.getId(), queryGene.getName());
        }
        for(Gene coExpressedGene:coExpressedGenes){
        	geneNames.put(coExpressedGene.getId(), coExpressedGene.getName());
        }
        for(ExpressionExperiment ee:ees){
        	eeNames.put(ee.getId(), ee.getShortName());
        }
    }

    /**
     * @param devs
     */
    private void distributeDesignElementDataVector( Set<DesignElementDataVector> devs ) {

        for ( DesignElementDataVector dev : devs ) {
            ExpressionExperiment ee = dev.getExpressionExperiment();
            if ( ee.getId() == null ) {
                System.err.println( ee + " wrong! " );
            }
            Map<Long, Collection<DesignElementDataVector>> geneToDevs = ee_Gene_DataVectors.get( ee.getId() );
            if ( geneToDevs == null ) {
                geneToDevs = new HashMap<Long, Collection<DesignElementDataVector>>();
                Collection<Object> coExpressionGeneNames = null;
                for ( Object geneId : this.queryGenesData.getRowNames() ) {
                    geneToDevs.put( (Long)geneId, new HashSet<DesignElementDataVector>() );
                    if(coExpressionGeneNames == null){
                    	coExpressionGeneNames = queryGene_coExpressedData.get(geneId).getRowNames();
                    }
                }
                for ( Object geneId : coExpressionGeneNames) {
                    geneToDevs.put( (Long)geneId, new HashSet<DesignElementDataVector>() );
                }
                ee_Gene_DataVectors.put( ee.getId(), geneToDevs );
            }
            HashSet<Gene> geneSet = ( HashSet<Gene> ) dataVector_Genes.get( dev );
            for ( Gene gene : geneSet ) {
                Collection<DesignElementDataVector> mappedDevs = geneToDevs.get( gene.getId() );
                /** The mapped gene for dev may not in both query genes and candidate genes** */
                if ( mappedDevs != null ) {
                    mappedDevs.add( dev );
                }
            }
        }
    }
    /**
     * @param numused
     * @param sxx
     * @param sx
     * @param syy
     * @param sy
     * @return
     */
    private double correlationNorm( int numused, double sxx, double sx, double syy, double sy ) {
        return ( sxx - sx * sx / numused ) * ( syy - sy * sy / numused );
    }

    /**
     * @param ival
     * @param jval
     * @return
     */
    private double correl( double[] ival, double[] jval ) {
        /* do it the old fashioned way */
        int numused = 0;
        double sxy = 0.0, sxx = 0.0, syy = 0.0, sx = 0.0, sy = 0.0;
        for ( int k = 0; k < ival.length; k++ ) {
            double xj = ival[k];
            double yj = jval[k];
            if ( !Double.isNaN( ival[k] ) && !Double.isNaN( jval[k] ) ) {
                sx += xj;
                sy += yj;
                sxy += xj * yj;
                sxx += xj * xj;
                syy += yj * yj;
                numused++;
            }
        }
        double denom = this.correlationNorm( numused, sxx, sx, syy, sy );
        double correl = ( sxy - sx * sy / numused ) / Math.sqrt( denom );
        return correl;
    }

    /**
     * @param ival
     * @param jval
     * @param meani
     * @param meanj
     * @param sqrti
     * @param sqrtj
     * @return
     */
    private double correlFast( double[] ival, double[] jval, double meani, double meanj, double sqrti, double sqrtj ) {
        double sxy = 0.0;
        for ( int k = 0, n = ival.length; k < n; k++ ) {
            sxy += ( ival[k] - meani ) * ( jval[k] - meanj );
        }
        return sxy / ( sqrti * sqrtj );
    }

    /**
     * @param devI
     * @param devJ
     * @return
     */
    private double weightedCoRelation( DesignElementDataVector devI, DesignElementDataVector devJ ) {
        double corr = coRelation( devI, devJ );
        if ( !Double.isNaN( corr ) ) {
            ByteArrayConverter bac = new ByteArrayConverter();
            byte[] bytes = devI.getData();
            double[] ival = bac.byteArrayToDoubles( bytes );
            bytes = devJ.getData();
            double[] jval = bac.byteArrayToDoubles( bytes );
            int numsamples = 0;
            for ( int i = 0; i < ival.length; i++ ) {
                if ( !Double.isNaN( ival[i] ) && !Double.isNaN( jval[i] ) ) numsamples++;
            }
            double samplingVariance = 1;// CorrelationEffectMetaAnalysis.samplingVariance(corr, numsamples);
            if ( Double.isNaN( samplingVariance ) ) {
                corr = Double.NaN;
            } else {
                corr = corr / samplingVariance;
            }
        }
        return corr;
    }

    /**
     * @param devI
     * @param devJ
     * @return
     */
    private double CoRelation_Pvalue( DesignElementDataVector devI, DesignElementDataVector devJ ) {
        double corr = coRelation( devI, devJ );
        if ( !Double.isNaN( corr ) ) {
            ByteArrayConverter bac = new ByteArrayConverter();
            byte[] bytes = devI.getData();
            double[] ival = bac.byteArrayToDoubles( bytes );
            bytes = devJ.getData();
            double[] jval = bac.byteArrayToDoubles( bytes );
            int numsamples = 0;
            for ( int i = 0; i < ival.length; i++ ) {
                if ( !Double.isNaN( ival[i] ) && !Double.isNaN( jval[i] ) ) numsamples++;
            }
            double p = CorrelationStats.pvalue( corr, numsamples );
            if ( p >= 0.10 )
                corr = Double.NaN;
            else {
                double samplingVariance = 1; // CorrelationEffectMetaAnalysis.samplingVariance(corr, numsamples);
                samplingVariance = 1;
                corr = corr / samplingVariance;
            }

        }
        return corr;
    }

    /**
     * @param devI
     * @param devJ
     * @return
     */
    private double coRelation( DesignElementDataVector devI, DesignElementDataVector devJ ) {
        double corr = 0;
        byte[] bytes = devI.getData();
        ByteArrayConverter bac = new ByteArrayConverter();
        double[] ival = bac.byteArrayToDoubles( bytes );
        bytes = devJ.getData();
        double[] jval = bac.byteArrayToDoubles( bytes );

        if ( ival.length != jval.length ) {
            // System.err.print("Error in Dimension " + devI.getId()+ " " + ival.length + " (" +
            // devI.getExpressionExperiment().getId() + ") ");
            // System.err.println(devJ.getId() + " " + jval.length + " (" + devJ.getExpressionExperiment().getId() + ")
            // ");
            return Double.NaN;
        }
        if ( ival.length < GeneCoExpressionAnalysis.MINIMUM_SAMPLE ) return Double.NaN;
        if ( devI.getId() == devJ.getId() ) {
            // System.err.println("Error in " + devI.getExpressionExperiment().getId());
            return Double.NaN;
        }
        int i;
        for ( i = 0; i < ival.length; i++ ) {
            if ( Double.isNaN( ival[i] ) || Double.isNaN( jval[i] ) ) break;
        }
        if ( i == ival.length ) {
            double meani, meanj, sqrti, sqrtj;
            Double mean, sqrt;
            mean = dataVector_CachedMeanValue.get( devI.getId() );
            sqrt = dataVector_CachedSTDValue.get( devI.getId() );
            if ( mean == null ) {
                double ax = 0.0, sxx = 0.0;
                for ( int j = 0; j < ival.length; j++ ) {
                    ax += ival[j];
                }
                meani = ( ax / ival.length );

                for ( int j = 0; j < ival.length; j++ ) {
                    double xt = ival[j] - meani; /* deviation from mean */
                    sxx += xt * xt; /* sum of squared error */
                }
                sqrti = Math.sqrt( sxx );

                dataVector_CachedMeanValue.put( devI.getId(), new Double( meani ) );
                dataVector_CachedSTDValue.put( devI.getId(), new Double( sqrti ) );
            } else {
                meani = mean.doubleValue();
                sqrti = sqrt.doubleValue();
            }
            mean = dataVector_CachedMeanValue.get( devJ.getId() );
            sqrt = dataVector_CachedSTDValue.get( devJ.getId() );
            if ( mean == null ) {
                double ay = 0.0, syy = 0.0;
                for ( int j = 0; j < ival.length; j++ ) {
                    ay += jval[j];
                }
                meanj = ( ay / ival.length );

                for ( int j = 0; j < ival.length; j++ ) {
                    double yt = jval[j] - meanj; /* deviation from mean */
                    syy += yt * yt; /* sum of squared error */
                }
                sqrtj = Math.sqrt( syy );

                dataVector_CachedMeanValue.put( devJ.getId(), new Double( meanj ) );
                dataVector_CachedSTDValue.put( devJ.getId(), new Double( sqrtj ) );
            } else {
                meanj = mean.doubleValue();
                sqrtj = sqrt.doubleValue();
            }

            corr = correlFast( ival, jval, meani, meanj, sqrti, sqrtj );
        } else {
            corr = correl( ival, jval );
        }
        return corr;
    }

    /**
     * 
     *
     */
    private void calculateCoRelation() {
        for ( Long eeId : ee_Gene_DataVectors.keySet() ) {
            /* Calculate the paired gene coexpression values**** */
            Map<Long, Collection<DesignElementDataVector>> geneToDevs = ee_Gene_DataVectors.get( eeId );
            for(Long queryGeneId:queryGene_correlationData.keySet()){
                Object[] devI = geneToDevs.get( queryGeneId ).toArray();
                DenseDoubleMatrix2DNamed correlationDataMatrix = queryGene_correlationData.get(queryGeneId);
                ObjectMatrix2DNamed coExpressedData = queryGene_coExpressedData.get(queryGeneId);
                
                for(Object coExpressedGeneId:correlationDataMatrix.getRowNames()){
                    Object[] devJ = geneToDevs.get( (Long)coExpressedGeneId  ).toArray();
                    //"shift" is used to code two integer (X,Y) into one bigger integer X*shift+Y 
                    int shift = devI.length > devJ.length ? devI.length : devJ.length;
                    TreeMap<Double, Integer> sortedData = new TreeMap<Double, Integer>();
                    for ( int ii = 0; ii < devI.length; ii++ )
                        for ( int jj = 0; jj < devJ.length; jj++ ) {
                            double corr = this.coRelation( ( DesignElementDataVector ) devI[ii],
                                    ( DesignElementDataVector ) devJ[jj] );
                            if ( !Double.isNaN( corr ) )
                                sortedData.put( new Double( corr ), new Integer( ii * shift + jj ) );
                        }
                    if ( sortedData.size() > 0 ) {
                        Object corrArray[] = sortedData.keySet().toArray();
                        Double medianCorr = ( Double ) corrArray[corrArray.length / 2];
                        int rowIndex = correlationDataMatrix.getRowIndexByName(coExpressedGeneId);
                        int colIndex = correlationDataMatrix.getColIndexByName(eeId);
                        correlationDataMatrix.setQuick(rowIndex, colIndex, medianCorr); //choose median value
                        
                        Integer combinedIndex = sortedData.get( medianCorr );
                        int devIndexI = combinedIndex.intValue() / shift;
                        int devIndexJ = combinedIndex.intValue() % shift;
                 
                        coExpressedData.setQuick(rowIndex, colIndex,new ExpressedData(
                                ( DesignElementDataVector ) devI[devIndexI],
                                ( DesignElementDataVector ) devJ[devIndexJ] ) );
                    } 
                }
            }
        }
    }

    /**
     * @param output
     * @param presencePercent
     */
    public void output( PrintStream output, double presencePercent ) {
        NumberFormat nf = NumberFormat.getNumberInstance();
        DecimalFormat df = ( DecimalFormat ) nf;
        df.applyPattern( "#.####" );
        output.print( "Experiments" );

        double totalExpressionValuesInOneExperssionExperiment = 0;
        for(Long queryGeneId:queryGene_correlationData.keySet()){
            DenseDoubleMatrix2DNamed correlationDataMatrix = queryGene_correlationData.get(queryGeneId);
            String queryGeneName = geneNames.get(queryGeneId);
            for(Object coExpressedGeneId:correlationDataMatrix.getRowNames()){
            	String coExpressedGeneName = geneNames.get(coExpressedGeneId);
                output.print( "\t" + queryGeneName + "_" + coExpressedGeneName );
                totalExpressionValuesInOneExperssionExperiment++;
            }
        }
        output.println();

        Object allEEs[] = queryGenesData.getColNames().toArray();
        for ( int ee = 0; ee < allEEs.length; ee++ ) {
            // Check the missing percentage
            double missing = 0;
            for(Long queryGeneId:queryGene_correlationData.keySet()){
                DenseDoubleMatrix2DNamed correlationDataMatrix = queryGene_correlationData.get(queryGeneId);
                int colIndex = correlationDataMatrix.getColIndexByName(allEEs[ee]);
                for(Object coExpressedGeneId:correlationDataMatrix.getRowNames()){
                    int rowIndex = correlationDataMatrix.getRowIndexByName(coExpressedGeneId);
                    if(Double.isNaN(correlationDataMatrix.getQuick(rowIndex,colIndex))) missing++;
                }
            }
            if ( ( totalExpressionValuesInOneExperssionExperiment - missing ) / totalExpressionValuesInOneExperssionExperiment < presencePercent ) continue;

            output.print( eeNames.get(allEEs[ee]) );
            for(Long queryGeneId:queryGene_correlationData.keySet()){
                DenseDoubleMatrix2DNamed correlationDataMatrix = queryGene_correlationData.get(queryGeneId);
                int colIndex = correlationDataMatrix.getColIndexByName(allEEs[ee]);
                for(Object coExpressedGeneId:correlationDataMatrix.getRowNames()){
                    int rowIndex = correlationDataMatrix.getRowIndexByName(coExpressedGeneId);
                    if(Double.isNaN(correlationDataMatrix.getQuick(rowIndex,colIndex))) 
                        output.print( "\t" );
                    else
                        output.print( "\t" + df.format( correlationDataMatrix.getQuick(rowIndex,colIndex)) );
                }
            }
        }
    }

    /**
     * @param expressedData
     * @return
     */
    private double getExpressionRank( ExpressedData expressedData ) {
    	Double rank1 = null, rank2 = null;
        rank1 = expressedData.query.getRank();
        rank2 = expressedData.coexpressed.getRank();
        if ( rank1 == null || rank2 == null) return 0;
        return (rank1+rank2)/2;
    }

    /**
     * @param dataMatrix
     * @return
     */
    public DoubleMatrixNamed getRankMatrix( DoubleMatrixNamed dataMatrix ) {
        double[][] rank = new double[dataMatrix.rows()][dataMatrix.columns()];
        DoubleMatrixNamed rankMatrix = new DenseDoubleMatrix2DNamed( rank );
        rankMatrix.setRowNames( dataMatrix.getRowNames() );
        rankMatrix.setColumnNames( dataMatrix.getColNames() );
        
        Object allEEs[] = queryGenesData.getColNames().toArray();
        for ( int ee = 0; ee < allEEs.length; ee++ ) {
            String rowName = eeNames.get(allEEs[ee]);
            int row = dataMatrix.getRowIndexByName( rowName );
            
            for(Long queryGeneId:queryGene_coExpressedData.keySet()){
                DenseDoubleMatrix2DNamed correlationDataMatrix = queryGene_correlationData.get(queryGeneId);
                ObjectMatrix2DNamed coExpressedDataMatrix = queryGene_coExpressedData.get(queryGeneId);
                int colIndex = correlationDataMatrix.getColIndexByName(allEEs[ee]);
                String queryGeneName = geneNames.get(queryGeneId);
                
                for(Object coExpressedGeneId:correlationDataMatrix.getRowNames()){
                    int rowIndex = correlationDataMatrix.getRowIndexByName(coExpressedGeneId);

                    String coExpressedGeneName = geneNames.get(coExpressedGeneId);
                	String colName =  queryGeneName + "_" + coExpressedGeneName;
                    int col = dataMatrix.getColIndexByName( colName );

                    if(Double.isNaN(correlationDataMatrix.getQuick(rowIndex,colIndex))) 
                    	rankMatrix.set( row, col, Double.NaN );
                    else
                        rankMatrix.set( row, col, getExpressionRank( (ExpressedData)coExpressedDataMatrix.getQuick(rowIndex, colIndex)) );
                }
            }
        }
        return rankMatrix;
    }

    /**
     * @param devs
     * @return
     */
    public boolean analysis( Set<DesignElementDataVector> devs ) {
        assert ( this.dataVector_Genes != null );
        assert ( this.eeService != null );
        this.distributeDesignElementDataVector( devs );
        this.calculateCoRelation();
        return true;
    }

    /**
     * @param devToGenes
     */
    public void setDevToGenes( Map<DesignElementDataVector, Collection<Gene>> dataVector_Genes ) {
        this.dataVector_Genes = ( HashMap<DesignElementDataVector, Collection<Gene>> ) dataVector_Genes;
    }

    /**
     * @param eeService
     */
    public void setExpressionExperimentService( ExpressionExperimentService eeService ) {
        this.eeService = eeService;
    }
}
