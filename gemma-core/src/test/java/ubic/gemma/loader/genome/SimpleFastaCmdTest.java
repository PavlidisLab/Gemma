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
package ubic.gemma.loader.genome;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.util.ConfigUtils;
import junit.framework.TestCase;

/**
 * @author pavlidis
 * @version $Id$
 */
public class SimpleFastaCmdTest extends TestCase {

    private static Log log = LogFactory.getLog( SimpleFastaCmdTest.class.getName() );

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    private boolean fastaCmdExecutableExists() {
        String fastacmdExe = ConfigUtils.getString( SimpleFastaCmd.FASTA_CMD_ENV_VAR );
        if ( fastacmdExe == null ) {
            log.warn( "No fastacmd executable is configured, skipping test" );
            return false;
        }

        File fi = new File( fastacmdExe );
        if ( !fi.canRead() ) {
            log.warn( fastacmdExe + " not found, skipping test" );
            return false;
        }
        return true;
    }

    public void testGetSingle() throws Exception {
        if ( !fastaCmdExecutableExists() ) {
            return;
        }
        SimpleFastaCmd fastaCmd = new SimpleFastaCmd();
        BioSequence bs = fastaCmd.getByIdentifier( 1435867, "testblastdb", ConfigUtils.getString( "gemma.home" )
                + "/gemma-core/src/test/resources/data/loader/genome/blast" );
        assertNotNull( bs );
        String expected = "CCACCTTTCCCTCCACTCCTCACGTTCTCACCTGTAAAGCGTCCCTCCCTCATCCCCATGCCCCCTTACCCTGCAGGGTA"
                + "GAGTAGGCTAGAAACCAGAGAGCTCCAAGCTCCATCTGTGGAGAGGTGCCATCCTTGGGCTGCAGAGAGAGGAGAATTTG"
                + "CCCCAAAGCTGCCTGCAGAGCTTCACCACCCTTAGTCTCACAAAGCCTTGAGTTCATAGCATTTCTTGAGTTTTCACCCT"
                + "GCCCAGCAGGACACTGCAGCACCCAAAGGGCTTCCCAGGAGTAGGGTTGCCCTCAAGAGGCTCTTGGGTCTGATGGCCAC"
                + "ATCCTGGAATTGTTTTCAAGTTGATGGTCACAGCCCTGAGGCATGTAGGGGCGTGGGGATGCGCTCTGCTCTGCTCTCCT"
                + "CTCCTGAACCCCTGAACCCTCTGGCTACCCCAGAGCACTTAGAGCCAG";
        assertEquals( expected, bs.getSequence() );
    }

    public void testGetMultiple() throws Exception {
        if ( !fastaCmdExecutableExists() ) {
            return;
        }
        SimpleFastaCmd fastaCmd = new SimpleFastaCmd();

        Collection<Integer> input = new ArrayList<Integer>();
        input.add( 1435867 );
        input.add( 1435868 );

        Collection<BioSequence> bs = fastaCmd.getBatchIdentifiers( input, "testblastdb", ConfigUtils
                .getString( "gemma.home" )
                + "/gemma-core/src/test/resources/data/loader/genome/blast" );
        assertNotNull( bs );
        assertEquals( 2, bs.size() );
    }

    public void testGetSingleAcc() throws Exception {
        if ( !fastaCmdExecutableExists() ) {
            return;
        }
        SimpleFastaCmd fastaCmd = new SimpleFastaCmd();
        BioSequence bs = fastaCmd.getByAccession( "AA000002", "testblastdb", ConfigUtils.getString( "gemma.home" )
                + "/gemma-core/src/test/resources/data/loader/genome/blast" );
        assertNotNull( bs );
        String expected = "CCACCTTTCCCTCCACTCCTCACGTTCTCACCTGTAAAGCGTCCCTCCCTCATCCCCATGCCCCCTTACCCTGCAGGGTA"
                + "GAGTAGGCTAGAAACCAGAGAGCTCCAAGCTCCATCTGTGGAGAGGTGCCATCCTTGGGCTGCAGAGAGAGGAGAATTTG"
                + "CCCCAAAGCTGCCTGCAGAGCTTCACCACCCTTAGTCTCACAAAGCCTTGAGTTCATAGCATTTCTTGAGTTTTCACCCT"
                + "GCCCAGCAGGACACTGCAGCACCCAAAGGGCTTCCCAGGAGTAGGGTTGCCCTCAAGAGGCTCTTGGGTCTGATGGCCAC"
                + "ATCCTGGAATTGTTTTCAAGTTGATGGTCACAGCCCTGAGGCATGTAGGGGCGTGGGGATGCGCTCTGCTCTGCTCTCCT"
                + "CTCCTGAACCCCTGAACCCTCTGGCTACCCCAGAGCACTTAGAGCCAG";
        assertEquals( expected, bs.getSequence() );
    }

    public void testGetSingleAccNotFound() throws Exception {
        if ( !fastaCmdExecutableExists() ) {
            return;
        }
        SimpleFastaCmd fastaCmd = new SimpleFastaCmd();
        BioSequence bs = fastaCmd.getByAccession( "FAKE.1", "testblastdb", ConfigUtils.getString( "gemma.home" )
                + "/gemma-core/src/test/resources/data/loader/genome/blast" );
        assertNull( bs );
    }

    public void testGetMultipleAcc() throws Exception {
        if ( !fastaCmdExecutableExists() ) {
            return;
        }
        SimpleFastaCmd fastaCmd = new SimpleFastaCmd();

        Collection<String> input = new ArrayList<String>();
        input.add( "AA000002.1" );
        input.add( "AA000003.1" );

        Collection<BioSequence> bs = fastaCmd.getBatchAccessions( input, "testblastdb", ConfigUtils
                .getString( "gemma.home" )
                + "/gemma-core/src/test/resources/data/loader/genome/blast" );
        assertNotNull( bs );
        assertEquals( 2, bs.size() );
    }

    public void testGetMultipleAccSomeNotFound() throws Exception {
        if ( !fastaCmdExecutableExists() ) {
            return;
        }
        SimpleFastaCmd fastaCmd = new SimpleFastaCmd();

        Collection<String> input = new ArrayList<String>();
        input.add( "FAKE.2" );
        input.add( "AA000002.1" );
        input.add( "FAKE.1" );
        input.add( "AA000003.1" );

        Collection<BioSequence> bs = fastaCmd.getBatchAccessions( input, "testblastdb", ConfigUtils
                .getString( "gemma.home" )
                + "/gemma-core/src/test/resources/data/loader/genome/blast" );
        assertNotNull( bs );
        assertEquals( 2, bs.size() );
    }
}
