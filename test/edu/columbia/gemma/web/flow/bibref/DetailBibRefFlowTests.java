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
package edu.columbia.gemma.web.flow.bibref;

import java.util.HashMap;
import java.util.Map;

import org.springframework.webflow.Event;

import edu.columbia.gemma.BaseFlowTestCase;

/**
 * Tests the pubMedSearch and edit flows.
 * 
 * @author keshav
 * @version $Id$
 */
public class DetailBibRefFlowTests extends BaseFlowTestCase {

    public DetailBibRefFlowTests() {
        setDependencyCheck( false );
    }

    public void testEditing() throws Exception {

        // need to to add this to the request context before the flow starts.
        // BibliographicReference bibRef = BibliographicReference.Factory.newInstance();
        // DatabaseEntry de = DatabaseEntry.Factory.newInstance();
        // de.setAccession( "15173114" );
        // bibRef.setPubAccession( de );
        // getFlowContext().getActiveSession().getScope().setAttribute( "bibliographicReference", bibRef );

        startFlow(); // this does a db lookup of the pubmed id by calling BibRefFormEditAction.createFormObject().

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put( "pubMedId", "15173114" );

        assertCurrentStateEquals( "pubMed.Edit.view" );

        this.signalEvent( new Event( this, "submit", properties ) );
        assertCurrentStateEquals( "save" );

    }

    protected String flowId() {
        return "pubMed.Edit";
    }
}