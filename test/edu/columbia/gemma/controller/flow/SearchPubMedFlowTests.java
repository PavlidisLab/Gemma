package edu.columbia.gemma.controller.flow;

import java.util.HashMap;
import java.util.Map;

import org.springframework.test.web.flow.AbstractFlowExecutionTests;
import org.springframework.web.flow.SimpleEvent;
import org.springframework.web.flow.ViewDescriptor;

/**
 * Tests the pubMedSearch-flow.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class SearchPubMedFlowTests extends AbstractFlowExecutionTests {
    /**
     * 
     *
     */
    public SearchPubMedFlowTests() {
        setDependencyCheck( false );
    }

    /**
     * Test criteria.view with an invalid argument. Expect to return to the criteria.view.
     */
    //TODO add validator object. Fail on validation should take me back to the criteria.view.
    //    public void testCriteriaView_Submit_Error() {
    //        startFlow();
    //        ViewDescriptor view = signalEvent( new SimpleEvent( this, "submit", null ) );
    //        assertCurrentStateEquals( "criteria.view" );
    //    }
    /**
     * Test criteria.view with a valid argument. Expect the results.view to be entered. Expect 1 bibliographic reference
     * to be returned from 1 pubMed Id.
     */
    public void testCriteriaView_Submit_Success() {
        startFlow();
        Map properties = new HashMap();
        properties.put( "pubMedId", "15173114" );
        ViewDescriptor view = signalEvent( new SimpleEvent( this, "submit", properties ) );
        assertCurrentStateEquals( "results.view" );
        asserts().assertCollectionAttributeSize( view, "bibliographicReferences", 1 );
    }

    public void testStartFlow() {
        startFlow();
        assertCurrentStateEquals( "criteria.view" );
    }

    protected String flowId() {
        return "pubMed.Search";
    }

    protected String[] getConfigLocations() {
        String[] paths = { "applicationContext-dataSource.xml", "applicationContext-hibernate.xml",
                "loader-servlet.xml" };
        return paths;
    }

}