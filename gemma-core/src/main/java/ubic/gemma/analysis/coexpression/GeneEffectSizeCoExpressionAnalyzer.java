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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cern.colt.list.DoubleArrayList;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix2DNamed;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.dataStructure.matrix.ObjectMatrix2DNamed;
import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.math.CorrelationStats;
import ubic.basecode.math.metaanalysis.CorrelationEffectMetaAnalysis;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;

/**
 * Compute the pairwise correlations for a bunch of dedvs for a bunch of genes. This uses the 'effect-size' approach.
 * 
 * @author xwan
 * @version $Id$
 */
public class GeneEffectSizeCoExpressionAnalyzer {
    private class ExpressedData {
        public DesignElementDataVector query = null;
        public DesignElementDataVector coexpressed = null;

        public ExpressedData( DesignElementDataVector query, DesignElementDataVector coexpressed ) {
            this.query = query;
            this.coexpressed = coexpressed;
        }
    }

    private Map<Long, Map<Long, Collection<DesignElementDataVector>>> ee2gene2dedvs = new HashMap<Long, Map<Long, Collection<DesignElementDataVector>>>();;

    private Map<DesignElementDataVector, Collection<Gene>> dedv2genes = new HashMap<DesignElementDataVector, Collection<Gene>>();;

    // Cached the mean value and STD value for DesignElementDataVector.
    private Map<Long, Double> dedv2cachedMeanValue = new HashMap<Long, Double>();
    private Map<Long, Double> dedv2cachedSTDValue = new HashMap<Long, Double>();

    // The dimension of next two NamedMatrix in the map: coExpressedGenes X expression_experiments
    // The following map save the correlation data between query gene and each coExpressedGene in all EEs
    private Map<Long, DenseDoubleMatrix2DNamed> queryGene2correlationData = new HashMap<Long, DenseDoubleMatrix2DNamed>();
    // The following map save the DesignElementDataVectors (Encapulated in ExpressedData) involved in the correlation
    // caculation
    // between query gene and each coExpressedGene in all EEs, which will be used to get the rank matrix.
    // The reason for using ExpressedData object is that there are many DEDVs for each gene and different correlation
    // calculation
    // may use different DEDV.
    private Map<Long, ObjectMatrix2DNamed> queryGene2coExpressedData = new HashMap<Long, ObjectMatrix2DNamed>();

    // The next NamedMatrix: queryGenes X expression_experiments
    // Each object is a designElementVector which is specifically associated with the query gene in a expression
    // experiment
    private ObjectMatrix2DNamed queryGenesData = null;

    // The following two maps are only used for the output.
    private Map<Long, String> geneNames = new HashMap<Long, String>();
    private Map<Long, String> eeNames = new HashMap<Long, String>();
    private Map<Long, Integer> eeSampleSizes = new HashMap<Long, Integer>();

    private static Log log = LogFactory.getLog( GeneEffectSizeCoExpressionAnalyzer.class.getName() );
    private ExpressionExperimentService eeService = null;
    public static int MINIMUM_SAMPLE = 5;

    public GeneEffectSizeCoExpressionAnalyzer( Collection<Gene> queryGenes, Collection<Gene> coExpressedGenes,
            Collection<ExpressionExperiment> ees ) {
        queryGenesData = new ObjectMatrix2DNamed( queryGenes.size(), ees.size() );
        for ( Gene queryGene : queryGenes ) {
            queryGenesData.addRowName( queryGene.getId() );
        }
        for ( ExpressionExperiment ee : ees ) {
            queryGenesData.addColumnName( ee.getId() );
        }
        for ( int i = 0; i < queryGenesData.rows(); i++ ) {
            for ( int j = 0; j < queryGenesData.columns(); j++ ) {
                queryGenesData.setQuick( i, j, null );
            }
        }

        for ( Gene queryGene : queryGenes ) {
            DenseDoubleMatrix2DNamed correlationData = new DenseDoubleMatrix2DNamed( coExpressedGenes.size(), ees
                    .size() );
            ObjectMatrix2DNamed coExpressedData = new ObjectMatrix2DNamed( coExpressedGenes.size(), ees.size() );
            for ( int i = 0; i < correlationData.rows(); i++ ) {
                for ( int j = 0; j < correlationData.columns(); j++ ) {
                    correlationData.setQuick( i, j, Double.NaN );
                    coExpressedData.setQuick( i, j, null );
                }
            }
            for ( Gene coExpressedGene : coExpressedGenes ) {
                correlationData.addRowName( coExpressedGene.getId() );
                coExpressedData.addRowName( coExpressedGene.getId() );
            }
            for ( ExpressionExperiment ee : ees ) {
                correlationData.addColumnName( ee.getId() );
                coExpressedData.addColumnName( ee.getId() );
            }
            queryGene2correlationData.put( queryGene.getId(), correlationData );
            queryGene2coExpressedData.put( queryGene.getId(), coExpressedData );
        }

        for ( Gene queryGene : queryGenes ) {
            geneNames.put( queryGene.getId(), queryGene.getName() );
        }
        for ( Gene coExpressedGene : coExpressedGenes ) {
            geneNames.put( coExpressedGene.getId(), coExpressedGene.getName() );
        }
        for ( ExpressionExperiment ee : ees ) {
            eeNames.put( ee.getId(), ee.getShortName() );
        }
    }

