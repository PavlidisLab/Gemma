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

import ubic.gemma.annotation.reference.BibliographicReferenceService;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.util.BeanPropertyCompleter;
import ubic.gemma.web.controller.BaseFormController;
import ubic.gemma.web.controller.WebConstants;

/**
 * Supports editing of bibliographic references.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class BibliographicReferenceEditFormController extends BaseFormController {
    private BibliographicReferenceService bibliographicReferenceService;
    private static final String PUB_MED = "PubMed";
    private ExternalDatabaseService externalDatabaseService;

    /**
     * 
     */
    @Override
    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response,
            Object command, BindException errors ) throws Exception {
        if ( request.getParameter( "cancel" ) != null ) {
            return new ModelAndView( new RedirectView( WebConstants.HOME_PAGE ) );
        }

        return super.processFormSubmission( request, response, command, errors );
    }

    /**
     * @param bibliographicReferenceService the bibliographicReferenceService to set
     */
    public void setBibliographicReferenceService( BibliographicReferenceService bibliographicReferenceService ) {
        this.bibliographicReferenceService = bibliographicReferenceService;
    }

    /**
     * @param externalDatabaseService the externalDatabaseService to set
     */
    public void setExternalDatabaseService( ExternalDatabaseService externalDatabaseService ) {
        this.externalDatabaseService = externalDatabaseService;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest
     * )
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

    /*
     * (non-Javadoc)
     * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
     * javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
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

}
