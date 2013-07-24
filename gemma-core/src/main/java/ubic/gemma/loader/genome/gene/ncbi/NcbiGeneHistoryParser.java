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
package ubic.gemma.loader.genome.gene.ncbi;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import ubic.gemma.loader.genome.gene.ncbi.model.NcbiGeneHistory;
import ubic.gemma.loader.util.parser.BasicLineMapParser;

/**
 * Parse the NCBI "gene_history" file. File format : tax_id, GeneID,Discontinued_GeneID, Discontinued_Symbol,
 * Discontinue_Date; (tab is used as a separator, pound sign - start of a comment) File is obtained from
 * ftp.ncbi.nih.gov.gene/DATA
 * 
 * @author paul
 * @version $Id$
 */
public class NcbiGeneHistoryParser extends BasicLineMapParser<String, NcbiGeneHistory> {

    private static final int GENE_HISTORY_FILE_NUM_FIELDS = 5;

    private Map<String, NcbiGeneHistory> id2history = new HashMap<String, NcbiGeneHistory>();

    private Map<Integer, Map<String, String>> discontinuedGenes = new HashMap<Integer, Map<String, String>>();

    @Override
    public boolean containsKey( String key ) {
        return id2history.containsKey( key );
    }

    /**
     * @param geneSymbol
     * @return null, or the NCBI ID of the gene that was discontinued.
     */
    public String discontinuedIdForSymbol( String geneSymbol, Integer taxonId ) {
        if ( !discontinuedGenes.containsKey( taxonId ) ) return null;
        return discontinuedGenes.get( taxonId ).get( geneSymbol );
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

        if ( fields.length > GENE_HISTORY_FILE_NUM_FIELDS ) {
            // sanity check.
            throw new IllegalStateException( "NCBI gene_history file has unexpected column count. Expected "
                    + GENE_HISTORY_FILE_NUM_FIELDS + ", got " + fields.length + " in line=" + line );
        }

        String geneId = fields[1];
        String discontinuedGeneId = fields[2];

        if ( StringUtils.isBlank( geneId ) || geneId.equals( "-" ) ) {
            String taxonId = fields[0];
            String discontinuedSymbol = fields[3];

            Integer taxonInt = Integer.parseInt( taxonId );

            if ( !( discontinuedGenes.containsKey( taxonInt ) ) ) {
                discontinuedGenes.put( taxonInt, new HashMap<String, String>() );
            }

            discontinuedGenes.get( taxonInt ).put( discontinuedSymbol, discontinuedGeneId );
            return null;
        }

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

}
