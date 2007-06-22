/**
 * 
 */
package ubic.gemma.analysis.linkAnalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
import ubic.gemma.ontology.GeneOntologyService;
import ubic.gemma.ontology.OntologyTerm;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PredictedGeneImpl;
import ubic.gemma.model.genome.ProbeAlignedRegionImpl;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneService;
import cern.colt.list.ObjectArrayList;

/**
 * This finder does the query on the Probe2ProbeCoexpression and outputs the meta links between genes
 * 
 * @author xwann
 */
public class MetaLinkFinder {
    private GeneService geneService = null;
    private ExpressionExperimentService eeService = null;
    private LinkAnalysisUtilService utilService = null;
    private BitMatrixUtil bitMatrixUtil = null;
    protected static final Log log = LogFactory.getLog( MetaLinkFinder.class );
    private final int STRINGENCY = 2;

    public MetaLinkFinder() {
    }

    public void find( Taxon taxon ) {
        Collection<Gene> allGenes = geneService.getGenesByTaxon( taxon );
        Collection<Gene> genes = new HashSet<Gene>();
        for ( Gene gene : allGenes ) {
            if ( !( gene instanceof PredictedGeneImpl ) && !( gene instanceof ProbeAlignedRegionImpl ) ) {
                genes.add( gene );
            }
        }
        log.info( "Get " + genes.size() + " genes" );
        if ( genes == null || genes.size() == 0 ) return;
        this.find( genes );
    }

    public void find( Collection<Gene> genes ) {
        if ( genes == null || genes.size() == 0 ) return;
        Taxon taxon = genes.iterator().next().getTaxon();
        Collection<ExpressionExperiment> ees = eeService.findByTaxon( taxon );
        if ( ees == null || ees.size() == 0 ) return;
        this.find( genes, ees );
    }

    public void find( Collection<Gene> genes, Collection<ExpressionExperiment> ees ) {
        if ( genes == null || ees == null || genes.size() == 0 || ees.size() == 0 ) return;
        Collection<Gene> genesInTaxon = geneService.getGenesByTaxon( genes.iterator().next().getTaxon() );
        if ( genesInTaxon == null || genesInTaxon.size() == 0 ) return;

        Collection<Gene> coExpressedGenes = new HashSet<Gene>();
        for ( Gene gene : genesInTaxon ) {
            if ( !( gene instanceof PredictedGeneImpl ) && !( gene instanceof ProbeAlignedRegionImpl ) ) {
                coExpressedGenes.add( gene );
            }
        }
        bitMatrixUtil = new BitMatrixUtil( ees, genes, coExpressedGenes );
        this.finder( genes );
    }

    private void finder( Collection<Gene> genes ) {
        int i = 1;
        for ( Gene gene : genes ) {
            System.out.println( i + "/" + genes.size() + "\t" + gene.getName() );
            // Get the gene->eeIds map
            CoexpressionCollectionValueObject coexpressed = ( CoexpressionCollectionValueObject ) geneService
                    .getCoexpressedGenes( gene, null, STRINGENCY );
            Map<Long, Collection<Long>> geneEEMap = coexpressed.getGeneCoexpressionType()
                    .getSpecificExpressionExperiments();
            this.count( gene.getId(), geneEEMap );
            i++;
        }
    }

    /**
     * Output the gene
     * @param gene
     * @param num
     */
    public void output( Gene gene, int num ) {
        int row = bitMatrixUtil.getMatrix().getRowIndexByName( gene.getId() );
        if ( row < 0 || row >= bitMatrixUtil.getMatrix().rows() ) {
            log.info( "Gene does not exist" );
            return;
        }
        for ( int col = 0; col < bitMatrixUtil.getMatrix().columns(); col++ )
            if ( bitMatrixUtil.getMatrix().bitCount( row, col ) >= num ) {
                System.err.println( bitMatrixUtil.getColGene( col ).getName() + " " + bitMatrixUtil.getMatrix().bitCount( row, col ) );
            }
        System.err.println( "=====================================================" );
        for ( int col = 0; col < bitMatrixUtil.getMatrix().columns(); col++ )
            if ( bitMatrixUtil.getMatrix().bitCount( row, col ) >= num ) {
                System.err.println( bitMatrixUtil.getColGene( col ).getName() );
            }
    }

    public void output( int num ) {
        int count = 0;
        for ( int i = 0; i < bitMatrixUtil.getMatrix().rows(); i++ )
            for ( int j = 0; j < bitMatrixUtil.getMatrix().columns(); j++ ) {
                if ( bitMatrixUtil.getMatrix().bitCount( i, j ) >= num ) {
                    System.err.println( bitMatrixUtil.getRowGene( i ).getName() + "  " + bitMatrixUtil.getColGene( j ).getName() + " "
                            + bitMatrixUtil.getMatrix().bitCount( i, j ) );
                    count++;
                }
            }
        System.err.println( "Total Links " + count );
    }

