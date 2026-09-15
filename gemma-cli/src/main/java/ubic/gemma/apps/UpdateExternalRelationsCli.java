package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import ubic.gemma.cli.util.AbstractAuthenticatedCLI;
import ubic.gemma.cli.util.RestCacheEviction;
import ubic.gemma.core.util.GemmaRestApiClient;
import ubic.gemma.core.ontology.providers.OntologyServiceResolver;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Rebuilds the {@code EXTERNAL} rows of {@code ANNOTATION_RELATION} from the third-party resources
 * that state relations Gemma cannot derive itself.
 *
 * <p>MGI: which mutant alleles its curators say model which diseases, and which they say do not.
 * Cellosaurus: which disease a cell line's donor had and which part of the body it came from — the
 * relations CLO barely asserts, for 141,670 lines including the ones that exist nowhere else.</p>
 *
 * <p>A command of its own for the reason {@code updateOntologyRelations} is one: the prerequisites
 * differ. That one needs CLO and CHEBI warmed; this one reads two files off MGI's download server and
 * needs only MONDO, which is what the reports' {@code DOID:} identifiers are translated out of.</p>
 *
 * <p>🛑 A failed download leaves the existing rows alone rather than emptying them. A fetch that did
 * not work is not MGI retracting every statement it has ever made.</p>
 */
public class UpdateExternalRelationsCli extends AbstractAuthenticatedCLI {

    /**
     * Not a source of relations here — it is what MGI's {@code DOID:} and Cellosaurus's {@code NCIt:}
     * identifiers are translated <i>out</i> of, so no disease relation can be stored without it.
     */
    private static final String XREF_ONTOLOGY = "MONDO";

    /**
     * Also not a source of relations: Cellosaurus gives an anatomic site as a UBERON identifier plus
     * a free-text sentence, and the term's label — what a curator reads as the object's name — is
     * only here. Without it every site row falls back to the sentence, which is the defect this was
     * added to fix, so leaving it cold makes the whole run a no-op that reports success.
     */
    private static final String SITE_ONTOLOGY = "UBERON";

    /**
     * Also not a source of relations: it is what makes MGI's reachable. Every MGI relation is keyed on
     * an allele URI and no corpus annotation uses one, so each fact is stored a second time under the
     * TGEMO class that cross-references the allele. Cold, that second subject is simply absent — the
     * run reports success and writes rows that match nothing, which is the state this was added to fix.
     */
    private static final String BRIDGE_ONTOLOGY = "TGEMO";

    /** All read by identifier and never by text, which is why search indexing stays off. */
    private static final List<String> REQUIRED_ONTOLOGIES = Collections.unmodifiableList(
            Arrays.asList( XREF_ONTOLOGY, SITE_ONTOLOGY, BRIDGE_ONTOLOGY ) );

    @Autowired
    private TableMaintenanceUtil tableMaintenanceUtil;

    @Autowired(required = false)
    private GemmaRestApiClient gemmaRestApiClient;

    @Autowired(required = false)
    private List<ubic.gemma.core.ontology.providers.OntologyService> ontologies;

    @Value("${load.ontologies}")
    private boolean autoLoadOntologies;

    @Nullable
    @Override
    public String getCommandName() {
        return "updateExternalRelations";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Rebuild the EXTERNAL ANNOTATION_RELATION rows from MGI and Cellosaurus";
    }

    @Override
    protected void buildOptions( Options options ) {
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
    }

    @Override
    protected void doAuthenticatedWork() throws Exception {
        warmUp();
        int written = tableMaintenanceUtil.updateExternalRelationEntries();
        log.info( "Wrote " + written + " EXTERNAL relation rows. Coverage is in the log above." );
        RestCacheEviction.evictAfterRebuild( gemmaRestApiClient, log );
    }

    /**
     * {@link #REQUIRED_ONTOLOGIES} only. Warming every ontology would spend the run on models this
     * command never reads.
     *
     * <p>A missing one is a warning rather than a stop, and the two fail differently: without MONDO
     * no disease relation can be stored at all, while without UBERON the site rows are still correct
     * and only carry the source's sentence where the term's name belongs.</p>
     */
    private void warmUp() throws InterruptedException {
        if ( ontologies == null || ontologies.isEmpty() ) {
            log.warn( "No ontology services are wired; foreign identifiers cannot be translated"
                    + " and anatomic sites cannot be named." );
            return;
        }
        for ( String token : REQUIRED_ONTOLOGIES ) {
            Optional<ubic.gemma.core.ontology.providers.OntologyService> match =
                    OntologyServiceResolver.resolve( ontologies, token );
            if ( !match.isPresent() ) {
                log.warn( "No ontology matched '" + token + "'." );
                continue;
            }
            warmUp( match.get() );
        }
    }

    private void warmUp( ubic.gemma.core.ontology.providers.OntologyService ontology )
            throws InterruptedException {
        if ( !ontology.isOntologyLoaded() ) {
            if ( autoLoadOntologies ) {
                log.info( "Waiting for " + ontology + " to finish loading..." );
                ontology.waitForInitializationThread();
            } else {
                log.info( "Loading " + ontology + "..." );
                // no search index: both are read by identifier, never by text
                ontology.setSearchEnabled( false );
                ontology.initialize( true, false );
            }
        }
        log.info( ontology + " is "
                + ( ontology.isOntologyLoaded() ? "loaded (version " + ontology.getVersion() + ")." : "STILL NOT loaded." ) );
    }
}
