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
package ubic.gemma.web.taglib.displaytag;

import java.lang.reflect.Method;

import org.displaytag.decorator.TableDecorator;

import ubic.gemma.search.SearchResult;

/**
 * @author paul
 * @version $Id$
 */
public class SearchResultDecorator extends TableDecorator {

    public String getName() {
        SearchResult sr = ( SearchResult ) getCurrentRowObject();
        Object o = sr.getResultObject();
        try {
            Method m = o.getClass().getMethod( "getName", new Class[] {} );
            return ( String ) m.invoke( o, new Object[] {} );
        } catch ( Exception e ) {
            return "?";
        }

    }

    public String getDescription() {
        SearchResult sr = ( SearchResult ) getCurrentRowObject();
        Object o = sr.getResultObject();
        try {
            Method m = o.getClass().getMethod( "getDescription", new Class[] {} );
            return ( String ) m.invoke( o, new Object[] {} );
        } catch ( Exception e ) {
            return "?";
        }
    }

}
