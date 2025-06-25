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
package ubic.gemma.core.analysis.sequence;

import org.junit.Ignore;
import org.junit.Test;
import ubic.gemma.core.config.Settings;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static ubic.gemma.core.util.test.Assumptions.assumeThatExecutableExists;

/**
 * @author pavlidis
 *
 */
public class RepeatScanTest {

    private static final String repeatMaskerExe = Settings.getString( "repeatMasker.exe" );

    @Test
    @Ignore("RepeatMasker appears to be broken, see https://github.com/PavlidisLab/Gemma/issues/53")
    public void testRepeatScan() {
        assumeThatExecutableExists( repeatMaskerExe );
        Taxon taxon = Taxon.Factory.newInstance( "human" );
        BioSequence b = BioSequence.Factory.newInstance( "test", taxon );
        b.setSequence( "AAAaaaaAAAAaaa" );
        RepeatScan r = new RepeatScan();
        r.repeatScan( Collections.singleton( b ) );
    }

    @Test
    public void testFraction() {
        Taxon taxon = Taxon.Factory.newInstance( "human" );
        BioSequence b = BioSequence.Factory.newInstance( "test", taxon );
        b.setSequence( "AAAaaaaAAAAaaa" );
        RepeatScan r = new RepeatScan();
        double d = r.computeFractionMasked( b );
        assertEquals( 0.5, d, 0.001 );
    }
}
