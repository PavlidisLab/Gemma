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
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.CompressedNamedBitMatrix;
import ubic.gemma.model.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PredictedGeneImpl;
import ubic.gemma.model.genome.ProbeAlignedRegionImpl;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.ontology.OntologyTerm;
import cern.colt.list.ObjectArrayList;

public class LinkBitMatrixUtil {
    private CompressedNamedBitMatrix linkCountMatrix = null;
    public static int shift = 50000; // to encode two geneid into one long id
    private HashMap<Long, Integer> eeIndexMap = null;
    private HashMap<Integer, ExpressionExperiment> eeMap = null;
    private HashMap<Long, Gene> geneMap = null;
    private Collection<Gene> targetGenes = null;
    protected static final Log log = LogFactory.getLog( LinkBitMatrixUtil.class );
    private GeneService geneService = null;
    private ExpressionExperimentService eeService = null;
    private CommandLineToolUtilService utilService = null;
    private int stringency = 2;

    public LinkBitMatrixUtil() {
    }

    public void init( Taxon taxon ) {
        Collection<Gene> allGenes = geneService.getGenesByTaxon( taxon );
        Collection<Gene> genes = new HashSet<Gene>();
        for ( Gene gene : allGenes ) {
            if ( !( gene instanceof PredictedGeneImpl ) && !( gene instanceof ProbeAlignedRegionImpl ) ) {
                genes.add( gene );
            }
        }
        log.info( "Get " + genes.size() + " genes" );
        if ( genes == null || genes.size() == 0 ) return;
        init( genes );
    }

    public void init( Collection<Gene> genes ) {
        if ( genes == null || genes.size() == 0 ) return;
        Taxon taxon = genes.iterator().next().getTaxon();
        Collection<ExpressionExperiment> ees = eeService.findByTaxon( taxon );
        if ( ees == null || ees.size() == 0 ) return;
        init( genes, ees );
    }

    public void init( Collection<Gene> genes, Collection<ExpressionExperiment> ees ) {
        if ( genes == null || ees == null || genes.size() == 0 || ees.size() == 0 ) return;
        Collection<Gene> genesInTaxon = geneService.getGenesByTaxon( genes.iterator().next().getTaxon() );
        if ( genesInTaxon == null || genesInTaxon.size() == 0 ) return;
        Collection<Gene> coExpressedGenes = new HashSet<Gene>();

        for ( Gene gene : genesInTaxon ) {
            if ( !( gene instanceof PredictedGeneImpl ) && !( gene instanceof ProbeAlignedRegionImpl ) ) {
                coExpressedGenes.add( gene );
            }
        }
        init( ees, genes, coExpressedGenes );
    }

    public void init( Collection<ExpressionExperiment> ees, Collection<Gene> targetGenes,
            Collection<Gene> coExpressedGenes ) {
        CompressedNamedBitMatrix linkCount = new CompressedNamedBitMatrix( targetGenes.size(), coExpressedGenes.size(),
                ees.size() );
        for ( Gene geneIter : targetGenes ) {
            linkCount.addRowName( geneIter.getId() );
        }
        for ( Gene geneIter : coExpressedGenes ) {
            linkCount.addColumnName( geneIter.getId() );
        }
        eeIndexMap = new HashMap<Long, Integer>();
        eeMap = new HashMap<Integer, ExpressionExperiment>();
        int index = 0;
        for ( ExpressionExperiment eeIter : ees ) {
            eeIndexMap.put( eeIter.getId(), new Integer( index ) );
            eeMap.put( new Integer( index ), eeIter );
            index++;
        }
        geneMap = new HashMap<Long, Gene>();
        for ( Gene gene : targetGenes ) {
            geneMap.put( gene.getId(), gene );
        }
        if ( targetGenes != coExpressedGenes ) {
            for ( Gene gene : coExpressedGenes ) {
                geneMap.put( gene.getId(), gene );
            }
        }
        this.targetGenes = targetGenes;
    }

