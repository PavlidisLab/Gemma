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
package ubic.gemma.web.flow.bibref;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.DataBinder;
import org.springframework.webflow.Event;
import org.springframework.webflow.RequestContext;
import org.springframework.webflow.ScopeType;

import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceImpl;
import ubic.gemma.model.common.description.BibliographicReferenceService;
import ubic.gemma.model.common.description.FileFormat;
import ubic.gemma.model.common.description.FileFormatService;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.web.flow.AbstractFlowFormAction;

/**
 * Action to add a PDF file to the bibliographic reference.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class PubMedAttachPdfAction extends AbstractFlowFormAction {

    protected final transient Log log = LogFactory.getLog( getClass() );
    private BibliographicReferenceService bibliographicReferenceService;
    private BibliographicReference bibRef = null;
    private FileFormatService fileFormatService;

    /**
     * Programmatically set the domain object, the class is refers to, and the scope.
     */
    public PubMedAttachPdfAction() {
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
        String pubMedId = ( String ) context.getSourceEvent().getAttribute( "pubMedId" );
        context.getFlowScope().setAttribute( "pubMedId", pubMedId );
        bibRef = bibliographicReferenceService.findByExternalId( pubMedId );
        if ( bibRef != null ) context.getRequestScope().setAttribute( "bibliographicReference", bibRef );

        return bibRef;
    }

    /**
     * This is the webflow equivalent of mvc's formBackingObject
     * 
     * @param context
     * @param binder
     */
    @Override
    @SuppressWarnings( { "unused" })
    protected void initBinder( RequestContext context, DataBinder binder ) {

        this.setBindOnSetupForm( true );

        log.info( new Boolean( this.isBindOnSetupForm() ) );

    }

    /**
     * @param context
     * @return
     * @throws Exception
     */
    public Event save( RequestContext context ) throws Exception {
        bibRef.setTitle( ( String ) context.getSourceEvent().getAttribute( "title" ) );
        bibRef.setAbstractText( ( String ) context.getSourceEvent().getAttribute( "abstractText" ) );
        bibRef.setVolume( ( String ) context.getSourceEvent().getAttribute( "volume" ) );

        LocalFile pdf = LocalFile.Factory.newInstance();

        FileFormat pdfFormat = fileFormatService.findByIdentifier( "PDF" );

        assert pdfFormat != null;

        pdf.setFormat( pdfFormat );
        pdf.setLocalURL( new File( ( String ) context.getSourceEvent().getAttribute( "pdfFile" ) ).toURI().toURL() );

        log.info( "ic reference " + bibRef.getPubAccession().getAccession() );

        this.bibliographicReferenceService.addPDF( pdf, bibRef );

        return success();
    }

    /**
     * @param bibliographicReferenceService The bibliographicReferenceService to set.
     */
    public void setBibliographicReferenceService( BibliographicReferenceService bibliographicReferenceService ) {
        this.bibliographicReferenceService = bibliographicReferenceService;
    }

    /**
     * @param fileFormatService The fileFormatService to set.
     */
    public void setFileFormatService( FileFormatService fileFormatService ) {
        this.fileFormatService = fileFormatService;
    }
}
