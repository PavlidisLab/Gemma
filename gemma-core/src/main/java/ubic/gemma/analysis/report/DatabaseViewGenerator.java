/*
 * The Gemma project
 * 
 * Copyright (c) 2009 University of British Columbia
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
package ubic.gemma.analysis.report;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection; 
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.analysis.expression.diff.ContrastResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.ProbeAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.ConfigUtils;

/**
 * Generates textual views of the database so other people can use the data.
 * <p>
 * Development of this was started due to the collaboration with NIF. See {@link http
 * ://www.chibi.ubc.ca/faculty/pavlidis/bugs/show_bug.cgi?id=1747}
 * <p>
 * It is essential that these views be created by a principal with Anonymous status, so as not to create views of
 * private data (that could be done, but would be separate).
 * 
 * @author paul
 * @version $Id$
 */
@Service
public class DatabaseViewGenerator {

    private static final double THRESH_HOLD = 0.01;

    private static Log log = LogFactory.getLog( DatabaseViewGenerator.class );

    public static final String VIEW_DIR = ConfigUtils.getString( "gemma.appdata.home" ) + File.separatorChar
            + "dataFiles" + File.separatorChar;

    public static final String VIEW_FILE_SUFFIX = ".view.txt.gz";

    private static final String DATASET_SUMMARY_VIEW_BASENAME = "DatasetSummary";
    private static final String DATASET_TISSUE_VIEW_BASENAME = "DatasetTissue";
    private static final String DATASET_DIFFEX_VIEW_BASENAME = "DatasetDiffEx";

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private CompositeSequenceService compositeSequenceService;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Autowired
    private DifferentialExpressionResultService differentialExpressionResultService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    /**
     * @param limit if null will run against every dataset in Gemma else will only do the 1st to the given limit
     */
    public void runAll( Integer limit ) {
        // TODO: put the loading and thawing of EE's here and pass the EE in as a parameter so that the
        // EE's are not thawed multiple times (will this matter?)
        try {
            generateDatasetView( limit );
            generateDatasetTissueView( limit );
            generateDifferentialExpressionView( limit );
        } catch ( FileNotFoundException e ) {
            throw new RuntimeException( e );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    public void runAll() {
        runAll( null );
    }

    /**
     * @throws IOException
     * @throws FileNotFoundException
     */
    public void generateDatasetView( int limit ) throws FileNotFoundException, IOException {

        log.info( "Generating dataset summary view" );

        /*
         * Get handle to output file
         */
        File file = getViewFile( DATASET_SUMMARY_VIEW_BASENAME );
        log.info( "Writing to " + file );
        Writer writer = new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( file ) ) );

        /*
         * Load all the data sets
         */
        Collection<ExpressionExperiment> vos = expressionExperimentService.loadAll();

        writer.write( "GemmaDsId\tSource\tSourceAccession\tShortName\tName\tDescription\ttaxon\tManufacturer\n" );

        /*
         * Print out their names etc.
         */
        int i = 0;
        for ( ExpressionExperiment vo : vos ) {
            vo = expressionExperimentService.thawLite( vo );
            log.info( "Processing: " + vo.getShortName() );

            String acc = "";
            String source = "";

            if ( vo.getAccession() != null && vo.getAccession().getAccession() != null ) {
                acc = vo.getAccession().getAccession();
                source = vo.getAccession().getExternalDatabase().getName();
            }

            Long gemmaId = vo.getId();
            String shortName = vo.getShortName();
            String name = vo.getName();
            String description = vo.getDescription();
            description = StringUtils.replaceChars( description, '\t', ' ' );
            description = StringUtils.replaceChars( description, '\n', ' ' );
            description = StringUtils.replaceChars( description, '\r', ' ' );

            Taxon taxon = expressionExperimentService.getTaxon( gemmaId );
            Collection<ArrayDesign> ads = expressionExperimentService.getArrayDesignsUsed( vo );
            StringBuffer manufacturers = new StringBuffer();

            // TODO could cache the arrayDesigns to make faster, thawing ad is time consuming
            for ( ArrayDesign ad : ads ) {
                ad = arrayDesignService.thawLite( ad );
                if ( ad.getDesignProvider() == null ) {
                    log.debug( "Array Design: " + ad.getShortName()
                            + " has no design provoider assoicated with it. Skipping" );
                    continue;
                }
                manufacturers.append( ad.getDesignProvider().getName() + "," );
            }

            writer.write( String.format( "%d\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n", gemmaId, source, acc, shortName, name,
                    description, taxon.getCommonName(), StringUtils.chomp( manufacturers.toString(), "," ) ) );

            if ( limit > 0 && ++i > limit ) break;

        }

        writer.close();
    }

