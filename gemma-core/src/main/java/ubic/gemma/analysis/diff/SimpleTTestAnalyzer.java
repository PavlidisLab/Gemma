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

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import ubic.basecode.math.MultipleTestCorrection;
import ubic.gemma.analysis.util.RCommander;
import cern.colt.list.DoubleArrayList;

/**
 * This class makes t-tests by calling R to detect significant genes for differential expression
 * 
 * @author gozde
 * @version $Id$
 */
public class SimpleTTestAnalyzer extends RCommander {

    public final double fdr = 0.01;

    ExpressionDataManager manager;

    public SimpleTTestAnalyzer() throws IOException {
        super();
    }

    /**
     * This method detects the significant genes of an experiment for differential expression and returns a list of
     * them.
     * 
     * @param fileName name of the file from which the expression levels will be read
     * @param subsetName1 name for the first subset of bioassays
     * @param subsetName2 name for the second subset of bioassays
     * @return Hashtable<String, Double> table of the probe ids of the significant genes with p values
     */
    protected Hashtable<String, Double> getSignificantGenes( String fileName, String subsetName1, String subsetName2 ) {
        manager = new ExpressionDataManager( fileName );

        Hashtable<String, Double> pValuesTable;

        pValuesTable = findPValues( subsetName1, subsetName2 );

        log.info( pValuesTable.size() + " p values have been calculated." );
        DoubleArrayList list = new DoubleArrayList();
        Enumeration pVals = pValuesTable.elements();
        while ( pVals.hasMoreElements() )
            list.add( ( Double ) pVals.nextElement() );

        double threshold = MultipleTestCorrection.BenjaminiHochbergCut( list, fdr );
        log.info( "P value cut-off : " + threshold );
        Hashtable<String, Double> significantGenes = manager.getSignificantGenes( pValuesTable, threshold );
        log.info( significantGenes.size() + " significant genes have been detected." );
        return significantGenes;
    }

    /**
     * This method finds the p-value for each gene, whose expression levels are seperated into two groups
     * 
     * @param fileName name of the file from which the expression levels will be read
     * @param subsetName1 name for the first subset of bioassays
     * @param subsetName2 name for the second subset of bioassays
     * @return a Hashtable in which each entry is (ProbeId -> p value)
     * @throws REXPMismatchException
     */
    protected Hashtable<String, Double> findPValues( String subsetName1, String subsetName2 ) {
        log.info( "Finding p values of the experiment." );
        Hashtable<String, Double> pValuesList = new Hashtable<String, Double>();
        Map<String, Object> table = manager.getExpressionData();

        String[] subsetNames = ( String[] ) table.get( "subsets" );
        table.remove( "subsets" );

        for ( String key : table.keySet() ) {
            // create two DoubleArrayLists for the expression levels of two groups
            DoubleArrayList group1ExpLevels = new DoubleArrayList();
            DoubleArrayList group2ExpLevels = new DoubleArrayList();

            // read the expression levels and seperate them into two groups
            double[] expLevels = ( double[] ) table.get( key );

            for ( int k = 0; k < subsetNames.length; k++ ) {
                if ( subsetNames[k].equals( subsetName1 ) )
                    group1ExpLevels.add( expLevels[k] );
                else if ( subsetNames[k].equals( subsetName2 ) ) group2ExpLevels.add( expLevels[k] );
            }

            double pVal = 0;
            // perform t-test between two groups of expression levels
            if ( !varianceZero( group1ExpLevels, group2ExpLevels ) ) {
                pVal = tTest( group1ExpLevels, group2ExpLevels );
                pValuesList.put( key, Double.valueOf( pVal ) );
            }
        }
        return pValuesList;
    }

    /**
     * This method performs t-test and returns the p value from the result
     * 
     * @throws REXPMismatchException
     */
    protected double tTest( DoubleArrayList list1, DoubleArrayList list2 ) {
        double[] list1values = list1.elements();
        double[] list2values = list2.elements();
        double[] pval = listTwoDoubleArrayEval( "t.test(x,y)$p.value", "x", list1values, "y", list2values );
        return pval[0];
    }

    /**
     * This method is the actual method that assigns parameters of the function to call R to perform t-test.
     * 
     * @return a list of values as the result of the t-test.
     * @throws REXPMismatchException
     */
    protected double[] listTwoDoubleArrayEval( String command, String argName, double[] arg, String argName2,
            double[] arg2 ) {
        rc.assign( argName, arg );
        rc.assign( argName2, arg2 );
        return rc.doubleArrayEval( command );
    }

    /**
     * This method checks if the variance is zero
     */
    protected boolean varianceZero( DoubleArrayList list1, DoubleArrayList list2 ) {
        list1.sort();
        list2.sort();
        return ( list1.get( 0 ) == list1.get( list1.size() - 1 ) && list2.get( 0 ) == list2.get( list2.size() - 1 ) );
    }

    /**
     * This method is used to write significant genes for differential expression with their p values to an output file
     */
    protected void writeSignificantGenesToFile( String fileName, Hashtable<String, Double> sigGenes ) {
        manager.writeSignificantGenesToFile( fileName, sigGenes );
    }

    /**
     * This method finds the significant genes among different experiments
     * 
     * @param List<String> list of the names of the experiments
     * @return a hashtable whose entries contain significant genes in the form: probeId: list of experiment names in
     *         which this gene is significant
     */
    protected Hashtable<String, List<String>> getSignificantGenesAcrossExperiments( List<String> list ) {
        return manager.getSignificantGenesAcrossExperiments( list );
    }

    /**
     * This method writes the hashtable of significant genes across experiments to an output file
     */
    protected void writeSignificantGenesAcrossExperimentsToFile( String fileName, Hashtable<String, List<String>> table ) {
        manager.writeSignificantGenesAcrossExperimentsToFile( fileName, table );
    }

    public static void main( String[] args ) {
        // write two experiments' significant genes to files
        SimpleTTestAnalyzer analyzer;
        try {
            analyzer = new SimpleTTestAnalyzer();
        } catch ( IOException e ) {
            log.fatal( e );
            return;
        }
        Hashtable<String, Double> sigGenes = analyzer.getSignificantGenes( "GDS1328", "saline", "OVA" );
        analyzer.writeSignificantGenesToFile( "GDS1328_siggenes.txt", sigGenes );

        Hashtable<String, Double> sigGenes2 = analyzer.getSignificantGenes( "GDS1110", "pre-neoplastic cell",
                "tumor cell" );
        analyzer.writeSignificantGenesToFile( "GDS1110_siggenes.txt", sigGenes2 );

        // write significant genes across these two experiments to file.
        List<String> list = new Vector<String>();
        list.add( "GDS1328" );
        list.add( "GDS1110" );
        Hashtable<String, List<String>> table = analyzer.getSignificantGenesAcrossExperiments( list );
        analyzer.writeSignificantGenesAcrossExperimentsToFile( "siggenes_across_experiments.txt", table );

        System.exit( 0 );
    }
}
