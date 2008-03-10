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
package ubic.gemma.analysis.expression.diff;

import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;

import org.rosuda.REngine.REXPMismatchException;

import ubic.basecode.math.MultipleTestCorrection;
import ubic.gemma.analysis.util.RCommander;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import cern.colt.list.DoubleArrayList;

/**
 * This class performs one-way ANOVA analysis in an experiment to detect the significant genes along with their
 * F-statistic derived p values.
 * 
 * @author gozde
 * @version $Id$
 */
public class SimpleOneWayAnovaAnalyzer extends RCommander {

    public final double fdr = 0.01;

    ExpressionDataManager manager;

    public SimpleOneWayAnovaAnalyzer() throws IOException {
        super();
    }

    /**
     * This method detects significant genes of an experiment at ANOVA analysis and returns a list of them.
     * 
     * @param fileName name of the file from which the expression levels will be read
     * @return Hashtable<String, Double> table of the probe ids of the genes with p values
     */
    protected Hashtable<String, Double> getSignificantGenes( String fileName ) {
        log.info( "Calculating  p values of the genes. (It takes long time.)" );
        Hashtable<String, Double> pValuesTable = getPValuesOfGenes( fileName );
        log.info( pValuesTable.size() + " p values have been calculated." );
        DoubleArrayList list = new DoubleArrayList();

        Enumeration elements = pValuesTable.elements();
        for ( int j = 0; j < pValuesTable.size(); j++ )
            list.add( ( Double ) elements.nextElement() );

        double threshold = MultipleTestCorrection.BenjaminiHochbergCut( list, fdr );
        log.info( "p value cut-off : " + threshold );

        Hashtable<String, Double> significantGenes = manager.getSignificantGenes( pValuesTable, threshold );
        log.info( significantGenes.size() + " significant genes have been detected." );
        return significantGenes;
    }

    /**
     * This method detects p values of genes of an experiment at ANOVA analysis and returns a list of them.
     * 
     * @param fileName name of the file from which the expression levels will be read
     * @return Hashtable<String, Double> table of the probe ids of the genes with p values
     */
    protected Hashtable<String, Double> getPValuesOfGenes( String fileName ) {
        log.info( "Calculating p values of the genes." );
        Hashtable<String, Double> genesToPValuesTable = new Hashtable<String, Double>();
        manager = new ExpressionDataManager( fileName );
        Collection<DesignElementDataVector> dataVectors = manager.getDesignElementDataVectors();
        String[] subsetNamesForBioAssays = manager.getSubsetNamesForBioAssays();

        for ( DesignElementDataVector dataVector : dataVectors ) {
            double[] expressionLevels = manager.getExpressionLevels( dataVector );
            double pVal = anovaAnalysis( subsetNamesForBioAssays, expressionLevels );
            genesToPValuesTable.put( dataVector.getDesignElement().getName(), Double.valueOf( pVal ) );
        }

        return genesToPValuesTable;
    }

    /**
     * This method calculates the p value of a gene using F-statistic by calling R
     * 
     * @param subsetNamesColumn array of the names of subsets corresponding to bioassays
     * @param expLevelsColumn array of the double values for expression levels
     * @return the p value for the gene
     * @throws REXPMismatchException
     */
    protected double anovaAnalysis( String[] subsetNamesColumn, double[] expLevelsColumn ) {
        rc.assign( "subsets", subsetNamesColumn );
        rc.assign( "expLevels", expLevelsColumn );

        rc.voidEval( "matrix <- data.frame(subsets, expLevels)" );
        rc.voidEval( "aov.result <- aov(expLevels ~ subsets, data = matrix)" );
        double[] pValList = rc.doubleArrayEval( "anova(aov.result)[[4]]" );

        // REXP tableExp = content.getBody();
        // Vector tableArray = ( Vector ) tableExp.getContent();
        // REXP pValExp = ( REXP ) tableArray.get( 4 ); // p value list
        // double[] pValList = ( double[] ) pValExp.getContent();
        double pVal = pValList[0];

        return pVal;
    }

    /**
     * This method is used to write significant genes for ANOVA with their p values to an output file
     */
    protected void writeSignificantGenesToFile( String fileName, Hashtable<String, Double> sigGenes ) {
        manager.writeSignificantGenesToFile( fileName, sigGenes );
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        SimpleOneWayAnovaAnalyzer analyzer;
        try {
            analyzer = new SimpleOneWayAnovaAnalyzer();
        } catch ( IOException e ) {
            log.fatal( e );
            return;
        }
        Hashtable<String, Double> sigGenes = analyzer.getSignificantGenes( "GDS1110" );
        analyzer.writeSignificantGenesToFile( "GDS1110", sigGenes );

        System.exit( 0 );
    }

}