    /**
     * @param dedvs
     */
    @SuppressWarnings("unchecked")
    private void distributeDesignElementDataVector( Set<DesignElementDataVector> dedvs ) {
        // First, get the sample sizes for Expression Experiments
        for ( DesignElementDataVector dedv : dedvs ) {
            ExpressionExperiment ee = dedv.getExpressionExperiment();
            if ( !eeSampleSizes.containsKey( ee.getId() ) ) {
                int sampleSize = getSampleSize( dedv );
                eeSampleSizes.put( ee.getId(), new Integer( sampleSize ) );
            }
        }
        // Second, distribute the dedvs to different buckets according to ee and genes, which will be used to caculate
        // the correlation
        // between genes in every expression experiments
        for ( DesignElementDataVector dedv : dedvs ) {
            ExpressionExperiment ee = dedv.getExpressionExperiment();
            if ( ee.getId() == null ) {
                System.err.println( ee + " wrong! " );
            }
            Map<Long, Collection<DesignElementDataVector>> gene2dedvs = ee2gene2dedvs.get( ee.getId() );
            if ( gene2dedvs == null ) {
                gene2dedvs = new HashMap<Long, Collection<DesignElementDataVector>>();
                Collection<Object> coExpressionGeneNames = null;
                for ( Object geneId : this.queryGenesData.getRowNames() ) {
                    gene2dedvs.put( ( Long ) geneId, new HashSet<DesignElementDataVector>() );
                    if ( coExpressionGeneNames == null ) {
                        coExpressionGeneNames = queryGene2coExpressedData.get( geneId ).getRowNames();
                    }
                }
                for ( Object geneId : coExpressionGeneNames ) {
                    gene2dedvs.put( ( Long ) geneId, new HashSet<DesignElementDataVector>() );
                }
                ee2gene2dedvs.put( ee.getId(), gene2dedvs );
            }
            HashSet<Gene> geneSet = ( HashSet<Gene> ) dedv2genes.get( dedv );
            for ( Gene gene : geneSet ) {
                Collection<DesignElementDataVector> mappedDevs = gene2dedvs.get( gene.getId() );
                /** The mapped gene for dev may not in both query genes and candidate genes** */
                if ( mappedDevs != null ) {
                    mappedDevs.add( dedv );
                }
            }
        }
    }

