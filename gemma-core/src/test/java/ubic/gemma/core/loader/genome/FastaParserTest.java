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
package ubic.gemma.core.loader.genome;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ubic.gemma.model.genome.biosequence.BioSequence;

import java.io.InputStream;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author pavlidis
 */
public class FastaParserTest {

    private static final Log log = LogFactory.getLog( FastaParserTest.class.getName() );
    private InputStream f;
    private InputStream g;

    @Test
    public void testParsecodelink() throws Exception {
        try (InputStream n = FastaParserTest.class
                .getResourceAsStream( "/data/loader/genome/codelink.testsequence.txt" )) {
            FastaParser p = new FastaParser();
            p.parse( n );
            Collection<BioSequence> actualResult = p.getResults();
            assertNotNull( actualResult );
            assertEquals( 22, actualResult.size() );
            for ( Object object : actualResult ) {
                BioSequence b = ( BioSequence ) object;
                FastaParserTest.log
                        .debug( "NAME=" + b.getName() + " DESC=" + b.getDescription() + " SEQ=" + b.getSequence() );
            }
        }
    }

    @Test
    public void testParseDoubleHeader() throws Exception {
        try (InputStream n = FastaParserTest.class.getResourceAsStream( "/data/loader/genome/fastaDoubleHeader.txt" )) {
            this.testParser( n );
        }
    }

    @Test
    public void testParseInputStream() throws Exception {
        FastaParser p = new FastaParser();
        p.parse( f );
        Collection<BioSequence> actualResult = p.getResults();
        assertNotNull( actualResult );
        assertEquals( 172, actualResult.size() );
        for ( Object object : actualResult ) {
            BioSequence b = ( BioSequence ) object;
            FastaParserTest.log
                    .debug( "NAME=" + b.getName() + " DESC=" + b.getDescription() + " SEQ=" + b.getSequence() );
        }
    }

    @Test
    public void testParseInputStreamAffyTarget() throws Exception {
        FastaParser p = new FastaParser();
        p.parse( g );
        Collection<BioSequence> actualResult = p.getResults();
        assertNotNull( actualResult );
        assertEquals( 172, actualResult.size() );
        for ( Object object : actualResult ) {
            BioSequence b = ( BioSequence ) object;

            assertTrue(
                    b.getSequenceDatabaseEntry() != null && b.getSequenceDatabaseEntry().getExternalDatabase() != null
                            && b.getSequenceDatabaseEntry().getExternalDatabase().getName()
                            .equalsIgnoreCase( "genbank" ) );

            if ( FastaParserTest.log.isDebugEnabled() )
                FastaParserTest.log
                        .debug( "NAME=" + b.getName() + " DESC=" + b.getDescription() + " SEQ=" + b.getSequence()
                                + " GB=" + b.getSequenceDatabaseEntry().getAccession() );

        }
    }

    @Test
    public void testParseMasked() throws Exception {
        try (InputStream n = FastaParserTest.class.getResourceAsStream( "/data/loader/genome/maskedSeq.fa" )) {
            FastaParser p = new FastaParser();
            p.parse( n );
            Collection<BioSequence> actualResult = p.getResults();
            assertNotNull( actualResult );
            assertEquals( 7, actualResult.size() );
            for ( Object object : actualResult ) {
                BioSequence b = ( BioSequence ) object;
                FastaParserTest.log
                        .debug( "NAME=" + b.getName() + " DESC=" + b.getDescription() + " SEQ=" + b.getSequence() );
            }
        }
    }

    @Test
    public void testParseNIA() throws Exception {
        try (InputStream n = FastaParserTest.class.getResourceAsStream( "/data/loader/genome/nia15k.sample.fa" )) {
            this.testParser( n );
        }
    }

    @BeforeEach
    protected void setUp() throws Exception {
        f = FastaParserTest.class.getResourceAsStream( "/data/loader/genome/testsequence.fa" );
        g = FastaParserTest.class.getResourceAsStream( "/data/loader/genome/testsequence.fa" );
    }

    private void testParser( InputStream n ) throws java.io.IOException {
        FastaParser p = new FastaParser();
        p.parse( n );
        Collection<BioSequence> actualResult = p.getResults();
        assertNotNull( actualResult );
        assertEquals( 2, actualResult.size() );
        for ( Object object : actualResult ) {
            BioSequence b = ( BioSequence ) object;
            FastaParserTest.log
                    .debug( "NAME=" + b.getName() + " DESC=" + b.getDescription() + " SEQ=" + b.getSequence() );
        }
    }

}
