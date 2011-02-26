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

import junit.framework.TestCase;

import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.util.ConfigUtils;

/**
 * @author pavlidis
 * @version $Id$
 */
public class SimpleFastaCmdTest extends TestCase {

    private static final String TEST_RESOURCE_PATH = ConfigUtils.getString( "gemma.home" )
            + "/gemma-core/src/test/resources/data/loader/genome/blast";
    private static final String TESTBLASTDB = "testblastdb";

    // Test may need to be disabled because it fails in continuum, sometimes (unpredictable)
    public void testGetMultiple() throws Exception {
        if ( !fastaCmdExecutableExists() ) {
            return;
        }
        SimpleFastaCmd fastaCmd = new SimpleFastaCmd();

        Collection<Integer> input = new ArrayList<Integer>();
        input.add( 1435867 );
        input.add( 1435868 );

        Collection<BioSequence> bs = fastaCmd.getBatchIdentifiers( input, TESTBLASTDB, TEST_RESOURCE_PATH );
        assertNotNull( bs );
        assertEquals( 2, bs.size() );
    }

    // Test may need to be disabled because it fails in continuum, sometimes (unpredictable)
    public void testGetMultipleAcc() throws Exception {
        if ( !fastaCmdExecutableExists() ) {
            return;
        }
        SimpleFastaCmd fastaCmd = new SimpleFastaCmd();

        Collection<String> input = new ArrayList<String>();
        input.add( "AA000002.1" );
        input.add( "AA000003.1" );

        Collection<BioSequence> bs = fastaCmd.getBatchAccessions( input, TESTBLASTDB, TEST_RESOURCE_PATH );
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

        Collection<BioSequence> bs = fastaCmd.getBatchAccessions( input, TESTBLASTDB, TEST_RESOURCE_PATH );
        assertNotNull( bs );
        assertEquals( 2, bs.size() );
    }

    public void testGetSingle() throws Exception {
        if ( !fastaCmdExecutableExists() ) {
            return;
        }
        SimpleFastaCmd fastaCmd = new SimpleFastaCmd();
        BioSequence bs = fastaCmd.getByIdentifier( 1435867, TESTBLASTDB, TEST_RESOURCE_PATH );
        assertNotNull( bs );
        String expected = "CCACCTTTCCCTCCACTCCTCACGTTCTCACCTGTAAAGCGTCCCTCCCTCATCCCCATGCCCCCTTACCCTGCAGGGTA"
                + "GAGTAGGCTAGAAACCAGAGAGCTCCAAGCTCCATCTGTGGAGAGGTGCCATCCTTGGGCTGCAGAGAGAGGAGAATTTG"
                + "CCCCAAAGCTGCCTGCAGAGCTTCACCACCCTTAGTCTCACAAAGCCTTGAGTTCATAGCATTTCTTGAGTTTTCACCCT"
                + "GCCCAGCAGGACACTGCAGCACCCAAAGGGCTTCCCAGGAGTAGGGTTGCCCTCAAGAGGCTCTTGGGTCTGATGGCCAC"
                + "ATCCTGGAATTGTTTTCAAGTTGATGGTCACAGCCCTGAGGCATGTAGGGGCGTGGGGATGCGCTCTGCTCTGCTCTCCT"
                + "CTCCTGAACCCCTGAACCCTCTGGCTACCCCAGAGCACTTAGAGCCAG";
        assertEquals( expected, bs.getSequence() );
    }

    public void testGetSingleAcc() throws Exception {
        if ( !fastaCmdExecutableExists() ) {
            return;
        }
        SimpleFastaCmd fastaCmd = new SimpleFastaCmd();
        String accession = "AA000002";
        BioSequence bs = fastaCmd.getByAccession( accession, TESTBLASTDB, TEST_RESOURCE_PATH );
        assertNotNull( "fastacmd failed to find " + accession, bs );
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
        BioSequence bs = fastaCmd.getByAccession( "FAKE.1", TESTBLASTDB, TEST_RESOURCE_PATH );
        assertNull( bs );
    }

    private boolean fastaCmdExecutableExists() {

        String fastacmdExe = ConfigUtils.getString( SimpleFastaCmd.FASTA_CMD_ENV_VAR );
        if ( fastacmdExe == null ) {
            return false;
        }

        File fi = new File( fastacmdExe );
        if ( !fi.canRead() ) {
            return false;
        }
        return true;
    }
}
