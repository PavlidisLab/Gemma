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

import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author pavlidis
 * @version $Id$
 */
public class MageMLParserTest extends AbstractMageTest {

    protected static final Log log = LogFactory.getLog( MageMLParserTest.class );

    MageMLParser mlp;
    InputStream istBioSequence;
    InputStream istExperiment;
    InputStream istArrayDesign;
    InputStream istBioMaterial;
    InputStream istQuantitationType;
    InputStream istDesignElement;
    InputStream istDrosDesignElement;
    InputStream istPhysicalBioAssay;
    InputStream istTIGRSimpleArrayDesign;
    InputStream istTIGRBiomaterial;

    InputStream istProtocol;
    InputStream istQTAffy;
    InputStream istQTGenePix;

    // zipped
    GZIPInputStream istBigBioSequence;
    GZIPInputStream istBigDesignElement;
    GZIPInputStream istBigArrayDesign;
    GZIPInputStream ist100CP;

    InputStream istDingledine;
    InputStream istExampleBioMaterial;
    InputStream istHematochromatosis;

    ZipInputStream istAffyGiantBioSequencePackage;

    @Override
    protected void onSetUp() throws Exception {
        // super.onSetUpInTransaction();
        mlp = new MageMLParser();
    }

    public void testParseCollectionRealA() throws Exception {
        log.debug( "Parsing MAGE from ArrayExpress (AFMX)" );

        xslSetup( mlp, MAGE_DATA_RESOURCE_PATH + "E-AFMX-13/E-AFMX-13.xml" );

        InputStream istMageExamples = MageMLParserTest.class.getResourceAsStream( MAGE_DATA_RESOURCE_PATH
                + "E-AFMX-13/E-AFMX-13.xml" );
        mlp.parse( istMageExamples );

        log.info( "Tally:\n" + mlp );
        istMageExamples.close();
    }

    public void testParseCollectionRealB() throws Exception {
        log.debug( "Parsing MAGE from ArrayExpress (WMIT)" );

        xslSetup( mlp, MAGE_DATA_RESOURCE_PATH + "/E-WMIT-4.xml" );

        InputStream istMageExamples = MageMLParserTest.class.getResourceAsStream( MAGE_DATA_RESOURCE_PATH
                + "E-WMIT-4.xml" );
        mlp.parse( istMageExamples );

        log.info( "Tally:\n" + mlp );
        istMageExamples.close();
    }

}
