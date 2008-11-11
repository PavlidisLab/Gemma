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
import java.util.concurrent.BlockingQueue;

import org.apache.commons.lang.StringUtils;

import ubic.gemma.loader.genome.gene.ncbi.model.NCBIGeneInfo;
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
    BlockingQueue<NcbiGeneHistory> queue = null;
    Map<String, NCBIGeneInfo> geneInfo = null;

    Map<String, NcbiGeneHistory> id2history = new HashMap<String, NcbiGeneHistory>();

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

        if ( fields.length != GENE_HISTORY_FILE_NUM_FIELDS ) {
            // sanity check.
            throw new IllegalStateException( "NCBI gene_history file has unexpected column count. Expected "
                    + GENE_HISTORY_FILE_NUM_FIELDS + ", got " + fields.length + " in line=" + line );
        }

        // String taxonId = fields[0];
        String geneId = fields[1];
        String discontinuedGeneId = fields[2];

        if ( StringUtils.isBlank( geneId ) || geneId.equals( "-" ) ) {
            return null;
        }

        // String discontinuedSymbol = fields[3];

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
    public boolean containsKey( String key ) {
        return id2history.containsKey( key );
    }

    @Override
    public NcbiGeneHistory get( String key ) {
        return id2history.get( key );
    }

    @Override
    protected String getKey( NcbiGeneHistory newItem ) {
        return newItem.getCurrentId();
    }

    @Override
    public Collection<String> getKeySet() {
        return id2history.keySet();
    }

    @Override
    protected void put( String key, NcbiGeneHistory value ) {
        id2history.put( key, value );
    }

}
