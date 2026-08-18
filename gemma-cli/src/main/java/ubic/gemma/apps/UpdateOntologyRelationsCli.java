package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ubic.gemma.cli.util.AbstractAuthenticatedCLI;
import ubic.gemma.cli.util.RestCacheEviction;
import ubic.gemma.core.ontology.providers.OntologyServiceResolver;
import ubic.gemma.core.ontology.relation.OntologyRelationProducer;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil;

import org.springframework.lang.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Rebuilds the {@code ONTOLOGY} rows of {@code ANNOTATION_RELATION} from the relations CLO and CHEBI
 * assert.
 *
 * <p>A command of its own rather than a flag on {@code updateEe2c}, which is where the {@code CURATED}
 * half lives. The two share a table and nothing else: the curated harvest reads
 * {@code EXPRESSION_EXPERIMENT2CHARACTERISTIC} and has to run in step with the EE2C rebuild, while this
 * one reads Jena models and needs CLO, CHEBI and MONDO warmed up first — minutes of loading that an
 * experiment-driven command should not be made to wait on.</p>
 */
public class UpdateOntologyRelationsCli extends AbstractAuthenticatedCLI {

    private static final String SOURCE_OPTION = "source";

    /**
     * MONDO is not a source of relations here; it is what the foreign identifiers CLO states its
     * diseases in are translated <i>out</i> of, so it has to be loaded whatever else is being rebuilt.
     */
    private static final String XREF_ONTOLOGY = "MONDO";

    @Autowired
    private TableMaintenanceUtil tableMaintenanceUtil;

    @Autowired(required = false)
    private ubic.gemma.core.util.GemmaRestApiClient gemmaRestApiClient;

    @Autowired
    private OntologyRelationProducer ontologyRelationProducer;

    @Autowired(required = false)
    private List<ubic.gemma.core.ontology.providers.OntologyService> ontologies;

    @Value("${load.ontologies}")
    private boolean autoLoadOntologies;

    private Collection<String> sources;

    @Nullable
    @Override
    public String getCommandName() {
        return "updateOntologyRelations";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Rebuild the ONTOLOGY rows of ANNOTATION_RELATION from what CLO and CHEBI assert";
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.METADATA;
    }

    @Override
    protected void buildOptions( Options options ) {
        options.addOption( null, SOURCE_OPTION, true,
                "Only rebuild these sources, comma-separated. The delete is narrowed the same way, so the "
                        + "others keep their rows. Default: all of them." );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        if ( commandLine.hasOption( SOURCE_OPTION ) ) {
            sources = new LinkedHashSet<>( Arrays.asList( commandLine.getOptionValue( SOURCE_OPTION ).split( "," ) ) );
        } else {
            sources = null;
        }
    }

    @Override
    protected void doAuthenticatedWork() throws Exception {
        warmUp();
        int written = tableMaintenanceUtil.updateOntologyRelationEntries( sources );
        log.info( "Wrote " + written + " ONTOLOGY relation rows."
                + " Per-property coverage is in the log above, one tab-separated block per source." );
        RestCacheEviction.evictAfterRebuild( gemmaRestApiClient, log );
    }

    /**
     * Load only what is needed: the sources being rebuilt, plus MONDO for the translation. CLO is ~38 MB
     * and MONDO the better part of a gigabyte, so warming every ontology the way
     * {@code fixOntologyTermLabels} does would spend most of the run on models this command never reads.
     */
    private void warmUp() throws InterruptedException {
        if ( ontologies == null || ontologies.isEmpty() ) {
            log.warn( "No ontology services are wired; nothing can be read." );
            return;
        }
        Set<String> wanted = new LinkedHashSet<>(
                sources != null && !sources.isEmpty() ? sources : ontologyRelationProducer.getSupportedSources() );
        wanted.add( XREF_ONTOLOGY );

        List<ubic.gemma.core.ontology.providers.OntologyService> toLoad = new ArrayList<>();
        for ( String token : wanted ) {
            Optional<ubic.gemma.core.ontology.providers.OntologyService> o =
                    OntologyServiceResolver.resolve( ontologies, token.trim() );
            if ( !o.isPresent() ) {
                log.warn( "No ontology matched '" + token.trim() + "'; it will be skipped." );
            } else if ( !toLoad.contains( o.get() ) ) {
                toLoad.add( o.get() );
            }
        }

        for ( ubic.gemma.core.ontology.providers.OntologyService ontology : toLoad ) {
            if ( ontology.isOntologyLoaded() ) {
                continue;
            }
            if ( autoLoadOntologies ) {
                log.info( "Waiting for " + ontology + " to finish loading..." );
                ontology.waitForInitializationThread();
            } else {
                log.info( "Loading " + ontology + "..." );
                // no search index: this reads axioms by URI and never queries by text
                ontology.setSearchEnabled( false );
                ontology.initialize( true, false );
            }
            log.info( ontology + " is "
                    + ( ontology.isOntologyLoaded() ? "loaded (version " + ontology.getVersion() + ")." : "STILL NOT loaded." ) );
        }
    }
}
