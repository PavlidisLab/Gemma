package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.AbstractCLIContextCLI;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;

@Component
public class FindObsoleteTermsCli extends AbstractCLIContextCLI {

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    @Qualifier("ontologyTaskExecutor")
    private AsyncTaskExecutor ontologyTaskExecutor;

    @Autowired
    private List<ubic.basecode.ontology.providers.OntologyService> ontologies;

    @Override
    public GemmaCLI.CommandGroup getCommandGroup() {
        return GemmaCLI.CommandGroup.METADATA;
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

    @SuppressWarnings("AccessStaticViaInstance")
    @Override
    protected void buildOptions( Options options ) {
    }

    @Override
    protected void doWork() throws Exception {

        log.info( "Warming up ontologies ..." );
        CompletionService<ubic.basecode.ontology.providers.OntologyService> completionService = new ExecutorCompletionService<>( ontologyTaskExecutor );
        for ( ubic.basecode.ontology.providers.OntologyService ontology : ontologies ) {
            completionService.submit( () -> {
                // we don't need all those features for detecting obsolete terms
                ontology.setSearchEnabled( false );
                ontology.setInferenceMode( ubic.basecode.ontology.providers.OntologyService.InferenceMode.NONE );
                ontology.initialize( true, false );
                return ontology;
            } );
        }

        for ( int i = 0; i < ontologies.size(); i++ ) {
            ubic.basecode.ontology.providers.OntologyService os = completionService.take().get();
            log.info( String.format( " === Ontology (%d/%d) warmed up: %s", i + 1, ontologies.size(), os ) );
        }

        log.info( "Ontologies warmed up, starting check..." );

        Map<String, CharacteristicValueObject> vos = ontologyService.findObsoleteTermUsage();

        AbstractCLI.log.info( "Obsolete term check finished, printing ..." );

        System.out.println( "Value\tValueUri\tCount" );
        for ( CharacteristicValueObject vo : vos.values() ) {
            System.out.println( vo.getValue() + "\t" + vo.getValueUri() + "\t" + vo.getNumTimesUsed() );
        }

    }
}
