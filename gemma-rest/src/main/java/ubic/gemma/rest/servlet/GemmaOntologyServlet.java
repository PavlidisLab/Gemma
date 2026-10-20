package ubic.gemma.rest.servlet;

import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import ubic.gemma.core.ontology.FactorValueOntologyService;
import ubic.gemma.core.ontology.FactorValueOntologyUtils;
import ubic.gemma.core.ontology.OntologyExternalLinks;
import ubic.gemma.core.ontology.model.*;
import ubic.gemma.core.ontology.providers.GemmaOntologyService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.EntityUrlBuilder;
import ubic.gemma.persistence.util.Slice;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.springframework.web.util.HtmlUtils.htmlEscape;
import static ubic.gemma.core.ontology.OntologyUtils.BASE_GEMMA_ONTOLOGY_URI;

/**
 * Dereferences the IRIs of Gemma's own ontologies under {@code /ont/}: the Temporary Gemma Ontology (TGEMO) and the
 * Gemma Factor Value Ontology (TGFVO).
 * <p>
 * The IRIs are of the form {@code http://gemma.msl.ubc.ca/ont/TGEMO_00184} and
 * {@code http://gemma.msl.ubc.ca/ont/TGFVO/123}. They are stored in characteristic URIs, written into the OWL files
 * and cited externally, so they must resolve at exactly that path. gemma-web served them from
 * {@code OntologyController} until it was retired; this is the gemma-rest replacement:
 * <ul>
 *     <li>{@code /ont/TGEMO} lists the terms,</li>
 *     <li>{@code /ont/TGEMO.OWL} redirects to the published OWL file,</li>
 *     <li>{@code /ont/TGEMO_*} (or {@code /ont/TGEMO:*}) shows one term with its definition and hierarchy,</li>
 *     <li>{@code /ont/TGFVO} lists the factor values a page at a time,</li>
 *     <li>{@code /ont/TGFVO/{fv}} and {@code /ont/TGFVO/{fv}/{annotation}} show a factor value or one of its
 *     annotations.</li>
 * </ul>
 * The TGFVO pages answer RDF/XML instead of HTML when the first type in {@code Accept} is
 * {@code application/rdf+xml}. TGFVO.OWL is not served. TGFVO is read as the anonymous user (see
 * {@code OntologySecurityConfig}), so a factor value of a private dataset is not found.
 * <p>
 * gemma-rest has no view layer (no JSP, no DispatcherServlet), so the pages are written directly.
 *
 * @author poirigui (original gemma-web controller)
 */
public class GemmaOntologyServlet extends HttpServlet {

    static final String TGEMO_URI_PREFIX = BASE_GEMMA_ONTOLOGY_URI;

    private static final Pattern TERM_PATH = Pattern.compile( "^/TGEMO[_:]([^/]+)$" );

    private static final Pattern FACTOR_VALUE_PATH = Pattern.compile( "^/TGFVO/(\\d+)(?:/(\\d+))?$" );

    private static final MediaType RDF_XML = MediaType.parseMediaType( "application/rdf+xml" );

    private static final String HTML_CONTENT_TYPE = "text/html;charset=UTF-8";
    private static final String RDF_CONTENT_TYPE = "application/rdf+xml;charset=UTF-8";

    /**
     * TGEMO only changes when it is reloaded; TGFVO follows curation.
     */
    private static final int TGEMO_MAX_AGE = 3600, TGFVO_MAX_AGE = 300;

    private static final int DEFAULT_LIMIT = 20, MAX_LIMIT = 100;

    private GemmaOntologyService gemmaOntologyService;
    private FactorValueOntologyService factorValueOntologyService;
    private ExpressionExperimentService expressionExperimentService;
    private EntityUrlBuilder entityUrlBuilder;
    private OntologyExternalLinks ontologyExternalLinks;
    private String hostUrl;

    @SuppressWarnings("unused")
    public GemmaOntologyServlet() {
        // dependencies are resolved from the root application context in init()
    }

