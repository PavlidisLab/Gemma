/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.analysis.preprocess.batcheffects;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;

/**
 * Unit tests. Note that these depend on BatchInfoPopulationService.MAX_GAP_BETWEEN_SAMPLES_TO_BE_SAME_BATCH
 * 
 * @author paul
 * @version $Id$
 */
public class BatchInfoPopulationServiceTest {

    @Test
    public void testDatesToBatchA() throws Exception {
        BatchInfoPopulationService ser = new BatchInfoPopulationService();

        Calendar cal = Calendar.getInstance();
        cal.set( 2004, 3, 10 );
        Date d = cal.getTime();

        Collection<Date> dates = new HashSet<Date>();

        dates.add( d );
        dates.add( DateUtils.addHours( d, 1 ) );
        dates.add( DateUtils.addHours( d, 2 ) );
        dates.add( DateUtils.addHours( d, 3 ) );
        dates.add( DateUtils.addHours( d, 25 ) );
        dates.add( DateUtils.addHours( d, 26 ) );
        dates.add( DateUtils.addHours( d, 27 ) );
        dates.add( DateUtils.addHours( d, 28 ) );

        Map<String, Collection<Date>> actual = ser.convertDatesToBatches( dates );
        /*
         * How many unique values?
         */
        Set<String> s = new HashSet<String>( actual.keySet() );
        assertEquals( 2, s.size() );
    }

    @Test
    public void testDatesToBatchB() throws Exception {
        BatchInfoPopulationService ser = new BatchInfoPopulationService();

        Calendar cal = Calendar.getInstance();
        cal.set( 2004, 3, 10 );
        Date d = cal.getTime();

        Collection<Date> dates = new HashSet<Date>();

        dates.add( d );
        dates.add( DateUtils.addSeconds( d, 3500 ) );
        dates.add( DateUtils.addSeconds( d, 7000 ) );
        dates.add( DateUtils.addSeconds( d, 8000 ) );// first batch, all within two hours of each other.
        dates.add( DateUtils.addHours( d, 11124 ) );
        dates.add( DateUtils.addHours( d, 11125 ) );// third batch
        dates.add( DateUtils.addHours( d, 2226 ) );// second batch
        dates.add( DateUtils.addHours( d, 11189 ) ); // fourth batch

        Map<String, Collection<Date>> actual = ser.convertDatesToBatches( dates );

        /*
         * How many unique values?
         */
        Set<String> s = new HashSet<String>( actual.keySet() );
        assertEquals( 4, s.size() );
    }

}
