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

import java.io.InputStream;
import java.util.Collection;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.loader.expression.mage.MageMLParser;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class MageMLParserTest extends MageBaseTest {

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

    protected void setUp() throws Exception {
        super.setUp();
        mlp = new MageMLParser();

    }

    protected void tearDown() throws Exception {
        super.tearDown();
        mlp = null;
    }

//    public void testMageExamplesTest() throws Exception {
//        log.debug( "Parsing MAGE Jamboree example" );
//
//        zipXslSetup( mlp, "/data/mage/mageml-example.zip" );
//
//        ZipInputStream istMageExamples = new ZipInputStream( MageMLParserTest.class
//                .getResourceAsStream( "/data/mage/mageml-example.zip" ) );
//        istMageExamples.getNextEntry();
//        mlp.parse( istMageExamples );
//
//        log.info( "Tally:\n" + mlp );
//        istMageExamples.close();
//        // type expectedValue = null;
//        // assertEquals( expectedValue, actualValue );
//    }

    public void testParseCollectionRealA() throws Exception {
        log.debug( "Parsing MAGE from ArrayExpress (AFMX)" );

        xslSetup( mlp, "/data/mage/E-AFMX-13.xml" );

        InputStream istMageExamples = MageMLParserTest.class.getResourceAsStream( "/data/mage/E-AFMX-13.xml" );
        mlp.parse( istMageExamples );

        log.info( "Tally:\n" + mlp );
        istMageExamples.close();
    }

    public void testParseCollectionRealB() throws Exception {
        log.debug( "Parsing MAGE from ArrayExpress (WMIT)" );

        xslSetup( mlp, "/data/mage/E-WMIT-4.xml" );

        InputStream istMageExamples = MageMLParserTest.class.getResourceAsStream( "/data/mage/E-WMIT-4.xml" );
        mlp.parse( istMageExamples );

        log.info( "Tally:\n" + mlp );
        istMageExamples.close();
    }

}