    /**
     * @param limit
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void generateDatasetTissueView( int limit ) throws FileNotFoundException, IOException {
        log.info( "Generating dataset tissue view" );

        /*
         * Get handle to output file
         */
        File file = getViewFile( DATASET_TISSUE_VIEW_BASENAME );
        log.info( "Writing to " + file );
        Writer writer = new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( file ) ) );

        /*
         * Load all the data sets
         */Collection<ExpressionExperiment> vos = expressionExperimentService.loadAll();

        /*
         * For all of their annotations... if it's a tissue, print out a line
         */
        writer.write( "GemmaDsId\tTerm\tTermURI\n" );
        int i = 0;
        for ( ExpressionExperiment vo : vos ) {
            vo = expressionExperimentService.thawLite( vo );

            log.info( "Processing: " + vo.getShortName() );

            Long gemmaId = vo.getId();

            for ( Characteristic c : vo.getCharacteristics() ) {

                if ( StringUtils.isBlank( c.getValue() ) ) {
                    continue;
                }

                /*
                 * check if vocab characteristic.
                 */

                if ( c.getCategory().equals( "OrganismPart" ) ) { // or tissue? check URI

                    String uri = "";

                    if ( c instanceof VocabCharacteristic ) {
                        VocabCharacteristic vocabCharacteristic = ( VocabCharacteristic ) c;
                        if ( StringUtils.isNotBlank( vocabCharacteristic.getValueUri() ) )
                            uri = vocabCharacteristic.getValueUri();
                    }

                    writer.write( String.format( "%d\t%s\t%s\n", gemmaId, c.getValue(), uri ) );

                }

            }

            if ( limit > 0 && ++i > limit ) break;

        }

        writer.close();
    }

    /**
     * @param limit how many experiments to use
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void generateDifferentialExpressionView( int limit ) throws FileNotFoundException, IOException {
        log.info( "Generating dataset diffex view" );

        /*
         * Get handle to output file
         */
        File file = getViewFile( DATASET_DIFFEX_VIEW_BASENAME );
        log.info( "Writing to " + file );
        Writer writer = new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( file ) ) );

        /*
         * Load all the data sets
         */Collection<ExpressionExperiment> experiments = expressionExperimentService.loadAll();

        /*
         * For each gene that is differentially expressed, print out a line per contrast
         */
        writer
                .write( "GemmaDsId\tEEShortName\tGeneNCBIId\tGemmaGeneId\tFactor\tFactorURI\tBaseline\tContrasting\tDirection\n" );
        int i = 0;
        for ( ExpressionExperiment ee : experiments ) {
            ee = expressionExperimentService.thawLite( ee );

            Collection<ExpressionAnalysisResultSet> results = differentialExpressionAnalysisService.getResultSets( ee );
            if ( results == null || results.isEmpty() ) {
                log.warn( "No differential expression results found for " + ee );
                continue;
            }

            log.info( "Processing: " + ee.getShortName() );

            for ( ExpressionAnalysisResultSet ears : results ) {
                if ( ears == null ) {
                    log.warn( "No  expression analysis results found for " + ee );
                    continue;
                }
                differentialExpressionResultService.thaw( ears );

                FactorValue baselineGroup = ears.getBaselineGroup();

                if ( baselineGroup == null ) {
                    // log.warn( "No baseline defined for " + ee ); // interaction
                    continue;
                }

                if ( ExperimentalDesignUtils.isBatch( baselineGroup.getExperimentalFactor() ) ) {
                    continue;
                }

                String baselineDescription = ExperimentalDesignUtils.prettyString( baselineGroup );

                // Get the factor category name
                String factorName = "";
                String factorURI = "";

                for ( ExperimentalFactor ef : ears.getExperimentalFactors() ) {
                    factorName += ef.getName() + ",";
                    if ( ef.getCategory() instanceof VocabCharacteristic ) {
                        factorURI += ( ( VocabCharacteristic ) ef.getCategory() ).getCategoryUri() + ",";
                    }
                }
                factorName = StringUtils.chomp( factorName, "," );
                factorURI = StringUtils.chomp( factorURI, "," );

                if ( ears.getResults() == null || ears.getResults().isEmpty() ) {
                    log.warn( "No  differential expression analysis results found for " + ee );
                    continue;
                }

                // Generate probe details
                for ( DifferentialExpressionAnalysisResult dear : ears.getResults() ) {

                    if ( dear == null ) {
                        log.warn( "Missing results for " + ee + " skipping to next. " );
                        continue;
                    }
                    if ( !( dear instanceof ProbeAnalysisResult ) ) {
                        log.warn( "probe details missing.  Unable to retrieve probe level information. Skipping  "
                                + dear.getClass() + " with id: " + dear.getId() );
                        continue;
                    }

                    if ( dear.getCorrectedPvalue() == null || dear.getCorrectedPvalue() > THRESH_HOLD ) continue;

                    String formatted = formatDiffExResult( ee, ( ProbeAnalysisResult ) dear, factorName, factorURI,
                            baselineDescription );

                    if ( StringUtils.isNotBlank( formatted ) ) writer.write( formatted );

                } // dear loop
            } // ears loop

            if ( limit > 0 && ++i > limit ) break;

        }// EE loop
        writer.close();
    }

    /**
     * @param probeAnalysisResult
     * @return
     */
    private String formatDiffExResult( ExpressionExperiment ee, ProbeAnalysisResult probeAnalysisResult,
            String factorName, String factorURI, String baselineDescription ) {

        CompositeSequence cs = probeAnalysisResult.getProbe();

        Collection<Gene> genes = compositeSequenceService.getGenes( cs );

        if ( genes.isEmpty() || genes.size() > 1 ) {
            return null;
        }

        Gene g = genes.iterator().next();

        if (   g.getNcbiGeneId() == null ) return null;

        Collection<ContrastResult> contrasts = probeAnalysisResult.getContrasts();

        StringBuilder buf = new StringBuilder();
        for ( ContrastResult cr : contrasts ) {
            FactorValue factorValue = cr.getFactorValue();

            String direction = cr.getLogFoldChange() < 0 ? "-" : "+";

            String factorValueDescription = ExperimentalDesignUtils.prettyString( factorValue );

            buf.append( String.format( "%d\t%s\t%s\t%d\t%s\t%s\t%s\t%s\t%s\n", ee.getId(), ee.getShortName(), g
                    .getNcbiGeneId().toString(), g.getId(), factorName, factorURI, baselineDescription, factorValueDescription,
                    direction ) );
        }

        return buf.toString();
    }

    /**
     * @param datasetDiffexViewBasename
     * @return
     */
    private File getViewFile( String datasetDiffexViewBasename ) {
        return getOutputFile( datasetDiffexViewBasename + VIEW_FILE_SUFFIX );
    }

    /**
     * @param filename
     * @return
     */
    public File getOutputFile( String filename ) {
        String fullFilePath = VIEW_DIR + filename;
        File f = new File( fullFilePath );

        if ( f.exists() ) {
            return f;
        }

        File parentDir = f.getParentFile();
        if ( !parentDir.exists() ) parentDir.mkdirs();
        return f;
    }

}
