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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.DataBinder;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.springframework.webflow.Event;
import org.springframework.webflow.RequestContext;
import org.springframework.webflow.ScopeType;
import org.springframework.webflow.execution.servlet.ServletEvent;

import ubic.gemma.Constants;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceImpl;
import ubic.gemma.model.common.description.BibliographicReferenceService;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.common.description.LocalFileService;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.web.flow.AbstractFlowFormAction;

/**
 * This webflow action bean is used to handle editing of BibliographicReference form data.
 * 
 * @spring.bean id="bibRefFormEditAction"
 * @spring.property name="bibliographicReferenceService" ref="bibliographicReferenceService"
 * @spring.property name="localFileService" ref="localFileService"
 * @author keshav
 * @author pavlidis
 * @version $Id$
 */
public class BibRefFormEditAction extends AbstractFlowFormAction {
    protected final transient Log log = LogFactory.getLog( getClass() );
    private BibliographicReferenceService bibliographicReferenceService;
    private LocalFileService localFileService;

    /**
     * Programmatically set the form object, the class is refers to, and the scope. This is an alternative to the
     * following XDoclet tags: (with the 'at' symbols removed to avoid interpretation by the XDoclet parser):
     * <p>
     * spring.property name="formObjectClass" value="ubic.gemma.model.common.description.BibliographicReferenceImpl"
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
            return error( new IllegalArgumentException() );
        }
        context.getRequestScope().setAttribute( "existsInSystem", Boolean.TRUE );
        context.getRequestScope().setAttribute( "accession", accession );
        addMessage( context, "bibliographicReference.alreadyInSystem", new Object[] { accession } );
        return success();
    }

    /**
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

        // / Deal with the file.
        CommonsMultipartFile file = ( CommonsMultipartFile ) context.getSourceEvent().getAttribute( "pdfFile" );

        if ( file == null ) {
            throw new IOException( "File was null" );
        }

        String baseOutputPath = ConfigUtils.getProperty( "local.userfile.basepath" );
        String user = ( ( ServletEvent ) context.getSourceEvent() ).getRequest().getRemoteUser();
        String uploadDir = baseOutputPath + user + "/";

        File dirPath = new File( uploadDir );
        File saveToFile = new File( uploadDir + file.getOriginalFilename() );

        try {
            uploadFile( file, dirPath, saveToFile );
        } catch ( IOException e ) {
            // this.getFormErrors( context ).reject( e.getLocalizedMessage() );
            // log.error( e, e );
            // return error( e );
            throw new RuntimeException( e );
        }

        /* place the data in flow scope to be used by the next view state */
        context.getFlowScope().setAttribute( "fileName", file.getOriginalFilename() );
        context.getFlowScope().setAttribute( "contentType", file.getContentType() );
        context.getFlowScope().setAttribute( "size", file.getSize() + " bytes" );
        context.getFlowScope().setAttribute( "location",
                dirPath.getAbsolutePath() + Constants.FILE_SEP + file.getOriginalFilename() );

        String link = uploadDir;

        context.getFlowScope().setAttribute( "link", link + file.getOriginalFilename() );

        log.warn( "Uploaded file!" );
        addMessage( context, "display.title", new Object[] {} );
        // end dealing with the file.

        LocalFile pdf = LocalFile.Factory.newInstance();
        pdf.setSize( file.getSize() );
        pdf.setLocalURL( saveToFile.toURI().toURL() );

        pdf = localFileService.findOrCreate( pdf );

        bibRef.setFullTextPDF( pdf );

        this.bibliographicReferenceService.updateBibliographicReference( bibRef );

        context.getRequestScope().setAttribute( "accession", bibRef.getPubAccession().getAccession() );
        context.getRequestScope().setAttribute( "bibliographicReference", bibRef );
        addMessage( context, "bibliographicReference.updated", new Object[] { bibRef.getPubAccession().getAccession() } );

        return success();
    }

    /**
     * @param file
     * @param dirPath
     * @param saveToFile
     * @throws IOException
     * @throws FileNotFoundException
     */
    private void uploadFile( CommonsMultipartFile file, File dirPath, File saveToFile ) throws IOException,
            FileNotFoundException {
        if ( file.getBytes().length == 0 ) {
            throw new IOException( "File was empty" );
        }

        if ( !dirPath.exists() ) {
            boolean success = dirPath.mkdirs();
            if ( !success ) {
                throw new IOException( "Could not make output directory " + dirPath );
            }
        } else if ( !dirPath.canRead() ) {
            throw new IOException( "Cannot accession output directory" + dirPath );
        }

        InputStream stream = file.getInputStream();
        log.info( "Saving file to " + saveToFile.getAbsolutePath() );
        OutputStream bos = new FileOutputStream( saveToFile );
        int bytesRead = 0;
        byte[] buffer = new byte[8192];
        while ( ( bytesRead = stream.read( buffer, 0, 8192 ) ) != -1 ) {
            bos.write( buffer, 0, bytesRead );
        }

        bos.close();

        stream.close();
    }

    /**
     * @param bibliographicReferenceService
     */
    public void setBibliographicReferenceService( BibliographicReferenceService bibliographicReferenceService ) {
        this.bibliographicReferenceService = bibliographicReferenceService;
    }

    /**
     * @param localFileService The localFileService to set.
     */
    public void setLocalFileService( LocalFileService localFileService ) {
        this.localFileService = localFileService;
    }

}