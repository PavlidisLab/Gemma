package ubic.gemma.web.controller.ontology;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ubic.basecode.ontology.model.OntologyIndividual;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.core.ontology.FactorValueOntologyService;
import ubic.gemma.core.ontology.providers.GemmaOntologyService;
import ubic.gemma.core.util.locking.FileLockManager;
import ubic.gemma.core.util.locking.LockedPath;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.web.controller.util.DownloadUtil;
import ubic.gemma.web.controller.util.EntityNotFoundException;
import ubic.gemma.web.controller.util.ServiceUnavailableException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * Provide minimal support for exposing Gemma ontology.
 *
 * @author poirigui
 */
@RequestMapping("/ont")
@Controller
@CommonsLog
@SuppressWarnings("HttpUrlsUsage")
public class OntologyController {

    public static final String
            TGEMO_URI_PREFIX = "http://gemma.msl.ubc.ca/ont/";
    public static final String TGFVO_URI_PREFIX = "http://gemma.msl.ubc.ca/ont/TGFVO/";

    private static final MediaType RDF_XML = MediaType.parseMediaType( "application/rdf+xml" );

    @Value("${gemma.hosturl}")
    private String hostUrl;

    @Autowired
    private GemmaOntologyService gemmaOntologyService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private FactorValueOntologyService factorValueOntologyService;

    @Autowired
    private DownloadUtil downloadUtil;

    @Autowired
    private FileLockManager fileLockManager;

    @Value("${tgfvo.path}")
    private Path tgfvoPath;

    @RequestMapping(value = { "/TGEMO" }, method = { RequestMethod.GET, RequestMethod.HEAD })
    public ModelAndView getGemmaOntologyHome() {
        List<OntologyTerm> terms = gemmaOntologyService.getAllURIs().stream()
                .sorted()
                .map( gemmaOntologyService::getTerm )
                .filter( Objects::nonNull )
                .filter( term -> term.getUri() != null && term.getUri().startsWith( TGEMO_URI_PREFIX ) )
                .sorted( Comparator.comparing( OntologyTerm::getLabel, Comparator.nullsFirst( Comparator.naturalOrder() ) ) )
                .collect( Collectors.toList() );
        return new ModelAndView( "tgemo" )
                .addObject( "terms", terms )
                .addObject( "hostUrl", hostUrl );
    }

    @RequestMapping(value = { "/TGEMO.OWL" }, method = { RequestMethod.GET, RequestMethod.HEAD })
    public RedirectView getGemmaOntologyAsRdf() {
        String gemmaOntologyUrl = gemmaOntologyService.getOntologyUrl();
        RedirectView redirectView = new RedirectView( gemmaOntologyUrl );
        redirectView.setStatusCode( HttpStatus.FOUND );
        return redirectView;
    }

    @RequestMapping(value = "/{termId:TGEMO[:_].*}", method = { RequestMethod.GET, RequestMethod.HEAD })
    public ModelAndView getGemmaOntologyTerm( @PathVariable("termId") String termId ) {
        if ( !gemmaOntologyService.isOntologyLoaded() ) {
            throw new ServiceUnavailableException( "TGEMO is not loaded." );
        }
        String iri = TGEMO_URI_PREFIX + termId;
        OntologyTerm term = gemmaOntologyService.getTerm( iri );
        if ( term == null ) {
            throw new EntityNotFoundException( String.format( "No term with IRI %s in TGEMO.", iri ) );
        }

        return new ModelAndView( "tgemo.term" )
                .addObject( "parentTerms", term.getParents( true ) )
                .addObject( "term", term );
    }

    @RequestMapping(value = "/TGFVO", method = { RequestMethod.GET, RequestMethod.HEAD }, produces = { MediaType.TEXT_HTML_VALUE, "application/rdf+xml" })
    public ModelAndView getFactorValueOntologyHome(
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestHeader(value = "Accept", required = false) String acceptHeader,
            HttpServletResponse response ) throws IOException {
        Assert.isTrue( offset >= 0, "The offset must be zero or greater." );
        Assert.isTrue( limit > 0 && limit <= 100, "The limit must be between 1 and 100." );
        offset = offset - ( offset % limit ); // round down to the nearest limit
        MediaType mediaType = Optional.ofNullable( acceptHeader )
                .map( MediaType::parseMediaTypes )
                .map( List::stream )
                .flatMap( Stream::findFirst )
                .orElse( MediaType.TEXT_HTML );
        if ( mediaType.isCompatibleWith( RDF_XML ) ) {
            Slice<String> individualUris = factorValueOntologyService.getFactorValueUris( offset, limit );
            response.setContentType( "application/rdf+xml" );
            try ( Writer writer = response.getWriter() ) {
                factorValueOntologyService.writeToRdfIgnoreAcls( individualUris, writer );
            }
            return null;
        } else {
            Slice<OntologyIndividual> individuals = factorValueOntologyService.getFactorValues( offset, limit );
            long count = requireNonNull( individuals.getTotalElements() );
            return new ModelAndView( "tgfvo" )
                    .addObject( "individuals", individuals )
                    .addObject( "hostUrl", hostUrl )
                    .addObject( "offset", offset )
                    .addObject( "limit", limit )
                    .addObject( "count", count );
        }
    }

