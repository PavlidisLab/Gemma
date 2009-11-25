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

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
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

    private static Log log = LogFactory.getLog( DatabaseViewGenerator.class );

    public static final String VIEW_DIR = ConfigUtils.getString( "gemma.appdata.home" ) + File.separatorChar
            + "dataFiles" + File.separatorChar;

    public static final String VIEW_FILE_SUFFIX = ".view.txt.gz";

    private static final String DATASET_SUMMARY_VIEW_BASENAME = "DatasetSummary";
    private static final String DATASET_TISSUE_VIEW_BASENAME = "DatasetTissue";
    private static final String DATASET_DIFFEX_VIEW_BASENAME = "DatasetDiffEx";

    @Autowired
    ExpressionExperimentService expressionExperimentService;

    public void runAll() {
        try {
            generateDatasetView();
            generateDatasetTissueView();
            generateDifferentialExpressionView();
        } catch ( FileNotFoundException e ) {
            throw new RuntimeException( e );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * @throws IOException
     * @throws FileNotFoundException
     */
    public void generateDatasetView() throws FileNotFoundException, IOException {

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

        writer.write( "GemmaDsId\tSource\tSourceAccession\tShortName\tName\n" );

        /*
         * Print out their names etc.
         */
        int i = 0;
        for ( ExpressionExperiment vo : vos ) {
            log.info( "Processing: " + vo.getShortName() );
            expressionExperimentService.thawLite( vo );

            String acc = "";
            String source = "";

            if ( vo.getAccession() != null && vo.getAccession().getAccession() != null ) {
                acc = vo.getAccession().getAccession();
                source = vo.getAccession().getExternalDatabase().getName();
            }

            Long gemmaId = vo.getId();

            String shortName = vo.getShortName();

            String name = vo.getName();

            writer.write( String.format( "%d\t%s\t%s\t%s\t%s\n", gemmaId, source, acc, shortName, name ) );

            // testing.
            if ( ++i > 100 ) break;

        }

        writer.close();
    }

    public void generateDatasetTissueView() throws FileNotFoundException, IOException {
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

            log.info( "Processing: " + vo.getShortName() );

            expressionExperimentService.thawLite( vo );

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
            if ( ++i > 100 ) break;
        }

        writer.close();
    }

    public void generateDifferentialExpressionView() throws FileNotFoundException, IOException {
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
         * For each gene that is differentially expressed, print out a line.F
         */
        writer.write( "GemmaDsId\tGeneNCBIId\tGemmaGeneId\n" );
        int i = 0;
        for ( ExpressionExperiment vo : vos ) {
            log.info( "Processing: " + vo.getShortName() );
            expressionExperimentService.thawLite( vo );

            /*
             * Get diff ex regardless of the factor.
             */

            /*
             * print gene + dataset.
             */
            if ( ++i > 100 ) break;
        }
        writer.close();
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
