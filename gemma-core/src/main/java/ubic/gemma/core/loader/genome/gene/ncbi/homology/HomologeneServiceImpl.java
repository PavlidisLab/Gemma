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

package ubic.gemma.core.loader.genome.gene.ncbi.homology;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.basecode.util.FileTools;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.Settings;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reads in the homologene list as specified in the Gemmea.properties file. Loads the list at startup and keeps a
 * mapping off
 *
 * @author kelsey
 */
@Component
public class HomologeneServiceImpl implements HomologeneService {

    protected static final Log log = LogFactory.getLog( HomologeneServiceImpl.class );
    private static final String COMMENT_CHARACTER = "#";
    private static final char DELIMITING_CHARACTER = '\t';
    private static final String HOMOLOGENE_FILE = "ncbi.homologene.fileName";
    private static final String LOAD_HOMOLOGENE = "load.homologene";

    // a collection of gene IDs
    private final Map<Long, Long> gene2Group = new ConcurrentHashMap<>();
    private final AtomicBoolean enabled = new AtomicBoolean( false );
    private final Map<Long, Collection<Long>> group2Gene = new ConcurrentHashMap<>(); // Homology group ID to Name of file in NCBI
    private final AtomicBoolean ready = new AtomicBoolean( false );
    private final AtomicBoolean running = new AtomicBoolean( false );
    private final GeneService geneService;
    private final TaxonService taxonService;
    
    private String homologeneFileName = "homologene.data";



    @Autowired
    public HomologeneServiceImpl( GeneService geneService, TaxonService taxonService ) {
        this.geneService = geneService;
        this.taxonService = taxonService;
    }



    @Override
    public Gene getHomologue( Gene gene, Taxon taxon ) {
        if ( Objects.equals( gene.getTaxon().getId(), taxon.getId() ) )
            return gene;

        Collection<Gene> homologues = this.getHomologues( gene );

        if ( homologues == null || homologues.isEmpty() )
            return null;

        for ( Gene g : homologues ) {
            if ( g.getTaxon().getId().equals( taxon.getId() ) )
                return g;

        }

        return null;
    }

    @Override
    public Collection<Gene> getHomologues( Gene gene ) {

        Collection<Gene> genes = new HashSet<>();

        if ( !this.ready.get() ) {
            return genes;
        }

        Long groupId;

        Integer ncbiGeneId = gene.getNcbiGeneId();
        if ( ncbiGeneId == null )
            return genes;
        try {
            groupId = this.getHomologeneGroup( ncbiGeneId.longValue() );
        } catch ( NumberFormatException e ) {
            return genes;
        }

        if ( groupId == null ) {
            return genes;
        }

        genes = this.getGenesInGroup( groupId );

        if ( genes != null )
            genes.remove( gene ); // remove the given gene from the list

        return genes;

    }

    @Override
    public Collection<Long> getHomologues( Long ncbiId ) {

        Collection<Long> NcbiGeneIds = new HashSet<>();

        if ( !this.ready.get() ) {
            return NcbiGeneIds;
        }

        Long groupId = this.getHomologeneGroup( ncbiId );
        if ( groupId == null )
            return NcbiGeneIds;
        NcbiGeneIds = this.getNCBIGeneIdsInGroup( groupId );
        NcbiGeneIds.remove( ncbiId ); // remove the given gene from the list

        return NcbiGeneIds;

    }

    @Override
    public GeneValueObject getHomologueValueObject( Long geneId, String taxonCommonName ) {
        Gene gene = geneService.load( geneId );
        final Taxon taxon = this.taxonService.findByCommonName( taxonCommonName );
        Gene geneToReturn;
        if ( Objects.equals( gene.getTaxon().getId(), taxon.getId() ) ) {
            geneToReturn = gene;
        } else {
            geneToReturn = getHomologue( gene, taxon );
        }
        return geneToReturn == null ? null : new GeneValueObject( geneToReturn );
    }

    @Override
    public Collection<GeneValueObject> getHomologueValueObjects( Long geneId ) {
        Gene gene = geneService.load( geneId );
        return geneService.loadValueObjects( getHomologues( gene ) );
    }

    @Override
    public synchronized void init( boolean force ) {

        if ( running.get() ) {
            return;
        }

        if ( ready.get() ) {
            return;
        }

        boolean loadHomologene = Settings.getBoolean( LOAD_HOMOLOGENE, true );
        this.homologeneFileName = Settings.getString( HOMOLOGENE_FILE );

        if ( !loadHomologene )
            return;

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
                File f;

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

                    try (InputStream is = FileTools.getInputStreamFromPlainOrCompressedFile( f.getAbsolutePath() )) {
                        parseHomologeneFile( is );
                    } catch ( IOException ioe ) {
                        log.error( "Unable to parse homologene file. Error is " + ioe );
                    }

                }
                running.set( false );

            }

        }, "Homologene_load_thread" );

        if ( running.get() )
            return;
        loadThread.setDaemon( true ); // So vm doesn't wait on these threads to shutdown (if shutting down)
        loadThread.start();

    }

    /**
     * Given an NCBI Homologene Group ID returns a list of all the NCBI Gene Ids in the given group
     *
     * @return Collection of NCBI Gene Ids, or null if not ready.
     */
    @Override
    public Collection<Long> getNCBIGeneIdsInGroup( long homologeneGroupId ) {
        if ( !this.ready.get() ) {
            return null;
        }
        return this.group2Gene.get( homologeneGroupId );
    }


    @Override
    public void parseHomologeneFile( InputStream is ) throws IOException {

        BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
        String line;

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
        log.info( "Gene Homology successfully loaded: " + gene2Group.keySet().size() + " genes covered in " + group2Gene
                .keySet().size() + " groups" );

    }



    /**
     * Given an NCBI Homologene Group ID returns a list of all the genes in gemma in that given group
     *
     * @param homologeneGroupId NCBI Homologene group ID
     * @return Collection genes in the given group.
     */
    private Collection<Gene> getGenesInGroup( Long homologeneGroupId ) {

        Collection<Gene> genes = new ArrayList<>();

        if ( homologeneGroupId == null || !this.ready.get() || !this.group2Gene.containsKey( homologeneGroupId ) ) {
            return genes;
        }

        Collection<Long> ncbiIds = this.group2Gene.get( homologeneGroupId );
        Collection<Long> skippedNcbiIds = new ArrayList<>();

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
     * @return homologene group id, or null if not ready
     */
    private Long getHomologeneGroup( long ncbiID ) {
        if ( !this.ready.get() ) {
            return null;
        }
        return this.gene2Group.get( ncbiID );
    }



}
