package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import ubic.gemma.core.ontology.model.OntologyTerm;
import ubic.gemma.cli.util.AbstractAuthenticatedCLI;
import ubic.gemma.cli.util.CLI;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.util.TsvUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @deprecated superseded by {@code GET /admin/ontologies/obsolete-terms}, which answers the same question from a
 * running application and additionally reports the replacement each ontology asserts.
 * <p>
 * This command exists only because a CLI has to load the ontologies itself — hence the {@code load.ontologies=false}
 * precondition below and the warm-up loop. A running Gemma already holds them.
 * <p>
 * Two things the endpoint does not carry over. Its {@code Count} column is always 1: {@code checkForObsolete}
 * increments the count but is only reached inside the {@code checkedUris} guard, so no term can be counted twice.
 * And it walks every characteristic in the corpus to answer a question about distinct URIs — the endpoint groups
 * CHARACTERISTIC by URI instead and asks each one once.
 * <p>
 * Scheduled for removal once the correction path lands; see {@code docs/design/OBSOLETE_TERM_CORRECTION.md}.
 */
@Deprecated
public class FindObsoleteTermsCli extends AbstractAuthenticatedCLI {

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    @Qualifier("ontologyTaskExecutor")
    private AsyncTaskExecutor ontologyTaskExecutor;

    @Value("${load.ontologies}")
    private boolean autoLoadOntologies;

    @Autowired
    private List<ubic.gemma.core.ontology.providers.OntologyService> ontologies;

    @Override
    public CommandGroup getCommandGroup() {
        return CLI.CommandGroup.METADATA;
    }

    @Override
    public String getShortDesc() {
        return "DEPRECATED, use GET /admin/ontologies/obsolete-terms instead. Check for characteristics using obsolete terms as values (excluding GO), prints to stdout";
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {
        // no extra options.
    }

    @Override
    public String getCommandName() {
        return "findObsoleteTerms";
    }

    @Override
    protected void doAuthenticatedWork() throws Exception {
        if ( autoLoadOntologies ) {
            throw new IllegalArgumentException( "Auto-loading of ontologies is enabled, disable it by setting load.ontologies=false in Gemma.properties." );
        }

        log.info( String.format( "Warming up %d ontologies ...", ontologies.size() ) );
        CompletionService<ubic.gemma.core.ontology.providers.OntologyService> completionService = new ExecutorCompletionService<>( ontologyTaskExecutor );
        Map<ubic.gemma.core.ontology.providers.OntologyService, Future<ubic.gemma.core.ontology.providers.OntologyService>> futures = new LinkedHashMap<>();
        for ( ubic.gemma.core.ontology.providers.OntologyService ontology : ontologies ) {
            futures.put( ontology, completionService.submit( () -> {
                // we don't need all those features for detecting obsolete terms
                ontology.setSearchEnabled( false );
                ontology.setInferenceMode( ubic.gemma.core.ontology.providers.OntologyService.InferenceMode.NONE );
                ontology.initialize( true, false );
                return ontology;
            } ) );
        }

        for ( int i = 0; i < ontologies.size(); i++ ) {
            ubic.gemma.core.ontology.providers.OntologyService os = completionService.take().get();
            log.info( String.format( " === Ontology (%d/%d) warmed up: %s", i + 1, ontologies.size(), os ) );
            int remainingToLoad = ontologies.size() - ( i + 1 );
            if ( remainingToLoad > 0 && remainingToLoad <= 5 ) {
                log.info( "Still loading:\n\t" + futures.entrySet().stream().filter( e -> !e.getValue().isDone() )
                        .map( Map.Entry::getKey )
                        .map( ubic.gemma.core.ontology.providers.OntologyService::toString )
                        .collect( Collectors.joining( "\n\t" ) ) );
            }
        }

        log.info( "Ontologies warmed up, starting check..." );

        Map<OntologyTerm, Long> vos = ontologyService.findObsoleteTermUsage( 4, TimeUnit.HOURS );

        log.info( "Obsolete term check finished, printing ..." );

        getCliContext().getOutputStream().println( "Value\tValueUri\tCount" );
        for ( Map.Entry<OntologyTerm, Long> vo : vos.entrySet() ) {
            getCliContext().getOutputStream().printf( "%s\t%s\t%s%n", TsvUtils.format( vo.getKey().getLabel() ),
                    TsvUtils.format( vo.getKey().getUri() ), TsvUtils.format( vo.getValue() ) );
        }
    }
}
