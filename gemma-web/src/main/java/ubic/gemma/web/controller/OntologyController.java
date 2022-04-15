package ubic.gemma.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;
import ubic.gemma.core.ontology.providers.GemmaOntologyService;
import ubic.gemma.persistence.util.Settings;

/**
 * Provide minimal support for exposing Gemma ontology.
 * @author poirigui
 */
@Controller
public class OntologyController {

    @RequestMapping(value = "/ont/TGEMO.OWL", produces = { "application/rdf+xml" })
    public RedirectView getObo() {
        String gemmaOntologyUrl = Settings.getString( GemmaOntologyService.GEMMA_ONTOLOGY_URL_CONFIG );
        RedirectView redirectView = new RedirectView( gemmaOntologyUrl );
        redirectView.setStatusCode( HttpStatus.FOUND );
        return redirectView;
    }
}