    @RequestMapping(value = "/TGFVO.OWL", method = { RequestMethod.GET, RequestMethod.HEAD }, produces = "application/rdf+xml")
    public void getFactorValueOntologyAsRdf( HttpServletRequest request, HttpServletResponse response, @RequestParam(value = "download", defaultValue = "false") boolean download ) throws IOException, IOException {
        String contentType, contentEncoding, downloadName;
        if ( download ) {
            contentType = "application/octet-stream";
            contentEncoding = null;
            downloadName = "TGFVO.OWL.gz";
        } else {
            contentType = "application/rdf+xml";
            contentEncoding = "gzip";
            downloadName = "TGFVO.OWL";
        }
        try ( LockedPath ignored = fileLockManager.tryAcquirePathLock( tgfvoPath, false, 5, TimeUnit.SECONDS ) ) {
            downloadUtil.download( tgfvoPath, contentType, contentEncoding, true, downloadName, request, response );
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new RuntimeException( e );
        } catch ( TimeoutException e ) {
            throw new ServiceUnavailableException( "TGFVO.OWL is not available at this time.", e );
        }
    }

    @RequestMapping(value = "/TGFVO/{factorValueId}", method = { RequestMethod.GET, RequestMethod.HEAD }, produces = { MediaType.TEXT_HTML_VALUE, "application/rdf+xml" })
    public ModelAndView getFactorValue( @PathVariable("factorValueId") Long factorValueId, @RequestHeader(value = "Accept", required = false) String acceptHeader, HttpServletResponse response ) throws IOException {
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
            response.setContentType( "application/rdf+xml" );
            try ( Writer writer = response.getWriter() ) {
                factorValueOntologyService.writeToRdf( Collections.singleton( iri ), writer );
            }
            return null;
        }
        ExpressionExperiment ee = expressionExperimentService.findByFactorValueId( factorValueId );
        return new ModelAndView( "tgfvo.factorValue" )
                .addObject( "factorValueId", factorValueId )
                .addObject( "oi", oi )
                .addObject( "ee", ee )
                .addObject( "annotations", factorValueOntologyService.getFactorValueAnnotations( iri ) )
                .addObject( "statements", factorValueOntologyService.getFactorValueStatements( iri ) )
                .addObject( "hostUrl", hostUrl );
    }

    @RequestMapping(value = "/TGFVO/{factorValueId}/{annotationId}", method = { RequestMethod.GET, RequestMethod.HEAD }, produces = { MediaType.TEXT_HTML_VALUE, "application/rdf+xml" })
    public ModelAndView getFactorValueAnnotation( @PathVariable("factorValueId") Long factorValueId, @PathVariable("annotationId") Long annotationId, @RequestHeader(value = "Accept", required = false) String acceptHeader,
            HttpServletResponse response ) throws IOException {
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
            response.setContentType( "application/rdf+xml" );
            try ( Writer writer = response.getWriter() ) {
                factorValueOntologyService.writeToRdf( Collections.singleton( iri ), writer );
            }
            return null;
        }
        OntologyIndividual factorValueOi = factorValueOntologyService.getIndividual( TGFVO_URI_PREFIX + factorValueId );
        ExpressionExperiment ee = expressionExperimentService.findByFactorValueId( factorValueId );
        return new ModelAndView( "tgfvo.factorValueAnnotation" )
                .addObject( "oi", oi )
                .addObject( "ee", ee )
                .addObject( "factorValueId", factorValueId )
                .addObject( "annotationId", annotationId )
                .addObject( "factorValueOi", factorValueOi )
                .addObject( "hostUrl", hostUrl );
    }
}
