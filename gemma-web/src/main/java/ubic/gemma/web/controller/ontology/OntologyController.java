package ubic.gemma.web.controller.ontology;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import ubic.basecode.ontology.model.AnnotationProperty;
import ubic.basecode.ontology.model.OntologyIndividual;
import ubic.basecode.ontology.model.OntologyResource;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.core.ontology.FactorValueOntologyService;
import ubic.gemma.core.ontology.providers.GemmaOntologyService;
import ubic.gemma.web.controller.util.EntityNotFoundException;
import ubic.gemma.web.controller.util.ServiceUnavailableException;

import javax.servlet.ServletContext;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

/**
 * Provide minimal support for exposing Gemma ontology.
 * @author poirigui
 */
@RequestMapping("/ont")
@Controller
@CommonsLog
@SuppressWarnings("HttpUrlsUsage")
public class OntologyController {

    private static final String
            TGEMO_URI_PREFIX = "http://gemma.msl.ubc.ca/ont/",
            TGFVO_URI_PREFIX = "http://gemma.msl.ubc.ca/ont/TGFVO/";

    private static final MediaType RDF_XML = MediaType.parseMediaType( "application/rdf+xml" );

    @Value("${gemma.hosturl}")
    private String hostUrl;

    @Autowired
    private GemmaOntologyService gemmaOntologyService;

    @Autowired
    private FactorValueOntologyService factorValueOntologyService;

    @Autowired
    private ServletContext servletContext;

    @RequestMapping(value = { "/TGEMO", "/TGEMO.OWL" }, method = { RequestMethod.GET, RequestMethod.HEAD })
    public RedirectView getOntology() {
        String gemmaOntologyUrl = gemmaOntologyService.getOntologyUrl();
        RedirectView redirectView = new RedirectView( gemmaOntologyUrl );
        redirectView.setStatusCode( HttpStatus.FOUND );
        return redirectView;
    }

    @ResponseBody
    @RequestMapping(value = "/{termId:TGEMO_.*}", method = { RequestMethod.GET, RequestMethod.HEAD })
    public String getTerm( @PathVariable("termId") String termId ) {
        if ( !gemmaOntologyService.isOntologyLoaded() ) {
            throw new ServiceUnavailableException( "TGEMO is not loaded." );
        }
        String iri = TGEMO_URI_PREFIX + termId;
        OntologyTerm term = gemmaOntologyService.getTerm( iri );
        if ( term == null ) {
            throw new EntityNotFoundException( String.format( "No term with IRI %s in TGEMO.", iri ) );
        }

        StringBuilder s = new StringBuilder( String.format( "<title>%s</title>", escapeHtml4( term.getLabel() ) ) +
                "<div class=\"padded\"><ul style=\"padding-bottom:0.5em\">" +
                String.format( "<h2 style=\"padding-bottom:0.5em\">%s: %s</h2>",
                        escapeHtml4( term.getLocalName() ),
                        escapeHtml4( term.getLabel() ) ) );
        if ( term.getUri() != null ) {
            s.append( String.format( "<li><a href=%s>%s</a></li>",
                    escapeHtml4( term.getUri()
                            .replaceFirst( "^" + Pattern.quote( TGEMO_URI_PREFIX ), servletContext.getContextPath() + "/ont/" ) ),
                    escapeHtml4( TGEMO_URI_PREFIX + term.getLocalName() ) ) );
        }

        for ( AnnotationProperty annot : term.getAnnotations() ) {
            if ( Arrays.asList( "hasDefinition", "definition" ).contains( annot.getProperty() ) ) {
                s.append( "<li>" )
                        .append( annot.getContents() )
                        .append( "</li>" );
            }
        }


        s.append( "</ul>" );
        s.append( String.format( "<p>Ontology IRI: <a href=%s>%s</a></p>",
                servletContext.getContextPath() + "/ont/TGEMO",
                TGEMO_URI_PREFIX + "TGEMO" ) );
        s.append( "</div>" );
        return s.toString();

    }

