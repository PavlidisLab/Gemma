/**
 * 
 */
package ubic.gemma.loader.genome.gene.ncbi;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.PersisterHelper;

/**
 * @author jsantos
 * @version $Id$
 */
public class NcbiGeneLoader {
    private static Log log = LogFactory.getLog( NcbiGeneConverter.class.getName() );
    private static final int QUEUE_SIZE = 1000;
    private static final int BATCH_SIZE = 1000;

    private AtomicBoolean generatorDone;
    private AtomicBoolean converterDone;
    private AtomicBoolean loaderDone;
    private PersisterHelper persisterHelper;
    private int loadedGeneCount = 0;
    
    private String GENEINFO_FILE = "gene_info";
    private String GENE2ACCESSION_FILE = "gene2accession";

    /**
     * @param persisterHelper the persisterHelper to set
     */
    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    /**
     * @return the loadedGeneCount
     */
    public int getLoadedGeneCount() {
        return loadedGeneCount;
    }

    public NcbiGeneLoader() {
        generatorDone = new AtomicBoolean( false );
        converterDone = new AtomicBoolean( false );
        loaderDone = new AtomicBoolean( false );
    }

    public NcbiGeneLoader( PersisterHelper persisterHelper ) {
        this();
        this.setPersisterHelper( persisterHelper );
    }

    /**
     * @param file the gene_info file
     * @param file the gene2accession file
     * @return
     * @throws IOException
     */
    public void load( String geneInfoFile, String gene2AccFile ) throws IOException {
        NcbiGeneDomainObjectGenerator sdog = new NcbiGeneDomainObjectGenerator();
        sdog.setProducerDoneFlag( generatorDone );
        NcbiGeneConverter converter = new NcbiGeneConverter();
        converter.setSourceDoneFlag( generatorDone );
        converter.setProducerDoneFlag( converterDone );
        // create queue for GeneInfo objects
        final BlockingQueue<NcbiGeneData> geneInfoQueue = new ArrayBlockingQueue<NcbiGeneData>( QUEUE_SIZE );
        final BlockingQueue<Gene> geneQueue = new ArrayBlockingQueue<Gene>( QUEUE_SIZE );
        // Threaded producer - loading files into queue as GeneInfo objects
        if (StringUtils.isEmpty( geneInfoFile ) || StringUtils.isEmpty( geneInfoFile ) ) {
            sdog.generate( geneInfoQueue );
        }
        else {
            sdog.generateLocal( geneInfoFile, gene2AccFile, geneInfoQueue );
        }
        // Threaded consumer/producer - consumes GeneInfo objects and generates
        // Gene/GeneProduct/DatabaseEntry entries
        converter.convert( geneInfoQueue, geneQueue );
        // Threaded consumer. Consumes Gene objects and persists them into
        // the database
        this.load( geneQueue );
    }
    
    /**
     * download the gene_info and gene2accession files, then call load
     * @return
     * @throws IOException
     */
    public void load( ) throws IOException {
        String geneInfoFile = "";
        String gene2AccFile = "";
        load(geneInfoFile,gene2AccFile);
    }

    /**
     * @param geneQueue a blocking queue of genes to be loaded into the database loads genes into the database
     * @param geneQueue
     */
    private void load( final BlockingQueue<Gene> geneQueue ) {
        final SecurityContext context = SecurityContextHolder.getContext();
        assert context != null;

        Thread loadThread = new Thread( new Runnable() {
            public void run() {
                SecurityContextHolder.setContext( context );
                while ( !( converterDone.get() && geneQueue.isEmpty() ) ) {
                    Gene gene = null;
                    try {
                        gene = geneQueue.take();
                        if ( gene != null ) {
                            // persist gene, then geneProducts
                            persisterHelper.persist( gene );

                            // geneService.create( data );
                            loadedGeneCount++;
                        }
                    } catch ( Exception e ) {
                        log.error( e, e );
                        loaderDone.set( true );
                        throw new RuntimeException( e );
                    }
                }
                loaderDone.set( true );
            }
        } );
        loadThread.start();

    }

    public boolean isLoaderDone() {
        return loaderDone.get();
    }
}
