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
import org.apache.commons.lang.time.StopWatch;
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
import ubic.gemma.model.genome.GeneImpl;
import ubic.gemma.model.genome.PredictedGeneImpl;
import ubic.gemma.model.genome.ProbeAlignedRegionImpl;
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
    private int STRINGENCY = 2;
    
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
 
    public void find(Taxon taxon){
    	Collection <Gene> allGenes = geneService.getGenesByTaxon(taxon);
    	Collection <Gene> genes = new HashSet<Gene>();
    	for(Gene gene:allGenes){
    		if(!(gene instanceof PredictedGeneImpl) && !(gene instanceof ProbeAlignedRegionImpl)){
    			genes.add(gene);
			}
    	}
    	log.info("Get " + genes.size() + " genes");
    	if(genes == null || genes.size() == 0) return;
    	this.find(genes);
    }
    public void find(Collection<Gene> genes){
    	if(genes == null || genes.size() == 0) return;
    	Taxon taxon = genes.iterator().next().getTaxon();
    	Collection <ExpressionExperiment> ees = this.eeService.findByTaxon(taxon);
    	if(ees == null || ees.size() == 0) return;
    	this.find(genes, ees);
    }
    public void find(Collection<Gene> genes, Collection <ExpressionExperiment> ees){
    	if(genes == null || ees == null ||  genes.size() == 0 || ees.size() == 0) return;
    	Collection <Gene> genesInTaxon = this.geneService.getGenesByTaxon(genes.iterator().next().getTaxon());
    	if(genesInTaxon == null || genesInTaxon.size() == 0) return;
    	
    	Collection <Gene> coExpressedGenes = new HashSet<Gene>();
    	for(Gene gene:genesInTaxon){
    		if(!(gene instanceof PredictedGeneImpl) && !(gene instanceof ProbeAlignedRegionImpl)){
    			coExpressedGenes.add(gene);
			}
    	}
    	this.init(genes, ees, coExpressedGenes);
    	this.finder(genes);
    }
    private void finder(Collection<Gene> genes){
    	for(Gene gene:genes){
    		System.out.println(gene.getName());
    		Map<Long, Collection<Long>> geneEEMap = geneService.getCoexpressedGeneMap(STRINGENCY, gene);
			this.count(gene.getId(),geneEEMap);
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
    public void output(Gene gene, int num){
    	int row = this.linkCount.getRowIndexByName(gene.getId());
    	if(row < 0 || row>= this.linkCount.rows()){
    		log.info("No this Gene");
    		return;
    	}
    	for(int col = 0; col < this.linkCount.columns(); col++)
			if(this.linkCount.bitCount(row,col) >= num){
				System.err.println(this.getColGene(col).getName() + " " + this.linkCount.bitCount(row,col));
    	}
    	System.err.println("=====================================================");
    	for(int col = 0; col < this.linkCount.columns(); col++)
			if(this.linkCount.bitCount(row,col) >= num){
				System.err.println(this.getColGene(col).getName());
    	}
    }
    public void output(int num){
    	int count = 0;
    	for(int i = 0; i < this.linkCount.rows(); i++)
    		for(int j = 0; j < this.linkCount.columns(); j++){
    			if(this.linkCount.bitCount(i,j) >= num){
    				System.err.println(this.getRowGene(i).getName() + "  " + this.getColGene(j).getName() + " " + this.linkCount.bitCount(i,j));
    				count++;
    			}
    		}
    	System.err.println("Total Links " + count);
    }
    public void outputStat(){
    	int maxNum = 50;
    	Vector count = new Vector(maxNum);
    	for(int i = 0; i < maxNum; i++)
    		count.add(0);
    	for(int i = 0; i < this.linkCount.rows(); i++){
	//		System.err.println(i);
    		for(int j = i+1; j < this.linkCount.columns(); j++){
    			int num = this.linkCount.bitCount(i,j);
    			if(num == 0)continue;
    			if(num > maxNum){
    				for(;maxNum < num; maxNum++)
    					count.add(0);
    			}
   				Integer tmpno = (Integer)count.elementAt(num-1);
   				tmpno = tmpno + 1;
   				count.setElementAt(tmpno, num-1);
    		}
    	}
    	for(int i = 0; i <count.size(); i++){
    		System.err.print(i+"["+count.elementAt(i)+"] ");
    		if(i%10 == 0) System.err.println("");
    	}
    }
    private void count(Long rowGeneId, Map <Long, Collection<Long>> geneEEsMap){
    	int rowIndex = -1, colIndex = -1, eeIndex = -1;
    	rowIndex = this.linkCount.getRowIndexByName(rowGeneId);
    	for(Long colGeneId:geneEEsMap.keySet()){
    		try{
    			Integer index = null;
    			Collection<Long> eeIds = geneEEsMap.get(colGeneId);
    			colIndex = this.linkCount.getColIndexByName(colGeneId);
    			for(Long eeId:eeIds){
    				index = this.eeMap.get(eeId);
    				if(index == null){
    					log.info("Couldn't find the ee index for ee " + eeId);
    					continue;
    				}
    				eeIndex = index.intValue();
    				this.linkCount.set(rowIndex,colIndex,eeIndex);
    			}
    		}catch(Exception e){
    			continue;
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
            int vectorSize = 0;
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
                if(Integer.valueOf(subItems[1].trim()).intValue() > vectorSize)
                	vectorSize =Integer.valueOf(subItems[1].trim()).intValue(); 
            }
            this.allEE = new Vector(vectorSize+1);
            for(int i = 0; i < vectorSize+1; i++)
            	this.allEE.addElement(new Long(i));
            
            for(Long iter:this.eeMap.keySet()){
            	int index = this.eeMap.get(iter).intValue();
            	this.allEE.setElementAt(iter, index);
            }
            log.info( "Got " + this.eeMap.size() + " in EE MAP" );
            in.close();
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }
    public CompressedNamedBitMatrix getCountMatrix(){
        return this.linkCount;
    }
    public Vector getEEIndex(){
    	return this.allEE;
    }
}
