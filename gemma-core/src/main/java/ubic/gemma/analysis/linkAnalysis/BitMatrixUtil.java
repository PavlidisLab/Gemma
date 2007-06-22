package ubic.gemma.analysis.linkAnalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ibm.icu.impl.ICUListResourceBundle.CompressedBinary;

import cern.colt.list.ObjectArrayList;

import ubic.basecode.dataStructure.matrix.CompressedNamedBitMatrix;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;

public class BitMatrixUtil {
	private CompressedNamedBitMatrix linkCount = null;
    public static int shift = 50000; // to encode two geneid into one long id
    private HashMap<Long, Integer> eeIndexMap = null;
    private HashMap<Integer, ExpressionExperiment> eeMap = null;
    private HashMap<Long, Gene> geneMap = null;
    protected static final Log log = LogFactory.getLog(BitMatrixUtil.class );
     
    public BitMatrixUtil(Collection<ExpressionExperiment> ees, Collection<Gene> rowGenes,Collection<Gene> colGenes){
        CompressedNamedBitMatrix linkCount = new CompressedNamedBitMatrix(rowGenes.size(), colGenes.size(), ees.size());
        for(Gene geneIter:rowGenes){
            linkCount.addRowName(geneIter.getId());
        }
        for(Gene geneIter:colGenes){
            linkCount.addColumnName(geneIter.getId());
        }
        eeIndexMap = new HashMap<Long, Integer>();
        eeMap = new HashMap<Integer, ExpressionExperiment>();
        int index = 0;
        for ( ExpressionExperiment eeIter : ees ) {
            eeIndexMap.put( eeIter.getId(), new Integer( index ) );
            eeMap.put(new Integer(index), eeIter);
            index++;
        }
        geneMap = new HashMap<Long, Gene>();
        for(Gene gene:rowGenes){
        	geneMap.put(gene.getId(), gene);
        }
        if(rowGenes != colGenes){
            for(Gene gene:colGenes){
            	geneMap.put(gene.getId(), gene);
            }
        }
    }
    public void toFile( String matrixFile, String eeMapFile ) throws IOException {
        linkCount.toFile( matrixFile );
        FileWriter out = new FileWriter( new File( eeMapFile ) );
        for ( Long index : this.eeIndexMap.keySet() ) {
            out.write( index + "\t" + this.eeIndexMap.get( index ) + "\n" );
        }
        out.close();
    }

    public void fromFile( String matrixFile, String eeMapFile, ExpressionExperimentService eeService, GeneService geneService ) throws IOException {
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
                linkCount = new CompressedNamedBitMatrix( Integer.valueOf( subItems[0] ),
                        Integer.valueOf( subItems[1] ), Integer.valueOf( subItems[2] ) );
                hasConfig = true;
            } else if ( !hasRowNames ) {
                if ( subItems.length != linkCount.rows() ) {
                    String mesg = "Data File Format Error for Row Names " + row;
                    log.info( mesg );
                    throw new IOException( mesg );
                }
                for ( i = 0; i < subItems.length; i++ ){
                    linkCount.addRowName( new Long( subItems[i].trim() ) );
                    geneIds.add(new Long( subItems[i].trim() ));
                }
                hasRowNames = true;
            } else if ( !hasColNames ) {
                if ( subItems.length != linkCount.columns() ) {
                    String mesg = "Data File Format Error for Col Names " + row;
                    log.info( mesg );
                    throw new IOException( mesg );
                }
                for ( i = 0; i < subItems.length; i++ ){
                    linkCount.addColumnName( new Long( subItems[i].trim() ) );
                	geneIds.add(new Long( subItems[i].trim() ));
                }
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
        Collection<Gene> allGenes = geneService.load(geneIds);
        for(Gene gene:allGenes){
        	geneMap.put(gene.getId(), gene);
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
                    throw new IOException(mesg);
                }
                this.eeIndexMap.put( new Long( subItems[0].trim() ), new Integer( subItems[1].trim() ) );
                if ( Integer.valueOf( subItems[1].trim() ).intValue() > vectorSize )
                    vectorSize = Integer.valueOf( subItems[1].trim() ).intValue();
            }
            eeMap = new HashMap<Integer, ExpressionExperiment>();
            for ( Long iter : this.eeIndexMap.keySet() ) {
            	ExpressionExperiment ee = eeService.load(iter);
            	eeMap.put(this.eeIndexMap.get(iter), ee);
            }
            log.info( "Got " + this.eeIndexMap.size() + " in EE MAP" );
            in.close();
        }
        shift = linkCount.rows() > linkCount.columns() ? linkCount.rows() : linkCount.columns();
    }

    public void saveLinkMatrix( String outFile, int stringency ) {
        try {
            ObjectArrayList nodes = new ObjectArrayList();

            FileWriter out = new FileWriter( new File( outFile ) );
            for ( int i = 0; i < linkCount.rows(); i++ ) {
                if ( i % 1000 == 0 ) System.err.println( i + " -> " + linkCount.rows() );
                for ( int j = i + 1; j < linkCount.columns(); j++ ) {
                    if ( linkCount.bitCount( i, j ) >= stringency ) {
                        TreeNode oneNode = new TreeNode( generateId( i, j ), linkCount
                                .getAllBits( i, j ), null );
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
    public int getEEIndex(long eeId){
    	Integer eeIndex = eeIndexMap.get(eeId);
    	if(eeIndex == null)
    		return -1;
    	return eeIndex;
    }
    public ExpressionExperiment getEE( int i ) {
        return eeMap.get(new Integer(i));
    }

    public String getEEName( int i ) {
        return getEE( i ).getShortName();
    }

    public Gene getRowGene( int i ) {
        Object geneId = linkCount.getRowName( i );
        return geneMap.get(geneId);
    }

    public Gene getColGene( int i ) {
        Object geneId = linkCount.getColName( i );
        return geneMap.get(geneId);
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
        return linkCount.check( rows, cols, eeIndex );
    }
    public boolean filter(int i, int j){
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
    public CompressedNamedBitMatrix getMatrix(){
    	return this.linkCount;
    }
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

}
