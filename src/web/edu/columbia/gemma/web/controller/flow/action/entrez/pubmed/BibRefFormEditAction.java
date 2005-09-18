/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.web.controller.flow.action.entrez.pubmed;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.DataBinder;
import org.springframework.webflow.Event;
import org.springframework.webflow.RequestContext;
import org.springframework.webflow.ScopeType;
import org.springframework.webflow.action.FormAction;

import edu.columbia.gemma.common.description.BibliographicReference;
import edu.columbia.gemma.common.description.BibliographicReferenceImpl;
import edu.columbia.gemma.common.description.BibliographicReferenceService;

/**
 * Webflow. This webflow action bean is used to handle editing of BibliographicReference form data.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class BibRefFormEditAction extends FormAction {
    protected final transient Log log = LogFactory.getLog( getClass() );
    private BibliographicReferenceService bibliographicReferenceService;
    private BibliographicReference br = null;

    /**
     * Programmatically set the domain object, the class is refers to, and the scope.
     */
    public BibRefFormEditAction() {
        setFormObjectName( "bibliographicReference" );
        setFormObjectClass( BibliographicReferenceImpl.class );
        setFormObjectScope( ScopeType.FLOW );
    }

    /**
     * flowScope - attributes in the flowScope are available for the duration of the flow requestScope - attributes in
     * the requestScope are available for the duration of the request sourceEvent - this is the event that originated
     * the request. SourceEvent contains parameters provided as input by the client.
     * 
     * @param context
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object createFormObject( RequestContext context ) {

        if ( log.isInfoEnabled() ) logScopes( context );
        // String name = ( String ) context.getFlowScope().getRequiredAttribute( "name", String.class );

        String pubMedId = ( String ) context.getSourceEvent().getAttribute( "pubMedId" );
        context.getFlowScope().setAttribute( "pubMedId", pubMedId );

        // FIXME again, getBibliographicReferenceService().findByExternalId(pubMedId) returns null.
        // Fix this and put it here instead of getting all arrayDesigns.
        Collection<BibliographicReference> col = getBibliographicReferenceService().getAllBibliographicReferences();
        for ( BibliographicReference b : col ) {
            if ( b.getPubAccession().getAccession().equals( pubMedId ) ) {
                br = b;
                break;
            }
        }

        if ( br != null ) context.getRequestScope().setAttribute( "bibliographicReference", br );

        return br;
    }

    /**
     * This is the webflow equivalent of mvc's formBackingObject
     * 
     * @param context
     * @param binder
     */
    @Override
    @SuppressWarnings( { "boxing", "unused" })
    protected void initBinder( RequestContext context, DataBinder binder ) {

        this.setBindOnSetupForm( true );

        log.info( ( this.isBindOnSetupForm() ) );

    }

    /**
     * @param context
     * @return
     * @throws Exception
     */
    public Event save( RequestContext context ) throws Exception {

        if ( log.isInfoEnabled() ) logScopes( context );

        br.setTitle( ( String ) context.getSourceEvent().getAttribute( "title" ) );
        br.setAbstractText( ( String ) context.getSourceEvent().getAttribute( "abstractText" ) );
        br.setVolume( ( String ) context.getSourceEvent().getAttribute( "volume" ) );
        // br.setName( ( String ) context.getFlowScope().getAttribute( "pubMedId", String.class ) );

        log.info( "updating bibliographic reference " + br.getPubAccession().getAccession() );

        getBibliographicReferenceService().updateBibliographicReference( br );

        return success();
    }

    private void logScopes( RequestContext context ) {

        log.info( "originating event: " + context.getSourceEvent() );
        log.info( "flow scope: " + context.getFlowScope().getAttributeMap() );
        log.info( "request scope: " + context.getRequestScope().getAttributeMap() );
    }

    /**
     * @param bibliographicReferenceService
     */
    public void setBibliographicReferenceService( BibliographicReferenceService bibliographicReferenceService ) {
        this.bibliographicReferenceService = bibliographicReferenceService;
    }

    /**
     * @return
     */
    public BibliographicReferenceService getBibliographicReferenceService() {
        return bibliographicReferenceService;
    }
}