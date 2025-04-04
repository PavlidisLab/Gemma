package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.cli.util.AbstractAuthenticatedCLI;
import ubic.gemma.cli.util.CLI;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class FindObsoleteTermsCli extends AbstractAuthenticatedCLI {

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
    public CommandGroup getCommandGroup() {
        return CLI.CommandGroup.METADATA;
    }

    @Override
    public String getShortDesc() {
        return "Check for characteristics using obsolete terms as values (excluding GO), prints to sdout";
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
    protected void buildOptions( Options options ) {
    }

    @Override
    protected void doAuthenticatedWork() throws Exception {
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

        Map<OntologyTerm, Long> vos = ontologyService.findObsoleteTermUsage( 4, TimeUnit.HOURS );

        log.info( "Obsolete term check finished, printing ..." );

        getCliContext().getOutputStream().println( "Value\tValueUri\tCount" );
        for ( Map.Entry<OntologyTerm, Long> vo : vos.entrySet() ) {
            getCliContext().getOutputStream().println( vo.getKey().getLabel() + "\t" + vo.getKey().getUri() + "\t" + vo.getValue() );
        }
    }
}
