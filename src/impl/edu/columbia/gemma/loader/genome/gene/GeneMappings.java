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
import edu.columbia.gemma.genome.Taxon;
import edu.columbia.gemma.genome.TaxonDao;
import edu.columbia.gemma.genome.gene.GeneProduct;
import edu.columbia.gemma.genome.gene.GeneProductType;

public class GeneMappings {
    protected static final Log log = LogFactory.getLog( GeneParser.class );
    Configuration conf = new PropertiesConfiguration( "Gemma.properties" );

    private static final int HOMOSAPIEN = 9606;
    private static final int MUSMUSCULUS = 10090;
    private static final int RATTUS = 10114;

    private static final String RNA = "-";

    // prefix of these constants determines the file from which the value is read.
    private final int GENE2ACCESSION_ACCESSION = conf.getInt( "gene2accession.accession" );

    private final int GENE2ACCESSION_ACCESSION_VERSION = conf.getInt( "gene2accession.accesion_version" );

    private final int GENE2ACCESSION_GENE_PRODUCT_TYPE = conf.getInt( "gene2accession.gene.product.type" );

    private final int GENEINFO_NCBI_ID = conf.getInt( "geneinfo.ncbi_id" );

    private final int GENEINFO_OFFICIAL_SYMBOL = conf.getInt( "geneinfo.official_symbol" );

    private final int GENEINFO_TAX_ID = conf.getInt( "geneinfo.tax_id" );

    private final int NCBI_ID = GENEINFO_NCBI_ID;

    private final int TAX_ID = GENEINFO_TAX_ID;

    private boolean taxonCreated = false;
    Map map = new HashMap();
    Map<Integer, Taxon> taxaMap = null;

    private TaxonDao taxonDao = null;

    /**
     * 
     */
    public GeneMappings( TaxonDao taxonDao ) throws ConfigurationException {
        if ( taxonDao == null ) throw new IllegalArgumentException();
        this.taxonDao = taxonDao;
        initializeTaxa();
    }

    /**
     * 
     */
    private void initializeTaxa() {
        Collection<Taxon> taxa = taxonDao.findAllTaxa();
        taxaMap = new HashMap<Integer, Taxon>();

        for ( Taxon t : taxa ) {
            taxaMap.put( new Integer( t.getNcbiId() ), t );
        }
    }

    /**
     * @param line
     * @param g
     * @return
     */
    public Object mapFromGene2Accession( String line, Gene g ) {
        String[] values = StringUtils.split( line, "\t" );

        if ( !validTaxonId( values[TAX_ID] ) ) {
            g = null;
            return g;
        }

        Taxon t = mapTaxon( values[TAX_ID] );

        g.setTaxon( t );

        g.setNcbiId( values[NCBI_ID] );

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

        if ( !validTaxonId( values[TAX_ID] ) ) {
            g = null;
            return g;
        }

        Taxon t = mapTaxon( values[TAX_ID] );

        g.setTaxon( t );

        g.setNcbiId( values[NCBI_ID] );

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

    /**
     * @param taxId
     * @return
     */
    private Taxon mapTaxon( String taxId ) {
        assert taxonDao != null;
        assert taxaMap != null;
        int taxonId = Integer.parseInt( taxId );

        if ( taxaMap.containsKey( taxonId ) ) return taxaMap.get( new Integer( taxonId ) );

        Taxon t = Taxon.Factory.newInstance();
        t.setNcbiId( Integer.parseInt( taxId ) );

        switch ( taxonId ) {

            case HOMOSAPIEN:
                t.setCommonName( "Human" );
                t.setScientificName( "Homo Sapien" );
                break;

            case MUSMUSCULUS:
                t.setCommonName( "Mouse" );
                t.setScientificName( "Mus Musculus" );
                break;

            case RATTUS:
                t.setCommonName( "Rat" );
                t.setScientificName( "Rattus" );
                break;
        }

        taxonDao.create( t );
        taxaMap.put( t.getNcbiId(), t );
        return t;
    }

    private boolean validTaxonId( String taxId ) {
        int taxonId = Integer.parseInt( taxId );

        switch ( taxonId ) {
            case HOMOSAPIEN:
                return true;
            case MUSMUSCULUS:
                return true;
            case RATTUS:
                return true;
            default:
                return false;
        }
    }

    /**
     * @return Returns the taxonDao.
     */
    public TaxonDao getTaxonDao() {
        return this.taxonDao;
    }

    /**
     * @param taxonDao The taxonDao to set.
     */
    public void setTaxonDao( TaxonDao taxonDao ) {
        this.taxonDao = taxonDao;
    }
}
