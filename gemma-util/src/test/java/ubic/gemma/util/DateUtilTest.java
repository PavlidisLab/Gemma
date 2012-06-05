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
package ubic.gemma.util;

import java.util.Date;

import org.apache.commons.lang.time.DateUtils;

import junit.framework.TestCase;

/**
 * @author pavlidis
 * @version $Id$
 */
public class DateUtilTest extends TestCase {

    public void testgetRelativeDateDayago() {

        Date now = new Date();

        Date expectedValue = DateUtils.addDays( now, -1 );
        Date actualValue = DateUtil.getRelativeDate( now, "-1d" );

        assertEquals( expectedValue, actualValue );

    }

    public void testgetRelativeTomorrow() {

        Date now = new Date();

        Date expectedValue = DateUtils.addDays( now, 1 );
        Date actualValue = DateUtil.getRelativeDate( now, "1d" );

        assertEquals( expectedValue, actualValue );

    }

    public void testgetRelative5yearsago() {

        Date now = new Date();

        Date expectedValue = DateUtils.addYears( now, -5 );
        Date actualValue = DateUtil.getRelativeDate( now, "-5y" );

        assertEquals( expectedValue, actualValue );

    }

    public void testgetRelative5yearsFromnow() {

        Date now = new Date();

        Date expectedValue = DateUtils.addYears( now, 5 );
        Date actualValue = DateUtil.getRelativeDate( now, "+5y" );

        assertEquals( expectedValue, actualValue );

    }
}