    GemmaOntologyServlet( GemmaOntologyService gemmaOntologyService, FactorValueOntologyService factorValueOntologyService,
            ExpressionExperimentService expressionExperimentService, EntityUrlBuilder entityUrlBuilder,
            OntologyExternalLinks ontologyExternalLinks, String hostUrl ) {
        this.gemmaOntologyService = gemmaOntologyService;
        this.factorValueOntologyService = factorValueOntologyService;
        this.expressionExperimentService = expressionExperimentService;
        this.entityUrlBuilder = entityUrlBuilder;
        this.ontologyExternalLinks = ontologyExternalLinks;
        this.hostUrl = hostUrl;
    }

    @Override
    public void init() {
        if ( gemmaOntologyService == null ) {
            WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext( getServletContext() );
            gemmaOntologyService = ctx.getBean( GemmaOntologyService.class );
            factorValueOntologyService = ctx.getBean( FactorValueOntologyService.class );
            expressionExperimentService = ctx.getBean( ExpressionExperimentService.class );
            entityUrlBuilder = ctx.getBean( EntityUrlBuilder.class );
            ontologyExternalLinks = ctx.getBean( OntologyExternalLinks.class );
            // Gemma's settings reach the placeholder configurer (SettingsConfig), not the Environment, so resolve the
            // placeholder as @Value would; getEnvironment().getProperty() does not see it
            hostUrl = ( ( ConfigurableWebApplicationContext ) ctx ).getBeanFactory().resolveEmbeddedValue( "${gemma.hosturl}" );
        }
    }

    @Override
    protected void doGet( HttpServletRequest req, HttpServletResponse resp ) throws IOException {
        String path = req.getPathInfo() == null ? "/" : req.getPathInfo();
        String ontBase = req.getContextPath() + req.getServletPath();
        Matcher m;
        if ( path.equals( "/" ) || path.equals( "/TGEMO/" ) ) {
            resp.sendRedirect( ontBase + "/TGEMO" );
        } else if ( path.equals( "/TGEMO.OWL" ) ) {
            resp.sendRedirect( gemmaOntologyService.getOntologyUrl() );
        } else if ( path.equals( "/TGEMO" ) ) {
            if ( !gemmaOntologyService.isOntologyLoaded() ) {
                resp.sendError( HttpServletResponse.SC_SERVICE_UNAVAILABLE, "TGEMO is not loaded." );
                return;
            }
            writeHome( ontBase, resp );
        } else if ( path.equals( "/TGFVO" ) || path.equals( "/TGFVO/" ) ) {
            writeFactorValues( req, ontBase, resp );
        } else if ( ( m = FACTOR_VALUE_PATH.matcher( path ) ).matches() ) {
            writeFactorValue( req, Long.parseLong( m.group( 1 ) ), m.group( 2 ) != null ? Long.parseLong( m.group( 2 ) ) : null, ontBase, resp );
        } else if ( ( m = TERM_PATH.matcher( path ) ).matches() ) {
            if ( !gemmaOntologyService.isOntologyLoaded() ) {
                resp.sendError( HttpServletResponse.SC_SERVICE_UNAVAILABLE, "TGEMO is not loaded." );
                return;
            }
            // TGEMO:00184 is the CURIE form of TGEMO_00184; the IRI always uses the underscore
            String iri = TGEMO_URI_PREFIX + "TGEMO_" + m.group( 1 );
            OntologyTerm term = gemmaOntologyService.getTerm( iri );
            if ( term == null ) {
                resp.sendError( HttpServletResponse.SC_NOT_FOUND, "No term with IRI " + iri + " in TGEMO." );
                return;
            }
            writeTerm( term, ontBase, resp );
        } else {
            resp.sendError( HttpServletResponse.SC_NOT_FOUND );
        }
    }

