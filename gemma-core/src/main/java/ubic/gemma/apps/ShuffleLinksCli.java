package ubic.gemma.apps;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;

import ubic.basecode.dataStructure.matrix.CompressedNamedBitMatrix;
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
    private HashMap<Long, Integer> eeMap = null;
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
    private Collection<ExpressionExperiment> getCandidateEE(String fileName, Collection<ExpressionExperiment> ees){
        if(fileName == null) return ees;
        Collection<ExpressionExperiment> candidates = new HashSet<ExpressionExperiment>();
        Collection<String> eeNames = new HashSet<String>();
        try {
            InputStream is = new FileInputStream( fileName );
            BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
            String shortName = null;
            while ( ( shortName = br.readLine() ) != null ) {
                if ( StringUtils.isBlank( shortName ) ) continue;
                eeNames.add( shortName.trim().toUpperCase());
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            return candidates;
        }
        for(ExpressionExperiment ee:ees){
            String shortName = ee.getShortName();
            if(eeNames.contains( shortName.trim().toUpperCase() )) candidates.add( ee );
        }
        return candidates;
    }
    private void shuffleLinks(Collection<Link> links){
        //Do shuffling
        Random random = new Random();
        Object[] linksInArray = links.toArray();
        for(int i = linksInArray.length - 1; i >= 0; i--){
            int pos = random.nextInt(i+1);
            Long tmpId = ((Link)linksInArray[pos]).getSecond_design_element_fk();
            ((Link)linksInArray[pos]).setSecond_design_element_fk(((Link)linksInArray[i]).getSecond_design_element_fk());
            ((Link)linksInArray[i]).setSecond_design_element_fk(tmpId);
        }
    }
    private CompressedNamedBitMatrix getMatrix(Collection<ExpressionExperiment> ees, Collection<Gene> genes){
        CompressedNamedBitMatrix linkCount = new CompressedNamedBitMatrix(genes.size(), genes.size(), ees.size());
        for(Gene geneIter:genes){
            linkCount.addRowName(geneIter.getId());
        }
        for(Gene geneIter:genes){
            linkCount.addColumnName(geneIter.getId());
        }
        return linkCount;
    }
    private void fillingMatrix(CompressedNamedBitMatrix linkCount, Collection<Link> links, ExpressionExperiment ee){
        Set<Long> csIds = new HashSet<Long>();
        for(Link link:links){
            csIds.add(link.getFirst_design_element_fk());
            csIds.add(link.getSecond_design_element_fk());
        }
        Map<Long, Collection<Long>> cs2genes = geneService.getCS2GeneMap( csIds );
        int eeIndex = eeMap.get( ee.getId() );
        for(Link link:links){
            Collection<Long> firstGeneIds =cs2genes.get( link.getFirst_design_element_fk() );
            Collection<Long> secondGeneIds =cs2genes.get( link.getSecond_design_element_fk() );
            if(firstGeneIds == null || secondGeneIds == null) continue;
            if(firstGeneIds.size() != 1 || secondGeneIds.size() != 1) continue;
            Long firstGeneId = firstGeneIds.iterator().next();
            Long secondGeneId = secondGeneIds.iterator().next();
            int rowIndex = linkCount.getRowIndexByName(firstGeneId);
            int colIndex = linkCount.getColIndexByName(secondGeneId);
            linkCount.set( rowIndex, colIndex, eeIndex );
        }
    }
    private int counting(CompressedNamedBitMatrix linkCount){
        int rows = linkCount.rows();
        int maxBits = 0;
        for(int i = 0; i < rows; i++){
            for(int j = i + 1; j < rows; j++){
                int bits = linkCount.bitCount( i, j );
                if(bits > maxBits)
                    maxBits = bits;
            }
        }
        return maxBits;
    }

    private int doShuffling(Collection<ExpressionExperiment> ees, Collection<Gene> genes){
        int maxBits = 0;
        CompressedNamedBitMatrix linkCount = getMatrix(ees,genes);
        for(ExpressionExperiment ee:ees){
            Collection<Link> links = p2pService.getProbeCoExpression( ee, this.taxonName );
            shuffleLinks(links);
            fillingMatrix(linkCount, links,ee);
        }
        maxBits = counting(linkCount);
        return maxBits;
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
        Collection <ExpressionExperiment> candidates = getCandidateEE(this.eeNameFile, ees);
        eeMap = new HashMap();
        int index = 0;
        for(ExpressionExperiment eeIter:candidates){
            eeMap.put(eeIter.getId(), new Integer(index));
            index++;
        }
        int maxBits = 0;
        for(int i = 0; i < 100; i++){
            int maximalBits = doShuffling(candidates, genes);
            if( maximalBits > maxBits ) maxBits = maximalBits;
        }
        log.info( "Finish the simulation! Get the significant threshold " + maxBits);
        
//        if(this.eeId > 0){
//        	ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
//        	ee.setId(this.eeId);
//        	p2pService.shuffle(ee, taxonName);
//        }
//        else{
//        	p2pService.shuffle(taxonName);
//        }
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