    public void fillCountMatrix() {
        int i = 1;
        for ( Gene gene : targetGenes ) {
            System.out.println( i + "/" + targetGenes.size() + "\t" + gene.getName() );
            // Get the gene->eeIds map
            CoexpressionCollectionValueObject coexpressed = ( CoexpressionCollectionValueObject ) geneService
                    .getCoexpressedGenes( gene, null, stringency );
            Map<Long, Collection<Long>> geneEEMap = coexpressed.getGeneCoexpressionType()
                    .getSpecificExpressionExperiments();
            this.count( gene.getId(), geneEEMap );
            i++;
        }
    }

    private void count( Long rowGeneId, Map<Long, Collection<Long>> geneEEsMap ) {
        int rowIndex = -1, colIndex = -1, eeIndex = -1;
        rowIndex = this.linkCountMatrix.getRowIndexByName( rowGeneId );
        for ( Long colGeneId : geneEEsMap.keySet() ) {
            try {
                Integer index = null;
                Collection<Long> eeIds = geneEEsMap.get( colGeneId );
                colIndex = this.linkCountMatrix.getColIndexByName( colGeneId );
                for ( Long eeId : eeIds ) {
                    eeIndex = getEEIndex( eeId );
                    if ( eeIndex < 0 ) {
                        log.info( "Couldn't find the ee index for ee " + eeId );
                        continue;
                    }
                    this.linkCountMatrix.set( rowIndex, colIndex, eeIndex );
                }
            } catch ( Exception e ) {
                continue;
            }
        }
    }

    public String getLinkName( long id ) {
        Gene[] pairedGene = getPairedGenes( id );
        return pairedGene[0].getName() + "_" + pairedGene[1].getName() + "_"
                + utilService.computeGOOverlap( pairedGene[0], pairedGene[1] );
    }

