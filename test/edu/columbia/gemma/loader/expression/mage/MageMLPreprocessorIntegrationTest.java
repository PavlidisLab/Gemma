/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.loader.expression.mage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.springframework.beans.factory.BeanFactory;

import edu.columbia.gemma.BaseServiceTestCase;
import edu.columbia.gemma.expression.bioAssay.BioAssay;

/**
 * Integration test of MageML: Parser, Converter and Preprocessor
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class MageMLPreprocessorIntegrationTest extends BaseServiceTestCase {
    protected static final Log log = LogFactory.getLog( MageMLPreprocessorIntegrationTest.class );

    BeanFactory ctx = null;
    private MageMLParser mageMLParser = null;
    private MageMLConverter mageMLConverter = null;
    private MageMLPreprocessor mageMLPreprocessor = null;

    private String intensity_qtype_filename = null;
    private String stdev_qtype_filename = null;
    private String pixel_qtype_filename = null;
    private String outlier_qtype_filename = null;
    private String masked_qtype_filename = null;

    public void setup() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        ctx = null;
    }

    /**
     * Tests the conversion of source domain objects (SDO) to gemma domain objects (GDO), then creates matricies for
     * each of the quantitation types for a give bioAssay.
     * 
     * @throws IOException
     * @throws TransformerException TODO a work in progress
     * @throws ConfigurationException
     */
    @SuppressWarnings("unchecked")
    public void testPreprocess() throws IOException, TransformerException, ConfigurationException {

        /* PARSING */
        log.info( "***** PARSING ***** \n" );

        this.setMageMLParser( ( MageMLParser ) ctx.getBean( "mageMLParser" ) );

        this.setMageMLConverter( ( MageMLConverter ) ctx.getBean( "mageMLConverter" ) );

        this.setMageMLPreprocessor( ( MageMLPreprocessor ) ctx.getBean( "mageMLPreprocessor" ) );

        /* invoke mageMLParser */
        InputStream istMageExamples = MageMLPreprocessorIntegrationTest.class
                .getResourceAsStream( "/data/mage/E-AFMX-13/E-AFMX-13.xml" );

        getMageMLParser().parse( istMageExamples );

        /* create the simplified xml file using the mageMLParser */
        InputStream ist2MageExamples = MageMLPreprocessorIntegrationTest.class
                .getResourceAsStream( "/data/mage/E-AFMX-13/E-AFMX-13.xml" );
        getMageMLParser().createSimplifiedXml( ist2MageExamples );

        /* get results from parsing step */
        log.info( "Tally:\n" + getMageMLParser() );
        Collection<Object> mageObjects = getMageMLParser().getResults();
        log.debug( "number of SDOs: " + mageObjects.size() );

        /* get xsl transformed xml file */
        Document simplifiedXml = getMageMLParser().getSimplifiedXml();
        log.debug( "simplified xml document: " + simplifiedXml );

        /* close input streams */
        istMageExamples.close();
        ist2MageExamples.close();

        /* CONVERTING */
        log.info( "***** CONVERTING ***** \n" );

        /* create input stream from xsl file. */
        if ( simplifiedXml == null ) {
            log.info( "simplfied xml file is null.  Exiting test ..." );
            System.exit( 0 );
        }

        /*
         * on Spring initialization, simplifiedXml is still null because it has not been passed a document. Therefore,
         * set it.
         */
        getMageMLConverter().setSimplifiedXml( simplifiedXml );

        Collection<Object> gemmaObjects = getMageMLConverter().convert( mageObjects );
        log.debug( "number of GDOs: " + gemmaObjects.size() );
        for ( Object obj : gemmaObjects ) {
            log.debug( obj.getClass() + ": " + obj );
        }

        /* PREPROCESSING */
        log.info( "***** PREPROCESSING ***** \n" );

        /*
         * FIXME reading form Gemma.properties put here because if put in setUp(), the resulting values are null.
         */
        Configuration conf = new PropertiesConfiguration( "Gemma.properties" );
        intensity_qtype_filename = conf.getString( "intensity.matrix.outfile" );
        stdev_qtype_filename = conf.getString( "stdev.matrix.outfile" );
        pixel_qtype_filename = conf.getString( "pixel.matrix.outfile" );
        outlier_qtype_filename = conf.getString( "outlier.matrix.outfile" );
        masked_qtype_filename = conf.getString( "masked.matrix.outfile" );

        /* get all the gemma bioassays from the converter */
        List<BioAssay> bioAssays = getMageMLConverter().getConvertedBioAssays();

        // you will have to parse a new file for each bioassay.
        InputStream[] is = new InputStream[bioAssays.size()];

        is[0] = this.getClass().getResourceAsStream(
                "/data/mage/E-AFMX-13/031128_jm 29 c1 72hrao_031128_JM 29 C1 72hrAO_CEL_externaldata.txt" );

        is[1] = this.getClass().getResourceAsStream(
                "/data/mage/E-AFMX-13/031128_jm 30 g1 72hrgen_031128_JM 30 G1 72hrGEN_CEL_externaldata.txt" );

        is[2] = this.getClass().getResourceAsStream(
                "/data/mage/E-AFMX-13/031128_jm 31 e1 72hre2_031128_JM 31 E1 72hrE2_CEL_externaldata.txt" );

        is[3] = this.getClass().getResourceAsStream(
                "/data/mage/E-AFMX-13/031128_jm 32 d1 72hrdes_031128_JM 32 D1 72hrDES_CEL_externaldata.txt" );

        is[4] = this.getClass().getResourceAsStream(
                "/data/mage/E-AFMX-13/031201_jm 37 c2 72hrao_031201_JM 37 C2 72hrAO_CEL_externaldata.txt" );

        is[5] = this.getClass().getResourceAsStream(
                "/data/mage/E-AFMX-13/031201_jm 38 g2 72hrgen_031201_JM 38 G2 72hrGEN_CEL_externaldata.txt" );

        is[6] = this.getClass().getResourceAsStream(
                "/data/mage/E-AFMX-13/031201_jm 39 e2 72hre2_031201_JM 39 E2 72hrE2_CEL_externaldata.txt" );

        is[7] = this.getClass().getResourceAsStream(
                "/data/mage/E-AFMX-13/031201_jm 40 d2 72hrdes_031201_JM 40 D2 72hrDES_CEL_externaldata.txt" );

        is[8] = this.getClass().getResourceAsStream(
                "/data/mage/E-AFMX-13/031205_jm 45 c3 72hrao_031205_JM 45 C3 72hrAO_CEL_externaldata.txt" );

        is[9] = this.getClass().getResourceAsStream(
                "/data/mage/E-AFMX-13/031205_jm 46 g3 72hrgen_031205_JM 46 G3 72hrGEN_CEL_externaldata.txt" );

        is[10] = this.getClass().getResourceAsStream(
                "/data/mage/E-AFMX-13/031205_jm 47 e3 72hre2_031205_JM 47 E3 72hrE2_CEL_externaldata.txt" );

        is[11] = this.getClass().getResourceAsStream(
                "/data/mage/E-AFMX-13/031205_jm 48 d3 72hrdes_031205_JM 48 D3 72hrDES_CEL_externaldata.txt" );

        for ( int i = 0; i < bioAssays.size(); i++ ) {
            BioAssay ba = bioAssays.get( i );
            List qtypes = getMageMLConverter().getBioAssayQuantitationTypeDimension( ba );
            List designElements = getMageMLConverter().getBioAssayDesignElementDimension( ba );

            getMageMLPreprocessor().preprocess( ba, qtypes, designElements, is[i] );

        }

        /* create matricies of doubles */

        // intensity matrix
        double[][] matrixOfIntensities = null;
        matrixOfIntensities = getMageMLPreprocessor().convertListOfDoubleArraysToMatrix(
                getMageMLPreprocessor().getIntensityList(), 0, bioAssays.size() );

        getMageMLPreprocessor().log2DDoubleMatrixToFile( matrixOfIntensities, intensity_qtype_filename );

        // standard deviation matrix
        double[][] matrixOfStdevs = null;
        matrixOfStdevs = getMageMLPreprocessor().convertListOfDoubleArraysToMatrix(
                getMageMLPreprocessor().getStdevList(), 0, bioAssays.size() );

        getMageMLPreprocessor().log2DDoubleMatrixToFile( matrixOfStdevs, stdev_qtype_filename );

        /* create matricies of ints */

        // pixel matrix
        int[][] matrixOfPixels = null;
        matrixOfPixels = getMageMLPreprocessor().convertListOfIntArraysToMatrix(
                getMageMLPreprocessor().getPixelList(), 0, bioAssays.size() );

        getMageMLPreprocessor().log2DIntMatrixToFile( matrixOfPixels, pixel_qtype_filename );

        /* create matricies of booleans */

        // outlier matrix
        boolean[][] matrixOfOutliers = null;
        matrixOfOutliers = getMageMLPreprocessor().convertListOfBooleanArraysToMatrix(
                getMageMLPreprocessor().getOutlierList(), 0, bioAssays.size() );

        getMageMLPreprocessor().log2DBooleanMatrixToFile( matrixOfOutliers, outlier_qtype_filename );

        // masked matrix
        boolean[][] matrixOfMasked = null;
        matrixOfMasked = getMageMLPreprocessor().convertListOfBooleanArraysToMatrix(
                getMageMLPreprocessor().getMaskedList(), 0, bioAssays.size() );

        getMageMLPreprocessor().log2DBooleanMatrixToFile( matrixOfMasked, masked_qtype_filename );
    }

    /**
     * @return Returns the mageMLParser.
     */
    public MageMLParser getMageMLParser() {
        return mageMLParser;
    }

    /**
     * @param mageMLParser The mageMLParser to set.
     */
    public void setMageMLParser( MageMLParser mageMLParser ) {
        this.mageMLParser = mageMLParser;
    }

    /**
     * @param mageMLConverter The mageMLConverter to set.
     */
    public void setMageMLConverter( MageMLConverter mageMLConverter ) {
        this.mageMLConverter = mageMLConverter;
    }

    /**
     * @return Returns the mageMLConverter.
     */
    public MageMLConverter getMageMLConverter() {
        return mageMLConverter;
    }

    /**
     * @return Returns the mageMLPreprocessor.
     */
    public MageMLPreprocessor getMageMLPreprocessor() {
        return mageMLPreprocessor;
    }

    /**
     * @param mageMLPreprocessor The mageMLPreprocessor to set.
     */
    public void setMageMLPreprocessor( MageMLPreprocessor mageMLPreprocessor ) {
        this.mageMLPreprocessor = mageMLPreprocessor;
    }
}
