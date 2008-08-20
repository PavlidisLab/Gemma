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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.loader.expression.geo.GeoConverter;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.GeoDatasetService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * This class contains the methods needed for differential expression analyzer classes for extracting the expression
 * data.
 * 
 * @author gozde
 * @version $Id$
 * @deprecated
 */
@Deprecated
public class ExpressionDataManager {

    protected static final Log log = LogFactory.getLog( ExpressionDataManager.class );

    final String actualExperimentsPath = "C:/TestData";
    final String analysisResultsPath = "C:/Results/";

    ExpressionExperiment experiment = null;
    String experimentName = null;

    /**
     * @param experimentName the name of the experiment whose data vectors will be loaded
     */
    public ExpressionDataManager( String experimentName ) {
        GeoDatasetService gds = new GeoDatasetService();
        GeoConverter geoConverter = new GeoConverter();
        gds.setGeoConverter( geoConverter );

        // if you want to read the experiments locally
        gds.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( actualExperimentsPath ) );

        // if you want to read the experiments from the geo repository
        // gds.setGenerator(new GeoDomainObjectGenerator());

        experiment = ( ExpressionExperiment ) gds.fetchAndLoad( experimentName, false, true, false, false );
        this.experimentName = experimentName;
    }

    public ExpressionDataManager() {
    }

    /**
     * @return a Collection of BioAssays of the experiment in the order they appear for DesignElementDataVectors
     */
    protected Collection<BioAssay> getDataVectorBioAssays() {
        Collection<DesignElementDataVector> dataVectors = getDesignElementDataVectors();
        return dataVectors.iterator().next().getBioAssayDimension().getBioAssays();
    }

    /**
     * @return a Collection of DesignElementDataVectors of the experiment, which contain expression levels
     */
    protected Collection<DesignElementDataVector> getDesignElementDataVectors() {
        Collection<DesignElementDataVector> expressionDataVectors = new Vector<DesignElementDataVector>();
        Collection<DesignElementDataVector> designElementDataVectors = experiment.getDesignElementDataVectors();

        for ( DesignElementDataVector dataVector : designElementDataVectors ) {
            if ( dataVector.getQuantitationType().getName().trim().equals( "VALUE" )
                    && dataVector.getQuantitationType().getRepresentation().toString().trim().equals( "DOUBLE" ) )
                expressionDataVectors.add( dataVector );
        }

        return expressionDataVectors;
    }

    /**
     * @return an array of the expression levels of a DesignElementDataVector
     */
    protected double[] getExpressionLevels( DesignElementDataVector dataVector ) {
        byte[] byteExpLevels = dataVector.getData();
        ByteArrayConverter bac = new ByteArrayConverter();
        double[] expressionLevels = bac.byteArrayToDoubles( byteExpLevels );
        return expressionLevels;
    }

    /**
     * This method returns a hashtable that contains the significant genes, whose p values are below the threshold.
     * 
     * @param table hashtable of probe ids and correspondent p values
     * @param threshold p value cut-off value for significance
     * @return a hashtable of significant genes' probe ids' along with their p values
     */
    protected Hashtable<String, Double> getSignificantGenes( Hashtable<String, Double> table, double threshold ) {
        Hashtable<String, Double> significantGenes = new Hashtable<String, Double>();
        Enumeration probeIds = table.keys();
        for ( int k = 0; k < table.size(); k++ ) {
            String probeId = ( String ) probeIds.nextElement();
            double pVal = table.get( probeId ).doubleValue();
            if ( pVal <= threshold && pVal > 0 ) significantGenes.put( probeId, Double.valueOf( pVal ) );
        }
        return significantGenes;
    }

    /**
     * This method returns a hashtable containing the subsets names corresponding to each bioassay in one entry and the
     * expression levels of the genes in each other entry, all that have been read from the file of experiment subsets ->
     * a String array of subset names of bioassays probeId -> a double array of expression levels
     */
    protected Hashtable<String, Object> readDataVectorsFromFile( String expName ) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader( new FileReader( analysisResultsPath + expName ) );
        } catch ( FileNotFoundException e ) {
            log.error( "File that contains data vectors cannot be opened." );
        }

        Hashtable<String, Object> table = new Hashtable<String, Object>();

        try {
            // read the subset names into first entry
            String firstLine = reader.readLine();
            String[] subsetNames = firstLine.split( "\t" );
            table.put( "subsets", subsetNames );

            // read the gene expression levels (geneName -> expression levels)
            String line;
            while ( ( line = reader.readLine() ) != null ) {
                String[] expLevelsStr = line.split( "\t" );
                String geneName = expLevelsStr[0];
                double[] expLevels = new double[expLevelsStr.length - 1];

                for ( int g = 0; g < expLevels.length; g++ )
                    expLevels[g] = Double.parseDouble( expLevelsStr[g + 1] );
                table.put( geneName, expLevels );
            }
        } catch ( IOException e ) {
            log.error( "Line cannot be read from the data vectors file." );
        }
        return table;
    }

    /**
     * This method is used to write significant genes of an experiment to an output file with their p values This method
     * is especially needed for t tests and ANOVA.
     */
    protected void writeSignificantGenesToFile( String fileName, Hashtable<String, Double> probesTable ) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter( new FileWriter( analysisResultsPath + fileName ) );
        } catch ( IOException e ) {
            log.error( "File to write significant genes cannot be opened." );
        }

        try {
            Enumeration keys = probesTable.keys();
            for ( int j = 0; j < probesTable.size(); j++ ) {
                String key = ( String ) keys.nextElement();
                writer.write( key + "\t" + probesTable.get( key ) + "\n" );
            }

            writer.close();
        } catch ( IOException e ) {
            log.error( "Probe Id cannot be written to file." );
        }
    }

    /**
     * This method is used to write significant genes of an experiment to an output file This method is especially
     * needed for SAM analysis(as it does not have p values)
     */
    protected void writeSignificantGenesToFileWithoutPValues( String fileName, List<Object> probeNames ) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter( new FileWriter( analysisResultsPath + fileName ) );
        } catch ( IOException e ) {
            log.error( "File to write significant genes cannot be opened." );
        }

        try {
            for ( int j = 0; j < probeNames.size(); j++ )
                writer.write( probeNames.get( j ) + "\n" );
            writer.close();
        } catch ( IOException e ) {
            log.error( "Probe Id cannot be written to file." );
        }
    }

    /**
     * This method reads the significant genes and their p values from the file with the specified name This method is
     * especially needed for t tests and ANOVA.
     * 
     * @return Hashtable<String, Double> a table whose each entry is (probeId -> p value)
     */
    protected Hashtable<String, Double> readSignificantGenesFromFile( String fileName ) {
        Hashtable<String, Double> table = new Hashtable<String, Double>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader( new FileReader( analysisResultsPath + fileName ) );
        } catch ( FileNotFoundException e ) {
            log.error( "File to read significant genes from cannot be opened." );
        }

        String line;
        try {
            while ( ( line = reader.readLine() ) != null ) {
                String[] array = line.split( "\t" );
                String probeId = array[0];
                Double pVal = Double.valueOf( Double.parseDouble( array[1] ) );
                table.put( probeId, pVal );
            }
        } catch ( IOException e ) {
            log.error( "Significant probe id - p value line cannot be read from file." );
        }
        return table;
    }

    /**
     * This method reads the significant genes from the file with the specified name This method is especially needed
     * for SAM analysis(as it does not have p values)
     * 
     * @return List<String> list of the names of the significant genes
     */
    protected List<String> readSignificantGenesFromFileWithoutPValues( String fileName ) {
        List<String> list = new Vector<String>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader( new FileReader( analysisResultsPath + fileName ) );
        } catch ( FileNotFoundException e ) {
            log.error( "File to read significant genes cannot be opened." );
        }

        String line;
        try {
            while ( ( line = reader.readLine() ) != null ) {
                list.add( line );
            }
        } catch ( IOException e ) {
            log.error( "Significant probe id - p value line cannot be read from file." );
        }
        return list;
    }

    /**
     * This method returns the significant genes for many experiments along with the names of experiments the genes are
     * significant at.
     * 
     * @param fileNames names of the files each of which contain significant genes for an experiment along with p values
     * @return a Hashtable whose entries are of the form ProbeId -> Names of Experiments this gene is significant in.
     */
    protected Hashtable<String, List<String>> getSignificantGenesAcrossExperiments( List<String> fileNames ) {
        Hashtable<String, List<String>> acrossExperimentsTable = new Hashtable<String, List<String>>();

        // create a significant genes for each experiment table
        // in the form [experiment name -> (significant gene->p value)]
        Hashtable<String, Hashtable<String, Double>> significantGenesTable = new Hashtable<String, Hashtable<String, Double>>();
        for ( int j = 0; j < fileNames.size(); j++ ) {
            String fileName = fileNames.get( j );
            Hashtable<String, Double> table = readSignificantGenesFromFile( actualExperimentsPath + fileName );
            significantGenesTable.put( fileName, table );
        }

        // collect keys for the experiments
        Vector<String> experimentsForSignificantGenes = new Vector<String>();
        Enumeration keysEnum = significantGenesTable.keys();
        for ( int z = 0; z < significantGenesTable.size(); z++ )
            experimentsForSignificantGenes.add( keysEnum.nextElement().toString() );

        // fill the table for significant genes across experiments
        for ( int y = 0; y < significantGenesTable.size(); y++ ) {
            String expName = experimentsForSignificantGenes.get( y );
            Hashtable genesToPValuesTable = significantGenesTable.get( expName );
            Enumeration keys = genesToPValuesTable.keys();

            for ( int t = 0; t < genesToPValuesTable.size(); t++ ) {
                String geneName = keys.nextElement().toString();
                List<String> experiments = new Vector<String>();

                for ( int k = 0; k < significantGenesTable.size(); k++ ) {
                    String otherExpName = experimentsForSignificantGenes.get( k );
                    if ( !otherExpName.equals( expName ) ) {
                        Hashtable otherGenesToPValuesTable = significantGenesTable.get( otherExpName );
                        if ( otherGenesToPValuesTable.containsKey( geneName ) ) experiments.add( otherExpName );
                    }
                }

                // add this gene name, experiments pairs to the hashtable
                if ( experiments.size() != 0 ) acrossExperimentsTable.put( geneName, experiments );
            }
        }
        return acrossExperimentsTable;
    }

    /**
     * This method returns the significant genes for many experiments along with the names of experiments the genes are
     * significant at.
     * 
     * @param fileNames names of the files each of which contain significant genes names for an experiment
     * @return a Hashtable whose entries are of the form ProbeId -> Names of Experiments this gene is significant in.
     */
    protected Hashtable<String, List<String>> getSignificantGenesAcrossExperimentsWithoutPValues( List<String> fileNames ) {
        Hashtable<String, List<String>> acrossExperimentsTable = new Hashtable<String, List<String>>();

        // create a significant genes for each experiment table
        // in the form [experiment name -> list of significant genes]
        Hashtable<String, List<String>> significantGenesTable = new Hashtable<String, List<String>>();
        for ( int j = 0; j < fileNames.size(); j++ ) {
            String fileName = fileNames.get( j );
            List<String> list = readSignificantGenesFromFileWithoutPValues( fileName );
            significantGenesTable.put( fileName, list );
        }

        // collect keys for the experiments
        Vector<String> experimentsForSignificantGenes = new Vector<String>();
        Enumeration keysEnum = significantGenesTable.keys();
        for ( int z = 0; z < significantGenesTable.size(); z++ )
            experimentsForSignificantGenes.add( keysEnum.nextElement().toString() );

        // fill the table for significant genes across experiments
        for ( int y = 0; y < significantGenesTable.size(); y++ ) {
            String expName = experimentsForSignificantGenes.get( y );
            List<String> list = significantGenesTable.get( expName );

            for ( int t = 0; t < list.size(); t++ ) {
                String geneName = list.get( t );
                List<String> experiments = new Vector<String>();

                for ( int k = 0; k < significantGenesTable.size(); k++ ) {
                    String otherExpName = experimentsForSignificantGenes.get( k );
                    if ( !otherExpName.equals( expName ) ) {
                        List<String> otherList = significantGenesTable.get( otherExpName );
                        if ( otherList.contains( geneName ) ) experiments.add( otherExpName );
                    }
                }
                // add this gene name, experiments pairs to the hashtable
                if ( experiments.size() != 0 ) acrossExperimentsTable.put( geneName, experiments );
            }
        }
        return acrossExperimentsTable;
    }

    /**
     * This method writes the significant genes across experiments to a file.
     * 
     * @param fileName name of the output file
     * @param table hashtable that contains the significant genes for each experiment in the form experimentName -> List
     *        of significant genes
     */
    protected void writeSignificantGenesAcrossExperimentsToFile( String fileName, Map<String, List<String>> table ) {
        log.info( "Writing significant genes across experiments to file." );
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter( new FileWriter( analysisResultsPath + fileName ) );
        } catch ( IOException e ) {
            log.error( "File to write significant genes across experiments cannot be opened." );
        }

        for ( String key : table.keySet() ) {
            try {
                writer.write( key + " : " );
                List list = table.get( key );
                for ( int j = 0; j < list.size(); j++ )
                    writer.write( list.get( j ) + "\t" );
                writer.write( "\n" );

            } catch ( IOException e ) {
                log.error( "Cannot write to file : " + fileName );
            }
        }
        try {
            writer.close();
        } catch ( IOException e ) {
            log.error( "Cannot close file : " + fileName );
        }
    }
}