    private void writeHome( String ontBase, HttpServletResponse resp ) throws IOException {
        // listClasses() includes anonymous class expressions (owl:Restriction), whose URI is null
        List<OntologyTerm> terms = gemmaOntologyService.getAllURIs().stream()
                .filter( uri -> uri != null && uri.startsWith( TGEMO_URI_PREFIX ) )
                .map( gemmaOntologyService::getTerm )
                .filter( Objects::nonNull )
                .sorted( Comparator.comparing( OntologyTerm::getLabel, Comparator.nullsFirst( Comparator.naturalOrder() ) ) )
                .collect( Collectors.toList() );
        PrintWriter w = startPage( resp, "Temporary Gemma Ontology", TGEMO_MAX_AGE );
        w.print( "<h1>Temporary Gemma Ontology</h1>" );
        w.print( "<p>The Temporary Gemma Ontology (TGEMO) is a manually curated ontology of terms needed by Gemma.</p>" );
        w.print( "<p><a href=\"" + htmlEscape( ontBase + "/TGEMO.OWL" ) + "\">Download TGEMO</a></p>" );
        w.print( "<p>Retrieve TGEMO in RDF/XML:</p>" );
        w.print( "<pre>curl -L -H Accept:application/rdf+xml " + htmlEscape( hostUrl + "/ont/TGEMO.OWL" ) + "</pre>" );
        w.print( "<ul>" );
        for ( OntologyTerm term : terms ) {
            w.print( "<li>" );
            writeResource( term, ontBase, w );
            w.print( "</li>" );
        }
        w.print( "</ul>" );
        writeOtherOntologies( ontBase, "/TGFVO", "The Gemma Factor Value Ontology", w );
        endPage( w );
    }

    private void writeTerm( OntologyTerm term, String ontBase, HttpServletResponse resp ) throws IOException {
        PrintWriter w = startPage( resp, label( term ), TGEMO_MAX_AGE );
        w.print( "<p><a href=\"" + htmlEscape( ontBase + "/TGEMO" ) + "\">Temporary Gemma Ontology</a></p>" );
        w.print( "<h1>" + htmlEscape( label( term ) ) + "</h1>" );
        for ( AnnotationProperty annotation : term.getAnnotations() ) {
            if ( "hasDefinition".equals( annotation.getProperty() ) || "definition".equals( annotation.getProperty() ) ) {
                w.print( "<p>" + htmlEscape( annotation.getContents() ) + "</p>" );
            }
        }
        w.print( "<p><b>IRI:</b> " + htmlEscape( term.getUri() ) + "</p>" );
        writeHierarchy( term, ontBase, w );
        writeOtherOntologies( ontBase, "/TGFVO", "The Gemma Factor Value Ontology", w );
        endPage( w );
    }

    /**
     * A page of factor values, as HTML with paging links or as RDF/XML.
     */
    private void writeFactorValues( HttpServletRequest req, String ontBase, HttpServletResponse resp ) throws IOException {
        int offset, limit;
        try {
            offset = req.getParameter( "offset" ) != null ? Integer.parseInt( req.getParameter( "offset" ) ) : 0;
            limit = req.getParameter( "limit" ) != null ? Integer.parseInt( req.getParameter( "limit" ) ) : DEFAULT_LIMIT;
        } catch ( NumberFormatException e ) {
            resp.sendError( HttpServletResponse.SC_BAD_REQUEST, "The offset and limit must be integers." );
            return;
        }
        if ( offset < 0 || limit < 1 || limit > MAX_LIMIT ) {
            resp.sendError( HttpServletResponse.SC_BAD_REQUEST, "The offset must be zero or greater and the limit between 1 and " + MAX_LIMIT + "." );
            return;
        }
        offset = offset - ( offset % limit ); // round down to the start of a page
        resp.setHeader( "Vary", "Accept" );
        if ( wantsRdf( req ) ) {
            // the IDs come from a query that already filters by ACL
            Slice<String> uris = factorValueOntologyService.getFactorValueUris( offset, limit );
            startRdf( resp );
            factorValueOntologyService.writeToRdfIgnoreAcls( uris, resp.getWriter() );
            return;
        }
        Slice<OntologyIndividual> individuals = factorValueOntologyService.getFactorValues( offset, limit );
        Long count = individuals.getTotalElements();
        PrintWriter w = startPage( resp, "The Gemma Factor Value Ontology", TGFVO_MAX_AGE );
        w.print( "<h1>The Gemma Factor Value Ontology</h1>" );
        w.print( "<p>The Gemma Factor Value Ontology (TGFVO) provides a structured view of all the annotations used in Gemma.</p>" );
        String page = "?offset=%d&limit=" + limit;
        boolean hasPrevious = offset > 0, hasNext = count != null && offset + limit < count;
        w.print( "<p class=\"pager\">" );
        writePageLink( w, "First page", hasPrevious, String.format( page, 0 ) );
        writePageLink( w, "Previous page", hasPrevious, String.format( page, offset - limit ) );
        writePageLink( w, "Next page", hasNext, String.format( page, offset + limit ) );
        writePageLink( w, "Last page", hasNext, String.format( page, count != null ? ( ( count - 1 ) / limit ) * limit : 0 ) );
        if ( count != null ) {
            w.print( "<span>" + ( count == 0 ? 0 : offset + 1 ) + "–" + Math.min( offset + limit, count ) + " of " + count + "</span>" );
        }
        w.print( "</p>" );
        w.print( "<ul>" );
        for ( OntologyIndividual oi : individuals ) {
            w.print( "<li>" );
            writeResource( oi, ontBase, w );
            w.print( "</li>" );
        }
        w.print( "</ul>" );
        w.print( "<p>Retrieve this page in RDF/XML:</p>" );
        w.print( "<pre>curl -H Accept:application/rdf+xml '" + htmlEscape( hostUrl + "/ont/TGFVO" + String.format( page, offset ) ) + "'</pre>" );
        writeOtherOntologies( ontBase, "/TGEMO", "Temporary Gemma Ontology", w );
        endPage( w );
    }

