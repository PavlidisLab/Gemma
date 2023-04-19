package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.ontology.providers.OntologyServiceFactory;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.AbstractCLIContextCLI;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class FindObsoleteTermsCli extends AbstractCLIContextCLI {

    @Autowired
    private OntologyService ontologyService;

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
        ExecutorService executorService = Executors.newFixedThreadPool( 5 );
        List<Future<ubic.basecode.ontology.providers.OntologyService>> futures = new ArrayList<>();

        for ( ubic.basecode.ontology.providers.OntologyService ontology : ontologies ) {
            Future<ubic.basecode.ontology.providers.OntologyService> future = executorService.submit( () -> {
                ontology.initialize( true, false );
                return ontology;
            } );
            futures.add( future );
        }

        for ( Future<ubic.basecode.ontology.providers.OntologyService> future : futures ) {
            try {
                ubic.basecode.ontology.providers.OntologyService os = future.get();
                log.info( " === Ontology warmed up: " + os );
            } catch ( InterruptedException | ExecutionException e ) {
                e.printStackTrace();
            }
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
