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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ProbeAnalysisResult;
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
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
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
    ExpressionExperimentService expressionExperimentService;
    @Autowired
    CompositeSequenceService compositeSequenceService;
    @Autowired
    DifferentialExpressionAnalysisService differentialExpressionAnalysisService;
    @Autowired
    DifferentialExpressionResultService differentialExpressionResultService;
    @Autowired
    ArrayDesignService arrayDesignService;

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
    public void generateDatasetView( Integer limit ) throws FileNotFoundException, IOException {

        log.info( "Generating dataset summary view" );

        /*
         * Get handle to output file
         */
        File file = getViewFile( DATASET_SUMMARY_VIEW_BASENAME );
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

            if ( limit != null ) {
                if ( ++i > limit ) break;
            }
        }

        writer.close();
    }

    public void generateDatasetTissueView( Integer limit ) throws FileNotFoundException, IOException {
        log.info( "Generating dataset tissue view" );

        /*
         * Get handle to output file
         */
        File file = getViewFile( DATASET_TISSUE_VIEW_BASENAME );
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
            if ( limit != null ) {
                if ( ++i > limit ) break;
            }
        }

        writer.close();
    }

    public void generateDifferentialExpressionView( Integer limit ) throws FileNotFoundException, IOException {
        log.info( "Generating dataset diffex view" );

        /*
         * Get handle to output file
         */
        File file = getViewFile( DATASET_DIFFEX_VIEW_BASENAME );
        Writer writer = new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( file ) ) );

        /*
         * Load all the data sets
         */Collection<ExpressionExperiment> vos = expressionExperimentService.loadAll();

        /*
         * For each gene that is differentially expressed, print out a line.
         */
        writer.write( "GemmaDsId\tEEShortName\tGeneNCBIId\tGemmaGeneId\tFactor\tFactorURI\n" );
        int i = 0;
        for ( ExpressionExperiment vo : vos ) {
            vo = expressionExperimentService.thawLite( vo );

            log.info( "Processing: " + vo.getShortName() );

            Collection<ExpressionAnalysisResultSet> results = differentialExpressionAnalysisService.getResultSets( vo );
            if ( results == null || results.isEmpty() ) {
                log.warn( "No differential expression results found for " + vo );
                continue;
            }

            for ( ExpressionAnalysisResultSet ears : results ) {
                if ( ears == null ) {
                    log.warn( "No  expression analysis results found for " + vo );
                    continue;
                }
                differentialExpressionResultService.thaw( ears );

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
                    log.warn( "No  differential expression analysis results found for " + vo );
                    continue;
                }

                // Better to thaw the cs's together, much faster.
                Collection<CompositeSequence> csList = new ArrayList<CompositeSequence>();
                // Generate probe details
                for ( DifferentialExpressionAnalysisResult dear : ears.getResults() ) {

                    if ( dear == null ) {
                        log.warn( "Missing results for " + vo + " skipping to next. " );
                        continue;
                    }
                    if ( dear instanceof ProbeAnalysisResult ) {
                        CompositeSequence cs = ( ( ProbeAnalysisResult ) dear ).getProbe();

                        // If p-value didn't make cut off then don't bother putting in file
                        // TODO This is a slow way to do this. Would be better to use a query to get only the
                        // thresholded data needed.
                        if ( dear.getCorrectedPvalue() == null || dear.getCorrectedPvalue() > THRESH_HOLD ) continue;
                        csList.add( cs );
                    } else {
                        log.warn( "probe details missing.  Unable to retrieve probe level information. Skipping  "
                                + dear.getClass() + " with id: " + dear.getId() );
                    }
                } // dear loop

                writeDiffExpressedGenes2File( csList, vo, factorName, factorURI, writer );

            } // ears loop

            if ( limit != null ) {
                if ( ++i > limit ) break;
            }
        }// EE loop
        writer.close();
    }

    private void writeDiffExpressedGenes2File( Collection<CompositeSequence> csList, ExpressionExperiment vo,
            String factorName, String factorURI, Writer writer ) throws IOException {

        // Figure out what gene is associated with the expressed probe.
        Map<CompositeSequence, Collection<Gene>> cs2genes = compositeSequenceService.getGenes( csList );

        for ( CompositeSequence cs : csList ) {

            Collection<Gene> genes = cs2genes.get( cs );
            if ( genes == null || genes.isEmpty() ) {
                log.debug( "Probe: " + cs.getName() + " met threshold but no genes associated with probe so skipping" );
                continue;
            } else if ( genes.size() > 1 ) {
                log.debug( "Probe: " + cs.getName() + " has " + genes.size()
                        + " assoicated with it. Skipping because not specific." );
                continue;
            } else if ( genes.iterator().next().getNcbiId() == null ) {
                log.debug( "Probe: " + cs.getName() + " has " + genes.iterator().next().getOfficialSymbol()
                        + " assoicated with it. This gene has no NCBI id so skipping" );
                continue;
            }

            // Write data to file.
            Gene gene = genes.iterator().next();
            writer.write( String.format( "%d\t%s\t%s\t%d\t%s\t%s\n", vo.getId(), vo.getShortName(), gene.getNcbiId(),
                    gene.getId(), factorName, factorURI ) );

        }

    }

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