    private static void writePageLink( PrintWriter w, String text, boolean enabled, String href ) {
        if ( enabled ) {
            w.print( "<a href=\"" + htmlEscape( href ) + "\">" + text + "</a>" );
        } else {
            w.print( "<span>" + text + "</span>" );
        }
    }

    /**
     * A factor value, or one of its annotations if {@code annotationId} is given, as HTML or RDF/XML.
     */
    private void writeFactorValue( HttpServletRequest req, Long factorValueId, Long annotationId, String ontBase, HttpServletResponse resp ) throws IOException {
        String fvIri = FactorValueOntologyUtils.getUri( factorValueId );
        String iri = annotationId != null ? fvIri + "/" + annotationId : fvIri;
        ExpressionExperiment ee;
        OntologyIndividual oi;
        try {
            // the dataset is the ACL gate: a factor value of a private dataset is reported as missing
            ee = expressionExperimentService.findByFactorValueId( factorValueId );
            oi = ee != null ? factorValueOntologyService.getIndividual( iri ) : null;
        } catch ( AccessDeniedException e ) {
            ee = null;
            oi = null;
        }
        if ( oi == null ) {
            resp.sendError( HttpServletResponse.SC_NOT_FOUND, "No individual with IRI " + iri + " in TGFVO." );
            return;
        }
        resp.setHeader( "Vary", "Accept" );
        if ( wantsRdf( req ) ) {
            startRdf( resp );
            factorValueOntologyService.writeToRdf( Collections.singleton( iri ), resp.getWriter() );
            return;
        }
        String datasetUrl = entityUrlBuilder.fromHostUrl().entity( ee ).ui().toString();
        PrintWriter w;
        if ( annotationId == null ) {
            w = startPage( resp, "FactorValue #" + factorValueId + ": " + label( oi ), TGFVO_MAX_AGE );
            w.print( "<p><a href=\"" + htmlEscape( ontBase + "/TGFVO" ) + "\">The Gemma Factor Value Ontology</a></p>" );
            w.print( "<h1>FactorValue #" + factorValueId + ": " + htmlEscape( label( oi ) ) + "</h1>" );
            w.print( "<p><b>IRI:</b> " + htmlEscape( iri ) + "</p>" );
            w.print( "<ul>" );
            writeDatasetItem( ee, datasetUrl, w );
            if ( oi.getInstanceOf() != null ) {
                w.print( "<li>instance of " );
                writeResource( oi.getInstanceOf(), ontBase, w );
                w.print( "</li>" );
            }
            for ( OntologyIndividual annotation : sortByLabel( factorValueOntologyService.getFactorValueAnnotations( iri ) ) ) {
                w.print( "<li>has annotation " );
                writeResource( annotation, ontBase, w );
                w.print( "</li>" );
            }
            List<OntologyStatement> statements = new ArrayList<>( factorValueOntologyService.getFactorValueStatements( iri ) );
            statements.sort( Comparator.comparing( ( OntologyStatement st ) -> label( st.getSubject() ) ).thenComparing( st -> label( st.getObject() ) ) );
            for ( OntologyStatement st : statements ) {
                w.print( "<li>" );
                writeResource( st.getSubject(), ontBase, w );
                w.print( " " );
                writeResource( st.getPredicate(), ontBase, w );
                w.print( " " );
                writeResource( st.getObject(), ontBase, w );
                w.print( "</li>" );
            }
            w.print( "</ul>" );
        } else {
            OntologyIndividual factorValueOi = factorValueOntologyService.getIndividual( fvIri );
            w = startPage( resp, "Annotation #" + annotationId + " of FactorValue #" + factorValueId + ": " + label( oi ), TGFVO_MAX_AGE );
            w.print( "<p><a href=\"" + htmlEscape( ontBase + "/TGFVO" ) + "\">The Gemma Factor Value Ontology</a></p>" );
            w.print( "<h1>Annotation #" + annotationId + " of FactorValue #" + factorValueId + ": " + htmlEscape( label( oi ) ) + "</h1>" );
            w.print( "<p><b>IRI:</b> " + htmlEscape( iri ) + "</p>" );
            w.print( "<ul>" );
            writeDatasetItem( ee, datasetUrl, w );
            if ( factorValueOi != null ) {
                w.print( "<li>annotation of " );
                writeResource( factorValueOi, ontBase, w );
                w.print( "</li>" );
            }
            if ( oi.getInstanceOf() != null ) {
                w.print( "<li>instance of " );
                writeResource( oi.getInstanceOf(), ontBase, w );
                w.print( "</li>" );
            }
            w.print( "</ul>" );
        }
        w.print( "<p>Retrieve this in RDF/XML:</p>" );
        w.print( "<pre>curl -H Accept:application/rdf+xml " + htmlEscape( hostUrl + "/ont/TGFVO/" + factorValueId + ( annotationId != null ? "/" + annotationId : "" ) ) + "</pre>" );
        writeOtherOntologies( ontBase, "/TGEMO", "Temporary Gemma Ontology", w );
        endPage( w );
    }

