package ubic.gemma.core.ontology;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import ubic.basecode.ontology.model.AnnotationProperty;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.OntologyService;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/**
 * Utilities for working with ontologies.
 * @author poirigui
 */
@CommonsLog
public class OntologyUtils {

    /**
     * Base URI used by PURL ontologies.
     * <p>
     * Read more about PURL <a href="https://github.com/OBOFoundry/purl.obolibrary.org/">here</a>.
     */
    public static final String BASE_PURL_URI = "http://purl.obolibrary.org/obo/";

    /**
     * Base URI used by Gemma's internal ontologies.
     */
    public static final String BASE_GEMMA_ONTOLOGY_URI = "http://gemma.msl.ubc.ca/ont/";

    /**
     * Base URI used by EFO.
     */
    public static final String BASE_EFO_URI = "http://www.ebi.ac.uk/efo/";

    // FIXME: digits are not allowed in the LOCALID part, but there are ontologies that violate this such as the protein
    //        ontology (e.g. PR:Q6PL45)
    private static final Pattern termIdPattern = Pattern.compile( "([A-Za-z]+):([A-Za-z0-9]+)" );
    private static final Pattern localNamePattern = Pattern.compile( "([A-Za-z]+)_([A-Za-z0-9]+)" );

    /**
     * Mapping of known ontology ID spaces to their URI prefix.
     */
    private static final Map<String, String> OBO_ID_SPACES = new TreeMap<>( String.CASE_INSENSITIVE_ORDER );

