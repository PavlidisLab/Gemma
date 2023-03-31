package ubic.gemma.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.view.RedirectView;
import ubic.gemma.core.ontology.providers.GemmaOntologyService;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Provide minimal support for exposing Gemma ontology.
 * @author poirigui
 */
@Controller
public class OntologyController {

    @Autowired
    @Qualifier("gemmaOntologyService")
    private Future<GemmaOntologyService> gemmaOntologyService;

    @RequestMapping(value = "/ont/TGEMO.OWL", produces = { "application/rdf+xml" }, method = RequestMethod.GET)
    public RedirectView getObo() throws ExecutionException, InterruptedException {
        String gemmaOntologyUrl = gemmaOntologyService.get().getOntologyUrl();
        RedirectView redirectView = new RedirectView( gemmaOntologyUrl );
        redirectView.setStatusCode( HttpStatus.FOUND );
        return redirectView;
    }
}
