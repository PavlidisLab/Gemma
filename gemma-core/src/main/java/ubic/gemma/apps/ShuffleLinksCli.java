package ubic.gemma.apps;

import java.util.Collection;
import java.util.HashSet;

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
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PredictedGeneImpl;
import ubic.gemma.model.genome.ProbeAlignedRegionImpl;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.util.AbstractSpringAwareCLI;

public class ShuffleLinksCli extends AbstractSpringAwareCLI {
	
	private String taxonName = "mouse";
	private boolean prepared = true;
	private String eeNameFile = null;
	//private long eeId = 312;
	private long eeId = 124;
	//private long eeId = -1;
	private Probe2ProbeCoexpressionService p2pService = null;
	private GeneService geneService = null;
	private ExpressionExperimentService eeService = null;
	private Link linkSet = null;

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
        geneService = (GeneService) this.getBean( "geneService" );
        eeService = (ExpressionExperimentService) this.getBean( "expressionExperimentService" );
    }
    private Taxon getTaxon( String name ) {
        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( name );
        TaxonService taxonService = ( TaxonService ) this.getBean( "taxonService" );
        taxon = taxonService.find( taxon );
        if ( taxon == null ) {
            log.info( "NO Taxon found!" );
        }
        return taxon;
    }
    private Collection<Gene> loadGenes(Taxon taxon){
    	Collection <Gene> allGenes = geneService.getGenesByTaxon(taxon);
    	Collection <Gene> genes = new HashSet<Gene>();
    	for(Gene gene:allGenes){
    		if(!(gene instanceof PredictedGeneImpl) && !(gene instanceof ProbeAlignedRegionImpl)){
    			genes.add(gene);
    		}
    	}
    	log.info("Get " + genes.size() + " genes");
    	return genes;
    }

    private int counting(){
    	int max = 0;
    	return 0;
    }
	@Override
	protected Exception doWork(String[] args) {
		// TODO Auto-generated method stub
        Exception err = processCommandLine( "Shuffle Links ", args );
        if ( err != null ) {
            return err;
        }
        if(!prepared){
        	log.info(" Create intermediate tables for shuffling ");
            StopWatch watch = new StopWatch();
            watch.start();
        	p2pService.prepareForShuffling(taxonName);
        	watch.stop();
        	log.info(" Spent " + watch.getTime()/1000 + " to finish the preparation ");
        }
        Taxon taxon = getTaxon( taxonName );
        Collection<Gene> genes = loadGenes(taxon);
    	Collection <ExpressionExperiment> ees = eeService.findByTaxon(taxon);
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
