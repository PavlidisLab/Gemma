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
package ubic.gemma.loader.expression.mage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.util.ConfigUtils;

/**
 * Integration test of MageML: Parser, Converter and Preprocessor
 * 
 * @author keshav
 * @version $Id$
 */
public class MageMLPreprocessorIntegrationTest extends AbstractMageTest {
    protected static final Log log = LogFactory.getLog( MageMLPreprocessorIntegrationTest.class );

    private MageMLParser mageMLParser = null;
    private MageMLConverter mageMLConverter = null;
    private MageMLPreprocessor mageMLPreprocessor = null;

    /**
     * @param mageMLConverter The mageMLConverter to set.
     */
    public void setMageMLConverter( MageMLConverter mageMLConverter ) {
        this.mageMLConverter = mageMLConverter;
    }

    /**
     * @param mageMLParser The mageMLParser to set.
     */
    public void setMageMLParser( MageMLParser mageMLParser ) {
        this.mageMLParser = mageMLParser;
    }

    @Override
    public void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        this.mageMLPreprocessor = new MageMLPreprocessor( "testPreprocess" );
        mageMLPreprocessor.setPersisterHelper( ( PersisterHelper ) this.getBean( "persisterHelper" ) );
    }

    /**
     * Tests the conversion of source domain objects (SDO) to gemma domain objects (GDO), then creates matricies for
     * each of the quantitation types for a give bioAssay, save to disk and persist the resulting vectors.
     * 
     * @throws IOException
     * @throws TransformerException
     */

    /*
     * This test partly disabled until we fix ArrayExpress composite sequence and reporter fetch/parse/convert
     */

    @SuppressWarnings("unchecked")
    public void testPreprocess() throws IOException, TransformerException {

        /* PARSING */
        log.info( "***** PARSING *****  " );

        /* invoke mageMLParser */
        InputStream istMageExamples = MageMLPreprocessorIntegrationTest.class
                .getResourceAsStream( MAGE_DATA_RESOURCE_PATH + "E-AFMX-13/E-AFMX-13.xml" );

        assert mageMLParser != null;

        mageMLParser.parse( istMageExamples );

        /* create the simplified xml file using the mageMLParser */
        InputStream ist2MageExamples = MageMLPreprocessorIntegrationTest.class
                .getResourceAsStream( MAGE_DATA_RESOURCE_PATH + "E-AFMX-13/E-AFMX-13.xml" );
        // mageMLParser.createSimplifiedXml( ist2MageExamples );

        /* get results from parsing step */
        log.info( "Tally:\n" + mageMLParser );
        Collection<Object> mageObjects = mageMLParser.getResults();
        log.debug( "number of SDOs: " + mageObjects.size() );

        /* get xsl transformed xml file */
        // Document simplifiedXml = mageMLParser.getSimplifiedXml();
        // log.debug( "simplified xml document: " + simplifiedXml );
        /* close input streams */
        istMageExamples.close();
        ist2MageExamples.close();

        /* CONVERTING */
        log.info( "***** CONVERTING ***** " );

        /* create input stream from xsl file. */
        // if ( simplifiedXml == null ) {
        // throw new IllegalStateException( "Simplfied xml file is null. Exiting test ..." );
        // }
        /*
         * on Spring initialization, simplifiedXml is still null because it has not been passed a document. Therefore,
         * set it. CURRENTLY NOT USING THIS
         */
        // mageMLConverter.setSimplifiedXml( simplifiedXml );

        mageMLConverter.addLocalExternalDataPath( ConfigUtils.getString( "gemma.home" ) + File.separatorChar
                + "gemma-core/src/test/resources" + MAGE_DATA_RESOURCE_PATH + "E-AFMX-13" );

        ExpressionExperiment expressionExperiment = null;
        Collection<Object> gemmaObjects = mageMLConverter.convert( mageObjects );
        log.debug( "number of GDOs: " + gemmaObjects.size() );

        int numExpExp = 0;
        for ( Object obj : gemmaObjects ) {
            if ( obj instanceof ExpressionExperiment ) {
                expressionExperiment = ( ExpressionExperiment ) obj;
                numExpExp++;
            }
            if ( log.isDebugEnabled() ) {
                log.debug( obj.getClass() + ": " + obj );
            }
        }

        assert expressionExperiment != null && numExpExp == 1;

        /* CONVERTING */
        log.info( "***** PREPROCESSING ***** " );

        // get them in some specific order but we don't care what the order is.
        List<BioAssay> bioAssays = new ArrayList( expressionExperiment.getBioAssays() );

        /*
         * This strange loop is just an artifact of testing - we have to iterate over the quantitation types, and feed
         * the converter the raw data files for each bioassay in a given order (the array that is built). Each stream is
         * read multiple times to extract the necessary quantitation type information. In 'real life' this wouldn't be
         * needed.
         */

        // for this test we just have to know how many there are.
        int numQuantitationTypes = 7;

        for ( int i = 0; i < numQuantitationTypes; i++ ) {

            log.info( " Quantitation type #" + ( i + 1 ) );

            mageMLPreprocessor.setSelector( i );
            InputStream[] is = new InputStream[bioAssays.size()];
            for ( int j = 0; j < bioAssays.size(); j++ ) {
                // of course we can't count on the file names matching anything we already know...
                String assayNameForFile = bioAssays.get( j ).getName().toLowerCase() + "_"
                        + bioAssays.get( j ).getName();
                String fileName = "E-AFMX-13/" + assayNameForFile + "_CEL_externaldata.txt.short";
                is[j] = this.getClass().getResourceAsStream( MAGE_DATA_RESOURCE_PATH + fileName );
                assert is[j] != null : "Failed to open stream for " + fileName;
            }
            mageMLPreprocessor.preprocessStreams( Arrays.asList( is ), expressionExperiment, bioAssays, mageMLConverter
                    .getBioAssayDimensions() );
        }

        // this has to be outside that loop...

        // FIXME Commented out until MAGE parsing/converting fixed (bug 432)
        // mageMLPreprocessor.makePersistent();

    }

    // FIXME - check the data is in the database as expected.
}
