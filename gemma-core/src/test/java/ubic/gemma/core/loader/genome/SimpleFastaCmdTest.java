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

import org.junit.BeforeClass;
import org.junit.Test;
import ubic.basecode.util.FileTools;
import ubic.gemma.core.config.Settings;
import ubic.gemma.model.genome.biosequence.BioSequence;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.*;
import static ubic.gemma.core.util.test.Assumptions.assumeThatExecutableExists;

/**
 * @author pavlidis
 */
public class SimpleFastaCmdTest {

    private static final String TESTBLASTDB = "testblastdb";
    private static String testBlastDbPath;

    @BeforeClass
    public static void checkFastaCmdExecutableExists() throws URISyntaxException {
        assumeThatExecutableExists( Settings.getString( SimpleFastaCmd.FASTA_CMD_CONFIG_NAME ) );
        testBlastDbPath = FileTools.resourceToPath( "/data/loader/genome/blast" );
    }

    // Test may need to be disabled because it fails in continuum, sometimes (unpredictable)
    @Test
    public void testGetMultiple() {
        SimpleFastaCmd fastaCmd = new SimpleFastaCmd();
        fastaCmd.setBlastHome( testBlastDbPath );

        Collection<Integer> input = new ArrayList<>();
        input.add( 1435867 );
        input.add( 1435868 );

        Collection<BioSequence> bs = fastaCmd
                .getBatchIdentifiers( input, SimpleFastaCmdTest.TESTBLASTDB );
        assertNotNull( bs );
        assertEquals( 2, bs.size() );
    }

    // Test may need to be disabled because it fails in continuum, sometimes (unpredictable)
    @Test
    public void testGetMultipleAcc() {
        SimpleFastaCmd fastaCmd = new SimpleFastaCmd();
        fastaCmd.setBlastHome( testBlastDbPath );

        Collection<String> input = new ArrayList<>();
        input.add( "AA000002.1" );
        input.add( "AA000003.1" );

        Collection<BioSequence> bs = fastaCmd
                .getBatchAccessions( input, SimpleFastaCmdTest.TESTBLASTDB );
        assertNotNull( bs );
        assertEquals( 2, bs.size() );
    }

    @Test
    public void testGetMultipleAccSomeNotFound() {
        SimpleFastaCmd fastaCmd = new SimpleFastaCmd();
        fastaCmd.setBlastHome( testBlastDbPath );

        Collection<String> input = new ArrayList<>();
        input.add( "FAKE.2" );
        input.add( "AA000002.1" );
        input.add( "FAKE.1" );
        input.add( "AA000003.1" );

        Collection<BioSequence> bs = fastaCmd
                .getBatchAccessions( input, SimpleFastaCmdTest.TESTBLASTDB );
        fastaCmd.setBlastHome( testBlastDbPath );
        assertNotNull( bs );
        assertEquals( 2, bs.size() );
    }

    @Test
    public void testGetSingle() {
        SimpleFastaCmd fastaCmd = new SimpleFastaCmd();
        fastaCmd.setBlastHome( testBlastDbPath );
        BioSequence bs = fastaCmd.getByIdentifier( 1435867, SimpleFastaCmdTest.TESTBLASTDB );
        assertNotNull( bs );
        String expected = "CCACCTTTCCCTCCACTCCTCACGTTCTCACCTGTAAAGCGTCCCTCCCTCATCCCCATGCCCCCTTACCCTGCAGGGTA"
                + "GAGTAGGCTAGAAACCAGAGAGCTCCAAGCTCCATCTGTGGAGAGGTGCCATCCTTGGGCTGCAGAGAGAGGAGAATTTG"
                + "CCCCAAAGCTGCCTGCAGAGCTTCACCACCCTTAGTCTCACAAAGCCTTGAGTTCATAGCATTTCTTGAGTTTTCACCCT"
                + "GCCCAGCAGGACACTGCAGCACCCAAAGGGCTTCCCAGGAGTAGGGTTGCCCTCAAGAGGCTCTTGGGTCTGATGGCCAC"
                + "ATCCTGGAATTGTTTTCAAGTTGATGGTCACAGCCCTGAGGCATGTAGGGGCGTGGGGATGCGCTCTGCTCTGCTCTCCT"
                + "CTCCTGAACCCCTGAACCCTCTGGCTACCCCAGAGCACTTAGAGCCAG";
        assertEquals( expected, bs.getSequence() );
    }

    @Test
    public void testGetSingleAcc() {
        SimpleFastaCmd fastaCmd = new SimpleFastaCmd();
        fastaCmd.setBlastHome( testBlastDbPath );
        String accession = "AA000002";

        BioSequence bs = fastaCmd.getByAccession( accession, SimpleFastaCmdTest.TESTBLASTDB );
        assertNotNull( "fastacmd failed to find " + accession, bs );
        String expected = "CCACCTTTCCCTCCACTCCTCACGTTCTCACCTGTAAAGCGTCCCTCCCTCATCCCCATGCCCCCTTACCCTGCAGGGTA"
                + "GAGTAGGCTAGAAACCAGAGAGCTCCAAGCTCCATCTGTGGAGAGGTGCCATCCTTGGGCTGCAGAGAGAGGAGAATTTG"
                + "CCCCAAAGCTGCCTGCAGAGCTTCACCACCCTTAGTCTCACAAAGCCTTGAGTTCATAGCATTTCTTGAGTTTTCACCCT"
                + "GCCCAGCAGGACACTGCAGCACCCAAAGGGCTTCCCAGGAGTAGGGTTGCCCTCAAGAGGCTCTTGGGTCTGATGGCCAC"
                + "ATCCTGGAATTGTTTTCAAGTTGATGGTCACAGCCCTGAGGCATGTAGGGGCGTGGGGATGCGCTCTGCTCTGCTCTCCT"
                + "CTCCTGAACCCCTGAACCCTCTGGCTACCCCAGAGCACTTAGAGCCAG";
        assertEquals( expected, bs.getSequence() );
    }

    @Test
    public void testGetSingleAccNotFound() {
        SimpleFastaCmd fastaCmd = new SimpleFastaCmd();
        fastaCmd.setBlastHome( testBlastDbPath );

        BioSequence bs = fastaCmd.getByAccession( "FAKE.1", SimpleFastaCmdTest.TESTBLASTDB );
        assertNull( bs );
    }
}
