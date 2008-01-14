/*
 * The Gemma project
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
package ubic.gemma.analysis.diff;

import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import org.rosuda.REngine.REXPMismatchException;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix2DNamed;
import ubic.basecode.dataStructure.matrix.DoubleMatrix2DNamedFactory;
import ubic.gemma.analysis.util.RCommander;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import cern.colt.list.DoubleArrayList;

/**
 * This class makes SAM analysis for differential expression by calling R with a newly constructed matrix of expression
 * levels.
 * 
 * @author gozde
 * @version $Id$
 */
public class SimpleSAMAnalyzer extends RCommander {

    ExpressionDataManager manager;
    DenseDoubleMatrix2DNamed expressionLevelsMatrix = null;
    int[] columnLabels;

    public SimpleSAMAnalyzer() {
        super();
        manager = new ExpressionDataManager();
    }

    /**
     * @param fileName name of the experiment file
     * @param subsetName1 name of the first subset of the experiment
     * @param subsetName2 name of the second subset of the experiment
     * @return a list of probeIds of the significant genes
     */
    protected List<Object> getSignificantGenes( String fileName, String subsetName1, String subsetName2 ) {
        createExpressionMatrix( fileName, subsetName1, subsetName2 );

        List<Object> sigGenes = SAMAnalysis();
        log.info( sigGenes.size() + " significant genes have been detected." );
        return sigGenes;

    }

    /**
     * This method creates and returns the special matrix that contains the expression levels of the genes for just the
     * specified 2 subsets.
     */
    protected DenseDoubleMatrix2DNamed createExpressionMatrix( String fileName, String subsetName1, String subsetName2 ) {
        log.info( "Creating the expression matrix of the two subsets : " + subsetName1 + " - " + subsetName2 );
        manager = new ExpressionDataManager( fileName );
        Collection<DesignElementDataVector> dataVectors = manager.getDesignElementDataVectors();
        Collection<BioAssay> bioAssays = manager.getDataVectorBioAssays();

        Collection<BioAssay> subset1BioAssays = manager.getBioAssaysOfSubset( subsetName1 );
        Collection<BioAssay> subset2BioAssays = manager.getBioAssaysOfSubset( subsetName2 );

        int numOfRows = dataVectors.size();
        int numOfColumns = subset1BioAssays.size() + subset2BioAssays.size();

        // initialize the expression matrix and give the column names
        expressionLevelsMatrix = DoubleMatrix2DNamedFactory.dense( numOfRows, numOfColumns );

        // write column labels
        columnLabels = new int[numOfColumns];
        for ( int k = 0; k < subset1BioAssays.size(); k++ )
            columnLabels[k] = 0;
        for ( int y = subset2BioAssays.size(); y < columnLabels.length; y++ )
            columnLabels[y] = 1;

        if ( subset1BioAssays.size() != 0 && subset2BioAssays.size() != 0 ) {
            int geneCtr = 0;
            for ( DesignElementDataVector dataVector : dataVectors ) {
                double[] expressionLevels = manager.getExpressionLevels( dataVector );
                DoubleArrayList group1ExpressionLevels = new DoubleArrayList();
                DoubleArrayList group2ExpressionLevels = new DoubleArrayList();

                for ( BioAssay group1bioAssay : subset1BioAssays ) {
                    int bioAssayCtr = 0;
                    for ( BioAssay bioAssay : bioAssays ) {
                        if ( group1bioAssay.getAccession().getAccession().equals(
                                bioAssay.getAccession().getAccession() ) ) {
                            group1ExpressionLevels.add( expressionLevels[bioAssayCtr] );
                            break;
                        }
                        bioAssayCtr++;
                    }
                }
                for ( BioAssay group2bioAssay : subset2BioAssays ) {
                    int bioAssayCtr = 0;
                    for ( BioAssay bioAssay : bioAssays ) {
                        if ( group2bioAssay.getAccession().getAccession().equals(
                                bioAssay.getAccession().getAccession() ) ) {
                            group2ExpressionLevels.add( expressionLevels[bioAssayCtr] );
                            break;
                        }
                        bioAssayCtr++;
                    }
                }

                // put the expression levels of two subsets into one row of the matrix
                expressionLevelsMatrix.addRowName( dataVector.getDesignElement().getName() );
                for ( int k = 0; k < group1ExpressionLevels.size(); k++ )
                    expressionLevelsMatrix.set( geneCtr, k, group1ExpressionLevels.get( k ) );
                for ( int y = 0; y < group2ExpressionLevels.size(); y++ )
                    expressionLevelsMatrix.set( geneCtr, group1ExpressionLevels.size() + y, group2ExpressionLevels
                            .get( y ) );
                geneCtr++;
            }
        }
        return expressionLevelsMatrix;
    }

