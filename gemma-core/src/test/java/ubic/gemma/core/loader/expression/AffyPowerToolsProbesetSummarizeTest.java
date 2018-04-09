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

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

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
        assertEquals( "RaGene-1_0-st-v1.r4.pgf", mpsnames.get( "GPL6247" ).get( "pgf" ) );

    }

}
