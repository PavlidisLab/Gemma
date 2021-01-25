package ubic.gemma.core.apps;

import org.apache.commons.cli.Option;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.AbstractCLIContextCLI;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;

import java.util.Map;

public class CountObsoleteTermsCli extends AbstractCLIContextCLI {
    private OntologyService ontologyService;

    private String startArg;
    private String stepArg = "100000";
    private String stopArg;

    @Override
    public GemmaCLI.CommandGroup getCommandGroup() {
        return GemmaCLI.CommandGroup.EXPERIMENT;
    }

    @Override
    public String getShortDesc() {
        return "Check for characteristics using obsolete terms in the given ID range.";
    }

    @Override
    protected void processOptions() {
        ontologyService = this.getBean( OntologyService.class );

        if ( this.hasOption( "start" ) ) {
            this.startArg = this.getOptionValue( "start" );
        }

        if ( this.hasOption( "step" ) ) {
            this.stepArg = this.getOptionValue( "step" );
        }

        if ( this.hasOption( "stop" ) ) {
            this.stopArg = this.getOptionValue( "stop" );
        }
    }

    @Override
    public String getCommandName() {
        return "countObsoleteTerms";
    }

    @SuppressWarnings("AccessStaticViaInstance")
    @Override
    protected void buildOptions() {

        Option startOption = Option.builder( "start" )
                .desc( "The ID of the first characteristic to check, i.e the end of the ID range." ).hasArg().required()
                .build();
        this.addOption( startOption );

        Option stepOption = Option.builder( "step" )
                .desc( "The amount of characteristics to load in one batch. Default value is 100000." ).hasArg()
                .build();
        this.addOption( stepOption );

        Option stopOption = Option.builder( "stop" )
                .desc( "The ID of the last characteristic to check, i.e the end of the ID range." ).hasArg().required()
                .build();
        this.addOption( stopOption );
    }

    @Override
    protected void doWork() throws Exception {
        int start = Integer.parseInt( startArg ) + 1;
        int step = Integer.parseInt( stepArg ) + 1;
        int stop = Integer.parseInt( stopArg ) + 1;

        Map<String, CharacteristicValueObject> vos = ontologyService.countObsoleteOccurrences( start, stop, step );

        // Output results

        AbstractCLI.log.info( "======================================================" );
        AbstractCLI.log.info( "Obsolete term check finished." );
        AbstractCLI.log.info( "Below is a tab-separated output of all found terms:" );
        AbstractCLI.log.info( "======================================================" );
        AbstractCLI.log.info( "======================================================" );
        AbstractCLI.log.info( "Value\tValueUri\tCount" );
        for ( CharacteristicValueObject vo : vos.values() ) {
            AbstractCLI.log.info( vo.getValue() + "\t" + vo.getValueUri() + "\t" + vo.getNumTimesUsed() );
        }
        AbstractCLI.log.info( "======================================================" );
        AbstractCLI.log.info( "======================================================" );
    }
}