    /**
     * This method calls R to perform SAM analysis using siggenes library.
     * 
     * @return a list of probe ids of the significant genes
     * @throws REXPMismatchException
     */
    protected List<Object> SAMAnalysis() {
        log.info( "Performing SAM analysis." );
        List<Object> significantGenes = new Vector<Object>();

        // change library to siggenes
        rc.loadLibrary( "siggenes)" );
        rc.assign( "cl", columnLabels );

        // sam.out <- sam(data, cl)
        rc.voidEval( "sam.out <- sam(" + rc.assignMatrix( expressionLevelsMatrix ) + ", cl)" );

        // rc.voidEval("sam.sum3 <- summary(sam.out, 3, ll=FALSE)");
        rc.voidEval( "sam.sum3 <- summary(sam.out, 3)" );

        int[] rowsOfSigGenes = rc.intArrayEval( "sam.sum3@row.sig.genes" );

        for ( int k = 0; k < rowsOfSigGenes.length; k++ )
            significantGenes.add( expressionLevelsMatrix.getRowName( rowsOfSigGenes[k] - 1 ) );

        return significantGenes;
    }

    /**
     * This method is used to write significant genes for differential expression to an output file
     */
    protected void writeSignificantGenesToFile( String fileName, List<Object> sigGenes ) {
        manager.writeSignificantGenesToFileWithoutPValues( fileName, sigGenes );
    }

    /**
     * This method finds the significant genes among different experiments
     * 
     * @param List<String> list of the names of the experiments
     * @return a hashtable whose entries contain significant genes in the form: probeId: list of experiment names in
     *         which this gene is significant
     */
    protected Hashtable<String, List<String>> getSignificantGenesAcrossExperiments( List<String> list ) {
        return manager.getSignificantGenesAcrossExperimentsWithoutPValues( list );
    }

    /**
     * This method writes the hashtable of significant genes across experiments to an output file
     */
    protected void writeSignificantGenesAcrossExperimentsToFile( String fileName, Hashtable<String, List<String>> table ) {
        manager.writeSignificantGenesAcrossExperimentsToFile( fileName, table );
    }

    public static void main( String[] args ) {
        SimpleSAMAnalyzer analyzer = new SimpleSAMAnalyzer();

        // in order not to get OutOfMemoryError, I run these all three sections below seperately, one after another.

        // --- part 1 ----
        List<Object> sigGenes = analyzer.getSignificantGenes( "GDS1318", "wild type", "HSL null" );
        analyzer.writeSignificantGenesToFile( "GDS1318", sigGenes );

        // --- part 2 ----
        List<Object> sigGenes2 = analyzer.getSignificantGenes( "GDS1328", "saline", "OVA" );
        analyzer.writeSignificantGenesToFile( "GDS1328", sigGenes2 );

        // --- part 3 ----
        List<String> list = new Vector<String>();
        list.add( "GDS1318" );
        list.add( "GDS1328" );
        Hashtable<String, List<String>> table = analyzer.getSignificantGenesAcrossExperiments( list );
        analyzer.writeSignificantGenesAcrossExperimentsToFile( "siggenes_across_experiments", table );

        System.exit( 0 );
    }

}
