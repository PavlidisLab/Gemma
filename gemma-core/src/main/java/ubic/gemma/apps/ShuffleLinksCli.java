package ubic.gemma.apps;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.time.StopWatch;

import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoImpl.Link;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.util.AbstractSpringAwareCLI;

public class ShuffleLinksCli extends AbstractSpringAwareCLI {
	
	private String taxonName = "human";
	private boolean prepared = true;
	private String eeNameFile = null;
	//private long eeId = 312;
	private long eeId = 124;
	//private long eeId = -1;
	private Probe2ProbeCoexpressionService p2pService = null;
	Link linkSet = null;

	@Override
	protected void buildOptions() {
//		 TODO Auto-generated method stub
        Option taxonOption = OptionBuilder.hasArg().withArgName( "Taxon" ).withDescription(
        "the taxon name" ).withLongOpt( "Taxon" ).create( 't' );
        addOption( taxonOption );
        Option eeId = OptionBuilder.hasArg().withArgName( "Expression Experiment Id" ).withDescription(
        "Expression Experiment Id" ).withLongOpt( "eeId" ).create( 'e' );
        addOption( eeId );
        Option eeNameFile = OptionBuilder.hasArg().withArgName( "File having Expression Experiment Names" ).withDescription(
        "File having Expression Experiment Names" ).withLongOpt( "eeFileName" ).create( 'f' );
        addOption( eeNameFile );
        Option startPreparing = OptionBuilder.withArgName( " Starting preparing " ).withDescription(
        " Starting preparing the temppory tables " ).withLongOpt( "startPreparing" ).create( 's' );
        addOption( startPreparing );
	}
    protected void processOptions() {
        super.processOptions();
        if ( hasOption( 't' ) ) {
            this.taxonName = getOptionValue( 't' );
        }
        if ( hasOption( 'f' ) ) {
            this.eeNameFile = getOptionValue( 'f' );
        }
        if ( hasOption( 'e' ) ) {
            this.eeId = Long.valueOf(getOptionValue( 'e' ));
        }
        if ( hasOption( 's' ) ) {
            this.prepared = false;
        }

        p2pService = (Probe2ProbeCoexpressionService) this.getBean ( "probe2ProbeCoexpressionService" );
    }

	@Override
	protected Exception doWork(String[] args) {
		// TODO Auto-generated method stub
        Exception err = processCommandLine( "Shuffle Links ", args );
        if ( err != null ) {
            return err;
        }
        if(!prepared) p2pService.prepareForShuffling(taxonName);
        
        if(this.eeId > 0){
        	ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        	ee.setId(this.eeId);
        	p2pService.shuffle(ee, taxonName);
        }
        else{
        	p2pService.shuffle(taxonName);
        }
		return null;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ShuffleLinksCli shuffle = new ShuffleLinksCli();
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Exception ex = shuffle.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
            watch.stop();
            log.info( watch.getTime()/1000 );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
	}

}
