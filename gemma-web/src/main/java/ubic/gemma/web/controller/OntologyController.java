package ubic.gemma.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;
import ubic.gemma.core.ontology.providers.GemmaOntologyService;

/**
 * Provide minimal support for exposing Gemma ontology.
 * @author poirigui
 */
@Controller
public class OntologyController {

    @Autowired
    private GemmaOntologyService gemmaOntologyService;

    @RequestMapping(value = "/ont/TGEMO.OWL", produces = { "application/rdf+xml" })
    public RedirectView getObo() {
        String gemmaOntologyUrl = gemmaOntologyService.getOntologyUrl();
        RedirectView redirectView = new RedirectView( gemmaOntologyUrl );
        redirectView.setStatusCode( HttpStatus.FOUND );
        return redirectView;
    }
}
