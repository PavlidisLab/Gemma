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
package ubic.gemma.core.analysis.preprocess.batcheffects;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests. Note that these depend on BatchInfoPopulationService.MAX_GAP_BETWEEN_SAMPLES_TO_BE_SAME_BATCH
 *
 * @author paul
 */
public class BatchInfoPopulationServiceTest {

    private static final Log log = LogFactory.getLog( BatchInfoPopulationServiceTest.class );

    @Test
    public void testDatesToBatchA() {
        BatchInfoPopulationHelperServiceImpl ser = new BatchInfoPopulationHelperServiceImpl();

        Calendar cal = Calendar.getInstance();
        cal.set( 2004, Calendar.APRIL, 10, 10, 1, 1 );
        Date d = cal.getTime();

        List<Date> dates = new ArrayList<>();

        dates.add( d );
        dates.add( DateUtils.addHours( d, 1 ) ); // first batch
        dates.add( DateUtils.addHours( d, 2 ) ); // first batch
        dates.add( DateUtils.addHours( d, 3 ) ); // first batch
        dates.add( DateUtils.addHours( d, 24 ) ); // second batch
        dates.add( DateUtils.addHours( d, 25 ) );// second batch
        dates.add( DateUtils.addHours( d, 26 ) );// second batch
        dates.add( DateUtils.addHours( d, 27 ) );// second batch

        Map<String, Collection<Date>> actual = ser.convertDatesToBatches( dates, null );

        /*
         * How many unique values?
         */
        Set<String> s = new HashSet<>( actual.keySet() );
        assertEquals( 2, s.size() );
    }

    @Test
    public void testDatesToBatchB() {
        BatchInfoPopulationHelperServiceImpl ser = new BatchInfoPopulationHelperServiceImpl();
        Calendar cal = Calendar.getInstance();
        cal.set( 2004, Calendar.APRIL, 10 );
        Date d = cal.getTime();

        List<Date> dates = new ArrayList<>();

        dates.add( d );

        dates.add( DateUtils.addSeconds( d, 3500 ) );// first batch, all within two hours of each other.
        dates.add( DateUtils.addSeconds( d, 7000 ) );// first batch, all within two hours of each other.
        dates.add( DateUtils.addSeconds( d, 8000 ) );// first batch, all within two hours of each other.
        dates.add( DateUtils.addHours( d, 2226 ) );// second batch, but singleton merged backwards

        dates.add( DateUtils.addHours( d, 11124 ) );// third batch , but second was a singleton so we're only on #2.
        dates.add( DateUtils.addHours( d, 11125 ) );// third batch, but gets merged in with second.
        dates.add( DateUtils.addHours( d, 11189 ) ); // fourth batch, but gets merged in with second.

        dates.sort( Date::compareTo );

        Map<String, Collection<Date>> actual = ser.convertDatesToBatches( dates, null );

        /*
         * How many unique values?
         */
        Set<String> s = new HashSet<>( actual.keySet() );
        assertEquals( 2, s.size() );

        this.debug( actual );
    }

    @Test
    public void testDatesToBatchC() {
        BatchInfoPopulationHelperServiceImpl ser = new BatchInfoPopulationHelperServiceImpl();
        Calendar cal = Calendar.getInstance();
        cal.set( 2004, Calendar.APRIL, 10, 10, 1, 1 );
        Date d = cal.getTime();

        List<Date> dates = new ArrayList<>();

        dates.add( d );
        dates.add( DateUtils.addHours( d, 2 ) ); // should be merged back.
        dates.add( DateUtils.addHours( d, 3 ) ); // merged back.
        dates.add( DateUtils.addHours( d, 4 ) ); // merged back.
        dates.add( DateUtils.addHours( d, 5 ) ); // merged back.
        dates.add( DateUtils.addHours( d, 6 ) ); // merged back.
        dates.add( DateUtils.addHours( d, 7 ) ); // merged back.
        dates.add( DateUtils.addHours( d, 8 ) ); // merged back.

        Map<String, Collection<Date>> actual = ser.convertDatesToBatches( dates, null );
        this.debug( actual );
        Set<String> s = new HashSet<>( actual.keySet() );
        assertEquals( 1, s.size() );

    }

    @Test
    public void testDatesToBatchD() {
        BatchInfoPopulationHelperServiceImpl ser = new BatchInfoPopulationHelperServiceImpl();
        Calendar cal = Calendar.getInstance();
        cal.set( 2004, Calendar.APRIL, 10, 10, 1, 1 );
        Date d = cal.getTime();

        List<Date> dates = new ArrayList<>();

        dates.add( d );
        dates.add( DateUtils.addHours( d, 200 ) ); // merged back, even though gap is big.
        dates.add( DateUtils.addHours( d, 201 ) ); // merge back
        dates.add( DateUtils.addHours( d, 202 ) ); // merge back
        dates.add( DateUtils.addHours( d, 203 ) ); // merge back
        dates.add( DateUtils.addHours( d, 301 ) ); // new batch
        dates.add( DateUtils.addHours( d, 302 ) ); // merge back
        dates.add( DateUtils.addHours( d, 402 ) ); // singleton merged.

        Map<String, Collection<Date>> actual = ser.convertDatesToBatches( dates, null );
        this.debug( actual );
        Set<String> s = new HashSet<>( actual.keySet() );
        assertEquals( 2, s.size() );

    }

    @Test
    public void testDatesToBatchE() {
        BatchInfoPopulationHelperServiceImpl ser = new BatchInfoPopulationHelperServiceImpl();
        Calendar cal = Calendar.getInstance();
        cal.set( 2004, Calendar.APRIL, 10, 10, 1, 1 );
        Date d = cal.getTime();

        List<Date> dates = new ArrayList<>();

        dates.add( d );
        dates.add( DateUtils.addHours( d, 200 ) ); // merged back, even though gap is big.
        dates.add( DateUtils.addHours( d, 201 ) ); // merge back
        dates.add( DateUtils.addHours( d, 202 ) ); // merge back
        dates.add( DateUtils.addHours( d, 203 ) ); // merge back
        dates.add( DateUtils.addHours( d, 301 ) ); // new batch -- but outlier, so last sample will be merged
        dates.add( DateUtils.addHours( d, 302 ) ); // merge back

        List<Boolean> usable = new ArrayList<>();
        for ( int i = 0; i < dates.size(); i++ ) {
            usable.add( true );
        }
        usable.set(5, false);

        Map<String, Collection<Date>> actual = ser.convertDatesToBatches( dates, usable );
        this.debug( actual );
        Set<String> s = new HashSet<>( actual.keySet() );
        assertEquals( 1, s.size() );

    }

    private void debug( Map<String, Collection<Date>> actual ) {
        if ( BatchInfoPopulationServiceTest.log.isDebugEnabled() ) {
            for ( String st : actual.keySet() ) {
                BatchInfoPopulationServiceTest.log.debug( st + " " + actual.get( st ).size() + " members." );
                for ( Date da : actual.get( st ) ) {
                    BatchInfoPopulationServiceTest.log.debug( da );
                }
            }
        }
    }

}