    private static void writeDatasetItem( ExpressionExperiment ee, String datasetUrl, PrintWriter w ) {
        w.print( "<li>belongs to <a href=\"" + htmlEscape( datasetUrl ) + "\">" + htmlEscape( ee.getShortName() ) + "</a></li>" );
    }

    private static List<OntologyIndividual> sortByLabel( Collection<OntologyIndividual> individuals ) {
        return individuals.stream()
                .sorted( Comparator.comparing( GemmaOntologyServlet::label, Comparator.nullsFirst( Comparator.naturalOrder() ) ) )
                .collect( Collectors.toList() );
    }

    /**
     * Whether the client asked for RDF/XML first. A wildcard does not count, so a bare {@code curl} gets HTML like a
     * browser.
     */
    private static boolean wantsRdf( HttpServletRequest req ) {
        String accept = req.getHeader( "Accept" );
        if ( accept == null ) {
            return false;
        }
        try {
            List<MediaType> types = MediaType.parseMediaTypes( accept );
            return !types.isEmpty() && !types.get( 0 ).isWildcardType() && types.get( 0 ).isCompatibleWith( RDF_XML );
        } catch ( InvalidMediaTypeException e ) {
            return false;
        }
    }

    private static void startRdf( HttpServletResponse resp ) {
        resp.setContentType( RDF_CONTENT_TYPE );
        resp.setHeader( "Cache-Control", "public, max-age=" + TGFVO_MAX_AGE );
    }