    public void outputStat() {
        int maxNum = 50;
        Vector count = new Vector( maxNum );
        for ( int i = 0; i < maxNum; i++ )
            count.add( 0 );
        for ( int i = 0; i < bitMatrixUtil.getMatrix().rows(); i++ ) {
            // System.err.println(i);
            for ( int j = i + 1; j < bitMatrixUtil.getMatrix().columns(); j++ ) {
                int num = bitMatrixUtil.getMatrix().bitCount( i, j );
                if ( num == 0 ) continue;
                if ( num > maxNum ) {
                    for ( ; maxNum < num; maxNum++ )
                        count.add( 0 );
                }
                Integer tmpno = ( Integer ) count.elementAt( num - 1 );
                tmpno = tmpno + 1;
                count.setElementAt( tmpno, num - 1 );
            }
        }
        for ( int i = 0; i < count.size(); i++ ) {
            System.err.print( i + "[" + count.elementAt( i ) + "] " );
            if ( i % 10 == 0 ) System.err.println( "" );
        }
    }

    private void count( Long rowGeneId, Map<Long, Collection<Long>> geneEEsMap ) {
        int rowIndex = -1, colIndex = -1, eeIndex = -1;
        rowIndex = bitMatrixUtil.getMatrix().getRowIndexByName( rowGeneId );
        for ( Long colGeneId : geneEEsMap.keySet() ) {
            try {
                Integer index = null;
                Collection<Long> eeIds = geneEEsMap.get( colGeneId );
                colIndex = bitMatrixUtil.getMatrix().getColIndexByName( colGeneId );
                for ( Long eeId : eeIds ) {
                    eeIndex = bitMatrixUtil.getEEIndex(eeId);
                    if ( eeIndex < 0 ) {
                        log.info( "Couldn't find the ee index for ee " + eeId );
                        continue;
                    }
                    bitMatrixUtil.getMatrix().set( rowIndex, colIndex, eeIndex );
                }
            } catch ( Exception e ) {
                continue;
            }
        }
    }
    public String getLinkName( long id ) {
        Gene[] pairedGene = bitMatrixUtil.getPairedGenes( id );
        return pairedGene[0].getName() + "_" + pairedGene[1].getName() + "_"
                + utilService.computeGOOverlap( pairedGene[0], pairedGene[1] );
    }

    // rank: the number of top ranked GO terms to return
    public Map<OntologyTerm, Integer> computeGOOverlap( Collection<Long> treeIds, int rank ) {
        Collection<Gene> genes = new HashSet<Gene>();
        for ( Long treeId : treeIds ) {
            int row = ( int ) ( treeId / BitMatrixUtil.shift );
            int col = ( int ) ( treeId % BitMatrixUtil.shift );
            genes.add( bitMatrixUtil.getRowGene( row ) );
            genes.add( bitMatrixUtil.getColGene( col ) );
        }
        Map<OntologyTerm, Integer> res = new HashMap<OntologyTerm, Integer>();
        ObjectArrayList counter = new ObjectArrayList( rank );
        for ( int i = 0; i < counter.size(); i++ )
            counter.add( new Integer( 0 ) );
        for ( Gene gene : genes ) {
            Collection<OntologyTerm> goEntries = utilService.getGoTerms( gene );
            for ( OntologyTerm goEntry : goEntries ) {
                Integer goNum = new Integer( 1 );
                if ( res.containsKey( goEntry ) ) {
                    goNum = res.get( goEntry );
                    goNum = goNum + 1;
                    res.put( goEntry, goNum );
                } else {
                    res.put( ( OntologyTerm ) goEntry, goNum );
                }
            }
        }
        if ( rank >= res.keySet().size() ) return res;
        for ( OntologyTerm ontologyTerm : res.keySet() ) {
            Integer goNum = res.get( ontologyTerm );
            counter.add( goNum );
        }
        counter.sort();
        Integer threshold = ( Integer ) counter.get( counter.size() - rank );
        Collection<OntologyTerm> removed = new HashSet<OntologyTerm>();
        for ( OntologyTerm ontologyTerm : res.keySet() ) {
            Integer goNum = res.get( ontologyTerm );
            if ( goNum < threshold ) removed.add( ontologyTerm );
        }
        for ( OntologyTerm ontologyTerm : removed )
            res.remove( ontologyTerm );
        return res;
    }
    public BitMatrixUtil getBitMatrixUtil(){
    	return this.bitMatrixUtil;
    }
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }
    public void setEEService( ExpressionExperimentService eeService ) {
        this.eeService = eeService;
    }
    public void setUtilService( LinkAnalysisUtilService utilService ) {
        this.utilService = utilService;
    }

}
