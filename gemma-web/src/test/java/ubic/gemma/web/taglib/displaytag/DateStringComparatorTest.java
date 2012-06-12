/*
 * The Gemma project
 * 
 * Copyright (c) 2007 Columbia University
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
package ubic.gemma.web.taglib.displaytag;

import junit.framework.TestCase;

/**
 * @author Paul
 * @version $Id$
 */
public class DateStringComparatorTest extends TestCase {

    public void testCompare1() {

        DateStringComparator d = new DateStringComparator();
        int a = d.compare( "<span title='2007-04-10 13:14:03.0'>2007-04-10</span>",
                "<span title='2007-04-10 13:14:03.0'>2007-04-10</span>" );
        assertEquals( 0, a );

    }

}
