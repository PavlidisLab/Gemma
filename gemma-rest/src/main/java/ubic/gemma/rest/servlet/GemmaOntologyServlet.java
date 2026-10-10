package ubic.gemma.rest.servlet;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import ubic.gemma.core.ontology.OntologyExternalLinks;
import ubic.gemma.core.ontology.model.AnnotationProperty;
import ubic.gemma.core.ontology.model.OntologyResource;
import ubic.gemma.core.ontology.model.OntologyTerm;
import ubic.gemma.core.ontology.providers.GemmaOntologyService;

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
 * Dereferences the IRIs of the Temporary Gemma Ontology (TGEMO) under {@code /ont/}.
 * <p>
 * The IRIs are of the form {@code http://gemma.msl.ubc.ca/ont/TGEMO_00184}. They are stored in characteristic URIs,
 * written into TGEMO.OWL and cited externally, so they must resolve at exactly that path. gemma-web served them from
 * {@code OntologyController} until it was retired; this is the gemma-rest replacement for its TGEMO half:
 * <ul>
 *     <li>{@code /ont/TGEMO} lists the terms,</li>
 *     <li>{@code /ont/TGEMO.OWL} redirects to the published OWL file,</li>
 *     <li>{@code /ont/TGEMO_*} (or {@code /ont/TGEMO:*}) shows one term with its definition and hierarchy.</li>
 * </ul>
 * TGFVO ({@code /ont/TGFVO/**}) is not served yet and answers 404.
 * <p>
 * gemma-rest has no view layer (no JSP, no DispatcherServlet), so the pages are written directly.
 *
 * @author poirigui (original gemma-web controller)
 */
public class GemmaOntologyServlet extends HttpServlet {

    static final String TGEMO_URI_PREFIX = BASE_GEMMA_ONTOLOGY_URI;

    private static final Pattern TERM_PATH = Pattern.compile( "^/TGEMO[_:]([^/]+)$" );

    private static final String CONTENT_TYPE = "text/html;charset=UTF-8";

    private GemmaOntologyService gemmaOntologyService;
    private OntologyExternalLinks ontologyExternalLinks;
    private String hostUrl;

    @SuppressWarnings("unused")
    public GemmaOntologyServlet() {
        // dependencies are resolved from the root application context in init()
    }

    GemmaOntologyServlet( GemmaOntologyService gemmaOntologyService, OntologyExternalLinks ontologyExternalLinks, String hostUrl ) {
        this.gemmaOntologyService = gemmaOntologyService;
        this.ontologyExternalLinks = ontologyExternalLinks;
        this.hostUrl = hostUrl;
    }

    @Override
    public void init() {
        if ( gemmaOntologyService == null ) {
            WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext( getServletContext() );
            gemmaOntologyService = ctx.getBean( GemmaOntologyService.class );
            ontologyExternalLinks = ctx.getBean( OntologyExternalLinks.class );
            hostUrl = ctx.getEnvironment().getRequiredProperty( "gemma.hosturl" );
        }
    }

    @Override
    protected void doGet( HttpServletRequest req, HttpServletResponse resp ) throws IOException {
        String path = req.getPathInfo() == null ? "/" : req.getPathInfo();
        String ontBase = req.getContextPath() + req.getServletPath();
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
        } else {
            Matcher m = TERM_PATH.matcher( path );
            if ( !m.matches() ) {
                resp.sendError( HttpServletResponse.SC_NOT_FOUND );
                return;
            }
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
        }
    }

    private void writeHome( String ontBase, HttpServletResponse resp ) throws IOException {
        List<OntologyTerm> terms = gemmaOntologyService.getAllURIs().stream()
                .filter( uri -> uri.startsWith( TGEMO_URI_PREFIX ) )
                .map( gemmaOntologyService::getTerm )
                .filter( Objects::nonNull )
                .sorted( Comparator.comparing( OntologyTerm::getLabel, Comparator.nullsFirst( Comparator.naturalOrder() ) ) )
                .collect( Collectors.toList() );
        PrintWriter w = startPage( resp, "Temporary Gemma Ontology" );
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
        endPage( w );
    }

    private void writeTerm( OntologyTerm term, String ontBase, HttpServletResponse resp ) throws IOException {
        PrintWriter w = startPage( resp, label( term ) );
        w.print( "<p><a href=\"" + htmlEscape( ontBase + "/TGEMO" ) + "\">Temporary Gemma Ontology</a></p>" );
        w.print( "<h1>" + htmlEscape( label( term ) ) + "</h1>" );
        for ( AnnotationProperty annotation : term.getAnnotations() ) {
            if ( "hasDefinition".equals( annotation.getProperty() ) || "definition".equals( annotation.getProperty() ) ) {
                w.print( "<p>" + htmlEscape( annotation.getContents() ) + "</p>" );
            }
        }
        w.print( "<p><b>IRI:</b> " + htmlEscape( term.getUri() ) + "</p>" );
        writeHierarchy( term, ontBase, w );
        endPage( w );
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
     * Link a TGEMO resource to its page here, and anything else to its external page in a new tab.
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

    private static String label( OntologyResource resource ) {
        if ( resource.getLabel() != null ) {
            return resource.getLabel();
        } else if ( resource.getLocalName() != null ) {
            return resource.getLocalName();
        } else {
            return resource.getUri();
        }
    }

    private static PrintWriter startPage( HttpServletResponse resp, String title ) throws IOException {
        resp.setContentType( CONTENT_TYPE );
        resp.setHeader( "Cache-Control", "public, max-age=3600" );
        PrintWriter w = resp.getWriter();
        w.print( "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>" + htmlEscape( title ) + "</title>"
                + "<style>"
                + ":root{color-scheme:light dark}"
                + "body{font-family:system-ui,sans-serif;line-height:1.5;max-width:60rem;margin:0 auto;padding:1rem}"
                + "pre{overflow-x:auto;padding:.5rem;border:1px solid #8884}"
                + ".hierarchy ul{padding-left:1.25rem}"
                + "</style></head><body>" );
        return w;
    }

    private static void endPage( PrintWriter w ) {
        w.print( "</body></html>" );
    }
}
