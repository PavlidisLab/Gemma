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
    private static GeneOntologyService geneOntologyService = null;
    private static Gene2GOAssociationService gene2GoAssociationService = null;
    private static GeneService geneService = null;
    private static ExpressionExperimentService eeService = null;

    private static Vector allEE = null;
    private static int shift = 50000; // to encode two geneid into one long id
    public static CompressedNamedBitMatrix linkCount = null;

    private HashMap<Long, Integer> eeMap = null;
    private HashMap<Object, Set> probeToGenes = null;
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
        this.init( genes, ees, coExpressedGenes );
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
    public static void output( Gene gene, int num ) {
        int row = linkCount.getRowIndexByName( gene.getId() );
        if ( row < 0 || row >= linkCount.rows() ) {
            log.info( "Gene does not exist" );
            return;
        }
        for ( int col = 0; col < linkCount.columns(); col++ )
            if ( linkCount.bitCount( row, col ) >= num ) {
                System.err.println( getColGene( col ).getName() + " " + linkCount.bitCount( row, col ) );
            }
        System.err.println( "=====================================================" );
        for ( int col = 0; col < linkCount.columns(); col++ )
            if ( linkCount.bitCount( row, col ) >= num ) {
                System.err.println( getColGene( col ).getName() );
            }
    }

    public static void output( int num ) {
        int count = 0;
        for ( int i = 0; i < linkCount.rows(); i++ )
            for ( int j = 0; j < linkCount.columns(); j++ ) {
                if ( linkCount.bitCount( i, j ) >= num ) {
                    System.err.println( getRowGene( i ).getName() + "  " + getColGene( j ).getName() + " "
                            + linkCount.bitCount( i, j ) );
                    count++;
                }
            }
        System.err.println( "Total Links " + count );
    }

    public static void outputStat() {
        int maxNum = 50;
        Vector count = new Vector( maxNum );
        for ( int i = 0; i < maxNum; i++ )
            count.add( 0 );
        for ( int i = 0; i < linkCount.rows(); i++ ) {
            // System.err.println(i);
            for ( int j = i + 1; j < linkCount.columns(); j++ ) {
                int num = linkCount.bitCount( i, j );
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
        rowIndex = linkCount.getRowIndexByName( rowGeneId );
        for ( Long colGeneId : geneEEsMap.keySet() ) {
            try {
                Integer index = null;
                Collection<Long> eeIds = geneEEsMap.get( colGeneId );
                colIndex = this.linkCount.getColIndexByName( colGeneId );
                for ( Long eeId : eeIds ) {
                    index = this.eeMap.get( eeId );
                    if ( index == null ) {
                        log.info( "Couldn't find the ee index for ee " + eeId );
                        continue;
                    }
                    eeIndex = index.intValue();
                    linkCount.set( rowIndex, colIndex, eeIndex );
                }
            } catch ( Exception e ) {
                continue;
            }
        }
    }

    private void init( Collection<Gene> genes, Collection<ExpressionExperiment> ees, Collection<Gene> genesInTaxon ) {
        linkCount = new CompressedNamedBitMatrix( genes.size(), genesInTaxon.size(), ees.size() );
        shift = linkCount.rows() > linkCount.columns() ? linkCount.rows() : linkCount.columns();
        this.probeToGenes = new HashMap<Object, Set>();
        for ( Gene geneIter : genes ) {
            linkCount.addRowName( geneIter.getId() );
        }
        for ( Gene geneIter : genesInTaxon ) {
            linkCount.addColumnName( geneIter.getId() );
        }
        eeMap = new HashMap();
        allEE = new Vector();
        int index = 0;
        for ( ExpressionExperiment eeIter : ees ) {
            eeMap.put( eeIter.getId(), new Integer( index ) );
            allEE.add( eeIter.getId() );
            index++;
        }
    }

    public void toFile( String matrixFile, String eeMapFile ) throws IOException {
        linkCount.toFile( matrixFile );
        FileWriter out = new FileWriter( new File( eeMapFile ) );
        for ( Long index : this.eeMap.keySet() ) {
            out.write( index + "\t" + this.eeMap.get( index ) + "\n" );
        }
        out.close();
    }

    public void fromFile( String matrixFile, String eeMapFile ) throws IOException {
        BufferedReader in = new BufferedReader( new FileReader( new File( matrixFile ) ) );
        String row = null;
        int i;
        boolean hasConfig = false, hasRowNames = false, hasColNames = false;
        while ( ( row = in.readLine() ) != null ) {
            row = row.trim();
            if ( StringUtils.isBlank( row ) ) continue;
            String[] subItems = row.split( "\t" );
            for ( i = 0; i < subItems.length; i++ )
                if ( StringUtils.isBlank( subItems[i] ) ) break;
            if ( i != subItems.length ) {
                String mesg = "The empty Element is not allowed: " + row;
                log.info( mesg );
                throw new IOException( mesg );
            }
            if ( !hasConfig ) {
                if ( subItems.length != 3 ) {
                    String mesg = "Data File Format Error for configuration " + row;
                    log.info( mesg );
                    throw new IOException( mesg );
                }
                linkCount = new CompressedNamedBitMatrix( Integer.valueOf( subItems[0] ),
                        Integer.valueOf( subItems[1] ), Integer.valueOf( subItems[2] ) );
                hasConfig = true;
            } else if ( !hasRowNames ) {
                if ( subItems.length != linkCount.rows() ) {
                    String mesg = "Data File Format Error for Row Names " + row;
                    log.info( mesg );
                    throw new IOException( mesg );
                }
                for ( i = 0; i < subItems.length; i++ )
                    linkCount.addRowName( new Long( subItems[i].trim() ) );
                hasRowNames = true;
            } else if ( !hasColNames ) {
                if ( subItems.length != linkCount.columns() ) {
                    String mesg = "Data File Format Error for Col Names " + row;
                    log.info( mesg );
                    throw new IOException( mesg );
                }
                for ( i = 0; i < subItems.length; i++ )
                    linkCount.addColumnName( new Long( subItems[i].trim() ) );
                hasColNames = true;
            } else {
                int rowIndex = Integer.valueOf( subItems[0] );
                int colIndex = Integer.valueOf( subItems[1] );
                double values[] = new double[subItems.length - 2];
                for ( i = 2; i < subItems.length; i++ )
                    values[i - 2] = Double.longBitsToDouble( Long.parseLong( subItems[i], 16 ) );
                if ( !linkCount.set( rowIndex, colIndex, values ) ) {
                    String mesg = "Data File Format Error for Data " + row;
                    log.info( mesg );
                    throw new IOException( mesg );
                }
            }
        }
        in.close();
        if ( eeMapFile != null ) {
            in = new BufferedReader( new FileReader( new File( eeMapFile ) ) );
            this.eeMap = new HashMap<Long, Integer>();
            int vectorSize = 0;
            row = null;
            while ( ( row = in.readLine() ) != null ) {
                row = row.trim();
                if ( StringUtils.isBlank( row ) ) continue;
                String[] subItems = row.split( "\t" );
                if ( subItems.length != 2 ) continue;
                for ( i = 0; i < subItems.length; i++ )
                    if ( StringUtils.isBlank( subItems[i] ) ) break;
                if ( i != subItems.length ) {
                    String mesg = "Data File Format Error for ee Map " + row;
                    log.info( mesg );
                    throw new IOException(mesg);
                }
                this.eeMap.put( new Long( subItems[0].trim() ), new Integer( subItems[1].trim() ) );
                if ( Integer.valueOf( subItems[1].trim() ).intValue() > vectorSize )
                    vectorSize = Integer.valueOf( subItems[1].trim() ).intValue();
            }
            allEE = new Vector( vectorSize + 1 );
            for ( i = 0; i < vectorSize + 1; i++ )
                allEE.addElement( new Long( i ) );

            for ( Long iter : this.eeMap.keySet() ) {
                int index = this.eeMap.get( iter ).intValue();
                allEE.setElementAt( iter, index );
            }
            log.info( "Got " + this.eeMap.size() + " in EE MAP" );
            in.close();
        }
        shift = linkCount.rows() > linkCount.columns() ? linkCount.rows() : linkCount.columns();
    }

    public void saveLinkMatrix( String outFile, int stringency ) {
        try {
            ObjectArrayList nodes = new ObjectArrayList();

            FileWriter out = new FileWriter( new File( outFile ) );
            for ( int i = 0; i < MetaLinkFinder.linkCount.rows(); i++ ) {
                if ( i % 1000 == 0 ) System.err.println( i + " -> " + MetaLinkFinder.linkCount.rows() );
                for ( int j = i + 1; j < MetaLinkFinder.linkCount.columns(); j++ ) {
                    if ( MetaLinkFinder.linkCount.bitCount( i, j ) >= stringency ) {
                        TreeNode oneNode = new TreeNode( MetaLinkFinder.generateId( i, j ), MetaLinkFinder.linkCount
                                .getAllBits( i, j ), null );
                        nodes.add( oneNode );
                    }
                }
            }
            for ( int rowIndex = 0; rowIndex < nodes.size(); rowIndex++ ) {
                TreeNode rowNode = ( TreeNode ) nodes.getQuick( rowIndex );
                for ( int colIndex = rowIndex + 1; colIndex < nodes.size(); colIndex++ ) {
                    TreeNode colNode = ( TreeNode ) nodes.getQuick( colIndex );
                    int commonBits = MetaLinkFinder.overlapBits( rowNode.mask, colNode.mask );
                    if ( commonBits >= stringency ) {
                        out.write( rowIndex + "\t" + colIndex + "\t" + commonBits + "\n" );
                        out.write( colIndex + "\t" + rowIndex + "\t" + commonBits + "\n" );
                    }
                }

            }
            out.close();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    public static Gene getGene( String geneName, Taxon taxon ) {
        Gene gene = Gene.Factory.newInstance();
        gene.setOfficialSymbol( geneName.trim() );
        gene.setTaxon( taxon );
        gene = geneService.find( gene );
        return gene;
    }

    public static ExpressionExperiment getEE( int i ) {
        Object eeId = allEE.elementAt( i );
        ExpressionExperiment ee = eeService.load( ( Long ) eeId );
        return ee;
    }

    public static String getEEName( int i ) {
        return getEE( i ).getShortName();
    }

    public static Gene getRowGene( int i ) {
        Object geneId = linkCount.getRowName( i );
        Gene gene = geneService.load( ( ( Long ) geneId ).longValue() );
        return gene;
    }

    public static Gene getColGene( int i ) {
        Object geneId = linkCount.getColName( i );
        Gene gene = geneService.load( ( ( Long ) geneId ).longValue() );
        return gene;
    }

    public static boolean filter( int row, int col ) {
        String geneName1 = getRowGene( row ).getName();
        String geneName2 = getColGene( col ).getName();
        // if(geneName1.matches("(RPL|RPS)(.*)") ||geneName2.matches("(RPL|RPS)(.*)"))
        // return true;
        return false;
    }

    public static Gene[] getPairedGenes( long id ) {
        Gene[] pairedGene = new Gene[2];
        int row = ( int ) ( id / shift );
        int col = ( int ) ( id % shift );
        pairedGene[0] = getRowGene( row );
        pairedGene[1] = getRowGene( col );
        return pairedGene;
    }

    public static String getLinkName( long id ) {
        Gene[] pairedGene = getPairedGenes( id );
        while ( !MetaLinkFinder.geneOntologyService.isGeneOntologyLoaded() )
            ;
        return pairedGene[0].getName() + "_" + pairedGene[1].getName() + "_"
                + computeGOOverlap( pairedGene[0], pairedGene[1] );
    }

    public static boolean checkEEConfirmation( long id, int eeIndex ) {
        int rows = ( int ) ( id / shift );
        int cols = ( int ) ( id % shift );
        return MetaLinkFinder.linkCount.check( rows, cols, eeIndex );
    }

    public static Set getEENames( long[] mask ) {
        HashSet<String> returnedSet = new HashSet<String>();
        for ( int i = 0; i < mask.length; i++ ) {
            for ( int j = 0; j < CompressedNamedBitMatrix.DOUBLE_LENGTH; j++ )
                if ( ( mask[i] & ( CompressedNamedBitMatrix.BIT1 << j ) ) != 0 ) {
                    returnedSet.add( getEEName( j + i * CompressedNamedBitMatrix.DOUBLE_LENGTH ) );
                }
        }
        return returnedSet;
    }

    public static long generateId( int row, int col ) {
        return ( long ) row * ( long ) shift + col;
    }

    public static long[] AND( long[] mask1, long[] mask2 ) {
        long res[] = new long[mask1.length];
        for ( int i = 0; i < mask1.length; i++ )
            res[i] = mask1[i] & mask2[i];
        return res;
    }

    public static long[] OR( long[] mask1, long[] mask2 ) {
        long res[] = new long[mask1.length];
        for ( int i = 0; i < mask1.length; i++ )
            res[i] = mask1[i] | mask2[i];
        return res;
    }

    public static int overlapBits( long[] mask1, long[] mask2 ) {
        int bits = 0;
        for ( int i = 0; i < mask1.length; i++ )
            bits = bits + Long.bitCount( mask1[i] & mask2[i] );
        return bits;
    }

    public static int countBits( long[] mask ) {
        int bits = 0;
        for ( int i = 0; i < mask.length; i++ )
            bits = bits + Long.bitCount( mask[i] );
        return bits;
    }

    public static boolean checkBits( long[] mask, int index ) {
        int num = ( int ) ( index / CompressedNamedBitMatrix.DOUBLE_LENGTH );
        int bit_index = index % CompressedNamedBitMatrix.DOUBLE_LENGTH;
        long res = mask[num] & CompressedNamedBitMatrix.BIT1 << bit_index;
        if ( res == 0 ) return false;
        return true;
    }

    public static boolean compare( long[] mask1, long mask2[] ) {
        for ( int i = 0; i < mask1.length; i++ )
            if ( mask1[i] != mask2[i] ) return false;
        return true;
    }

    // rank: the number of top ranked GO terms to return
    public static Map<OntologyTerm, Integer> computeGOOverlap( Collection<Long> treeIds, int rank ) {
        Collection<Gene> genes = new HashSet<Gene>();
        for ( Long treeId : treeIds ) {
            int row = ( int ) ( treeId / shift );
            int col = ( int ) ( treeId % shift );
            genes.add( getRowGene( row ) );
            genes.add( getColGene( col ) );
        }
        Map<OntologyTerm, Integer> res = new HashMap<OntologyTerm, Integer>();
        ObjectArrayList counter = new ObjectArrayList( rank );
        for ( int i = 0; i < counter.size(); i++ )
            counter.add( new Integer( 0 ) );
        for ( Gene gene : genes ) {
            Collection<OntologyTerm> goEntries = MetaLinkFinder.getGoTerms( gene );
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

    public static Collection<OntologyTerm> getGoTerms( Gene gene ) {
        Collection<OntologyTerm> annotatedGoEntries = MetaLinkFinder.gene2GoAssociationService.findByGene( gene );
        Collection<OntologyTerm> allGoEntriesInBP = new HashSet<OntologyTerm>();
        Collection<OntologyTerm> useless = new HashSet<OntologyTerm>();
        for ( OntologyTerm entry : annotatedGoEntries ) {
            if ( entry.getLabel().toUpperCase().contains( "BIOLOGICAL_PROCESS" ) ) {
                Collection<OntologyTerm> parentEntries = MetaLinkFinder.geneOntologyService.getAllParents( entry );
                allGoEntriesInBP.add( entry );
                for ( OntologyTerm parentEntry : parentEntries ) {
                    if ( geneOntologyService.asRegularGoId( parentEntry ) != null
                            && !geneOntologyService.asRegularGoId( parentEntry ).contains( "GO:0008150" ) ) {
                        allGoEntriesInBP.add( parentEntry );
                    }
                }
            }
        }
        return allGoEntriesInBP;
    }

    public static int computeGOOverlap( Gene gene1, Gene gene2 ) {
        int res = 0;
        Collection<Long> geneIds = new HashSet<Long>();
        geneIds.add( gene2.getId() );
        try {
            Map<Long, Collection<OntologyTerm>> overlapMap = MetaLinkFinder.geneOntologyService.calculateGoTermOverlap(
                    gene1, geneIds );
            if ( overlapMap != null ) {
                Collection<OntologyTerm> overlapGOTerms = overlapMap.get( gene2.getId() );
                if ( overlapGOTerms != null ) res = overlapGOTerms.size();
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            res = 0;
        }
        return res;
    }

    public static int computeGOOverlap( long id1, long id2 ) {
        Gene gene1 = MetaLinkFinder.geneService.load( id1 );
        Gene gene2 = MetaLinkFinder.geneService.load( id2 );
        return computeGOOverlap( gene1, gene2 );
    }

    public static int computeGOOverlap( long packedId ) {
        int row = ( int ) ( packedId / shift );
        int col = ( int ) ( packedId % shift );
        return computeGOOverlap( getRowGene( row ), getColGene( col ) );
    }

    public void setEeService( ExpressionExperimentService eeService ) {
        MetaLinkFinder.eeService = eeService;
    }

    public void setGeneService( GeneService geneService ) {
        MetaLinkFinder.geneService = geneService;
    }

    public void setGeneOntologyService( GeneOntologyService geneOntologyService ) {
        MetaLinkFinder.geneOntologyService = geneOntologyService;
    }

    public void setGene2GoAssociationService( Gene2GOAssociationService gene2GoAssociationService ) {
        MetaLinkFinder.gene2GoAssociationService = gene2GoAssociationService;
    }

}
