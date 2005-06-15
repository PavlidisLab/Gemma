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
package edu.columbia.gemma.loader.mage;

import java.io.InputStream;

import junit.framework.TestCase;

import org.biomage.BioSequence.BioSequence;

import edu.columbia.gemma.loader.expression.mage.MageMLConverter;
import edu.columbia.gemma.loader.expression.mage.MageMLParser;

public class MageMLConverterTest extends TestCase {

    MageMLParser mlp;
    MageMLConverter mlc;

    InputStream bs;
    InputStream cs;
    InputStream rep;
    InputStream qt;
    InputStream desel;
    InputStream bassdat;

    protected void setUp() throws Exception {
        super.setUp();
        mlp = new MageMLParser();

        mlc = new MageMLConverter();

    }

    protected void tearDown() throws Exception {
        super.tearDown();
        mlp = null;
        mlc = null;
    }

    public final void testConvert() throws Exception {
        // mlp.parse( MageMLConverterTest.class.getResourceAsStream( "/data/mage/MGP-Biosequence.xml" ) );
        BioSequence bst = new BioSequence();
        Object result = mlc.convert( bst );
        assertTrue( result instanceof edu.columbia.gemma.genome.biosequence.BioSequence );
    }

}
