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
import org.springframework.core.io.Resource;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Reads in the homologene list as specified in the Gemmea.properties file. Loads the list at startup and keeps a
 * mapping off.
 * <p>
 * You almost certainly want to call {@link #refresh()} before using this service. This is done automatically if you use
 * the {@link HomologeneServiceFactory} to lazy-load this service.
 *
 * @author kelsey
 * @see HomologeneServiceFactory
 */
public class HomologeneServiceImpl implements HomologeneService {

    private static final Log log = LogFactory.getLog( HomologeneServiceImpl.class );
    private static final String COMMENT_CHARACTER = "#";
    private static final char DELIMITING_CHARACTER = '\t';

    // a collection of gene IDs
    private final Map<Long, Long> gene2Group = new ConcurrentHashMap<>();
    private final Map<Long, Collection<Long>> group2Gene = new ConcurrentHashMap<>(); // Homology group ID to Name of file in NCBI

    private final GeneService geneService;
    private final TaxonService taxonService;
    private final Resource homologeneFile;

    public HomologeneServiceImpl( GeneService geneService, TaxonService taxonService, Resource homologeneFile ) {
        this.geneService = geneService;
        this.taxonService = taxonService;
        this.homologeneFile = homologeneFile;
    }

    public Gene getHomologue( Gene gene, Taxon taxon ) {
        if ( Objects.equals( gene.getTaxon().getId(), taxon.getId() ) ) return gene;

        Collection<Gene> homologues = this.getHomologues( gene );

        if ( homologues == null || homologues.isEmpty() ) return null;

        for ( Gene g : homologues ) {
            if ( g.getTaxon().getId().equals( taxon.getId() ) ) return g;

        }

        return null;
    }

    public Collection<Gene> getHomologues( Gene gene ) {

        Collection<Gene> genes = new HashSet<>();

        Long groupId;

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

    public Collection<Long> getHomologues( Long ncbiId ) {

        Collection<Long> NcbiGeneIds = new HashSet<>();

        Long groupId = this.getHomologeneGroup( ncbiId );
        if ( groupId == null ) return NcbiGeneIds;
        NcbiGeneIds = this.getNCBIGeneIdsInGroup( groupId );
        NcbiGeneIds.remove( ncbiId ); // remove the given gene from the list

        return NcbiGeneIds;
    }

    @Override
    public void refresh() throws IOException {
        // Load the homologene groups for searching
        StopWatch loadTime = new StopWatch();
        loadTime.start();
        HomologeneServiceImpl.log.info( String.format( "Loading Homologene from %s...", homologeneFile ) );
        try ( InputStream is = homologeneFile.getInputStream() ) {
            parseHomologeneFile( is );
        }
        HomologeneServiceImpl.log.info( String.format( "Gene Homology successfully loaded: %d genes covered in %d groups in %d ms.",
                gene2Group.keySet().size(), group2Gene.keySet().size(), loadTime.getTime( TimeUnit.MILLISECONDS ) ) );
    }

    public GeneValueObject getHomologueValueObject( Long geneId, String taxonCommonName ) {
        Gene gene = geneService.load( geneId );
        if ( gene == null ) {
            return null;
        }
        final Taxon taxon = this.taxonService.findByCommonName( taxonCommonName );
        if ( taxon == null ) {
            throw new RuntimeException( "No taxon found for " + taxonCommonName );
        }
        Gene geneToReturn;
        if ( Objects.equals( gene.getTaxon().getId(), taxon.getId() ) ) {
            geneToReturn = gene;
        } else {
            geneToReturn = this.getHomologue( gene, taxon );
        }
        return geneToReturn == null ? null : new GeneValueObject( geneToReturn );
    }

    /**
     * Given an NCBI Homologene Group ID returns a list of all the NCBI Gene Ids in the given group
     *
     * @return Collection of NCBI Gene Ids, or null if not ready.
     */
    public Collection<Long> getNCBIGeneIdsInGroup( long homologeneGroupId ) {
        return this.group2Gene.get( homologeneGroupId );
    }

    private void parseHomologeneFile( InputStream is ) throws IOException {

        BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
        String line;

        while ( ( line = br.readLine() ) != null ) {

            if ( StringUtils.isBlank( line ) || line.startsWith( HomologeneServiceImpl.COMMENT_CHARACTER ) ) {
                continue;
            }
            String[] fields = StringUtils.splitPreserveAllTokens( line, HomologeneServiceImpl.DELIMITING_CHARACTER );

            int taxonId = Integer.parseInt( fields[1] );
            Long groupId;
            Long geneId;
            try {
                groupId = Long.parseLong( fields[0] );
                geneId = Long.parseLong( fields[2] );
            } catch ( NumberFormatException e ) {
                HomologeneServiceImpl.log.warn( "Unparseable line from homologene: " + line );
                continue;
            }
            String geneSymbol = fields[3];

            if ( !group2Gene.containsKey( groupId ) ) {
                group2Gene.put( groupId, new ArrayList<Long>() );
            }
            if ( !group2Gene.get( groupId ).contains( geneId ) ) {
                group2Gene.get( groupId ).add( geneId );
            } else {
                HomologeneServiceImpl.log.warn( "Duplicate gene ID encountered (group2Gene).  Skipping: geneID=" + geneId + " , taxonID = " + taxonId + " , geneSymbol = " + geneSymbol + " for group " + groupId );
            }

            if ( !gene2Group.containsKey( geneId ) ) {
                gene2Group.put( geneId, groupId );
            } else {
                HomologeneServiceImpl.log.warn( "Duplicate gene ID encountered (gene2Group).  Skipping: geneID=" + geneId + " , taxonID = " + taxonId + " , geneSymbol = " + geneSymbol + " for group " + groupId );
            }
        }
    }

    /**
     * Given an NCBI Homologene Group ID returns a list of all the genes in gemma in that given group
     *
     * @param homologeneGroupId NCBI Homologene group ID
     * @return Collection genes in the given group.
     */
    private Collection<Gene> getGenesInGroup( Long homologeneGroupId ) {

        Collection<Gene> genes = new ArrayList<>();

        if ( homologeneGroupId == null || !this.group2Gene.containsKey( homologeneGroupId ) ) {
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

        if ( HomologeneServiceImpl.log.isDebugEnabled() && !skippedNcbiIds.isEmpty() ) {
            HomologeneServiceImpl.log.debug( "Skipped " + skippedNcbiIds.size() + " homologous genes cause unable to find in Gemma. NCBI ids are:  " + skippedNcbiIds );
        }
        return genes;

    }

    /**
     * Given an NCBI Gene ID returns the NCBI homologene group
     *
     * @return homologene group id, or null if not ready
     */
    private Long getHomologeneGroup( long ncbiID ) {
        return this.gene2Group.get( ncbiID );
    }
}
