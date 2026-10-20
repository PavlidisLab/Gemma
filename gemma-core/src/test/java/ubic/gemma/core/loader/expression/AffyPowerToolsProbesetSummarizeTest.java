/*
 * The gemma-core project
 * 
 * Copyright (c) 2018 University of British Columbia
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

package ubic.gemma.core.loader.expression;

import org.junit.jupiter.api.Test;
import ubic.gemma.core.loader.expression.geo.model.GeoPlatform;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 
 * 
 * @author paul
 */
public class AffyPowerToolsProbesetSummarizeTest {

    @Test
    public void testLoadMPSNames() {
        AffyPowerToolsProbesetSummarize t = new AffyPowerToolsProbesetSummarize();
        Map<String, Map<String, String>> mpsnames = t.loadMpsNames();

        assertEquals( "MoGene-2_1-st.mps", mpsnames.get( "GPL17400" ).get( "mps" ) );
        assertEquals( "RaEx-1_0-st-v1.r2.pgf", mpsnames.get( "GPL6543" ).get( "pgf" ) );

    }

    /**
     * Reprocessing a CEL walks THREE separate tables, and a platform missing from any one of them fails at a
     * different depth with a different message. GPL24242 was missing from all three and they surfaced one at a
     * time over an afternoon (2026-09-22), so walk the whole chain here rather than one link:
     * <ol>
     *     <li>{@code affy.celmappings.properties} — the chip-type string in the CEL header to the ORIGINAL
     *         platform. Absent: {@code Couldn't figure out the GPL for Clariom_S_Mouse_HT}, thrown by
     *         {@code DataUpdaterImpl.determinePlatformsFromCELs} before anything else is consulted.</li>
     *     <li>{@code affy.altmappings.txt} — the original platform to the one whose library we summarize with.
     *         Absent: {@code is not recognized as an Affymetrix platform}, refused by the CLI. Covered by
     *         {@code GeoPlatformTest}.</li>
     *     <li>{@code affy.mps.properties} — that platform to its four library files. Absent: {@code There is
     *         no CDF or MPS configuration for}, thrown when the apt command is built.</li>
     * </ol>
     * Clariom_S_Mouse_HT resolves to itself throughout: its CEL files carry
     * {@code affymetrix-array-type=Clariom_S_Mouse_HT}, which apt matches against {@code #%chip_type} in the
     * pgf, and the non-HT pgf describes a different probe layout.
     */
    @Test
    public void testClariomSMouseHTIsConfiguredAtEveryStepOfTheCelChain() {
        AffyPowerToolsProbesetSummarize t = new AffyPowerToolsProbesetSummarize();

        assertEquals( "GPL24242", AffyPowerToolsProbesetSummarize
                .loadMapFromConfig( AffyPowerToolsProbesetSummarize.AFFY_CHIPNAME_PROPERTIES_FILE_NAME )
                .get( "Clariom_S_Mouse_HT" ) );

        Map<String, String> ht = t.loadMpsNames().get( "GPL24242" );
        assertNotNull( ht, "GPL24242 has no entry in affy.mps.properties" );
        assertEquals( "Clariom_S_Mouse_HT.r1.pgf", ht.get( "pgf" ) );
        assertEquals( "Clariom_S_Mouse_HT.r1.clf", ht.get( "clf" ) );
        assertEquals( "Clariom_S_Mouse_HT.r1.mps", ht.get( "mps" ) );
        assertEquals( "Clariom_S_Mouse_HT.r1.qcc", ht.get( "qcc" ) );
    }

    /**
     * Every chip type we claim to recognize has to be configured at all three steps, or reprocessing dies part
     * way in with a message that names only the step it reached. Nothing checked that, so GPL24242's three
     * gaps surfaced one run at a time.
     * <p>
     * The four exceptions below are pre-existing and are listed rather than fixed: what they should resolve to
     * is a question about those arrays, not about this invariant. A NEW name in one file and not the others
     * fails here instead of on frink.
     */
    @Test
    public void testEveryKnownChipTypeIsConfiguredThroughToItsLibrary() {
        Map<String, String> chipToPlatform = AffyPowerToolsProbesetSummarize
                .loadMapFromConfig( AffyPowerToolsProbesetSummarize.AFFY_CHIPNAME_PROPERTIES_FILE_NAME );
        Map<String, String> cdfs = AffyPowerToolsProbesetSummarize
                .loadMapFromConfig( AffyPowerToolsProbesetSummarize.AFFY_CDFS_PROPERTIES_FILE_NAME );
        Map<String, Map<String, String>> mps = new AffyPowerToolsProbesetSummarize().loadMpsNames();

        // GPL7440 (NuGO_Mm1a520177) is absent from affy.altmappings.txt; GPL14613, GPL74 and GPL8786 are
        // resolved by it but have neither a CDF nor an MPS quartet.
        Set<String> known = new HashSet<>( Arrays.asList( "GPL7440", "GPL14613", "GPL74", "GPL8786" ) );

        for ( Map.Entry<String, String> e : chipToPlatform.entrySet() ) {
            String originalPlatform = e.getValue();
            if ( known.contains( originalPlatform ) ) {
                continue;
            }
            String target = GeoPlatform.alternativeToProperAffyPlatform( originalPlatform );
            assertNotNull( target, "CEL chip type '" + e.getKey() + "' maps to " + originalPlatform
                    + ", which affy.altmappings.txt does not resolve; reprocessing refuses it as not an"
                    + " Affymetrix platform." );
            if ( known.contains( target ) ) {
                continue;
            }
            assertTrue( cdfs.containsKey( target ) || mps.containsKey( target ),
                    "CEL chip type '" + e.getKey() + "' resolves to " + target + ", which has neither a CDF in"
                            + " affy.cdfs.properties nor a library set in affy.mps.properties." );
        }
    }

    @Test
    public void testCELnameregex() {
        Pattern regex = Pattern.compile( AffyPowerToolsProbesetSummarize.GEO_CEL_FILE_NAME_REGEX );

        String[] tests = new String[] { "GSM467834_77_(huex-1_0-st-v2).cel.gz", "GSM467779_55.CEL.gz", "GSM467779.CEL.gz",
                "GSM467865_A10_HuEx-1_0-st-v2_2.CEL.gz",
                "GSM1440859_1273-FC.CEL", "GSM467780_35-real_HuEx-1_0-st-v2_.CEL.gz" };

        for ( String fn : tests ) {
            Matcher matcher = regex.matcher( fn );
            if ( matcher.matches() ) {

                String geoAcc = matcher.group( 1 );

                if ( geoAcc == null ) {
                    fail( fn + " matched but failed to extract GSM ID" );
                    break;
                }

                assertTrue( geoAcc.matches( "GSM[0-9]+" ) );
            } else {
                fail( fn + " didn't match" );
            }
        }

    }

}
