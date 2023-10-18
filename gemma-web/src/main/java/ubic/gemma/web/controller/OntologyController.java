package ubic.gemma.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.core.ontology.providers.GemmaOntologyService;
import ubic.gemma.web.util.EntityNotFoundException;

/**
 * Provide minimal support for exposing Gemma ontology.
 * @author poirigui
 */
@Controller
public class OntologyController {

    @Autowired
    private GemmaOntologyService gemmaOntologyService;

    @RequestMapping(value = "/ont/TGEMO.OWL", method = RequestMethod.GET)
    public RedirectView getOntology() {
        String gemmaOntologyUrl = gemmaOntologyService.getOntologyUrl();
        RedirectView redirectView = new RedirectView( gemmaOntologyUrl );
        redirectView.setStatusCode( HttpStatus.FOUND );
        return redirectView;
    }

    @RequestMapping(value = "/ont/{termId}", method = RequestMethod.GET)
    public RedirectView getTerm( @PathVariable("termId") String termId ) {
        String iri = "http://gemma.msl.ubc.ca/ont/" + termId;
        OntologyTerm term = gemmaOntologyService.getTerm( iri );
        if ( term == null ) {
            throw new EntityNotFoundException( String.format( "No term with IRI %s in TGEMO.", iri ) );
        }
        String gemmaOntologyUrl = gemmaOntologyService.getOntologyUrl();
        String urlWithFragment = UriComponentsBuilder.fromHttpUrl( gemmaOntologyUrl )
                .fragment( iri )
                .build()
                .toUriString();
        RedirectView redirectView = new RedirectView( urlWithFragment );
        redirectView.setStatusCode( HttpStatus.FOUND );
        return redirectView;
    }
}
