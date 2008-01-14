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

import java.util.Enumeration;
import java.util.Hashtable;

import ubic.basecode.math.MultipleTestCorrection;
import ubic.basecode.math.metaanalysis.MetaAnalysis;
import cern.colt.list.DoubleArrayList;

/**
 * This class finds significant genes across experiments by combining the p-values of genes, all of which are detected
 * by one-way ANOVA.
 * 
 * @author gozde
 * @version $Id$
 */
public class SimpleCombinedPValuesAnalyzer extends SimpleOneWayAnovaAnalyzer {

    ExpressionDataManager[] managers = null;

    public SimpleCombinedPValuesAnalyzer( int numberOfExperiments ) {
        super();

        managers = new ExpressionDataManager[numberOfExperiments];
        for ( int j = 0; j < numberOfExperiments; j++ )
            managers[j] = new ExpressionDataManager();
    }

    /**
     * This method returns the significant genes across experiments
     * 
     * @param experimentNames a String array of names of the experiments whose genes will be analyzed
     * @return a hashtable which contains significant probe ids with their combined p values
     */
    protected Hashtable<String, Double> getSignificantGenes( String[] experimentNames ) {
        Hashtable<String, Double> pValuesTable = findPValues( experimentNames );
        log.info( pValuesTable.size() + " p values have been calculated." );
        DoubleArrayList list = new DoubleArrayList();
        Enumeration pVals = pValuesTable.elements();
        while ( pVals.hasMoreElements() )
            list.add( ( Double ) pVals.nextElement() );

        double threshold = MultipleTestCorrection.BenjaminiHochbergCut( list, fdr );
        log.info( "P value cut-off : " + threshold );
        Hashtable<String, Double> significantGenes = managers[0].getSignificantGenes( pValuesTable, threshold );
        log.info( significantGenes.size() + " significant genes have been detected." );
        return significantGenes;
    }

    /**
     * This method writes the expression levels of genes in each experiment given as parameter
     */
    protected void writeExpressionLevelsToFiles( String[] experimentNames ) {
        managers = new ExpressionDataManager[experimentNames.length];
        for ( int j = 0; j < managers.length; j++ ) {
            managers[j] = new ExpressionDataManager( experimentNames[j] );
            managers[j].writeDataVectorsToFile();
        }
        log.info( experimentNames.length + " experiments' expression levels have been written to files." );
    }

    /**
     * This method finds and returns p values of genes across experiments
     * 
     * @param fileNames an array of file names that contain expression levels of genes
     * @return a hashtable that contains probeId -> p value entries
     */
    protected Hashtable<String, Double> findPValues( String[] experimentNames ) {
        log.info( "Calculating p values of the genes.(It takes long time.)" );
        Hashtable<String, Double> pValuesTable = new Hashtable<String, Double>();

        int experimentsSize = experimentNames.length;
        Hashtable[] tables = new Hashtable[experimentsSize];
        Enumeration[] keys = new Enumeration[experimentsSize];

        String[][] subsetNames = new String[experimentsSize][];
        for ( int y = 0; y < experimentNames.length; y++ ) {
            tables[y] = managers[y].readDataVectorsFromFile( experimentNames[y] );
            keys[y] = tables[y].keys();
            subsetNames[y] = ( String[] ) tables[y].get( "subsets" );
            tables[y].remove( "subsets" );
        }

        int numberOfGenes = tables[0].size();
        for ( int k = 0; k < numberOfGenes; k++ ) {
            String probeId = null;
            DoubleArrayList pValuesOfGene = new DoubleArrayList();

            for ( int y = 0; y < experimentsSize; y++ ) {
                probeId = ( String ) keys[y].nextElement();
                double[] expLevels = ( double[] ) tables[y].get( probeId );
                double pVal = anovaAnalysis( subsetNames[y], expLevels );
                pValuesOfGene.add( pVal );
            }

            double combinedPValue = MetaAnalysis.fisherCombinePvalues( pValuesOfGene );
            pValuesTable.put( probeId, combinedPValue );
        }
        return pValuesTable;
    }

    /**
     * This method is used to write significant genes for all experiments with their combined p values to an output file
     */
    @Override
    protected void writeSignificantGenesToFile( String fileName, Hashtable<String, Double> sigGenes ) {
        managers[0].writeSignificantGenesToFile( fileName, sigGenes );
    }

    public static void main( String[] args ) {
        SimpleCombinedPValuesAnalyzer analyzer = new SimpleCombinedPValuesAnalyzer( 2 );

        // in order not to get OutOfMemoryError, I run these all three sections below seperately, one after another.

        // --- part 1 ---
        String[] experimentNames = new String[1];
        experimentNames[0] = "GDS1328";
        analyzer.writeExpressionLevelsToFiles( experimentNames );

        // --- part 2 ---
        String[] experimentNames2 = new String[1];
        experimentNames2[0] = "GDS1110";
        analyzer.writeExpressionLevelsToFiles( experimentNames2 );

        // --- part 3 ---
        String[] experimentNames3 = new String[2];
        experimentNames3[0] = "GDS1328";
        experimentNames3[1] = "GDS1110";
        Hashtable<String, Double> sigGenes = analyzer.getSignificantGenes( experimentNames3 );
        analyzer.writeSignificantGenesToFile( "siggenes_across_experiments", sigGenes );
        System.exit( 0 );
    }

}
