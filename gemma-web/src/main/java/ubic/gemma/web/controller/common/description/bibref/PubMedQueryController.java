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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.loader.entrez.pubmed.PubMedXMLFetcher;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceService;
import ubic.gemma.web.controller.BaseFormController;

/**
 * Allow users to search for and view PubMed abstracts from NCBI, or from Gemma.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class PubMedQueryController extends BaseFormController {
    private BibliographicReferenceService bibliographicReferenceService;

    private PubMedXMLFetcher pubMedXmlFetcher = new PubMedXMLFetcher();

    public void setBibliographicReferenceService( BibliographicReferenceService bibliographicReferenceService ) {
        this.bibliographicReferenceService = bibliographicReferenceService;
    }

    /**
     * @param pubMedXmlFetcher The pubMedXmlFetcher to set.
     */
    public void setPubMedXmlFetcher( PubMedXMLFetcher pubMedXmlFetcher ) {
        this.pubMedXmlFetcher = pubMedXmlFetcher;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
     * javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
     */
    @Override
    protected ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {
        PubMedSearchCommand search = ( PubMedSearchCommand ) command;

        // in the future we can search in other ways.
        String accession = search.getAccession();

        if ( StringUtils.isBlank( accession ) ) {
            errors.rejectValue( "search", "errors.pubmed.noaccession", "Accession was missing" );
            return showForm( request, response, errors );
        }

        // first see if we already have it in the system.
        BibliographicReference bibRefFound = bibliographicReferenceService.findByExternalId( accession );
        if ( bibRefFound != null ) {
            request.setAttribute( "existsInSystem", Boolean.TRUE );
            this.saveMessage( request, "bibliographicReference.alreadyInSystem", accession, "Already in Gemma" );
        } else {
            request.setAttribute( "existsInSystem", Boolean.FALSE );
            try {
                bibRefFound = this.pubMedXmlFetcher.retrieveByHTTP( Integer.parseInt( accession ) );

                if ( bibRefFound == null ) {
                    log.debug( accession + " not found in NCBI" );

                    errors.rejectValue( "accession", "bibliographicReference.notfoundInNCBI", "Not found in NCBI" );
                    return showForm( request, response, errors );
                }

                this.saveMessage( request, "bibliographicReference.found", accession, "Found" );

            } catch ( NumberFormatException e ) {
                errors.rejectValue( "accession", "error.integer", "Not a number" );
                return showForm( request, response, errors );
            } catch ( IOException e ) {
                if ( e.getStackTrace()[1].getFileName().startsWith( "HttpURLConnection" )
                        || e.getMessage().startsWith( "Server returned HTTP response code 502" ) ) {
                    errors.reject( "ncbi.error.502", "NCBI server was not accessible" );
                    return showForm( request, response, errors );
                }
            }
        }
        return new ModelAndView( getSuccessView() ).addObject( "bibliographicReference", bibRefFound );
    }

}
