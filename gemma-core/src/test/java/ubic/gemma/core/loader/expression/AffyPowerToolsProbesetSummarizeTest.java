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

import java.util.Map;
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
     * getMPSCommand reads all four names for a platform and hands each to checkFileReadable, so a quartet
     * missing one member fails at the apt call rather than here. GPL24242 (Clariom_S_Mouse_HT) needs its own
     * set: its CEL files carry {@code affymetrix-array-type=Clariom_S_Mouse_HT}, which apt matches against
     * {@code #%chip_type} in the pgf, so GPL23038's non-HT library cannot summarize them.
     */
    @Test
    public void testClariomSMouseHTHasItsOwnCompleteLibrarySet() {
        AffyPowerToolsProbesetSummarize t = new AffyPowerToolsProbesetSummarize();
        Map<String, String> ht = t.loadMpsNames().get( "GPL24242" );

        assertNotNull( ht, "GPL24242 has no entry in affy.mps.properties" );
        assertEquals( "Clariom_S_Mouse_HT.r1.pgf", ht.get( "pgf" ) );
        assertEquals( "Clariom_S_Mouse_HT.r1.clf", ht.get( "clf" ) );
        assertEquals( "Clariom_S_Mouse_HT.r1.mps", ht.get( "mps" ) );
        assertEquals( "Clariom_S_Mouse_HT.r1.qcc", ht.get( "qcc" ) );
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
