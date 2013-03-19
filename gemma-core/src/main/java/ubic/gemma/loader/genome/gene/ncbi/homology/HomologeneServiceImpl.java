/*
 * The Gemma project
 * 
 * Copyright (c) 2009 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package ubic.gemma.loader.genome.gene.ncbi.homology;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.basecode.util.FileTools;
import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.util.ConfigUtils;

/**
 * Reads in the homologene list as specified in the Gemmea.properties file. Loads the list at startup and keeps a
 * mapping off
 * 
 * @author kelsey
 * @version $Id$
 */
@Component
public class HomologeneServiceImpl implements HomologeneService {

    protected static final Log log = LogFactory.getLog( HomologeneServiceImpl.class );

    private static final String COMMENT_CHARACTER = "#";

    private static final char DELIMITING_CHARACTER = '\t';

    private static final String HOMOLOGENE_FILE = "ncbi.homologene.fileName";

    private static final String LOAD_HOMOLOGENE = "load.homologene";

    private AtomicBoolean enabled = new AtomicBoolean( false );

    // a collection of gene
    // IDs
    private Map<Long, Long> gene2Group = new ConcurrentHashMap<Long, Long>();

    @Autowired
    private GeneService geneService;

    @Autowired
    private TaxonService taxonService;

    private Map<Long, Collection<Long>> group2Gene = new ConcurrentHashMap<Long, Collection<Long>>(); // Homology group
                                                                                                      // ID to

    /*
     * Name of file in NCBI
     */
    private String homologeneFileName = "homologene.data";

    private AtomicBoolean ready = new AtomicBoolean( false );

