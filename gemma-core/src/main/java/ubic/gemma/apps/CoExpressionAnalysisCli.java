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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix2DNamed;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.dataStructure.matrix.StringMatrix2DNamed;
import ubic.basecode.gui.ColorMap;
import ubic.basecode.gui.ColorMatrix;
import ubic.basecode.gui.JMatrixDisplay;
import ubic.basecode.io.reader.StringMatrixReader;
import ubic.gemma.analysis.coexpression.GeneCoExpressionAnalysis;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * "Global" coexpression analysis: for a set of coexpressed genes, get all the data for all the genes it is coexpressed
 * with and construct a summary matrix.
 * 
 * @author xwan
 * @version $Id$
 */
public class CoExpressionAnalysisCli extends AbstractSpringAwareCLI {

    DesignElementDataVectorService devService;
    ExpressionExperimentService eeService;
    GeneService geneService;
    private String geneList = null;
    private String taxonName = null;
    private String outputFile = null;
    private static String DIVIDOR = "-----";

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        Option geneFileOption = OptionBuilder.hasArg().isRequired().withArgName( "geneFile" ).withDescription(
                "Short names of the genes to analyze" ).withLongOpt( "geneFile" ).create( 'g' );
        addOption( geneFileOption );
        Option taxonOption = OptionBuilder.hasArg().isRequired().withArgName( "Taxon" ).withDescription(
                "the taxon of the genes to analyze" ).withLongOpt( "Taxon" ).create( 't' );
        addOption( taxonOption );
        Option outputFileOption = OptionBuilder.hasArg().isRequired().withArgName( "outFile" ).withDescription(
                "File for saving the corelation data" ).withLongOpt( "outFile" ).create( 'o' );
        addOption( outputFileOption );

    }

    /**
     * 
     */
    protected void processOptions() {
        super.processOptions();
        if ( hasOption( 'g' ) ) {
            this.geneList = getOptionValue( 'g' );
        }
        if ( hasOption( 't' ) ) {
            this.taxonName = getOptionValue( 't' );
        }
        if ( hasOption( 'o' ) ) {
            this.outputFile = getOptionValue( 'o' );
        }

        devService = ( DesignElementDataVectorService ) this.getBean( "designElementDataVectorService" );
        eeService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );
        geneService = ( GeneService ) this.getBean( "geneService" );
    }

    private Collection<Gene> getTestGenes( GeneService geneService, Taxon taxon ) {
        String geneNames[] = { "RPL8", "BC071678", "c1orf151", "RPS18", "PCOLCE2", "RPS14", "BC072682", "Ak130913" };

        // String geneNames[] = {"RPS18", "BC071678"};

        return this.getGenes( geneService, geneNames, taxon );
    }

    /**
     * @param geneService
     * @param geneNames
     * @param taxon
     * @return
     */
    private Collection<Gene> getGenes( GeneService geneService, Object[] geneNames, Taxon taxon ) {
        HashSet<Gene> genes = new HashSet<Gene>();
        for ( int i = 0; i < geneNames.length; i++ ) {
            Gene gene = getGene( geneService, ( String ) geneNames[i], taxon );
            if ( gene != null ) genes.add( gene );
        }
        return genes;
    }

    /**
     * @param geneService
     * @param geneName
     * @param taxon
     * @return
     */
    private Gene getGene( GeneService geneService, String geneName, Taxon taxon ) {
        Gene gene = Gene.Factory.newInstance();
        gene.setOfficialSymbol( geneName.trim() );
        gene.setTaxon( taxon );
        gene = geneService.find( gene );
        if ( gene == null ) {
            log.info( "Can't Load gene " + geneName );
        }
        return gene;
    }

    /**
     * @param name
     * @return
     */
    private Taxon getTaxon( String name ) {
        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( name );
        TaxonService taxonService = ( TaxonService ) this.getBean( "taxonService" );
        taxon = taxonService.find( taxon );
        if ( taxon == null ) {
            log.info( "NO Taxon found!" );
        }
        return taxon;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "CoExpression Analysis", args );
        if ( err != null ) {
            return err;
        }

        Taxon taxon = getTaxon();
        Collection<ExpressionExperiment> allEE = eeService.findByTaxon( taxon );

        // Collection <Gene> testGenes = getTestGenes(geneService,taxon);
        HashSet<String> geneNames = new HashSet<String>();
        HashSet<String> targetGeneNames = new HashSet<String>();
        boolean targetGene = true;
        /*
         * I'm not too clear on the file format here, but I think you have 'genes' and 'target genes' in a list,
         * separated by "-----". I'm not sure what the difference between 'gene' and 'target gene' is.
         */
        try {
            InputStream is = new FileInputStream( this.geneList );
            BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
            String shortName = null;
            while ( ( shortName = br.readLine() ) != null ) {
                if ( StringUtils.isBlank( shortName ) ) continue;
                if ( shortName.trim().contains( DIVIDOR ) ) {
                    targetGene = false;
                    continue;
                }
                if ( targetGene ) {
                    targetGeneNames.add( shortName.trim() );
                } else {
                    geneNames.add( shortName.trim() );
                }
            }
        } catch ( Exception e ) {
            return e;
        }

        Collection<Gene> genes = this.getGenes( geneService, geneNames.toArray(), taxon );
        Collection<Gene> targetGenes = this.getGenes( geneService, targetGeneNames.toArray(), taxon );
        HashSet<Gene> queryGenes = new HashSet<Gene>();
        queryGenes.addAll( genes );
        queryGenes.addAll( targetGenes );
        log.info( "Start the Query for " + queryGenes.size() + " genes" );

        Map<DesignElementDataVector, Collection<Gene>> geneMap = getExpressionData( allEE, queryGenes );

        GeneCoExpressionAnalysis coExperssion = new GeneCoExpressionAnalysis( targetGenes, genes, allEE );

        coExperssion.setDevToGenes( geneMap );
        coExperssion.setExpressionExperimentService( eeService );
        log.info( geneMap.size() );
        coExperssion.analysis( ( Set ) geneMap.keySet() );

        try {
            makeClusterGrams( coExperssion );
        } catch ( Exception e ) {
            return e;
        }

        return null;
    }

    /**
     * Retrieve all the expression data for a bunch of genes in a bunch of expression experiments.
     * 
     * @param allEE
     * @param queryGenes
     * @return
     */
    @SuppressWarnings("unchecked")
    private Map<DesignElementDataVector, Collection<Gene>> getExpressionData( Collection<ExpressionExperiment> allEE,
            HashSet<Gene> queryGenes ) {
        StopWatch qWatch = new StopWatch();
        qWatch.start();
        int count = 0;
        int CHUNK_LIMIT = 30;
        int total = queryGenes.size();
        Collection<Gene> genesInOneChunk = new HashSet<Gene>();
        Map<DesignElementDataVector, Collection<Gene>> geneMap = new HashMap<DesignElementDataVector, Collection<Gene>>();
        for ( Gene gene : queryGenes ) {
            genesInOneChunk.add( gene );
            count++;
            total--;
            if ( count == CHUNK_LIMIT || total == 0 ) {
                geneMap.putAll( devService.getVectors( allEE, genesInOneChunk ) );
                count = 0;
                genesInOneChunk.clear();
                log.info( "Analyzed " + count + " more genes..." );
            }
        }
        qWatch.stop();
        log.info( "Data retrieval took " + qWatch.getTime() );
        return geneMap;
    }

    /**
     * @return
     */
    private Taxon getTaxon() {
        Taxon taxon = getTaxon( this.taxonName );
        if ( taxon == null ) {
            log.error( "No taxon is found " + this.taxonName );
            bail( ErrorCode.INVALID_OPTION );
        }
        return taxon;
    }

    /**
     * @param coExpression
     * @throws FileNotFoundException
     * @throws IOException
     * @throws InterruptedException
     */
    private void makeClusterGrams( GeneCoExpressionAnalysis coExpression ) throws FileNotFoundException, IOException,
            InterruptedException {
        // Generate the data file for Cluster3
        PrintStream output = new PrintStream( new FileOutputStream( new File( this.outputFile ) ) );
        double presencePercent = 0.5;
        coExpression.output( output, presencePercent );
        output.close();

        // Running Cluster3 to geneate .cdt file
        Runtime rt = Runtime.getRuntime();
        Process clearOldFiles = rt.exec( "rm *.cdt -f" );
        clearOldFiles.waitFor();

        String clusterCmd = "cluster";
        String commonOptions = "-g 7 -e 7 -m c";
        Process cluster = rt.exec( clusterCmd + " -f " + this.outputFile + " " + commonOptions );
        cluster.waitFor();

        DoubleMatrixNamed dataMatrix = getClusteredMatrix();

        // Get the rank Matrix
        DoubleMatrixNamed rankMatrix = coExpression.getRankMatrix( dataMatrix );

        // generate the png figures
        ColorMatrix dataColorMatrix = new ColorMatrix( dataMatrix );
        dataColorMatrix.setColorMap( ColorMap.GREENRED_COLORMAP );
        ColorMatrix rankColorMatrix = new ColorMatrix( rankMatrix );
        rankColorMatrix.setColorMap( ColorMap.GREENRED_COLORMAP );

        JMatrixDisplay dataMatrixDisplay = new JMatrixDisplay( dataColorMatrix );
        JMatrixDisplay rankMatrixDisplay = new JMatrixDisplay( rankColorMatrix );

        dataMatrixDisplay.saveImage( "dataMatrix.png", true );
        rankMatrixDisplay.saveImage( "rankMatrix.png", true );
    }

    /**
     * @return
     * @throws IOException
     */
    private DoubleMatrixNamed getClusteredMatrix() throws IOException {
        // Read the generated file into a String Matrix
        StringMatrixReader mReader = new StringMatrixReader();
        int dotIndex = this.outputFile.lastIndexOf( '.' );
        String CDTMatrixFile = this.outputFile.substring( 0, dotIndex );
        StringMatrix2DNamed cdtMatrix = ( StringMatrix2DNamed ) mReader.read( CDTMatrixFile + ".cdt" );

        // Read String Matrix and convert into DenseDoubleMatrix
        int extra_rows = 2, extra_cols = 3;
        double[][] data = new double[cdtMatrix.rows() - extra_rows][];
        List<String> rowLabels = new ArrayList<String>();
        List<String> colLabels = new ArrayList<String>();

        List colNames = cdtMatrix.getColNames();
        for ( int i = extra_cols; i < colNames.size(); i++ )
            colLabels.add( ( String ) colNames.get( i ) );

        int rowIndex = 0;
        for ( int i = extra_rows; i < cdtMatrix.rows(); i++ ) {
            Object row[] = cdtMatrix.getRow( i );
            rowLabels.add( ( String ) row[0] );
            data[rowIndex] = new double[row.length - extra_cols];
            for ( int j = extra_cols; j < row.length; j++ )
                try {
                    data[rowIndex][j - extra_cols] = Double.valueOf( ( String ) row[j] );
                } catch ( Exception e ) {
                    data[rowIndex][j - extra_cols] = Double.NaN;
                    continue;
                }
            rowIndex++;
        }
        DoubleMatrixNamed dataMatrix = new DenseDoubleMatrix2DNamed( data );
        dataMatrix.setRowNames( rowLabels );
        dataMatrix.setColumnNames( colLabels );
        return dataMatrix;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        CoExpressionAnalysisCli analysis = new CoExpressionAnalysisCli();
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Exception ex = analysis.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
            watch.stop();
            log.info( watch.getTime() );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

}
