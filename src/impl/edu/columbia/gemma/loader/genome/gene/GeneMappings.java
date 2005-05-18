package edu.columbia.gemma.loader.genome.gene;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.common.description.DatabaseEntry;
import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.genome.gene.GeneProduct;
import edu.columbia.gemma.genome.gene.GeneProductType;

public class GeneMappings {
    private static final int ACCESSION = 8;

    private static final int ACCESSION_VERSION = 7;
    private static final int GENE_PRODUCT_TYPE = 5;
    private static final int NCBI_ID = 1;
    private static final int OFFICIAL_SYMBOL = 2;
    private static final String RNA = "-";
    private static final int TAX_ID = 0;
    protected static final Log log = LogFactory.getLog( GeneParser.class );
    Map map = new HashMap();

    // static Gene gene = Gene.Factory.newInstance();

    private Gene geneExists( Gene g ) {
        if ( map.containsKey( g.getNcbiId() ) )
            g = ( Gene ) map.get( g.getNcbiId() );
        else {
            map.put( g.getNcbiId(), g );
        }

        return g;
    }

    Object mapFromGene2Accession( String line, Gene g ) {
        String[] values = StringUtils.split( line, "\t" );

        g.setNcbiId( values[NCBI_ID] );

        g = geneExists( g );

        // accessions association
        // 1) Create DatabaseEntry
        DatabaseEntry databaseEntry = DatabaseEntry.Factory.newInstance();
        databaseEntry.setAccessionVersion( values[ACCESSION_VERSION] );
        databaseEntry.setAccession( values[ACCESSION] );
        // 2) Add DatabaseEntry to the collection
        Collection deCol = new HashSet();
        deCol = g.getAccessions();
        deCol.add( databaseEntry );
        // 3) Add collection to gene
        g.setAccessions( deCol );

        // products association
        // 1) Create GeneProduct
        GeneProduct geneProduct = GeneProduct.Factory.newInstance();
        if ( values[GENE_PRODUCT_TYPE] == RNA ) {
            geneProduct.setType( GeneProductType.RNA );
            geneProduct.setName( "RNA" );
        } else {
            geneProduct.setType( GeneProductType.PROTEIN );
            geneProduct.setName( "PROTEIN" );
        }
        // 2) Add GeneProduct to the collection
        Collection geneProductsCol = new HashSet();
        geneProductsCol = g.getProducts();
        geneProductsCol.add( geneProduct );
        // 3) Add collection to gene
        g.setProducts( geneProductsCol );

        return g;

    }

    Object mapFromGene2Go( String line, Gene g ) {
        return null;
        // TODO Auto-generated method stub

    }

    Object mapFromGene2RefSeq( String line, Gene g ) {
        return null;
        // TODO Auto-generated method stub

    }

    Object mapFromGene2Sts( String line, Gene g ) {
        return null;
        // TODO Auto-generated method stub

    }

    Object mapFromGene2Unigene( String line, Gene g ) {
        return null;
        // TODO Auto-generated method stub

    }

    Object mapFromGeneHistory( String line, Gene g ) {
        return null;
        // TODO Auto-generated method stub

    }

    Object mapFromGeneInfo( String line, Gene g ) {

        String[] values = StringUtils.split( line, "\t" );

        g.setNcbiId( values[NCBI_ID] );

        g = geneExists( g );

        g.setOfficialSymbol( values[OFFICIAL_SYMBOL] );

        return g;
    }

    Object mapFromMin2Gene( String line, Gene g ) {
        return null;
        // TODO Auto-generated method stub

    }

    /**
     * Map according to the filetype. Using default visibility because there is no need to restrict access from outside
     * this package.
     * 
     * @param filename
     * @param line
     * @param g
     * @param gene
     * @return
     */
    Object mapLine( String filename, String line, String[] keys, Object obj ) {
        String[] f = StringUtils.split( filename, "\\" );
        switch ( java.util.Arrays.asList( keys ).indexOf( f[f.length - 1] ) ) {
            case 0:
                return mapFromGene2Accession( line, ( Gene ) obj );
            case 1:
                return mapFromGene2Go( line, ( Gene ) obj );
            case 2:
                return mapFromGene2RefSeq( line, ( Gene ) obj );
            case 3:
                return mapFromGene2Sts( line, ( Gene ) obj );
            case 4:
                return mapFromGene2Unigene( line, ( Gene ) obj );
            case 5:
                return mapFromGeneHistory( line, ( Gene ) obj );
            case 6:
                return mapFromGeneInfo( line, ( Gene ) obj );
            case 7:
                return mapFromMin2Gene( line, ( Gene ) obj );
            default:
                return null;
        }

    }

}