    // rank: the number of top ranked GO terms to return
    public Map<OntologyTerm, Integer> computeGOOverlap( Collection<Long> treeIds, int rank ) {
        Collection<Gene> genes = new HashSet<Gene>();
        for ( Long treeId : treeIds ) {
            int row = ( int ) ( treeId / LinkBitMatrixUtil.shift );
            int col = ( int ) ( treeId % LinkBitMatrixUtil.shift );
            genes.add( getRowGene( row ) );
            genes.add( getColGene( col ) );
        }
        Map<OntologyTerm, Integer> res = new HashMap<OntologyTerm, Integer>();
        ObjectArrayList counter = new ObjectArrayList( rank );
        for ( int i = 0; i < counter.size(); i++ )
            counter.add( new Integer( 0 ) );
        for ( Gene gene : genes ) {
            Collection<OntologyTerm> goEntries = utilService.getGOTerms( gene );
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

    public int computeGOOverlap( long packedId ) {
        int row = ( int ) ( packedId / shift );
        int col = ( int ) ( packedId % shift );
        return utilService.computeGOOverlap( getRowGene( row ), getColGene( col ) );
    }

    public void toFile( String matrixFile, String eeMapFile ) throws IOException {
        linkCountMatrix.toFile( matrixFile );
        FileWriter out = new FileWriter( new File( eeMapFile ) );
        for ( Long index : this.eeIndexMap.keySet() ) {
            out.write( index + "\t" + this.eeIndexMap.get( index ) + "\n" );
        }
        out.close();
    }

    public void fromFile( String matrixFile, String eeMapFile, ExpressionExperimentService eeService,
            GeneService geneService ) throws IOException {
        BufferedReader in = new BufferedReader( new FileReader( new File( matrixFile ) ) );
        String row = null;
        int i;
        boolean hasConfig = false, hasRowNames = false, hasColNames = false;
        Collection<Long> geneIds = new HashSet<Long>();
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
                linkCountMatrix = new CompressedNamedBitMatrix( Integer.valueOf( subItems[0] ), Integer
                        .valueOf( subItems[1] ), Integer.valueOf( subItems[2] ) );
                hasConfig = true;
            } else if ( !hasRowNames ) {
                if ( subItems.length != linkCountMatrix.rows() ) {
                    String mesg = "Data File Format Error for Row Names " + row;
                    log.info( mesg );
                    throw new IOException( mesg );
                }
                for ( i = 0; i < subItems.length; i++ ) {
                    linkCountMatrix.addRowName( new Long( subItems[i].trim() ) );
                    geneIds.add( new Long( subItems[i].trim() ) );
                }
                hasRowNames = true;
            } else if ( !hasColNames ) {
                if ( subItems.length != linkCountMatrix.columns() ) {
                    String mesg = "Data File Format Error for Col Names " + row;
                    log.info( mesg );
                    throw new IOException( mesg );
                }
                for ( i = 0; i < subItems.length; i++ ) {
                    linkCountMatrix.addColumnName( new Long( subItems[i].trim() ) );
                    geneIds.add( new Long( subItems[i].trim() ) );
                }
                hasColNames = true;
            } else {
                int rowIndex = Integer.valueOf( subItems[0] );
                int colIndex = Integer.valueOf( subItems[1] );
                double values[] = new double[subItems.length - 2];
                for ( i = 2; i < subItems.length; i++ )
                    values[i - 2] = Double.longBitsToDouble( Long.parseLong( subItems[i], 16 ) );
                if ( !linkCountMatrix.set( rowIndex, colIndex, values ) ) {
                    String mesg = "Data File Format Error for Data " + row;
                    log.info( mesg );
                    throw new IOException( mesg );
                }
            }
        }
        in.close();
        Collection<Gene> allGenes = geneService.loadMultiple( geneIds );
        for ( Gene gene : allGenes ) {
            geneMap.put( gene.getId(), gene );
        }
        if ( eeMapFile != null ) {
            in = new BufferedReader( new FileReader( new File( eeMapFile ) ) );
            this.eeIndexMap = new HashMap<Long, Integer>();
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
                    throw new IOException( mesg );
                }
                this.eeIndexMap.put( new Long( subItems[0].trim() ), new Integer( subItems[1].trim() ) );
                if ( Integer.valueOf( subItems[1].trim() ).intValue() > vectorSize )
                    vectorSize = Integer.valueOf( subItems[1].trim() ).intValue();
            }
            eeMap = new HashMap<Integer, ExpressionExperiment>();
            for ( Long iter : this.eeIndexMap.keySet() ) {
                ExpressionExperiment ee = eeService.load( iter );
                eeMap.put( this.eeIndexMap.get( iter ), ee );
            }
            log.info( "Got " + this.eeIndexMap.size() + " in EE MAP" );
            in.close();
        }
        shift = linkCountMatrix.rows() > linkCountMatrix.columns() ? linkCountMatrix.rows() : linkCountMatrix.columns();
    }

    public void saveLinkMatrix( String outFile, int stringency ) {
        try {
            ObjectArrayList nodes = new ObjectArrayList();

            FileWriter out = new FileWriter( new File( outFile ) );
            for ( int i = 0; i < linkCountMatrix.rows(); i++ ) {
                if ( i % 1000 == 0 ) System.err.println( i + " -> " + linkCountMatrix.rows() );
                for ( int j = i + 1; j < linkCountMatrix.columns(); j++ ) {
                    if ( linkCountMatrix.bitCount( i, j ) >= stringency ) {
                        TreeNode oneNode = new TreeNode( generateId( i, j ), linkCountMatrix.getAllBits( i, j ), null );
                        nodes.add( oneNode );
                    }
                }
            }
            for ( int rowIndex = 0; rowIndex < nodes.size(); rowIndex++ ) {
                TreeNode rowNode = ( TreeNode ) nodes.getQuick( rowIndex );
                for ( int colIndex = rowIndex + 1; colIndex < nodes.size(); colIndex++ ) {
                    TreeNode colNode = ( TreeNode ) nodes.getQuick( colIndex );
                    int commonBits = overlapBits( rowNode.mask, colNode.mask );
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

    public int getEEIndex( long eeId ) {
        Integer eeIndex = eeIndexMap.get( eeId );
        if ( eeIndex == null ) return -1;
        return eeIndex;
    }

    public ExpressionExperiment getEE( int i ) {
        return eeMap.get( new Integer( i ) );
    }

    public String getEEName( int i ) {
        return getEE( i ).getShortName();
    }

    public Gene getRowGene( int i ) {
        Object geneId = linkCountMatrix.getRowName( i );
        return geneMap.get( geneId );
    }

    public Gene getColGene( int i ) {
        Object geneId = linkCountMatrix.getColName( i );
        return geneMap.get( geneId );
    }

    public Gene[] getPairedGenes( long id ) {
        Gene[] pairedGene = new Gene[2];
        int row = ( int ) ( id / shift );
        int col = ( int ) ( id % shift );
        pairedGene[0] = getRowGene( row );
        pairedGene[1] = getColGene( col );
        return pairedGene;
    }

    public boolean checkEEConfirmation( long id, int eeIndex ) {
        int rows = ( int ) ( id / shift );
        int cols = ( int ) ( id % shift );
        return linkCountMatrix.check( rows, cols, eeIndex );
    }

    public boolean filter( int i, int j ) {
        String geneName1 = getRowGene( i ).getName();
        String geneName2 = getColGene( j ).getName();
        // if(geneName1.matches("(RPL|RPS)(.*)") ||geneName2.matches("(RPL|RPS)(.*)"))
        // return true;
        return false;
    }

    public Collection getEENames( long[] mask ) {
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

    public CompressedNamedBitMatrix getMatrix() {
        return this.linkCountMatrix;
    }

    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    public void setEEService( ExpressionExperimentService eeService ) {
        this.eeService = eeService;
    }

    public void setUtilService( CommandLineToolUtilService utilService ) {
        this.utilService = utilService;
    }

    public CommandLineToolUtilService getUtilService() {
        return this.utilService;
    }

    public void setStringency( int stringency ) {
        this.stringency = stringency;
    }

    // The following codes for testing matrix and output
    public void testBitMatrix() {
        CompressedNamedBitMatrix matrix = new CompressedNamedBitMatrix( 21, 11, 125 );
        for ( int i = 0; i < 21; i++ )
            matrix.addRowName( new Long( i ) );
        for ( int i = 0; i < 11; i++ )
            matrix.addColumnName( new Long( i ) );
        matrix.set( 0, 0, 0 );
        matrix.set( 0, 0, 12 );
        matrix.set( 0, 0, 24 );
        matrix.set( 20, 0, 0 );
        matrix.set( 20, 0, 12 );
        matrix.set( 20, 0, 24 );
        matrix.set( 0, 10, 0 );
        matrix.set( 0, 10, 12 );
        matrix.set( 0, 10, 24 );
        matrix.set( 20, 10, 0 );
        matrix.set( 20, 10, 12 );
        matrix.set( 20, 10, 24 );
        try {
            matrix.toFile( "test.File" );
        } catch ( IOException e ) {
            System.out.println( e.getMessage() );
        }
    }

    /**
     * Output the gene
     * 
     * @param gene
     * @param num
     */
    public void output( Gene gene, int num ) {
        int row = this.linkCountMatrix.getRowIndexByName( gene.getId() );
        if ( row < 0 || row >= this.linkCountMatrix.rows() ) {
            log.info( "Gene does not exist" );
            return;
        }
        for ( int col = 0; col < this.linkCountMatrix.columns(); col++ )
            if ( this.linkCountMatrix.bitCount( row, col ) >= num ) {
                System.err.println( getColGene( col ).getName() + " " + this.linkCountMatrix.bitCount( row, col ) );
            }
        System.err.println( "=====================================================" );
        for ( int col = 0; col < this.linkCountMatrix.columns(); col++ )
            if ( this.linkCountMatrix.bitCount( row, col ) >= num ) {
                System.err.println( getColGene( col ).getName() );
            }
    }

    public void output( int num ) {
        int count = 0;
        for ( int i = 0; i < this.linkCountMatrix.rows(); i++ )
            for ( int j = 0; j < this.linkCountMatrix.columns(); j++ ) {
                if ( this.linkCountMatrix.bitCount( i, j ) >= num ) {
                    System.err.println( getRowGene( i ).getName() + "  " + getColGene( j ).getName() + " "
                            + this.linkCountMatrix.bitCount( i, j ) );
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
        for ( int i = 0; i < this.linkCountMatrix.rows(); i++ ) {
            // System.err.println(i);
            for ( int j = i + 1; j < this.linkCountMatrix.columns(); j++ ) {
                int num = this.linkCountMatrix.bitCount( i, j );
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
}
