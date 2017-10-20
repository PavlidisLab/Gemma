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
package ubic.gemma.core.loader.genome;

import java.io.InputStream;
import java.util.Collection;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

/**
 * @author pavlidis
 *
 */
public class GffParserTest extends TestCase {
    private static Log log = LogFactory.getLog( GffParserTest.class.getName() );
    InputStream is;

    public void testParseInputStream() throws Exception {
        GffParser parser = new GffParser();
        Taxon t = Taxon.Factory.newInstance();
        t.setCommonName( "mouse" );
        t.setScientificName( "Mus musculus" );
        t.setIsSpecies( true );
        t.setIsGenesUsable( true );

        parser.setTaxon( t );
        parser.parse( is );
        Collection<Gene> res = parser.getResults();

        for ( Object object : res ) {
            assertEquals( Gene.class, object.getClass() );
            Gene gene = ( Gene ) object;
            assertTrue( gene.getName() != null );
            assertFalse( gene.getName().contains( "\"" ) );
            assertEquals( 1, gene.getProducts().size() );
            log.debug( object );
        }
        assertEquals( 382, res.size() );
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        is = GffParserTest.class.getResourceAsStream( "/data/loader/genome/microrna-mmu.gff" );
    }

    @Override
    protected void tearDown() throws Exception {
        if ( is != null ) is.close();
        super.tearDown();
    }

}
