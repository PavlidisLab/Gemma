/**
 * 
 */
package ubic.gemma.analysis.linkAnalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.CompressedNamedBitMatrix;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneService;

/**
 * @author xwan
 * This finder does the query on the Probe2ProbeCoexpression and outputs the meta links between genes
 *
 */
public class MetaLinkFinder {
    private Probe2ProbeCoexpressionService ppService = null;
    private DesignElementDataVectorService deService = null;
    private GeneService geneService = null;
    private ExpressionExperimentService eeService = null;
    private CompressedNamedBitMatrix linkCount = null;
    private HashMap<Long, Integer> eeMap = null;
    private HashMap<Object, Set> probeToGenes = null;
    private Vector allEE = null;
    protected static final Log log = LogFactory.getLog( MetaLinkFinder.class );
    private int bitNum = 0;
    
    public MetaLinkFinder(Probe2ProbeCoexpressionService ppService, DesignElementDataVectorService deService, ExpressionExperimentService eeService, GeneService geneService){
    	assert(ppService != null);
    	assert(deService != null);
    	assert(geneService != null);
    	assert(eeService != null);
    	this.ppService = ppService;
    	this.deService = deService;
    	this.geneService = geneService;
    	this.eeService = eeService;
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
                    	log.info(" Get Quantitation Type : " + qt.getName()+ ":"+qt.getType());
                        eeCollection = new HashSet<ExpressionExperiment>();
                        eemap.put( qt, eeCollection );
                    }
                    eeCollection.add( ee );
                    break;
                }
            }
        }
		return eemap;
	}
    public void find(Taxon taxon){
    	Collection <Gene> genes = geneService.getGenesByTaxon(taxon);
    	if(genes == null || genes.size() == 0) return;
    	this.find(genes);
    }
    public void find(Collection<Gene> genes){
    	if(genes == null || genes.size() == 0) return;
    	Taxon taxon = genes.iterator().next().getTaxon();
    	Collection <ExpressionExperiment> ees = this.eeService.getByTaxon(taxon);
    	if(ees == null || ees.size() == 0) return;
    	this.find(genes, ees);
    }
    public void find(Collection<Gene> genes, Collection <ExpressionExperiment> ees){
    	if(genes == null || ees == null ||  genes.size() == 0 || ees.size() == 0) return;
    	Collection <Gene> genesInTaxon = this.geneService.getGenesByTaxon(genes.iterator().next().getTaxon());
    	if(genesInTaxon == null || genesInTaxon.size() == 0) return;
    	this.init(genes, ees, genesInTaxon);

    	Map<QuantitationType, Collection> eeMap = preprocess(ees);
        for(QuantitationType qt:eeMap.keySet()){
            ees = eeMap.get( qt );
            this.finder(genes, ees, qt);
        }
    }
    private void finder(Collection<Gene> genes, Collection <ExpressionExperiment> ees, QuantitationType qt){
    	
    	for(Gene gene:genes){
    		Collection<DesignElementDataVector> p2plinks = ppService.findCoexpressionRelationships(gene,ees,qt);
    		if(p2plinks == null || p2plinks.size() == 0) continue;
    		log.info("Get "+ p2plinks.size() + " links for " + gene.getName());
    		this.count(gene, p2plinks);
    	}

    }
    public Gene getRowGene(int i){
    	Object geneId = this.linkCount.getRowName(i);
    	Gene gene = this.geneService.load(((Long)geneId).longValue());
    	return gene;
    }
    
    public Gene getColGene(int i){
    	Object geneId = this.linkCount.getColName(i);
    	Gene gene = this.geneService.load(((Long)geneId).longValue());
    	return gene;
    }
    
    public ExpressionExperiment getEE(int i){
    	Object eeId = this.allEE.elementAt(i);
    	ExpressionExperiment ee = this.eeService.findById((Long)eeId);
    	return ee;
    }

    public void output(int num){
    	int count = 0;
    	for(int i = 0; i < this.linkCount.rows(); i++)
    		for(int j = 0; j < this.linkCount.columns(); j++){
    			if(this.linkCount.bitCount(i,j) >= num){
    				System.err.println(this.getRowGene(i).getName() + "  " + this.getColGene(j).getName());
    				count++;
    			}
    		}
    	System.err.println("Total Links " + count);
    }
    private void count(Gene rowGene, Collection<DesignElementDataVector> p2plinks){
    	int rowIndex = -1, colIndex = -1, eeIndex = -1;
    	rowIndex = this.linkCount.getRowIndexByName(rowGene.getId());
    	
    	if(this.probeToGenes.size() > 50000) this.probeToGenes.clear();
    	Collection<DesignElementDataVector> probes = new HashSet<DesignElementDataVector>();
    	for(DesignElementDataVector p2pIter:p2plinks){
    		HashSet <Gene> pairedGenes = (HashSet)probeToGenes.get(p2pIter);
    		if(pairedGenes == null)
    			probes.add(p2pIter);
    	}
    	HashMap<Object, Set> tmpProbeToGenes = (HashMap)this.deService.getGenes(probes);
    	this.probeToGenes.putAll(tmpProbeToGenes);
    	
    	//this.probeToGenes= (HashMap)this.deService.getGenes(p2plinks);
    	Integer index = null;
    	ExpressionExperiment ee = null;
    	for(DesignElementDataVector p2pIter:p2plinks){
    		ee = p2pIter.getExpressionExperiment();
    		index = this.eeMap.get(ee.getId());
    		if(index == null){
    			log.info("Couldn't find the ee index for ee " + ee.getId());
    			continue;
    		}
    		eeIndex = index.intValue();
    		
    		HashSet <Gene> pairedGenes = (HashSet)probeToGenes.get(p2pIter);
    		if(pairedGenes == null || pairedGenes.size() == 0){
    			continue;
    		}
    		if(pairedGenes.contains(rowGene)){
    			continue;
    		}
    		for(Gene colGene:pairedGenes){
        		colIndex = this.linkCount.getColIndexByName(colGene.getId());
        		if(colIndex >= 0 && colIndex < this.linkCount.columns())
        			this.linkCount.set(rowIndex,colIndex,eeIndex);
    		}
    	}
    }
    private void init(Collection<Gene> genes, Collection <ExpressionExperiment> ees, Collection<Gene> genesInTaxon){
    	this.linkCount = new CompressedNamedBitMatrix(genes.size(), genesInTaxon.size(), ees.size());
    	this.probeToGenes = new HashMap<Object, Set>();
    	for(Gene geneIter:genes){
    		this.linkCount.addRowName(geneIter.getId());
    	}
    	for(Gene geneIter:genesInTaxon){
    		this.linkCount.addColumnName(geneIter.getId());
    	}
    	this.eeMap = new HashMap();
    	this.allEE = new Vector();
    	int index = 0;
    	for(ExpressionExperiment eeIter:ees){
    		eeMap.put(eeIter.getId(), new Integer(index));
    		this.allEE.add(eeIter.getId());
    		index++;
    	}
    }
    public boolean toFile(String matrixFile, String eeMapFile){
        if(!this.linkCount.toFile( matrixFile )) return false;
        try{
            FileWriter out = new FileWriter(new File(eeMapFile));
            for(Long index:this.eeMap.keySet()){
                out.write( index + "\t"+(Integer)this.eeMap.get( index )+ "\n" );
            }
            out.close();
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }
    public boolean fromFile(String matrixFile, String eeMapFile){
        try{
            BufferedReader in = new BufferedReader(new FileReader(new File(matrixFile)));
            String row = null;
            int i;
            boolean hasConfig = false, hasRowNames = false, hasColNames = false;
            while ( ( row = in.readLine() ) != null ) {
                row = row.trim();
                if(StringUtils.isBlank( row )) continue;
                String [] subItems = row.split( "\t" );
                for( i = 0; i < subItems.length; i++)
                    if(StringUtils.isBlank( subItems[i] )) break;
                if( i != subItems.length){
                    log.info( "The empty Element is not allowed: " + row );
                    return false;
                }
                if(!hasConfig){
                    if(subItems.length != 3){
                        log.info( "Data File Format Error for configuration " + row );
                        return false;
                    }
                    this.linkCount = new CompressedNamedBitMatrix(Integer.valueOf( subItems[0] ),Integer.valueOf( subItems[1] ),Integer.valueOf( subItems[2] ));
                    hasConfig = true;
                } else if(!hasRowNames){
                    if(subItems.length != this.linkCount.rows()){
                        log.info( "Data File Format Error for Row Names " + row );
                        return false;
                    }
                    for( i = 0; i < subItems.length; i++)
                        this.linkCount.addRowName( new Long(subItems[i].trim()) );
                    hasRowNames = true;;
                }
                else if(!hasColNames){
                    if(subItems.length != this.linkCount.columns()){
                        log.info( "Data File Format Error for Col Names " + row );
                        return false;
                    }
                    for( i = 0; i < subItems.length; i++)
                        this.linkCount.addColumnName( new Long(subItems[i].trim()) );
                    hasColNames = true;
                } else{                    
                    int rowIndex = Integer.valueOf( subItems[0] );
                    int colIndex = Integer.valueOf( subItems[1] );
                    double values[] = new double[subItems.length - 2];
                    for( i = 2; i < subItems.length; i++)
                        values[i-2] = Double.longBitsToDouble(Long.parseLong(subItems[i],16));
                    if(!this.linkCount.set( rowIndex, colIndex, values)){
                        log.info( "Data File Format Error for Data " + row );
                        return false;
                    }
                }
            }
            in.close();
            
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
        try{
            BufferedReader in = new BufferedReader(new FileReader(new File(eeMapFile)));
            this.eeMap = new HashMap<Long, Integer>();
            String row = null;
            while ( ( row = in.readLine() ) != null ) {
                row = row.trim();
                if(StringUtils.isBlank( row )) continue;
                String [] subItems = row.split( "\t" );
                if(subItems.length != 2) continue;
                int i = 0;
                for( i = 0; i < subItems.length; i++)
                    if(StringUtils.isBlank( subItems[i] )) break;
                if( i != subItems.length){
                    log.info( "Data File Format Error for ee Map " + row );
                    return false;
                }
                this.eeMap.put( new Long( subItems[0].trim() ), new Integer( subItems[1].trim() ) );
            }
            log.info( "Got " + this.eeMap.size() + " in EE MAP" );
            in.close();
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
