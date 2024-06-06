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

import junit.framework.TestCase;

/**
 * Test values are from hg18 all_est.
 *
 * @author pavlidis
 *
 */
public class SequenceBinUtilsTest extends TestCase {

    public void testBinFromRangeA() {
        int start = 2802;
        int end = 3149;
        int expectedBin = 585;
        int actualBin = SequenceBinUtils.binFromRange( start, end );
        assertEquals( expectedBin, actualBin );
    }

    public void testBinFromRangeB() {
        int start = 6263823;
        int end = 6310008;
        int expectedBin = 9;
        int actualBin = SequenceBinUtils.binFromRange( start, end );
        assertEquals( expectedBin, actualBin );
    }

    public void testBinFromRangeC() {
        int start = 36694488;
        int end = 36702427;
        int expectedBin = 13;
        int actualBin = SequenceBinUtils.binFromRange( start, end );
        assertEquals( expectedBin, actualBin );
    }

    public void testBinFromRangeD() {
        int start = 31582661;
        int end = 31594399;
        int expectedBin = 103;
        int actualBin = SequenceBinUtils.binFromRange( start, end );
        assertEquals( expectedBin, actualBin );
    }

}
