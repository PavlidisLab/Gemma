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

    /**
     * Programmatically set the form object, the class is refers to, and the scope. This is an alternative to the
     * following XDoclet tags: (with the 'at' symbols removed to avoid interpretation by the XDoclet parser):
     * <p>
     * spring.property name="formObjectClass" value="edu.columbia.gemma.common.description.BibliographicReferenceImpl"
     * <p>
     * spring.property name="formObjectName" value="bibliographicReference"
     * <p>
     * spring.property name="formObjectScopeAsString" value="flow"
     * <p>
     * These will end up in the bean definition in actin-servlet.xml. DO NOT use both methods!
     */
    public BibRefFormEditAction() {
        setFormObjectName( "bibliographicReference" );
        setFormObjectClass( BibliographicReferenceImpl.class );
        setFormObjectScope( ScopeType.FLOW );
    }

    /**
     * This is the webflow equivalent of mvc's formBackingObject
     * 
     * @param context
     */
    @Override
    public Object loadFormObject( RequestContext context ) {
        // depending on where we came from, we either get the object, or we get a pubmed id.
        BibliographicReference bibRef = ( BibliographicReference ) context.getFlowScope().getAttribute(
                "bibliographicReference" );
        if ( bibRef == null ) {
            String accession = ( String ) context.getSourceEvent().getAttribute( "accession" );
            if ( accession == null ) {
                throw new IllegalStateException( "Accession for Bibliographic Reference was null" );
            }
            if ( log.isDebugEnabled() ) {
                log.debug( "Seeking accession " + accession );
            }
            bibRef = bibliographicReferenceService.findByExternalId( accession );
        }

        if ( bibRef == null ) {
            throw new IllegalStateException( "Could not get bibliographicReference for editing" );
        }

        return bibRef;
    }

    /**
     * User has changed their mind about editing. Bail out.
     * 
     * @param context
     * @return
     * @throws Exception
     */
    public Event cancel( RequestContext context ) throws Exception {
        String accession = ( String ) context.getSourceEvent().getParameter( "pubAccession.accession" );
        if ( accession == null ) {
            this.getFormErrors( context ).reject( "error.noCriteria", "You must enter an accession number." );
            return error(new IllegalArgumentException() );
        }
        context.getRequestScope().setAttribute( "existsInSystem", Boolean.TRUE );
        context.getRequestScope().setAttribute( "accession", accession );
        addMessage( context, "bibliographicReference.alreadyInSystem", new Object[] { accession } );
        return success();
    }

    /**
     * 
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
    public Event update( RequestContext context ) throws Exception {
        BibliographicReference bibRef = ( BibliographicReference ) context.getFlowScope().getRequiredAttribute(
                "bibliographicReference" );

        // edited fields are automatically bound this. way.

        log.info( "Updating bibliographic reference " + bibRef.getPubAccession().getAccession() );

        this.bibliographicReferenceService.updateBibliographicReference( bibRef );

        context.getRequestScope().setAttribute( "accession", bibRef.getPubAccession().getAccession() );
        context.getRequestScope().setAttribute( "bibliographicReference", bibRef );
        addMessage( context, "bibliographicReference.updated", new Object[] { bibRef.getPubAccession().getAccession() } );

        return success();
    }

    /**
     * @param bibliographicReferenceService
     */
    public void setBibliographicReferenceService( BibliographicReferenceService bibliographicReferenceService ) {
        this.bibliographicReferenceService = bibliographicReferenceService;
    }

}