package edu.columbia.gemma.web.controller.flow.entrez.pubmed;

import org.springframework.web.flow.SimpleEvent;
import org.springframework.web.flow.ViewDescriptor;

import edu.columbia.gemma.BaseFlowTestCase;

/**
 * Tests the pubMedSearch-flow.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class DetailBibRefFlowTests extends BaseFlowTestCase {
    /**
     * 
     *
     */
    public DetailBibRefFlowTests() {
        setDependencyCheck( false );
    }

    /**
     * Test criteria.view with an invalid argument. Expect to return to the criteria.view.
     */
    // TODO add validator object. Fail on validation should take me back to the criteria.view.
    // public void testCriteriaView_Submit_Error() {
    // startFlow();
    // ViewDescriptor view = signalEvent( new SimpleEvent( this, "submit", null ) );
    // assertCurrentStateEquals( "criteria.view" );
    // }
    /**
     * Test criteria.view with a valid argument. Expect the results.view to be entered. Expect 1 bibliographic reference
     * to be returned from 1 pubMed Id.
     */
    public void testCriteriaView_Submit_getBibRef_Success() {
        startFlow();
        ViewDescriptor view = signalEvent( new SimpleEvent( this, "getBibRef" ) );

        assertCurrentStateEquals( "bibRef.results.view" );
        // asserts().assertCollectionAttributeSize( view, "bibliographicReferences", 7 );
    }

    public void testStartFlow() {
        startFlow();
        assertCurrentStateEquals( "criteria.view" );
    }

    protected String flowId() {
        return "pubMed.Search";
    }
}