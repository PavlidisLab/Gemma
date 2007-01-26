package ubic.gemma.apps;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.time.StopWatch;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.ObjectArrayList;

import ubic.gemma.analysis.sequence.BlatResultGeneSummary;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneProductService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.model.genome.sequenceAnalysis.BlastResult;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationService;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.util.AbstractSpringAwareCLI;

public class MicroRNAFinderCli extends AbstractSpringAwareCLI {

	private String arrayDesignName = null;
	private String outFileName = null;
	private String taxonName = null;
    private Collection<GeneProduct> miRNAs = new HashSet<GeneProduct>();

	@Override
    protected void processOptions() {
        super.processOptions();
        if ( hasOption( 'a' ) ) {
            this.arrayDesignName = getOptionValue( 'a' );
        }
        if ( hasOption( 'o' ) ) {
            this.outFileName = getOptionValue( 'o' );
        }
        if ( hasOption( 't' ) ) {
            this.taxonName = getOptionValue( 't' );
        }

    }
	@Override
	protected void buildOptions() {
		// TODO Auto-generated method stub
		Option ADOption = OptionBuilder.hasArg().isRequired().withArgName( "arrayDesign" ).withDescription(
		"Array Design Short Name (GPLXXX) " )
		.withLongOpt( "arrayDesign" ).create( 'a' );
		addOption( ADOption );
		Option OutOption = OptionBuilder.hasArg().isRequired().withArgName( "outputFile" ).withDescription(
		"The name of the file to save the output " )
		.withLongOpt( "outputFile" ).create( 'o' );
		addOption( OutOption );
		
		Option TaxonOption = OptionBuilder.hasArg().isRequired().withArgName( "taxonName" ).withDescription(
		"The name of the speci " )
		.withLongOpt( "taxonName" ).create( 't' );
		addOption( TaxonOption );


	}
	
	private Taxon getTaxon(String name){
		Taxon taxon = Taxon.Factory.newInstance();
		taxon.setCommonName(name);
		TaxonService taxonService = (TaxonService)this.getBean("taxonService");
		taxon = taxonService.find(taxon);
		if(taxon == null){
			log.info("NO Taxon found!");
		}
		return taxon;
	}
	Collection<GeneProduct> checkMappedRNAs(long start, long end){
		Collection<GeneProduct> returnedRNAs = new HashSet<GeneProduct>();
		for(GeneProduct miRNA:miRNAs){
			//if(!miRNA.getName().equals("mmu-mir-29c")) continue;
			PhysicalLocation location = miRNA.getPhysicalLocation();
			if(location.getNucleotide() >= start && location.getNucleotide()+location.getNucleotideLength() <= end)
					returnedRNAs.add(miRNA);
		}
		return returnedRNAs;
	}
	Collection<GeneProduct> checkMappedRNAs(PhysicalLocation targetLocation){
		/*
		if(targetLocation == null){
			Collection<GeneProduct> returnedRNAs = new HashSet<GeneProduct>();
			return returnedRNAs;
		}else
			return checkMappedRNAs(targetLocation.getNucleotide(),targetLocation.getNucleotide()+targetLocation.getNucleotideLength());
		*/
		Collection<GeneProduct> returnedRNAs = new HashSet<GeneProduct>();
		if(targetLocation == null) return returnedRNAs;
		for(GeneProduct miRNA:miRNAs){
			//if(!miRNA.getName().equals("mmu-mir-29c")) continue;
			PhysicalLocation location = miRNA.getPhysicalLocation();
			if(targetLocation.nearlyEquals(location))
					returnedRNAs.add(miRNA);
		}
		return returnedRNAs;
	}

	@Override
	protected Exception doWork(String[] args) {
		// TODO Auto-generated method stub
        Exception err = processCommandLine( "MicroRNAFinder", args );
        if ( err != null ) {
            return err;
        }
        ArrayDesignService adService = (ArrayDesignService) this.getBean( "arrayDesignService" );
        GeneService geneService = (GeneService) this.getBean( "geneService" );
        BlatAssociationService blatAssociationService = (BlatAssociationService) this.getBean( "blatAssociationService" );
        
        Taxon taxon = this.getTaxon(this.taxonName);
        if(taxon == null){
        	System.err.println(" Taxon " + this.taxonName + " doesn't exist");
        	return null;
        }
        Collection<Gene> genes = geneService.getMicroRnaByTaxon(taxon);

        for(Gene gene:genes){
        	miRNAs.addAll(gene.getProducts());
        }
        for(GeneProduct geneProduct:miRNAs){
        	System.err.print(geneProduct.getName());
        	System.err.print("\t" + geneProduct.getPhysicalLocation().getNucleotide());
        	System.err.println("\t" + geneProduct.getPhysicalLocation().getNucleotideLength());
        }
        
        ArrayDesign arrayDesign = adService.findByShortName(this.arrayDesignName);
        if(arrayDesign == null){
        	System.err.println(" Array Design " + this.arrayDesignName + " doesn't exist");
        	return null;
        }
        
        HashMap<CompositeSequence, HashSet<GeneProduct>> results = new HashMap<CompositeSequence, HashSet<GeneProduct>>();

        //adService.thaw(arrayDesign);
        Collection<CompositeSequence> allCSs= adService.loadCompositeSequences(arrayDesign);
        for(CompositeSequence cs:allCSs){
        	//if(!cs.getName().equals("1460033_at")) continue;
        	Collection bs2gps = cs.getBiologicalCharacteristic().getBioSequence2GeneProduct();
        	HashSet<GeneProduct> mappedRNAs = new HashSet<GeneProduct>();
            for ( Object object : bs2gps ) {
                BioSequence2GeneProduct bs2gp = (BioSequence2GeneProduct) object;
                if (bs2gp instanceof BlatAssociation) {
                    BlatAssociation blatAssociation =  (BlatAssociation) bs2gp;
                    blatAssociationService.thaw(blatAssociation);
                    GeneProduct associatedGeneProduct = blatAssociation.getGeneProduct();
                    //BlatResult blatResult = blatAssociation.getBlatResult();
                    //mappedRNAs.addAll(checkMappedRNAs(blatResult.getTargetStart(), blatResult.getTargetEnd()));
                    mappedRNAs.addAll(checkMappedRNAs(associatedGeneProduct.getPhysicalLocation()));
                }
            }
            if(mappedRNAs.size() > 0)
            	results.put(cs, mappedRNAs);
        }
        
        
        try{
       		PrintStream output = new PrintStream(new FileOutputStream(new File(this.outFileName)));
       		for(CompositeSequence cs:results.keySet()){
       			output.print(cs.getName());
       			HashSet<GeneProduct> mappedmiRNAs = results.get(cs);
       			for(GeneProduct miRNA:mappedmiRNAs){
       				output.print("\t"+ miRNA.getName());
       			}
       			output.println();
       		}
        	output.close();
        }catch(Exception e){
        	return e;
        }
		return null;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		MicroRNAFinderCli finder = new MicroRNAFinderCli();
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Exception ex = finder.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
            watch.stop();
            log.info( "Elapsed time: " + watch.getTime() / 1000 + " seconds" );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
	}

}