    /**
     * @param dedvI
     * @param dedvJ
     * @return
     */
    private double weightedCoRelation( DesignElementDataVector dedvI, DesignElementDataVector dedvJ ) {
        double corr = coRelation( dedvI, dedvJ );
        if ( !Double.isNaN( corr ) ) {
            ByteArrayConverter bac = new ByteArrayConverter();
            byte[] bytes = dedvI.getData();
            double[] ival = bac.byteArrayToDoubles( bytes );
            bytes = dedvJ.getData();
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
     * @param dedvI
     * @param dedvJ
     * @return
     */
    private double correlationPvalue( DesignElementDataVector dedvI, DesignElementDataVector dedvJ ) {
        double corr = coRelation( dedvI, dedvJ );
        if ( !Double.isNaN( corr ) ) {
            ByteArrayConverter bac = new ByteArrayConverter();
            byte[] bytes = dedvI.getData();
            double[] ival = bac.byteArrayToDoubles( bytes );
            bytes = dedvJ.getData();
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
     * @param dedv
     * @return
     */
    private int getSampleSize( DesignElementDataVector dedv ) {
        byte[] bytes = dedv.getData();
        ByteArrayConverter bac = new ByteArrayConverter();
        double[] val = bac.byteArrayToDoubles( bytes );
        return val.length;
    }

    /**
     * @param devI
     * @param devJ
     * @return
     */
    private double coRelation( DesignElementDataVector dedvI, DesignElementDataVector dedvJ ) {
        double corr = 0;
        byte[] bytes = dedvI.getData();
        ByteArrayConverter bac = new ByteArrayConverter();
        double[] ival = bac.byteArrayToDoubles( bytes );
        bytes = dedvJ.getData();
        double[] jval = bac.byteArrayToDoubles( bytes );

        if ( ival.length != jval.length ) {
            // System.err.print("Error in Dimension " + devI.getId()+ " " + ival.length + " (" +
            // devI.getExpressionExperiment().getId() + ") ");
            // System.err.println(devJ.getId() + " " + jval.length + " (" + devJ.getExpressionExperiment().getId() + ")
            // ");
            return Double.NaN;
        }
        if ( ival.length < GeneEffectSizeCoExpressionAnalyzer.MINIMUM_SAMPLE ) return Double.NaN;
        if ( dedvI.getId() == dedvJ.getId() ) {
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
            mean = dedv2cachedMeanValue.get( dedvI.getId() );
            sqrt = dedv2cachedSTDValue.get( dedvI.getId() );
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

                dedv2cachedMeanValue.put( dedvI.getId(), new Double( meani ) );
                dedv2cachedSTDValue.put( dedvI.getId(), new Double( sqrti ) );
            } else {
                meani = mean.doubleValue();
                sqrti = sqrt.doubleValue();
            }
            mean = dedv2cachedMeanValue.get( dedvJ.getId() );
            sqrt = dedv2cachedSTDValue.get( dedvJ.getId() );
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

                dedv2cachedMeanValue.put( dedvJ.getId(), new Double( meanj ) );
                dedv2cachedSTDValue.put( dedvJ.getId(), new Double( sqrtj ) );
            } else {
                meanj = mean.doubleValue();
                sqrtj = sqrt.doubleValue();
            }

            corr = CorrelationStats.correlFast( ival, jval, meani, meanj, sqrti, sqrtj );
        } else {
            corr = CorrelationStats.correl( ival, jval );
        }
        return corr;
    }

    /**
     * 
     *
     */
    private void calculateCoRelation() {
        for ( Long eeId : ee2gene2dedvs.keySet() ) {
            /* Calculate the paired gene coexpression values**** */
            Map<Long, Collection<DesignElementDataVector>> gene2dedvs = ee2gene2dedvs.get( eeId );
            for ( Long queryGeneId : queryGene2correlationData.keySet() ) {
                Object[] dedvI = gene2dedvs.get( queryGeneId ).toArray();
                DenseDoubleMatrix2DNamed correlationDataMatrix = queryGene2correlationData.get( queryGeneId );
                ObjectMatrix2DNamed coExpressedData = queryGene2coExpressedData.get( queryGeneId );

                for ( Object coExpressedGeneId : correlationDataMatrix.getRowNames() ) {
                    if ( coExpressedGeneId.equals( queryGeneId ) ) continue;
                    Object[] dedvJ = gene2dedvs.get( coExpressedGeneId ).toArray();
                    // "shift" is used to code two integer (X,Y) into one bigger integer X*shift+Y
                    int shift = dedvI.length > dedvJ.length ? dedvI.length : dedvJ.length;
                    TreeMap<Double, Integer> sortedData = new TreeMap<Double, Integer>();
                    for ( int ii = 0; ii < dedvI.length; ii++ )
                        for ( int jj = 0; jj < dedvJ.length; jj++ ) {
                            double corr = this.coRelation( ( DesignElementDataVector ) dedvI[ii],
                                    ( DesignElementDataVector ) dedvJ[jj] );
                            if ( !Double.isNaN( corr ) )
                                sortedData.put( new Double( corr ), new Integer( ii * shift + jj ) );
                        }
                    if ( sortedData.size() > 0 ) {
                        Object corrArray[] = sortedData.keySet().toArray();
                        Double medianCorr = ( Double ) corrArray[corrArray.length / 2];
                        int rowIndex = correlationDataMatrix.getRowIndexByName( coExpressedGeneId );
                        int colIndex = correlationDataMatrix.getColIndexByName( eeId );
                        correlationDataMatrix.setQuick( rowIndex, colIndex, medianCorr ); // choose median value

                        Integer combinedIndex = sortedData.get( medianCorr );
                        int dedvIndexI = combinedIndex.intValue() / shift;
                        int dedvIndexJ = combinedIndex.intValue() % shift;

                        coExpressedData.setQuick( rowIndex, colIndex, new ExpressedData(
                                ( DesignElementDataVector ) dedvI[dedvIndexI],
                                ( DesignElementDataVector ) dedvJ[dedvIndexJ] ) );
                    }
                }
            }
        }
    }

    /**
     * @return
     */
    public Map calculateMatrixEffectSize() {
        Map<Long, Double> effectSizeMap = new HashMap<Long, Double>();
        double matrixEffectSize = 0.0;
        CorrelationEffectMetaAnalysis metaAnalysis = new CorrelationEffectMetaAnalysis( true, false );
        DoubleArrayList correlations = new DoubleArrayList();
        DoubleArrayList sampleSizes = new DoubleArrayList();
        DoubleArrayList effectSizes = new DoubleArrayList();
        for ( Long queryGeneId : queryGene2correlationData.keySet() ) {
            DenseDoubleMatrix2DNamed correlationDataMatrix = queryGene2correlationData.get( queryGeneId );
            for ( Object coExpressedGeneId : correlationDataMatrix.getRowNames() ) {
                if ( coExpressedGeneId.equals( queryGeneId ) ) continue;
                int rowIndex = correlationDataMatrix.getRowIndexByName( coExpressedGeneId );
                for ( Long eeId : eeSampleSizes.keySet() ) {
                    Integer eeSampleSize = eeSampleSizes.get( eeId );
                    int colIndex = correlationDataMatrix.getColIndexByName( eeId );
                    double correlation = correlationDataMatrix.getQuick( rowIndex, colIndex );
                    if ( !Double.isNaN( correlation ) && eeSampleSize > 3 ) {
                        correlations.add( correlation );
                        sampleSizes.add( eeSampleSize );
                    }
                }
                if ( correlations.size() > eeSampleSizes.keySet().size() / 2 ) {
                    metaAnalysis.run( correlations, sampleSizes );
                    double effectSize = Math.abs( metaAnalysis.getE() );
                    effectSizes.add( effectSize );
                    matrixEffectSize = matrixEffectSize + effectSize;
                    // if(effectSize > 0.90)
                    // log.info("Effect Size " + queryGeneName + "_" + geneNames.get(coExpressedGeneId) + ":" +
                    // effectSize + ":"+correlations.size()+":"+metaAnalysis.getP());
                }
                correlations.clear();
                sampleSizes.clear();
            }
            effectSizes.sort();
            int num = ( int ) ( effectSizes.size() * 1.0 );
            double total = 0;
            for ( int i = 0; i < num; i++ ) {
                total = total + effectSizes.get( effectSizes.size() - i - 1 );
            }
            matrixEffectSize = total / num;
            effectSizeMap.put( queryGeneId, matrixEffectSize );
            effectSizes.clear();
        }
        return effectSizeMap;
    }

    /**
     * @param output
     * @param presencePercent
     */
    public void output( PrintStream output, double presencePercent, Gene inputGene ) {
        NumberFormat nf = NumberFormat.getNumberInstance();
        DecimalFormat df = ( DecimalFormat ) nf;
        df.applyPattern( "#.####" );
        output.print( "Experiments" );

        Long inputGeneId = inputGene.getId();

        double totalExpressionValuesInOneExperssionExperiment = 0;
        for ( Long queryGeneId : queryGene2correlationData.keySet() ) {
            if ( queryGeneId != inputGeneId ) continue;
            DenseDoubleMatrix2DNamed correlationDataMatrix = queryGene2correlationData.get( queryGeneId );
            String queryGeneName = geneNames.get( queryGeneId );
            for ( Object coExpressedGeneId : correlationDataMatrix.getRowNames() ) {
                if ( coExpressedGeneId.equals( queryGeneId ) ) continue;
                String coExpressedGeneName = geneNames.get( coExpressedGeneId );
                output.print( "\t" + queryGeneName + "_" + coExpressedGeneName );
                totalExpressionValuesInOneExperssionExperiment++;
            }
        }
        output.println();

        Object allEEs[] = queryGenesData.getColNames().toArray();
        for ( int ee = 0; ee < allEEs.length; ee++ ) {
            // Check the missing percentage
            double missing = 0;
            for ( Long queryGeneId : queryGene2correlationData.keySet() ) {
                DenseDoubleMatrix2DNamed correlationDataMatrix = queryGene2correlationData.get( queryGeneId );
                int colIndex = correlationDataMatrix.getColIndexByName( allEEs[ee] );
                for ( Object coExpressedGeneId : correlationDataMatrix.getRowNames() ) {
                    if ( coExpressedGeneId.equals( queryGeneId ) ) continue;
                    int rowIndex = correlationDataMatrix.getRowIndexByName( coExpressedGeneId );
                    if ( Double.isNaN( correlationDataMatrix.getQuick( rowIndex, colIndex ) ) ) missing++;
                }
            }
            if ( ( totalExpressionValuesInOneExperssionExperiment - missing )
                    / totalExpressionValuesInOneExperssionExperiment < presencePercent ) continue;

            output.print( eeNames.get( allEEs[ee] ) );
            for ( Long queryGeneId : queryGene2correlationData.keySet() ) {
                if ( queryGeneId != inputGeneId ) continue;
                DenseDoubleMatrix2DNamed correlationDataMatrix = queryGene2correlationData.get( queryGeneId );
                int colIndex = correlationDataMatrix.getColIndexByName( allEEs[ee] );
                for ( Object coExpressedGeneId : correlationDataMatrix.getRowNames() ) {
                    if ( coExpressedGeneId.equals( queryGeneId ) ) continue;
                    int rowIndex = correlationDataMatrix.getRowIndexByName( coExpressedGeneId );
                    if ( Double.isNaN( correlationDataMatrix.getQuick( rowIndex, colIndex ) ) )
                        output.print( "\t" );
                    else
                        output.print( "\t" + df.format( correlationDataMatrix.getQuick( rowIndex, colIndex ) ) );
                }
            }
            output.println();
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
        if ( rank1 == null || rank2 == null ) return 0;
        return ( rank1 + rank2 ) / 2;
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
            String rowName = eeNames.get( allEEs[ee] );
            int row = 0;
            // The output method may filter out some expression experiment, which will cause the getRowIndexByName
            // throw an noFoundError, which can be skipped
            try {
                row = dataMatrix.getRowIndexByName( rowName );
            } catch ( Exception e ) {
                continue;
            }

            for ( Long queryGeneId : queryGene2coExpressedData.keySet() ) {
                DenseDoubleMatrix2DNamed correlationDataMatrix = queryGene2correlationData.get( queryGeneId );
                ObjectMatrix2DNamed coExpressedDataMatrix = queryGene2coExpressedData.get( queryGeneId );
                int colIndex = correlationDataMatrix.getColIndexByName( allEEs[ee] );
                String queryGeneName = geneNames.get( queryGeneId );

                for ( Object coExpressedGeneId : correlationDataMatrix.getRowNames() ) {
                    if ( coExpressedGeneId.equals( queryGeneId ) ) continue;
                    int rowIndex = correlationDataMatrix.getRowIndexByName( coExpressedGeneId );
                    String coExpressedGeneName = geneNames.get( coExpressedGeneId );
                    String colName = queryGeneName + "_" + coExpressedGeneName;

                    try {
                        int col = dataMatrix.getColIndexByName( colName );

                        if ( Double.isNaN( correlationDataMatrix.getQuick( rowIndex, colIndex ) ) )
                            rankMatrix.set( row, col, Double.NaN );
                        else
                            rankMatrix.set( row, col, getExpressionRank( ( ExpressedData ) coExpressedDataMatrix
                                    .getQuick( rowIndex, colIndex ) ) );
                    } catch ( IllegalArgumentException e ) {
                        continue;
                    }
                }
            }
        }
        return rankMatrix;
    }

    /**
     * @param dedvs
     * @return
     */
    public boolean analyze( Set<DesignElementDataVector> dedvs ) {
        assert ( this.dedv2genes != null );
        assert ( this.eeService != null );
        this.distributeDesignElementDataVector( dedvs );
        this.calculateCoRelation();
        return true;
    }

    /**
     * @param devToGenes
     */
    public void setDedv2Genes( Map<DesignElementDataVector, Collection<Gene>> dedv2genes ) {
        this.dedv2genes = dedv2genes;
    }

    /**
     * @param eeService
     */
    public void setExpressionExperimentService( ExpressionExperimentService eeService ) {
        this.eeService = eeService;
    }
}
