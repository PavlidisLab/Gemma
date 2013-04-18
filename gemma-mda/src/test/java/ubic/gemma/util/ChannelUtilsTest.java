/*
 * The Gemma project
 * 
 * Copyright (c) 2008-2011 University of British Columbia
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
package ubic.gemma.util;

import junit.framework.TestCase;

/**
 * @author paul
 * @version $Id$
 */
public class ChannelUtilsTest extends TestCase {

    public final void testChannelASignal() {
        assertTrue( ChannelUtils.isSignalChannelA( "Cy3_foreground_low_pmt" ) );
        assertTrue( ChannelUtils.isSignalChannelA( "ch1_sig_mean" ) );
        assertTrue( ChannelUtils.isSignalChannelA( "CH1_SIGNAL_MEDIAN" ) );
        assertTrue( ChannelUtils.isSignalChannelB( "CH2_SIGNAL_MEDIAN" ) );

    }

    public final void testChannelBSignal() {
        assertTrue( ChannelUtils.isSignalChannelB( "Cy5_foreground_low_pmt" ) );
        assertTrue( ChannelUtils.isSignalChannelB( "ch2_sig_mean" ) );
        assertTrue( ChannelUtils.isSignalChannelB( "CH2_SIGNAL_MEDIAN" ) );

    }
}
