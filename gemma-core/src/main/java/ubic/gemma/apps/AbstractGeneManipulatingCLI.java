package ubic.gemma.apps;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.search.SearchService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Class for CLIs that manipulate a list of genes
 * 
 * @author Raymond
 */
public abstract class AbstractGeneManipulatingCLI extends AbstractSpringAwareCLI {
    protected GeneService geneService;
    protected SearchService searchService;
    
    protected void initBeans() {
        geneService = (GeneService) getBean("geneService");
        searchService = (SearchService) getBean("searchService");
    }

    protected Collection<Gene> readGeneIdListFile(String inFile) throws IOException {
        GeneService geneService = ( GeneService ) getBean( "geneService" );
        Collection<Long> geneIds = new ArrayList<Long>();
        BufferedReader in = new BufferedReader( new FileReader( inFile ) );
        String line;
        while ( ( line = in.readLine() ) != null ) {
            if ( line.startsWith( "#" ) ) continue;
            Long geneId = Long.parseLong( line );
            geneIds.add( geneId );
        }
        return geneService.loadMultiple( geneIds );
    }

    /**
     * Read in a list of genes
     * 
     * @param inFile - file name to read
     * @param taxon
     * @param type format that gene is in
     * @return collection of genes
     * @throws IOException
     */
    protected Collection<Gene> readGeneListFile( String inFile, Taxon taxon) throws IOException {
        log.info( "Reading " + inFile );

        Collection<Gene> genes = new ArrayList<Gene>();
        BufferedReader in = new BufferedReader( new FileReader( inFile ) );
        String line;
        while ( ( line = in.readLine() ) != null ) {
            if ( line.startsWith( "#" ) ) continue;
            String s = line.trim();
            Collection<Gene> c = null;
//            c = searchService.compassGeneSearch( s );
            c = geneService.findByOfficialSymbolInexact( s );
            if ( c == null || c.size() == 0 ) {
                log.error( "ERROR: Cannot find genes for " + s );
            }
            for ( Gene gene : c ) {
                if ( taxon.equals( gene.getTaxon() ) ) genes.add( gene );
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
    protected Collection<Long> readGeneListFileToIds( String inFile, Taxon taxon) throws IOException {
        Collection<Long> ids = new ArrayList<Long>();
        for ( Gene gene : readGeneListFile( inFile, taxon) )
            ids.add( gene.getId() );
        return ids;
    }
}