    static {
        // Gemma-specific ontologies
        OBO_ID_SPACES.put( "tgemo", BASE_GEMMA_ONTOLOGY_URI );
        OBO_ID_SPACES.put( "tgfvo", BASE_GEMMA_ONTOLOGY_URI );
        // EFO
        OBO_ID_SPACES.put( "efo", BASE_EFO_URI );
        // OBO Foundry ontologies
        try ( InputStream is = OntologyUtils.class.getResourceAsStream( "/ubic/gemma/core/ontology/ontology.idspaces.txt" ) ) {
            for ( String line : IOUtils.readLines( requireNonNull( is ), StandardCharsets.UTF_8 ) ) {
                line = StringUtils.strip( line );
                // ignore comments
                if ( line.startsWith( "#" ) ) {
                    continue;
                }
                OBO_ID_SPACES.put( line, BASE_PURL_URI );
            }
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        log.debug( "Known ontology ID spaces: " + OBO_ID_SPACES );
    }


    /**
     * Ensure that a given ontology is initialized, force-loading it via {@link OntologyService#initialize(boolean, boolean)}
     * if necessary.
     * <p>
     * If the ontology was started via {@link OntologyService#startInitializationThread(boolean, boolean)}, this method
     * will patiently wait until it completes.
     * @throws InterruptedException in case the ontology initialization thread is started, we will wait which implies a
     *                              possible interrupt
     */
    public static void ensureInitialized( OntologyService service ) throws InterruptedException {
        if ( service.isOntologyLoaded() ) {
            log.info( String.format( "%s is already loaded.", service ) );
        } else if ( service.isInitializationThreadAlive() ) {
            log.info( String.format( "Waiting for %s to load...", service ) );
            service.waitForInitializationThread();
        } else {
            log.info( String.format( "Force-loading %s...", service ) );
            service.initialize( true, false );
        }
    }

    /**
     * Ensure that a given ontology is initialized, force-loading it via {@link OntologyService#initialize(boolean, boolean)},
     * but setting the language level to LITE, inferencing to NONE, processImports to false, and enable search to false,
     * if the ontology isn't already loaded.
     */
    public static void ensureInitializedLite( OntologyService service ) throws InterruptedException {
        ensureInitialized( service, OntologyService.InferenceMode.NONE, OntologyService.LanguageLevel.LITE, false, false );
    }

    /**
     * Ensure that a given ontology is initialized, force-loading it via {@link OntologyService#initialize(boolean, boolean)},
     * but first setting how we load it.
     * <p>
     * However, those parameters are ignored if the ontology is already loaded or in progress.
     * @throws InterruptedException in case the ontology initialization thread is started, we will wait which implies a
     *                              possible interrupt
     */
    public static void ensureInitialized( OntologyService service, OntologyService.InferenceMode mode, OntologyService.LanguageLevel level, boolean searchEnabled, boolean processImports ) throws InterruptedException {
        if ( service.isOntologyLoaded() ) {
            if ( isConfiguredProperly( service, mode, level, searchEnabled, processImports ) ) {
                log.info( String.format( "%s is already loaded.", service ) );
                return;
            } else {
                log.warn( String.format( "%s is loaded, but not configured properly.", service ) );
            }
        }

        if ( service.isInitializationThreadAlive() ) {
            log.info( String.format( "Waiting for %s to load...", service ) );
            service.waitForInitializationThread();
            if ( isConfiguredProperly( service, mode, level, searchEnabled, processImports ) ) {
                return;
            } else {
                log.warn( String.format( "%s is loaded, but not configured properly.", service ) );
            }
        }

        service.setInferenceMode( mode );
        service.setSearchEnabled( searchEnabled );
        service.setLanguageLevel( level );
        service.setProcessImports( processImports );
        log.info( String.format( "Force-loading %s...", service ) );
        service.initialize( true, false );
    }

    private static boolean isConfiguredProperly( OntologyService service, OntologyService.InferenceMode mode, OntologyService.LanguageLevel level, boolean searchEnabled, boolean processImports ) {
        return service.getInferenceMode() == mode
                && service.getLanguageLevel() == level
                && service.isSearchEnabled() == searchEnabled
                && service.getProcessImports() == processImports;
    }

    /**
     * Check if a string is in the format of an ontology URI.
     */
    public static boolean isTermUri( String s ) {
        return s.startsWith( BASE_PURL_URI ) || s.startsWith( BASE_GEMMA_ONTOLOGY_URI ) || s.startsWith( BASE_EFO_URI );
    }

    /**
     * Check if a given prefix is a known OBO ID space.
     */
    public static boolean isKnownIdSpace( String prefix ) {
        return OBO_ID_SPACES.containsKey( prefix );
    }

    /**
     * Check if a string is in the format of an OBO term ID.
     */
    public static boolean isTermId( String s ) {
        return isTermId( s, false );
    }

    /**
     * Check if a string is in the format of an OBO term ID.
     *
     * @param checkIfSpaceIsKnown if true, also check that the ID space is known
     */
    public static boolean isTermId( String s, boolean checkIfSpaceIsKnown ) {
        Matcher match = termIdPattern.matcher( s );
        return match.matches() && ( !checkIfSpaceIsKnown || isKnownIdSpace( match.group( 1 ) ) );
    }

    /**
     * Obtain the ontology term ID, also known as the OBO ID, for a given term.
     */
    @Nullable
    public static String getTermId( OntologyTerm term ) {
        AnnotationProperty annot = term.getAnnotation( "http://www.geneontology.org/formats/oboInOwl#id" );
        if ( annot != null ) {
            // best case scenario, there is an OBO ID annotation
            return annot.getContents();
        } else if ( term.getUri() != null && isTermUri( term.getUri() ) ) {
            return uriToTermId( term.getUri() );
        } else {
            // if it's not a PURL URI, we cannot reliably convert it to an OBO ID
            return null;
        }
    }

    /**
     * Converts an OBO term ID to a URI.
     * <p>
     * The exact translation scheme is described in <a href="https://obofoundry.org/id-policy.html">OBO Foundry Identifier Policy</a>.
     */
    public static String termIdToUri( String termId ) {
        String[] parts = termId.split( ":", 2 );
        if ( parts.length != 2 ) {
            throw new IllegalArgumentException( "Term ID is not in the expected '{IDSPACE}:{LOCALID}' format." );
        }
        return OBO_ID_SPACES.getOrDefault( parts[0], BASE_PURL_URI ) + toUriIdSpace( parts[0] ) + "_" + parts[1];
    }

    /**
     * Converts an OBO ID space to the format used in URIs, which is typically uppercase, but with some exceptions
     * (e.g. NCBITaxon).
     */
    private static String toUriIdSpace( String idSpace ) {
        if ( idSpace.equalsIgnoreCase( "ncbitaxon" ) ) {
            return "NCBITaxon";
        }
        if ( idSpace.equalsIgnoreCase( "HsapDv" ) ) {
            return "HsapDv";
        }
        return idSpace.toUpperCase();
    }

    /**
     * Converts a URI to an OBO term ID.
     * <p>
     * The exact translation scheme is described in <a href="https://obofoundry.org/id-policy.html">OBO Foundry Identifier Policy</a>.
     *
     * @throws IllegalArgumentException if the URI is not a valid ontology URI
     */
    public static String uriToTermId( String uri ) {
        String localName;
        if ( uri.startsWith( BASE_PURL_URI ) ) {
            localName = uri.substring( BASE_PURL_URI.length() );
        } else if ( uri.startsWith( BASE_GEMMA_ONTOLOGY_URI ) ) {
            localName = uri.substring( BASE_GEMMA_ONTOLOGY_URI.length() );
        } else if ( uri.startsWith( BASE_EFO_URI ) ) {
            localName = uri.substring( BASE_EFO_URI.length() );
        } else {
            throw new IllegalArgumentException( "URI does not start with expected base PURL, Gemma nor EFO ontology prefix." );
        }
        Matcher match = localNamePattern.matcher( localName );
        if ( match.matches() ) {
            return match.group( 1 ) + ":" + match.group( 2 );
        } else {
            throw new IllegalArgumentException( "The local name is not in the expected '{IDSPACE}_{LOCALID}' format." );
        }
    }
}
