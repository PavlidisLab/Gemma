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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;
import ubic.gemma.core.loader.entrez.pubmed.PubMedSearch;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.persistence.service.common.description.BibliographicReferenceService;
import ubic.gemma.web.controller.util.MessageUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Allow users to search for and view PubMed abstracts from NCBI, or from Gemma.
 *
 * @author pavlidis
 */
@Controller
@RequestMapping("/bibRefSearch.html")
public class PubMedQueryController implements InitializingBean {

    protected final Log log = LogFactory.getLog( getClass().getName() );
    @Autowired
    protected MessageSource messageSource;
    @Autowired
    protected MessageUtil messageUtil;
    @Autowired
    private BibliographicReferenceService bibliographicReferenceService;

    @Value("${entrez.efetch.apikey}")
    private String ncbiApiKey;

    private PubMedSearch pubMedXmlFetcher;

    @Override
    public void afterPropertiesSet() {
        pubMedXmlFetcher = new PubMedSearch( ncbiApiKey );
    }

    @RequestMapping(method = { RequestMethod.GET, RequestMethod.HEAD })
    public String getView() {
        return "bibRefSearch";
    }

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
            this.messageUtil.saveMessage( "bibliographicReference.alreadyInSystem", accession, "Already in Gemma" );
        } else {
            request.setAttribute( "existsInSystem", Boolean.FALSE );
            int pubMedId;
            try {
                pubMedId = Integer.parseInt( accession );
            } catch ( NumberFormatException e ) {
                result.rejectValue( "accession", "error.integer", "Not a number" );
                return new ModelAndView( "bibRefSearch", result.getModel() );
            }
            try {
                bibRefFound = this.pubMedXmlFetcher.retrieve( String.valueOf( pubMedId ) );
            } catch ( IOException e ) {
                log.error( "Failed to retrieve bibliographic reference from PubMed with ID: " + pubMedId, e );
            }

            if ( bibRefFound == null ) {
                log.debug( pubMedId + " not found in NCBI" );

                result.rejectValue( "accession", "bibliographicReference.notfoundInNCBI", "Not found in NCBI" );
                return new ModelAndView( "bibRefSearch", result.getModel() );
            }

            this.messageUtil.saveMessage( "bibliographicReference.found", accession, "Found" );
        }

        status.setComplete();
        return new ModelAndView( "bibRefView" )
                .addObject( "bibliographicReference", bibRefFound );
    }

}
