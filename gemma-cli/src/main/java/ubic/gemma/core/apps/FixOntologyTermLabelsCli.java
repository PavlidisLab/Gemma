package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.CLI;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class FixOntologyTermLabelsCli extends AbstractCLI {

    boolean dryRun = false;


    @Autowired
    private OntologyService ontologyService;

    @Autowired
    @Qualifier("ontologyTaskExecutor")
    private AsyncTaskExecutor ontologyTaskExecutor;

    @Value("${load.ontologies}")
    private boolean autoLoadOntologies;

    @Autowired
    private List<ubic.basecode.ontology.providers.OntologyService> ontologies;

    @Override
    public CLI.CommandGroup getCommandGroup() {
        return CLI.CommandGroup.METADATA;
    }

    @Override
    public String getShortDesc() {
        return "Check and correct characteristics & statements using the wrong label for an ontology term";
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {
        // no extra options.
    }

    @Override
    public String getCommandName() {
        return "fixOntologyTermLabels";
    }

    @Override
    protected void buildOptions( Options options ) {
        options.addOption( "d", "dryRun", false, "Dry run, do not update the database [default: " + dryRun + "]" );
    }

    @Override
    protected void doWork() throws Exception {
        if ( autoLoadOntologies ) {
            throw new IllegalArgumentException( "Auto-loading of ontologies is enabled, disable it by setting load.ontologies=false in Gemma.properties." );
        }

        log.info( String.format( "Warming up %d ontologies ...", ontologies.size() ) );
        CompletionService<ubic.basecode.ontology.providers.OntologyService> completionService = new ExecutorCompletionService<>( ontologyTaskExecutor );
        Map<ubic.basecode.ontology.providers.OntologyService, Future<ubic.basecode.ontology.providers.OntologyService>> futures = new LinkedHashMap<>();
        for ( ubic.basecode.ontology.providers.OntologyService ontology : ontologies ) {
            futures.put( ontology, completionService.submit( () -> {
                // we don't need all those features for detecting obsolete terms
                ontology.setSearchEnabled( false );
                ontology.setInferenceMode( ubic.basecode.ontology.providers.OntologyService.InferenceMode.NONE );
                ontology.initialize( true, false );
                return ontology;
            } ) );
        }

        for ( int i = 0; i < ontologies.size(); i++ ) {
            ubic.basecode.ontology.providers.OntologyService os = completionService.take().get();
            log.info( String.format( " === Ontology (%d/%d) warmed up: %s", i + 1, ontologies.size(), os ) );
            int remainingToLoad = ontologies.size() - ( i + 1 );
            if ( remainingToLoad > 0 && remainingToLoad <= 5 ) {
                log.info( "Still loading:\n\t" + futures.entrySet().stream().filter( e -> !e.getValue().isDone() )
                        .map( Map.Entry::getKey )
                        .map( ubic.basecode.ontology.providers.OntologyService::toString )
                        .collect( Collectors.joining( "\n\t" ) ) );
            }
        }

        log.info( "Ontologies warmed up, starting check..." );

        ontologyService.fixOntologyTermLabels( dryRun );


    }
}