    /**
     * Ancestors as nested lists from the roots down, then the term in bold with its descendants.
     */
    private void writeHierarchy( OntologyTerm term, String ontBase, PrintWriter w ) {
        LinkedList<Collection<OntologyTerm>> ancestors = new LinkedList<>();
        Set<OntologyTerm> seen = new HashSet<>();
        Collection<OntologyTerm> level = term.getParents( true );
        while ( !level.isEmpty() ) {
            ancestors.addFirst( level );
            seen.addAll( level );
            level = level.stream()
                    .flatMap( t -> t.getParents( true ).stream() )
                    .filter( t -> !seen.contains( t ) )
                    .collect( Collectors.toSet() );
        }
        w.print( "<div class=\"hierarchy\">" );
        for ( Collection<OntologyTerm> parents : ancestors ) {
            w.print( "<ul>" );
            for ( OntologyTerm parent : parents ) {
                w.print( "<li>" );
                writeResource( parent, ontBase, w );
            }
        }
        w.print( "<ul><li><strong>" );
        writeResource( term, ontBase, w );
        w.print( "</strong>" );
        writeDescendants( term, ontBase, w, new HashSet<>( Collections.singleton( term ) ) );
        w.print( "</li></ul>" );
        for ( Collection<OntologyTerm> parents : ancestors ) {
            w.print( "</li>".repeat( parents.size() ) );
            w.print( "</ul>" );
        }
        w.print( "</div>" );
    }

    private void writeDescendants( OntologyTerm term, String ontBase, PrintWriter w, Set<OntologyTerm> visited ) {
        Collection<OntologyTerm> children = term.getChildren( true );
        if ( children.isEmpty() ) {
            return;
        }
        w.print( "<ul>" );
        for ( OntologyTerm child : children ) {
            w.print( "<li>" );
            writeResource( child, ontBase, w );
            if ( visited.add( child ) ) {
                writeDescendants( child, ontBase, w, visited );
            }
            w.print( "</li>" );
        }
        w.print( "</ul>" );
    }

    /**
     * Link a TGEMO or TGFVO resource to its page here, and anything else to its external page in a new tab.
     */
    private void writeResource( OntologyResource resource, String ontBase, PrintWriter w ) {
        if ( resource.getUri() == null ) {
            w.print( "<span>" + htmlEscape( label( resource ) ) + "</span>" );
        } else if ( resource.getUri().startsWith( TGEMO_URI_PREFIX ) ) {
            String href = ontBase + "/" + resource.getUri().substring( TGEMO_URI_PREFIX.length() );
            w.print( "<a href=\"" + htmlEscape( href ) + "\">" + htmlEscape( label( resource ) ) + "</a>" );
        } else {
            String href = ontologyExternalLinks.getExternalLink( resource );
            w.print( "<a href=\"" + htmlEscape( href ) + "\" target=\"_blank\" rel=\"noopener noreferrer\">"
                    + htmlEscape( label( resource ) ) + " 🗗</a>" );
        }
    }

    private static void writeOtherOntologies( String ontBase, String path, String name, PrintWriter w ) {
        w.print( "<p>Other ontologies used by Gemma:</p><ul><li><a href=\"" + htmlEscape( ontBase + path ) + "\">"
                + htmlEscape( name ) + "</a></li></ul>" );
    }

    private static String label( OntologyResource resource ) {
        if ( resource.getLabel() != null ) {
            return resource.getLabel();
        } else if ( resource.getLocalName() != null ) {
            return resource.getLocalName();
        } else {
            return resource.getUri();
        }
    }

    private static PrintWriter startPage( HttpServletResponse resp, String title, int maxAge ) throws IOException {
        resp.setContentType( HTML_CONTENT_TYPE );
        resp.setHeader( "Cache-Control", "public, max-age=" + maxAge );
        PrintWriter w = resp.getWriter();
        w.print( "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>" + htmlEscape( title ) + "</title>"
                + "<style>"
                + ":root{color-scheme:light dark}"
                + "body{font-family:system-ui,sans-serif;line-height:1.5;max-width:60rem;margin:0 auto;padding:1rem}"
                + "pre{overflow-x:auto;padding:.5rem;border:1px solid #8884}"
                + ".hierarchy ul{padding-left:1.25rem}"
                + ".pager{display:flex;flex-wrap:wrap;gap:1rem}"
                + "</style></head><body>" );
        return w;
    }

    private static void endPage( PrintWriter w ) {
        w.print( "</body></html>" );
    }
}
