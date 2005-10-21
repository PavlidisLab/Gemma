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
package edu.columbia.gemma.web.flow.bibref;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.DataBinder;
import org.springframework.webflow.Event;
import org.springframework.webflow.RequestContext;
import org.springframework.webflow.ScopeType;

import edu.columbia.gemma.common.description.BibliographicReference;
import edu.columbia.gemma.common.description.BibliographicReferenceImpl;
import edu.columbia.gemma.common.description.BibliographicReferenceService;
import edu.columbia.gemma.web.flow.AbstractFlowFormAction;

/**
 * This webflow action bean is used to handle editing of BibliographicReference form data.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @spring.bean id="bibRefFormEditAction"
 * @spring.property name="bibliographicReferenceService" ref="bibliographicReferenceService"
 * @author keshav
 * @author pavlidis
 * @version $Id$
 */
public class BibRefFormEditAction extends AbstractFlowFormAction {
    protected final transient Log log = LogFactory.getLog( getClass() );
    private BibliographicReferenceService bibliographicReferenceService;
    private BibliographicReference bibRef = null;

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
    public Object createFormObject( RequestContext context ) {
        return bibliographicReferenceService.findByExternalId( ( String ) context.getSourceEvent().getAttribute(
                "pubMedId" ) );
    }

    /**
     * This is the webflow equivalent of mvc's formBackingObject
     * 
     * @param context
     * @param binder
     */
    @Override
    @SuppressWarnings("unused")
    protected void initBinder( RequestContext context, DataBinder binder ) {
        this.setBindOnSetupForm( true );
    }

    /**
     * After editing a bibliographic reference, persist the changes.
     * 
     * @param context
     * @return
     * @throws Exception
     */
    public Event save( RequestContext context ) throws Exception {

        // copy all attributes that we get from the form. Can't we do this binding automatically?
        bibRef.setTitle( ( String ) context.getSourceEvent().getAttribute( "title" ) );
        bibRef.setAbstractText( ( String ) context.getSourceEvent().getAttribute( "abstractText" ) );
        bibRef.setVolume( ( String ) context.getSourceEvent().getAttribute( "volume" ) );

        log.info( "Updating bibliographic reference " + bibRef.getPubAccession().getAccession() );

        this.bibliographicReferenceService.updateBibliographicReference( bibRef );

        addMessage( context, "bibliographicReference.updated", null );

        return success();
    }

    /**
     * @param bibliographicReferenceService
     */
    public void setBibliographicReferenceService( BibliographicReferenceService bibliographicReferenceService ) {
        this.bibliographicReferenceService = bibliographicReferenceService;
    }

}