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
package ubic.gemma.loader.expression.arrayExpress;

import java.io.InputStream;
import java.util.Collection;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.designElement.CompositeSequence;

/**
 * Test of ArrayExpress array design parsing.
 * 
 * @author paul
 * @version $Id$
 */
public class ArrayDesignParserTest extends TestCase {

    protected Log log = LogFactory.getLog( getClass() );

    final public void testA() throws Exception {
        InputStream is = ArrayDesignParserTest.class
                .getResourceAsStream( "/data/loader/expression/mage/A-AFFY-6.arrayDesignDetails.test.txt" );
        ArrayDesignParser parser = new ArrayDesignParser();
        parser.parse( is );
        is.close();

        Collection<CompositeSequence> probes = parser.getResults();
        assertEquals( 258, probes.size() );

        for ( CompositeSequence cs : probes ) {
            assertNotNull( cs.getName() );
            assertTrue( cs.getName(), cs.getName().endsWith( "_at" ) || cs.getName().endsWith( "_st" ) );
        }

    }

    /**
     * Each reporter is there twice...
     * 
     * @throws Exception
     */
    final public void testB() throws Exception {

        InputStream is = ArrayDesignParserTest.class
                .getResourceAsStream( "/data/loader/expression/mage/A-FPMI-3.arrayDesignDetails.test.txt" );
        ArrayDesignParser parser = new ArrayDesignParser();
        parser.setUseReporterId( true );
        parser.parse( is );
        is.close();

        Collection<CompositeSequence> probes = parser.getResults();
        assertEquals( 442, probes.size() );

        for ( CompositeSequence cs : probes ) {
            assertNotNull( cs.getName() );
        }

    }

    /**
     * Illumina
     * 
     * @throws Exception
     */
    final public void testC() throws Exception {

        InputStream is = ArrayDesignParserTest.class
                .getResourceAsStream( "/data/loader/expression/mage/A-MEXP-691.arrayDesignDetails.test.txt" );
        ArrayDesignParser parser = new ArrayDesignParser();
        parser.parse( is );
        is.close();

        Collection<CompositeSequence> probes = parser.getResults();
        assertEquals( 227, probes.size() );

        for ( CompositeSequence cs : probes ) {
            assertNotNull( cs.getName() );
            assertTrue( cs.getName(), cs.getName().startsWith( "GI_" ) );
        }

    }

    /**
     * @throws Exception
     */
    final public void testD() throws Exception {

        InputStream is = ArrayDesignParserTest.class
                .getResourceAsStream( "/data/loader/expression/mage/A-SMDB-681.arrayDesignDetails.test.txt" );
        ArrayDesignParser parser = new ArrayDesignParser();
        parser.setUseReporterId( false );
        parser.parse( is );
        is.close();

        Collection<CompositeSequence> probes = parser.getResults();
        assertEquals( 104, probes.size() );

        for ( CompositeSequence cs : probes ) {
            assertNotNull( cs.getName() );
            assertTrue( cs.getName(), cs.getName().endsWith( "CompositeSequence" ) );
        }

    }

    /**
     * @throws Exception
     */
    final public void testE() throws Exception {

        InputStream is = ArrayDesignParserTest.class
                .getResourceAsStream( "/data/loader/expression/mage/A-MEXP-153.arrayDesignDetails.newformat.test.txt" );
        ArrayDesignParser parser = new ArrayDesignParser();
        parser.setUseReporterId( false );
        parser.parse( is );
        is.close();

        assertEquals( "Mus musculus", parser.getTaxonName() );

        Collection<CompositeSequence> probes = parser.getResults();
        assertEquals( 9, probes.size() );

        for ( CompositeSequence cs : probes ) {
            assertNotNull( cs.getName() );
            assertTrue( cs.getName(), cs.getName().startsWith( "NMA" ) );
        }

    }
}
