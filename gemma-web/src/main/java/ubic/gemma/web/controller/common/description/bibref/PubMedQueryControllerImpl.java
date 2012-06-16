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

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.annotation.reference.BibliographicReferenceService;
import ubic.gemma.loader.entrez.pubmed.PubMedXMLFetcher;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.web.controller.BaseController;

/**
 * Allow users to search for and view PubMed abstracts from NCBI, or from Gemma. Note: do not use parameterized
 * collections as parameters for ajax methods in this class! Type information is lost during proxy creation so DWR can't
 * figure out what type of collection the method should take. See bug 2756. Use arrays instead.
 * 
 * @author pavlidis
 * @version $Id$
 */
@Controller("/bibRefSearch.html")
public class PubMedQueryControllerImpl extends BaseController implements PubMedQueryController {

    @Autowired
    private BibliographicReferenceService bibliographicReferenceService;

    private PubMedXMLFetcher pubMedXmlFetcher = new PubMedXMLFetcher();

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.common.description.bibref.PubMedQueryController#getView()
     */
    @Override
    @RequestMapping(method = RequestMethod.GET)
    public String getView() {
        return "bibRefSearch";
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.common.description.bibref.PubMedQueryController#onSubmit(javax.servlet.http.
     * HttpServletRequest, ubic.gemma.web.controller.common.description.bibref.PubMedSearchCommand,
     * org.springframework.validation.BindingResult, org.springframework.web.bind.support.SessionStatus)
     */
    @Override
    @RequestMapping(method = RequestMethod.POST)
    public ModelAndView onSubmit( HttpServletRequest request, PubMedSearchCommand command, BindingResult result,
            SessionStatus status ) {

        // in the future we can search in other ways.
        String accession = command.getAccession();

        if ( StringUtils.isBlank( accession ) ) {

            result.rejectValue( "search", "errors.pubmed.noaccession", "Accession was missing" );
            return new ModelAndView( "bibRefSearch" );
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

                    result.rejectValue( "accession", "bibliographicReference.notfoundInNCBI", "Not found in NCBI" );
                    return new ModelAndView( "bibRefSearch", result.getModel() );
                }

                this.saveMessage( request, "bibliographicReference.found", accession, "Found" );

            } catch ( NumberFormatException e ) {
                result.rejectValue( "accession", "error.integer", "Not a number" );
                return new ModelAndView( "bibRefSearch", result.getModel() );
            }
        }

        status.setComplete();
        return new ModelAndView( "bibRefView" ).addObject( "bibliographicReference", bibRefFound );
    }

}