    @ResponseBody
    @RequestMapping(value = "/TGFVO/{factorValueId}", method = { RequestMethod.GET, RequestMethod.HEAD }, produces = { MediaType.TEXT_HTML_VALUE, "application/rdf+xml" })
    public String getFactorValue( @PathVariable("factorValueId") Long factorValueId, @RequestHeader(value = "Accept", required = false) String acceptHeader ) {
        String iri = TGFVO_URI_PREFIX + factorValueId;
        OntologyIndividual oi = factorValueOntologyService.getIndividual( iri );
        if ( oi == null ) {
            throw new EntityNotFoundException( String.format( "No individual with IRI %s in TGFVO.", iri ) );
        }
        MediaType mediaType = Optional.ofNullable( acceptHeader )
                .map( MediaType::parseMediaTypes )
                .map( List::stream )
                .flatMap( Stream::findFirst )
                .orElse( MediaType.TEXT_HTML );
        if ( mediaType.isCompatibleWith( RDF_XML ) ) {
            StringWriter sw = new StringWriter();
            factorValueOntologyService.writeToRdf( iri, sw );
            return sw.toString();
        }
        StringBuilder s = new StringBuilder();
        s.append( String.format( "<title>FactorValue #%d: %s</title>", factorValueId, escapeHtml4( oi.getLabel() ) ) );
        s.append( "<div class=\"padded\">" );
        s.append( String.format( "<h2 style=\"padding-bottom:0.5em\">FactorValue #%d: %s</h2>", factorValueId, renderOntologyResource( oi ) ) );
        s.append( "<ul>" );
        if ( oi.getInstanceOf() != null ) {
            s.append( "<li>instance of " ).append( renderOntologyResource( oi.getInstanceOf() ) ).append( "</li>" );
        }
        for ( OntologyIndividual relatedOi : factorValueOntologyService.getFactorValueAnnotations( iri ) ) {
            s.append( "<li>has annotation " ).append( renderOntologyResource( relatedOi ) ).append( "</li>" );
        }
        for ( FactorValueOntologyService.OntologyStatement st : factorValueOntologyService.getFactorValueStatements( iri ) ) {
            s.append( String.format( "<li>%s %s %s</li>", renderOntologyResource( st.getSubject() ), renderOntologyResource( st.getPredicate() ), renderOntologyResource( st.getObject() ) ) );
        }
        s.append( "</ul>" );
        s.append( "<p style=\"margin-bottom: 0em;\">Retrieve this in RDF/XML:</p>" );
        s.append( String.format( "<pre>curl -H Accept:application/rdf+xml %s/ont/TGFVO/%d</pre>", hostUrl, factorValueId ) );
        s.append( "</div>" );
        return s.toString();
    }

    @ResponseBody
    @RequestMapping(value = "/TGFVO/{factorValueId}/{annotationId}", method = { RequestMethod.GET, RequestMethod.HEAD }, produces = { MediaType.TEXT_HTML_VALUE, "application/rdf+xml" })
    public String getFactorValueAnnotation( @PathVariable("factorValueId") Long factorValueId, @PathVariable("annotationId") Long annotationId, @RequestHeader(value = "Accept", required = false) String acceptHeader ) {
        String iri = TGFVO_URI_PREFIX + factorValueId + "/" + annotationId;
        OntologyIndividual oi = factorValueOntologyService.getIndividual( iri );
        if ( oi == null ) {
            throw new EntityNotFoundException( String.format( "No individual with IRI %s in TGFVO.", iri ) );
        }
        MediaType mediaType = Optional.ofNullable( acceptHeader )
                .map( MediaType::parseMediaTypes )
                .map( List::stream )
                .flatMap( Stream::findFirst )
                .orElse( MediaType.TEXT_HTML );
        if ( mediaType.isCompatibleWith( RDF_XML ) ) {
            StringWriter sw = new StringWriter();
            factorValueOntologyService.writeToRdf( iri, sw );
            return sw.toString();
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
            s.append( "<li>annotation of " ).append( renderOntologyResource( factorValueOi ) ).append( "</li>" );
        }
        s.append( "</ul>" );
        s.append( "<p>Retrieve this in RDF/XML:</p>" );
        s.append( String.format( "<pre>curl -H Accept:application/rdf+xml %s/ont/TGFVO/%d/%d</pre>", hostUrl, factorValueId, annotationId ) );
        s.append( "</div>" );
        return s.toString();
    }

    private String renderOntologyResource( OntologyResource oi ) {
        if ( oi.getUri() == null ) {
            return escapeHtml4( oi.getLabel() );
        } else {
            //noinspection SpellCheckingInspection
            return String.format( "<a href=\"%s\">%s</a>",
                    escapeHtml4( oi.getUri()
                            .replaceFirst( "^" + Pattern.quote( TGFVO_URI_PREFIX ), servletContext.getContextPath() + "/ont/TGFVO/" )
                            .replaceFirst( "^" + Pattern.quote( TGEMO_URI_PREFIX ), servletContext.getContextPath() + "/ont/" ) ),
                    escapeHtml4( oi.getLabel() ) );
        }
    }
}