    private AtomicBoolean running = new AtomicBoolean( false );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.genome.gene.ncbi.homology.HomologeneService#getHomologue(ubic.gemma.model.genome.Gene,
     * ubic.gemma.model.genome.Taxon)
     */

    @Override
    public Gene getHomologue( Gene gene, Taxon taxon ) {
        if ( gene.getTaxon().getId() == taxon.getId() ) return gene;

        Collection<Gene> homologues = this.getHomologues( gene );

        if ( homologues == null || homologues.isEmpty() ) return null;

        for ( Gene g : homologues ) {
            if ( g.getTaxon().getId().equals( taxon.getId() ) ) return g;

        }

        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.genome.gene.ncbi.homology.HomologeneService#getHomologues(ubic.gemma.model.genome.Gene)
     */

    @Override
    public Collection<Gene> getHomologues( Gene gene ) {

        Collection<Gene> genes = new HashSet<Gene>();

        if ( !this.ready.get() ) {
            return genes;
        }

        Long groupId = null;

        Integer ncbiGeneId = gene.getNcbiGeneId();
        if ( ncbiGeneId == null ) return genes;
        try {
            groupId = this.getHomologeneGroup( ncbiGeneId.longValue() );
        } catch ( NumberFormatException e ) {
            return genes;
        }

        if ( groupId == null ) {
            return genes;
        }

        genes = this.getGenesInGroup( groupId );

        if ( genes != null ) genes.remove( gene ); // remove the given gene from the list

        return genes;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.genome.gene.ncbi.homology.HomologeneService#getHomologues(java.lang.Long)
     */
    @Override
    public Collection<Long> getHomologues( Long ncbiId ) {

        Collection<Long> NcbiGeneIds = new HashSet<Long>();

        if ( !this.ready.get() ) {
            return NcbiGeneIds;
        }

        Long groupId = this.getHomologeneGroup( ncbiId );
        if ( groupId == null ) return NcbiGeneIds;
        NcbiGeneIds = this.getNCBIGeneIdsInGroup( groupId );
        NcbiGeneIds.remove( ncbiId ); // remove the given gene from the list

        return NcbiGeneIds;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.genome.gene.ncbi.homology.HomologeneService#init(boolean)
     */
    @Override
    public synchronized void init( boolean force ) {

        if ( running.get() ) {
            return;
        }

        if ( ready.get() ) {
            return;
        }

        boolean loadHomologene = ConfigUtils.getBoolean( LOAD_HOMOLOGENE, true );
        this.homologeneFileName = ConfigUtils.getString( HOMOLOGENE_FILE );

        if ( !loadHomologene ) return;

        // if loading homologene is disabled in the configuration, return
        if ( !force && !loadHomologene ) {
            log.info( "Loading Homologene is disabled (force=" + force + ", load.homologene =" + loadHomologene + ")" );
            return;
        }

        enabled.set( true );

        // Load the homologene groups for searching

        Thread loadThread = new Thread( new Runnable() {
            @Override
            public void run() {

                running.set( true );

                log.info( "Loading Homologene..." );
                StopWatch loadTime = new StopWatch();
                loadTime.start();

                boolean interrupted = false;

                HomologeneFetcher hf = new HomologeneFetcher();
                Collection<LocalFile> downloadedFiles = hf.fetch( homologeneFileName );
                File f = null;

                if ( downloadedFiles == null || downloadedFiles.isEmpty() ) {
                    log.warn( "Unable to download Homologene File. Aborting" );
                    return;
                }

                if ( downloadedFiles.size() > 1 )
                    log.info( "Downloaded more than 1 file for homologene.  Using 1st.  " );

                f = downloadedFiles.iterator().next().asFile();
                if ( !f.canRead() ) {
                    log.warn( "Downloaded Homologene File. But unable to read Aborting" );
                    return;
                }

                while ( !interrupted && !ready.get() ) {

                    try {
                        InputStream is = FileTools.getInputStreamFromPlainOrCompressedFile( f.getAbsolutePath() );
                        parseHomologGeneFile( is );
                    } catch ( IOException ioe ) {
                        log.error( "Unable to parse homologene file. Error is " + ioe );
                    }

                }
                running.set( false );

            }

        }, "Homologene_load_thread" );

        if ( running.get() ) return;
        loadThread.setDaemon( true ); // So vm doesn't wait on these threads to shutdown (if shutting down)
        loadThread.start();

    }

    /**
     * Given an NCBI Homologene Group ID returns a list of all the NCBI Gene Ids in the given group
     * 
     * @param homologeneGrouopId
     * @return Collection of NCBI Gene Ids, or null if not ready.
     */
    protected Collection<Long> getNCBIGeneIdsInGroup( long homologeneGrouopId ) {

        if ( !this.ready.get() ) {
            return null;
        }

        return this.group2Gene.get( homologeneGrouopId );
    }

    /**
     * @param is
     * @throws IOException
     */
    protected void parseHomologGeneFile( InputStream is ) throws IOException {

        BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
        String line = null;

        while ( ( line = br.readLine() ) != null ) {

            if ( StringUtils.isBlank( line ) || line.startsWith( COMMENT_CHARACTER ) ) {
                continue;
            }
            String[] fields = StringUtils.splitPreserveAllTokens( line, DELIMITING_CHARACTER );

            Integer taxonId = Integer.parseInt( fields[1] );
            Long groupId;
            Long geneId;
            try {
                groupId = Long.parseLong( fields[0] );
                geneId = Long.parseLong( fields[2] );
            } catch ( NumberFormatException e ) {
                log.warn( "Unparseable line from homologene: " + line );
                continue;
            }
            String geneSymbol = fields[3];

            if ( !group2Gene.containsKey( groupId ) ) {
                group2Gene.put( groupId, new ArrayList<Long>() );
            }
            group2Gene.get( groupId ).add( geneId );

            if ( !gene2Group.containsKey( geneId ) ) {
                gene2Group.put( geneId, groupId );
            } else {
                log.warn( "Duplicate gene ID encountered.  Skipping: geneID=" + geneId + " ,taxonID = " + taxonId
                        + " ,geneSymbol = " + geneSymbol );
            }
        }
        ready.set( true );
        log.info( "Gene Homology successfully loaded: " + gene2Group.keySet().size() + " genes covered in "
                + group2Gene.keySet().size() + " groups" );

    }

    /**
     * Given an NCBI Homologene Group ID returns a list of all the genes in gemma in that given group
     * 
     * @param homologeneGrouopId NCBI Homologene group ID
     * @return Collection genes in the given group.
     */

    private Collection<Gene> getGenesInGroup( Long homologeneGroupId ) {

        Collection<Gene> genes = new ArrayList<Gene>();

        if ( homologeneGroupId == null || !this.ready.get() || !this.group2Gene.containsKey( homologeneGroupId ) ) {
            return genes;
        }

        Collection<Long> ncbiIds = this.group2Gene.get( homologeneGroupId );

        Collection<Long> skippedNcbiIds = new ArrayList<Long>();

        for ( Long ncbiId : ncbiIds ) {
            Gene gene = this.geneService.findByNCBIId( ncbiId.intValue() );
            if ( gene == null ) {
                skippedNcbiIds.add( ncbiId );
                continue;
            }

            genes.add( gene );
        }

        if ( log.isDebugEnabled() && !skippedNcbiIds.isEmpty() ) {
            log.debug( "Skipped " + skippedNcbiIds.size()
                    + " homologous genes cause unable to find in Gemma. NCBI ids are:  " + skippedNcbiIds );
        }
        return genes;

    }

    /**
     * Given an NCBI Gene ID returns the NCBI homologene group
     * 
     * @param ncbiID
     * @return homologene group id, or null if not ready
     */
    private Long getHomologeneGroup( long ncbiID ) {

        if ( !this.ready.get() ) {
            return null;
        }

        return this.gene2Group.get( ncbiID );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.genome.gene.ncbi.homology.HomologeneService#getHomologueValueObject(java.lang.Long,
     * java.lang.String)
     */

    @Override
    public GeneValueObject getHomologueValueObject( Long geneId, String taxonCommonName ) {

        Gene gene = geneService.load( geneId );
        final Taxon mouse = this.taxonService.findByCommonName( "mouse" );
        Gene geneToReturn = null;
        if ( gene.getTaxon().getId() == mouse.getId() ) {
            geneToReturn = gene;
        } else {
            geneToReturn = getHomologue( gene, mouse );
        }

        if ( geneToReturn == null ) return null;
        return new GeneValueObject( geneToReturn );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.genome.gene.ncbi.homology.HomologeneService#getHomologueValueObjects(java.lang.Long)
     */

    @Override
    public Collection<GeneValueObject> getHomologueValueObjects( Long geneId ) {

        Gene gene = geneService.load( geneId );

        Collection<Gene> genes = getHomologues( gene );
        if ( genes == null ) return null;

        return GeneValueObject.convert2ValueObjects( getHomologues( gene ) );

    }

}
