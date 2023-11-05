package ubic.gemma.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;
import ubic.basecode.ontology.model.OntologyIndividual;
import ubic.basecode.ontology.model.OntologyResource;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.core.ontology.FactorValueOntologyService;
import ubic.gemma.core.ontology.providers.GemmaOntologyService;
import ubic.gemma.web.util.EntityNotFoundException;
import ubic.gemma.web.util.ServiceUnavailableException;

import java.util.regex.Pattern;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

/**
 * Provide minimal support for exposing Gemma ontology.
 * @author poirigui
 */
@Controller
public class OntologyController {

    private static final String
            TGEMO_URI_PREFIX = "http://gemma.msl.ubc.ca/ont/",
            TGFVO_URI_PREFIX = "http://gemma.msl.ubc.ca/ont/TGFVO/";

    @Autowired
    private GemmaOntologyService gemmaOntologyService;

    @Autowired
    private FactorValueOntologyService factorValueOntologyService;

    @RequestMapping(value = "/ont/TGEMO.OWL", method = RequestMethod.GET)
    public RedirectView getOntology() {
        String gemmaOntologyUrl = gemmaOntologyService.getOntologyUrl();
        RedirectView redirectView = new RedirectView( gemmaOntologyUrl );
        redirectView.setStatusCode( HttpStatus.FOUND );
        return redirectView;
    }

    @RequestMapping(value = "/ont/{termId}", method = RequestMethod.GET)
    public RedirectView getTerm( @PathVariable("termId") String termId ) {
        if ( !gemmaOntologyService.isOntologyLoaded() ) {
            throw new ServiceUnavailableException( "TGEMO is not loaded." );
        }
        String iri = TGEMO_URI_PREFIX + termId;
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

    @ResponseBody
    @RequestMapping(value = "/ont/TGFVO/{factorValueId}", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String getFactorValue( @PathVariable("factorValueId") Long factorValueId ) {
        String iri = TGFVO_URI_PREFIX + factorValueId;
        OntologyIndividual oi = factorValueOntologyService.getIndividual( iri );
        if ( oi == null ) {
            throw new EntityNotFoundException( String.format( "No individual with IRI %s in TGFVO.", iri ) );
        }
        StringBuilder s = new StringBuilder();
        s.append( String.format( "<title>FactorValue #%d: %s</title>", factorValueId, escapeHtml4( oi.getLabel() ) ) );
        s.append( "<div class=\"padded\">" );
        s.append( String.format( "<h2>FactorValue #%d: %s</h2>", factorValueId, renderOntologyResource( oi ) ) );
        s.append( "<ul>" );
        if ( oi.getInstanceOf() != null ) {
            s.append( "<li>instance of " ).append( renderOntologyResource( oi.getInstanceOf() ) ).append( "</li>" );
        }
        for ( OntologyIndividual relatedOi : factorValueOntologyService.getRelatedIndividuals( iri ) ) {
            s.append( "<li>has part " ).append( renderOntologyResource( relatedOi ) ).append( "</li>" );
        }
        s.append( "</ul>" );
        s.append( "</div>" );
        return s.toString();
    }

    @ResponseBody
    @RequestMapping(value = "/ont/TGFVO/{factorValueId}/{annotationId}", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String getFactorValueAnnotation( @PathVariable("factorValueId") Long factorValueId, @PathVariable("annotationId") Long annotationId ) {
        String iri = TGFVO_URI_PREFIX + factorValueId + "/" + annotationId;
        OntologyIndividual oi = factorValueOntologyService.getIndividual( iri );
        if ( oi == null ) {
            throw new EntityNotFoundException( String.format( "No individual with IRI %s in TGFVO.", iri ) );
        }
        StringBuilder s = new StringBuilder();
        s.append( String.format( "<title>Annotation #%d of FactorValue #%d: %s</title>", annotationId, factorValueId, oi.getLabel() ) );
        s.append( "<div class=\"padded\">" );
        s.append( String.format( "<h2>Annotation #%d of FactorValue #%d: %s</h2>", annotationId, factorValueId, renderOntologyResource( oi ) ) );
        s.append( "<ul>" );
        if ( oi.getInstanceOf() != null ) {
            s.append( "<li>instance of " ).append( renderOntologyResource( oi.getInstanceOf() ) ).append( "</li>" );
        }
        OntologyIndividual factorValueOi = factorValueOntologyService.getIndividual( TGFVO_URI_PREFIX + factorValueId );
        if ( factorValueOi != null ) {
            s.append( "<li>part of " ).append( renderOntologyResource( factorValueOi ) ).append( "</li>" );
        }
        s.append( "</ul>" );
        s.append( "</div>" );
        return s.toString();
    }

    private String renderOntologyResource( OntologyResource oi ) {
        if ( oi.getUri() == null ) {
            return escapeHtml4( oi.getLabel() );
        } else {
            return String.format( "<a href=\"%s\">%s</a>",
                    escapeHtml4( oi.getUri()
                            .replaceFirst( "^" + Pattern.quote( TGFVO_URI_PREFIX ), "/ont/TGFVO/" )
                            .replaceFirst( "^" + Pattern.quote( TGEMO_URI_PREFIX ), "/ont/" ) ),
                    escapeHtml4( oi.getLabel() ) );
        }
    }
}
