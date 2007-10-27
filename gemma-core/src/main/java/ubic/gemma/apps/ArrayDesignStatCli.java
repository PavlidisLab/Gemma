package ubic.gemma.apps;

import java.io.File;
import java.io.FileWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PredictedGeneImpl;
import ubic.gemma.model.genome.ProbeAlignedRegionImpl;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneService;

/**
 * CLI for ArrayDesignMapSummaryService
 * 
 * @author Paul
 * @version $Id$
 */
public class ArrayDesignStatCli extends ArrayDesignSequenceManipulatingCli {

	private ArrayDesignService adService;
	private GeneService geneService;
	private final static int MAXIMUM_COUNT = 10;
	private Collection<Long> geneIds = new HashSet<Long>();
    
    @Override
    protected void processOptions() {
        super.processOptions();
        // FIXME: add HTML output option.
    }
    Map<Long, Collection<Long>> getGeneId2CSIdsMap(Map<Long, Collection<Long>> csId2geneIds){
    	Map<Long, Collection<Long>> geneId2csIds = new HashMap<Long, Collection<Long>>();
    	for(Long csId:csId2geneIds.keySet()){
    		Collection<Long> geneIds = csId2geneIds.get(csId);
    		for(Long geneId:geneIds){
    			Collection<Long> csIds = geneId2csIds.get(geneId);
    			if(csIds == null){
    				csIds = new HashSet<Long>();
    				geneId2csIds.put(geneId,csIds);
    			}
    			csIds.add(csId);
    		}
    	}
    	return geneId2csIds;
    }
    int[] getStats(Map<Long, Collection<Long>> dataMap, boolean geneIdKey){
    	int[] res = new int[MAXIMUM_COUNT];
    	for(Long id:dataMap.keySet()){
    		if(geneIdKey){
    			if(!geneIds.contains(id))
    				continue;
    		}
    		int size = dataMap.get(id).size();
    		if(!geneIdKey){
    			size = 0;
    			Collection<Long> ids = dataMap.get(id);
    			for(Long geneId:ids){
    				if(geneIds.contains(geneId)) size++;
    			}
    		}
    		if(size == 0) continue;
    		if(size > MAXIMUM_COUNT)
    			size = MAXIMUM_COUNT;
    		res[size - 1]++;
    	}
    	return res;
    }
    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "Array design stat summary", args );
        if ( err != null ) return err;
        adService = ( ArrayDesignService ) this.getBean( "arrayDesignService" );
        geneService = ( GeneService ) this.getBean( "geneService" );
        Collection<ArrayDesign> allArrayDesigns = adService.loadAll();
        Map<Taxon, Collection<ArrayDesign>> taxon2arraydesign = new HashMap<Taxon, Collection<ArrayDesign>>();
        Collection<Long> adIds = new HashSet<Long>();
        for(ArrayDesign ad:allArrayDesigns){
        	adIds.add(ad.getId());
        	Taxon taxon = adService.getTaxon(ad.getId());
        	if(taxon == null){
        		System.err.println("ArrayDesign " + ad.getName() + " doesn't have a taxon");
        		continue;
        	}
        	Collection<ArrayDesign> ads = null;
        	ads = taxon2arraydesign.get(taxon);
        	if(ads == null){
        		ads = new HashSet<ArrayDesign>();
        		taxon2arraydesign.put(taxon, ads);
        	}
        	ads.add(ad);
        }
        Map<Long, Boolean> isMerged = adService.isMerged(adIds);
        Map<Long, Boolean> isSubsumed = adService.isSubsumed(adIds);
        try{
            FileWriter out = new FileWriter( new File( "arraydesignsummary.txt" ) );
            out.write("taxon\tarray design name\tgenes\tprobes\tcsKnownGenes\tcsPredictedGenes\tcsProbeAlignedRegions\tcsBioSequences\tcsBlatResults");
            for(int i = 0; i <= MAXIMUM_COUNT; i++)
            	out.write("\tP2G_"+i);
            for(int i = 1; i <= MAXIMUM_COUNT; i++)
            	out.write("\tG2P_"+i);
            out.write("\n");
            System.err.print("taxon\tarray design name\tgenes\tprobes\tcsGenes\tcsPredictedGenes\tcsProbeAlignedRegions\tcsBioSequences\tcsBlatResults\n");
        	for(Taxon taxon:taxon2arraydesign.keySet()){
        		Collection<ArrayDesign> ads = taxon2arraydesign.get(taxon);
        		Collection<Gene> allGenes = geneService.getGenesByTaxon(taxon);
            	for(Gene gene:allGenes){
            		if(!(gene instanceof PredictedGeneImpl) && !(gene instanceof ProbeAlignedRegionImpl)){
            			geneIds.add(gene.getId());
            		}
            	}
        		for(ArrayDesign ad:ads){
        			boolean merged = isMerged.get(ad.getId());
        			if(merged) continue;
        			boolean subsumed = isSubsumed.get(ad.getId());
        			if(subsumed) continue;
        			long numProbes = arrayDesignService.getCompositeSequenceCount( ad );
        			long numCsBioSequences = arrayDesignService.numCompositeSequenceWithBioSequences( ad );
        			long numCsBlatResults = arrayDesignService.numCompositeSequenceWithBlatResults( ad );
        			long numCsGenes = arrayDesignService.numCompositeSequenceWithGenes( ad );
        			long numCsPredictedGenes = arrayDesignService.numCompositeSequenceWithPredictedGenes( ad );
        			long numCsProbeAlignedRegions = arrayDesignService.numCompositeSequenceWithProbeAlignedRegion( ad );
        			long numCsPureGenes = numCsGenes - numCsPredictedGenes - numCsProbeAlignedRegions;
        			long numGenes = arrayDesignService.numGenes( ad );
        			Collection<DesignElement> allCSs = arrayDesignService.loadCompositeSequences(ad);
        			Collection<Long> csIds = new HashSet<Long>();
        			for(DesignElement cs:allCSs)
        				csIds.add(cs.getId());
                    // FIXME this used to provide only known genes.
        			Map<Long, Collection<Long>> csId2geneIds = geneService.getCS2GeneMap( csIds );
        			Map<Long, Collection<Long>> geneId2csIds = getGeneId2CSIdsMap(csId2geneIds);
        			int[] csStats = getStats(csId2geneIds, false);
        			int[] geneStats = getStats(geneId2csIds,true);
        			int cs2NoneGene = allCSs.size() - csId2geneIds.keySet().size();
        			out.write(taxon.getCommonName() + "\t" + ad.getName() + "\t" + numGenes + "\t" + numProbes
        					+ "\t"+ numCsGenes + "\t" + numCsPredictedGenes + "\t" + numCsProbeAlignedRegions + "\t"
        					+ numCsBioSequences + "\t" + numCsBlatResults + "\t" + cs2NoneGene);
        			for(int i = 0; i < MAXIMUM_COUNT; i++)
        				out.write("\t"+csStats[i]);
        			for(int i = 0; i < MAXIMUM_COUNT; i++)
        				out.write("\t"+geneStats[i]);
        			out.write("\n");
        			System.err.print(taxon.getCommonName() + "\t" + ad.getName() + "\t" + numGenes + "\t" + numProbes
        					+ "\t"+ numCsPureGenes + "\t" + numCsPredictedGenes + "\t" + numCsProbeAlignedRegions + "\t"
        					+ numCsBioSequences + "\t" + numCsBlatResults + "\n");
        		}
        	}
        	out.close();
        }catch(Exception e){
        	return e;
        }
        return null;
    }

    public static void main( String[] args ) {
        ArrayDesignStatCli s = new ArrayDesignStatCli();
        try {
            Exception ex = s.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

}
