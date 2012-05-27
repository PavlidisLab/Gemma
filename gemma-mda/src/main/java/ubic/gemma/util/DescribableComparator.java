/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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

import java.util.Comparator;

import ubic.gemma.model.common.Describable;

/**
 * @author luke
 */
public class DescribableComparator implements Comparator<Describable> {
    private static DescribableComparator _instance = new DescribableComparator();

    public static DescribableComparator getInstance() {
        return _instance;
    }

    /*
     * (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare( Describable d1, Describable d2 ) {
        String s1 = d1.getName();
        String s2 = d2.getName();
        if ( s1 != null ) {
            if ( s2 != null ) return s1.compareTo( s2 );

            return 1;
        }
        if ( s2 != null ) return -1;

        return 0;

    }
}
