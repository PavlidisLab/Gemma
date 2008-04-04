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
package ubic.gemma.web.controller.common.description.bibref;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceService;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.common.description.LocalFileService;
import ubic.gemma.util.BeanPropertyCompleter;
import ubic.gemma.web.controller.BaseFormController;

/**
 * Supports editing of bibliographic references.
 * 
 * @spring.bean id="bibliographicReferenceEditFormController"
 * @spring.property name="commandClass" value="ubic.gemma.model.common.description.BibliographicReference"
 * @spring.property name="commandName" value="bibliographicReference"
 * @spring.property name="formView" value="bibRefEdit"
 * @spring.property name="successView" value="bibRefView"
 * @spring.property name="validator" ref="bibliographicReferenceValidator"
 * @spring.property name="bibliographicReferenceService" ref="bibliographicReferenceService"
 * @spring.property name="externalDatabaseService" ref="externalDatabaseService"
 * @spring.property name="localFileService" ref="localFileService"
 * @author pavlidis
 * @version $Id$
 */
public class BibliographicReferenceEditFormController extends BaseFormController {
    private BibliographicReferenceService bibliographicReferenceService;
    private LocalFileService localFileService;
    private static final String PUB_MED = "PubMed";
    private ExternalDatabaseService externalDatabaseService;

    /**
     * @param externalDatabaseService the externalDatabaseService to set
     */
    public void setExternalDatabaseService( ExternalDatabaseService externalDatabaseService ) {
        this.externalDatabaseService = externalDatabaseService;
    }

    /**
     * @param bibliographicReferenceService the bibliographicReferenceService to set
     */
    public void setBibliographicReferenceService( BibliographicReferenceService bibliographicReferenceService ) {
        this.bibliographicReferenceService = bibliographicReferenceService;
    }

    /**
     * @param localFileService the localFileService to set
     */
    public void setLocalFileService( LocalFileService localFileService ) {
        this.localFileService = localFileService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
     */
    @Override
    protected ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        BibliographicReference bibRef = ( BibliographicReference ) command;

        // fix this later
        // FileUploadUtil.uploadFile( request, fileUpload, "file" );

        if ( bibRef.getId() == null ) {

            BibliographicReference bibRefFound = this.bibliographicReferenceService.find( bibRef );

            if ( bibRefFound != null ) {
                // hmm. what to do. It's in the system already. Really should do an update.
                BeanPropertyCompleter.complete( bibRefFound, bibRef );
                bibliographicReferenceService.update( bibRefFound );
                saveMessage( request, "object.updated", new Object[] { "Reference",
                        bibRef.getPubAccession().getAccession() }, "Updated" );

            } else {

                // fill in the accession and the external database.
                if ( bibRef.getPubAccession() == null ) {
                    DatabaseEntry dbEntry = DatabaseEntry.Factory.newInstance();
                    dbEntry.setAccession( bibRef.getPubAccession().getAccession() );
                    bibRef.setPubAccession( dbEntry );
                }
                ExternalDatabase pubMedDb = this.externalDatabaseService.find( PUB_MED );

                if ( pubMedDb == null ) {
                    log.error( "There was no external database '" + PUB_MED + "'" );
                    errors.rejectValue( "bibRef", "external.database.missing", new Object[] { "PubMed" },
                            "No pubmed database" );
                    return showForm( request, response, errors );
                }

                bibRef.getPubAccession().setExternalDatabase( pubMedDb );
                bibRef = bibliographicReferenceService.create( bibRef );
                saveMessage( request, "object.saved", new Object[] { "Reference",
                        bibRef.getPubAccession().getAccession() }, "Created" );

            }
        } else {
            bibliographicReferenceService.update( bibRef );
            saveMessage( request, "object.updated",
                    new Object[] { "Reference", bibRef.getPubAccession().getAccession() }, "Updated" );
        }

        return new ModelAndView( getSuccessView() ).addObject( "bibliographicReference", bibRef );
    }

    private void upLoadPdf() {
        // String baseOutputPath = ConfigUtils.getString( "local.userfile.basepath" );
        // String user = ( ( ServletEvent ) context.getSourceEvent() ).getRequest().getRemoteUser();
        // String uploadDir = baseOutputPath + user + "/";
        //
        // File dirPath = new File( uploadDir );
        // File saveToFile = new File( uploadDir + file.getOriginalFilename() );
        //
        // try {
        // uploadFile( file, dirPath, saveToFile );
        // } catch ( IOException e ) {
        // // this.getFormErrors( context ).reject( e.getLocalizedMessage() );
        // // log.error( e, e );
        // // return error( e );
        // throw new RuntimeException( e );
        // }
        //
        // /* place the data in flow scope to be used by the next view state */
        // context.getFlowScope().setAttribute( "fileName", file.getOriginalFilename() );
        // context.getFlowScope().setAttribute( "contentType", file.getContentType() );
        // context.getFlowScope().setAttribute( "size", file.getSize() + " bytes" );
        // context.getFlowScope().setAttribute( "location",
        // dirPath.getAbsolutePath() + Constants.FILE_SEP + file.getOriginalFilename() );
        //
        // String link = uploadDir;
        //
        // context.getFlowScope().setAttribute( "link", link + file.getOriginalFilename() );
        //
        // log.warn( "Uploaded file!" );
        // addMessage( context, "display.title", new Object[] {} );
        // // end dealing with the file.
        //
        // LocalFile pdf = LocalFile.Factory.newInstance();
        // pdf.setSize( file.getSize() );
        // pdf.setLocalURL( saveToFile.toURI().toURL() );
        //
        // pdf = localFileService.findOrCreate( pdf );
        //
        // bibRef.setFullTextPDF( pdf );
        //
        // this.bibliographicReferenceService.updateBibliographicReference( bibRef );
        //
        // context.getRequestScope().setAttribute( "accession", bibRef.getPubAccession().getAccession() );
        // context.getRequestScope().setAttribute( "bibliographicReference", bibRef );
        // addMessage( context, "bibliographicReference.updated", new Object[] { bibRef.getPubAccession().getAccession()
        // } );
    }

    /**
     * 
     */
    @Override
    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response,
            Object command, BindException errors ) throws Exception {
        if ( request.getParameter( "cancel" ) != null ) {
            return new ModelAndView( new RedirectView( "mainMenu.html" ) );
        }

        return super.processFormSubmission( request, response, command, errors );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
     */
    @Override
    protected Object formBackingObject( HttpServletRequest request ) throws Exception {
        BibliographicReference bibRef = null;
        String ids = request.getParameter( "id" );
        if ( ids != null ) {
            Long id = Long.parseLong( ids );
            bibRef = bibliographicReferenceService.load( id );
            if ( bibRef == null ) {
                bibRef = BibliographicReference.Factory.newInstance();
            }
        }
        return bibRef;
    }
}
