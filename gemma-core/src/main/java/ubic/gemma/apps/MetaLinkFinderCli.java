/**
 * 
 */
package ubic.gemma.apps;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.time.StopWatch;

import ubic.gemma.analysis.linkAnalysis.MetaLinkFinder;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * @author xwan
 *
 */
public class MetaLinkFinderCli extends AbstractSpringAwareCLI {

	/* (non-Javadoc)
	 * @see ubic.gemma.util.AbstractCLI#buildOptions()
	 */
    private ExpressionExperimentService eeService;
	@Override
	protected void buildOptions() {
		// TODO Auto-generated method stub

	}
	protected void processOptions() {
		super.processOptions();
	}
	/****distribute the expression experiments to the different classes of quantitation type.
     *  The reason to do this is because the collection of expression experiment for Probe2Probe2
     *  Query should share the same preferred quantitation type. The returned object is a map between
     *  quantitation type and a set of expression experiment perferring this quantitation type.
     * @return
	 */
	private Map<QuantitationType, Collection> preprocess(Collection<ExpressionExperiment> ees){
        Map eemap = new HashMap<QuantitationType, Collection>();
        for(ExpressionExperiment ee:ees){
            Collection<QuantitationType> eeQT = this.eeService.getQuantitationTypes(ee);
            for (QuantitationType qt : eeQT) {
                if(qt.getIsPreferred()){
                    Collection<ExpressionExperiment> eeCollection = (Collection)eemap.get( qt );
                    if(eeCollection == null){
                        eeCollection = new HashSet<ExpressionExperiment>();
                        eemap.put( qt, eeCollection );
                    }
                    eeCollection.add( ee );
                    break;
                }
            }
        }
		return null;
	}
	private Collection <Gene> getGenes(GeneService geneService){
		HashSet<Gene> genes = new HashSet();
		Gene gene = geneService.load(461722);
		genes.add(gene);
		return genes;
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
	/* (non-Javadoc)
	 * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
	 */
	@Override
	protected Exception doWork(String[] args) {
		// TODO Auto-generated method stub
		Exception err = processCommandLine("Link Analysis Data Loader", args);
		if (err != null) {
			return err;
		}
		try {
			Probe2ProbeCoexpressionService p2pService = (Probe2ProbeCoexpressionService) this.getBean("probe2ProbeCoexpressionService");

			DesignElementDataVectorService deService = (DesignElementDataVectorService) this.getBean("designElementDataVectorService");

			eeService = (ExpressionExperimentService) this.getBean("expressionExperimentService");			
			
			GeneService geneService = (GeneService) this.getBean("geneService");
			
			MetaLinkFinder linkFinder = new MetaLinkFinder(p2pService, deService, eeService, geneService);
			
			Taxon taxon = this.getTaxon("human");
			Collection<ExpressionExperiment> ees = null;
			if(taxon != null)
				ees = eeService.getByTaxon(taxon);
            else
                ees = eeService.loadAll();
			if(ees == null || ees.size() == 0){
				log.info("No Expression Experiment is found");
				return null;
			}
			else
				log.info("Found " + ees.size() + " Expression Experiment");
            
            Map<QuantitationType, Collection> eeMap = preprocess(ees);
            
            for(QuantitationType qt:eeMap.keySet()){
                ees = eeMap.get( qt );
                linkFinder.find(this.getGenes(geneService), ees, qt);
            }

            linkFinder.output(2);
		} catch (Exception e) {
			log.error(e);
			return e;
		}
		return null;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		MetaLinkFinderCli linkFinderCli = new MetaLinkFinderCli();
		StopWatch watch = new StopWatch();
		watch.start();
		try {
			Exception ex = linkFinderCli.doWork(args);
			if (ex != null) {
				ex.printStackTrace();
			}
			watch.stop();
			log.info(watch.getTime());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
