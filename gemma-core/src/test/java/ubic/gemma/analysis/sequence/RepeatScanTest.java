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
package ubic.gemma.analysis.sequence;

import junit.framework.TestCase;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * @author pavlidis
 * @version $Id$
 */
public class RepeatScanTest extends TestCase {

    public final void testFraction() {
        BioSequence b = BioSequence.Factory.newInstance();
        b.setSequence( "AAAaaaaAAAAaaa" );
        RepeatScan r = new RepeatScan();
        double d = r.computeFractionMasked( b );
        assertEquals( 0.5, d, 0.001 );
    }
}
