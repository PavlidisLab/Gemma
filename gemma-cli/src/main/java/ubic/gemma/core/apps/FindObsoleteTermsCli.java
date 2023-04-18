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

import java.util.List;
import java.util.Map;

public class FindObsoleteTermsCli extends AbstractCLIContextCLI {

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private List<ubic.basecode.ontology.providers.OntologyService> ontologies;

    private int start = 1;
    private int step = 100000;
    private int stop = -1;

    @Override
    public GemmaCLI.CommandGroup getCommandGroup() {
        return GemmaCLI.CommandGroup.METADATA;
    }

    @Override
    public String getShortDesc() {
        return "Check for characteristics using obsolete terms as values (excluding GO)";
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
        for ( ubic.basecode.ontology.providers.OntologyService ontology : ontologies ) {
            ontology.startInitializationThread( true, false );
        }

        log.info( "Waiting for ontologies to warm up ..." );
        for ( ubic.basecode.ontology.providers.OntologyService ontology : ontologies ) {
             ontology.waitForInitializationThread();
        }

        log.info( "Ontologies warmed up, starting check..." );

        Map<String, CharacteristicValueObject> vos = ontologyService.findObsoleteTermUsage( );

        AbstractCLI.log.info( "Obsolete term check finished, printing ..." );


        System.out.println( "Value\tValueUri\tCount" );
        for ( CharacteristicValueObject vo : vos.values() ) {
            System.out.println( vo.getValue() + "\t" + vo.getValueUri() + "\t" + vo.getNumTimesUsed() );
        }
        AbstractCLI.log.info( "========= done ========" );
    }
}
