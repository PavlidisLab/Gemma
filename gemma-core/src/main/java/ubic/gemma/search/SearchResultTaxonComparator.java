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
package ubic.gemma.search;

import java.util.Comparator;

/**
 * @author tvrossum
 * @version $Id$
 */
public class SearchResultTaxonComparator implements Comparator<SearchResultDisplayObject> {

    @Override
    public int compare( SearchResultDisplayObject o1, SearchResultDisplayObject o2 ) {
        return ( o1.getTaxonId() < o2.getTaxonId() ? -1
                : ( o1.getTaxonId() == o2.getTaxonId() ? o1.compareTo( o2 ) : 1 ) );
    }
}