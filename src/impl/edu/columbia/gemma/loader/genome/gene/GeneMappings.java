package edu.columbia.gemma.loader.genome.gene;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.common.description.DatabaseEntry;
import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.genome.gene.GeneProduct;
import edu.columbia.gemma.genome.gene.GeneProductType;

public class GeneMappings {
    Configuration conf = new PropertiesConfiguration( "Gemma.properties" );
    protected static final Log log = LogFactory.getLog( GeneParser.class );
    
    private static final String RNA = "-";
//  prefix of these constants determines the file from which the value is read.
    private final int GENE2ACCESSION_ACCESSION = conf.getInt( "gene2accession.accession" );
    private final int GENE2ACCESSION_ACCESSION_VERSION = conf.getInt( "gene2accession.accesion_version" );

    private final int GENE2ACCESSION_GENE_PRODUCT_TYPE = conf.getInt( "gene2accession.gene.product.type" );
    private final int GENEINFO_NCBI_ID = conf.getInt( "geneinfo.ncbi_id" );
    private final int GENEINFO_OFFICIAL_SYMBOL = conf.getInt( "geneinfo.official_symbol" );
    private final int GENEINFO_TAX_ID = conf.getInt( "geneinfo.tax_id" );

    Map map = new HashMap();

    /**
     * 
     */
    public GeneMappings() throws ConfigurationException {

    }

    /**
     * @param line
     * @param g
     * @return
     */
    public Object mapFromGene2Accession( String line, Gene g ) {
        String[] values = StringUtils.split( line, "\t" );

        g.setNcbiId( values[GENEINFO_NCBI_ID] );

        g = geneExists( g );

        // accessions association
        // 1) Create DatabaseEntry
        DatabaseEntry databaseEntry = DatabaseEntry.Factory.newInstance();
        databaseEntry.setAccessionVersion( values[GENE2ACCESSION_ACCESSION_VERSION] );
        databaseEntry.setAccession( values[GENE2ACCESSION_ACCESSION] );
        // 2) Add DatabaseEntry to the collection
        Collection deCol = new HashSet();
        deCol = g.getAccessions();
        deCol.add( databaseEntry );
        // 3) Add collection to gene
        g.setAccessions( deCol );

        // products association
        // 1) Create GeneProduct
        GeneProduct geneProduct = GeneProduct.Factory.newInstance();
        if ( values[GENE2ACCESSION_GENE_PRODUCT_TYPE] == RNA ) {
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

    /**
     * @param line
     * @param g
     * @return
     */
    public Object mapFromGene2Go( String line, Gene g ) {
        return null;
        // TODO Auto-generated method stub

    }

    /**
     * @param line
     * @param g
     * @return
     */
    public Object mapFromGene2RefSeq( String line, Gene g ) {
        return null;
        // TODO Auto-generated method stub

    }

    /**
     * @param line
     * @param g
     * @return
     */
    public Object mapFromGene2Sts( String line, Gene g ) {
        return null;
        // TODO Auto-generated method stub

    }

    /**
     * @param line
     * @param g
     * @return
     */
    public Object mapFromGene2Unigene( String line, Gene g ) {
        return null;
        // TODO Auto-generated method stub

    }

    /**
     * @param line
     * @param g
     * @return
     */
    public Object mapFromGeneHistory( String line, Gene g ) {
        return null;
        // TODO Auto-generated method stub

    }

    /**
     * @param line
     * @param g
     * @return
     */
    public Object mapFromGeneInfo( String line, Gene g ) {

        String[] values = StringUtils.split( line, "\t" );

        g.setNcbiId( values[GENEINFO_NCBI_ID] );

        g = geneExists( g );

        g.setOfficialSymbol( values[GENEINFO_OFFICIAL_SYMBOL] );

        return g;
    }

    /**
     * @param line
     * @param g
     * @return
     */
    public Object mapFromMin2Gene( String line, Gene g ) {
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

    /**
     * @param g
     * @return
     */
    private Gene geneExists( Gene g ) {
        if ( map.containsKey( g.getNcbiId() ) )
            g = ( Gene ) map.get( g.getNcbiId() );
        else {
            map.put( g.getNcbiId(), g );
        }

        return g;
    }
}
