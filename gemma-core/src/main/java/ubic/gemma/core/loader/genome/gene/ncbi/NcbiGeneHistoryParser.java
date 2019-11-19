/*
 * The Gemma project
 *
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.core.loader.genome.gene.ncbi;

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.loader.genome.gene.ncbi.model.NcbiGeneHistory;
import ubic.gemma.core.loader.util.parser.BasicLineMapParser;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Parse the NCBI "gene_history" file. File format : tax_id, GeneID,Discontinued_GeneID, Discontinued_Symbol,
 * Discontinue_Date; (tab is used as a separator, pound sign - start of a comment) File is obtained from
 * ftp.ncbi.nih.gov.gene/DATA
 * See <a href='ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/README'>ncbi readme</a>
 * There are two kinds of lines. Lines with a "-" for the GeneID (the majority) seems to be used when the
 * record was withdrawn (Field is defined as "the current unique identified for a gene"). Lines with a symbol means it
 * was replaced, so far as I can tell.
 *
 * @author paul
 */
public class NcbiGeneHistoryParser extends BasicLineMapParser<String, NcbiGeneHistory> {

    private static final int GENE_HISTORY_FILE_NUM_FIELDS = 5;

    private final Map<String, NcbiGeneHistory> id2history = new HashMap<>();

    /*
     * Taxon -> Symbol -> NCBI ID
     */
    private final Map<Integer, Map<String, String>> discontinuedGenes = new HashMap<>();

    @Override
    public boolean containsKey( String key ) {
        return id2history.containsKey( key );
    }

    @Override
    public NcbiGeneHistory get( String key ) {
        return id2history.get( key );
    }

    @Override
    public Collection<String> getKeySet() {
        return id2history.keySet();
    }

    @Override
    public Collection<NcbiGeneHistory> getResults() {
        return id2history.values();
    }

    @Override
    public NcbiGeneHistory parseOneLine( String line ) {
        if ( line.startsWith( "#" ) ) {
            return null;
        }
        String[] fields = StringUtils.split( line, '\t' );

        if ( fields.length > NcbiGeneHistoryParser.GENE_HISTORY_FILE_NUM_FIELDS ) {
            // sanity check.
            throw new IllegalStateException( "NCBI gene_history file has unexpected column count. Expected "
                    + NcbiGeneHistoryParser.GENE_HISTORY_FILE_NUM_FIELDS + ", got " + fields.length + " in line="
                    + line );
        }

        // #tax_id  GeneID  Discontinued_GeneID Discontinued_Symbol Discontinue_Date

        String geneId = fields[1];
        String discontinuedGeneId = fields[2];

        // Case of discontinued gene. Since we don't have a symbol for it, we can't provide history
        //  if ( StringUtils.isBlank( geneId ) || geneId.equals( "-" ) ) {
        String taxonId = fields[0];
        String discontinuedSymbol = fields[3];

        Integer taxonInt = Integer.parseInt( taxonId );

        if ( !( discontinuedGenes.containsKey( taxonInt ) ) ) {
            discontinuedGenes.put( taxonInt, new HashMap<String, String>() );
        }

        if ( log.isDebugEnabled() ) log.debug( discontinuedSymbol + ": discontinued id=" + discontinuedGeneId );
        discontinuedGenes.get( taxonInt ).put( discontinuedSymbol, discontinuedGeneId );

        if ( StringUtils.isBlank( geneId ) || geneId.equals( "-" ) ) {
            return null;
        }

        // case of replaced gene
        NcbiGeneHistory his;
        if ( id2history.containsKey( discontinuedGeneId ) ) {
            his = id2history.get( discontinuedGeneId );
            his.update( discontinuedGeneId, geneId );
            id2history.remove( discontinuedGeneId );
            id2history.put( geneId, his );
        } else {
            his = new NcbiGeneHistory( discontinuedGeneId );
            his.update( discontinuedGeneId, geneId );
        }
        return his;
    }

    @Override
    protected String getKey( NcbiGeneHistory newItem ) {
        return newItem.getCurrentId();
    }

    @Override
    protected void put( String key, NcbiGeneHistory value ) {
        id2history.put( key, value );
    }

    /**
     * @param  geneSymbol gene symbol
     * @param  taxonId    taxon id
     * @return            null, or the NCBI ID of the gene that was discontinued.
     */
    public String discontinuedIdForSymbol( String geneSymbol, Integer taxonId ) {
        if ( !discontinuedGenes.containsKey( taxonId ) )
            return null;
        return discontinuedGenes.get( taxonId ).get( geneSymbol );
    }

}
