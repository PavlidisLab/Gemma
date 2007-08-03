package ubic.gemma.apps;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Class for CLIs that manipulate a list of genes
 * 
 * @author Raymond
 */
public abstract class AbstractGeneManipulatingCLI extends AbstractSpringAwareCLI {

    public static final String OFFICIAL_NAME = "official name";
    public static final String OFFICIAL_SYMBOL = "official symbol";
    public static final String GENE_ID = "gene ID";

    /**
     * Read in a list of genes
     * 
     * @param inFile - file name to read
     * @param taxon
     * @param type format that gene is in
     * @return collection of genes
     * @throws IOException
     */
    protected Collection<Gene> readInGeneListFile( String inFile, Taxon taxon, String type ) throws IOException {
        log.info( "Reading " + inFile );
        GeneService geneService = ( GeneService ) getBean( "geneService" );

        Collection<Gene> genes = new ArrayList<Gene>();
        BufferedReader in = new BufferedReader( new FileReader( inFile ) );
        String line;
        if ( type == GENE_ID ) {
            Collection<Long> geneIds = new ArrayList<Long>();
            while ( ( line = in.readLine() ) != null ) {
                if ( line.startsWith( "#" ) ) continue;
                Long geneId = Long.parseLong( line );
                geneIds.add( geneId );
            }
            genes = geneService.loadMultiple( geneIds );
        } else {
            while ( ( line = in.readLine() ) != null ) {
                if ( line.startsWith( "#" ) ) continue;
                String s = line.trim();
                Collection<Gene> c = null;
                if ( type == OFFICIAL_NAME )
                    c = geneService.findByOfficialName( s );
                else if ( type == OFFICIAL_SYMBOL ) {
                    c = geneService.findByOfficialSymbol( s );
                } else
                    continue;
                if ( c == null || c.size() == 0 ) {
                    log.error( "ERROR: Cannot find genes for " + s );
                }
                for ( Gene gene : c ) {
                    if ( taxon.equals( gene.getTaxon() ) ) genes.add( gene );
                }
            }
        }
        return genes;
    }

    /**
     * Read in a list of genes
     * 
     * @param inFile file to read
     * @param taxon
     * @param type format that genes are in
     * @return a list of gene IDs
     * @throws IOException
     */
    protected Collection<Long> readInGeneListFileToIds( String inFile, Taxon taxon, String type ) throws IOException {
        Collection<Long> ids = new ArrayList<Long>();
        for ( Gene gene : readInGeneListFile( inFile, taxon, type ) )
            ids.add( gene.getId() );
        return ids;
    }
}
