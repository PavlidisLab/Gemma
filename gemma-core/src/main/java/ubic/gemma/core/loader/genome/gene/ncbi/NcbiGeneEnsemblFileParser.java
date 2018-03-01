/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.loader.genome.gene.ncbi;

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.loader.util.parser.BasicLineMapParser;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author paul
 */
public class NcbiGeneEnsemblFileParser extends BasicLineMapParser<String, String> {
    private static final int GENE_ENSEMBL_FILE_NUM_FIELDS = 7;
    private final Map<String, String> id2ensembl = new HashMap<>();

    @Override
    public boolean containsKey( String key ) {
        return id2ensembl.containsKey( key );
    }

    @Override
    public String get( String key ) {
        return id2ensembl.get( key );
    }

    @Override
    public Collection<String> getKeySet() {
        return id2ensembl.keySet();
    }

    @Override
    public Collection<String> getResults() {
        return id2ensembl.values();
    }

    @Override
    public String parseOneLine( String line ) {
        if ( line.startsWith( "#" ) ) {
            return null;
        }
        String[] fields = StringUtils.split( line, '\t' );

        if ( fields.length > NcbiGeneEnsemblFileParser.GENE_ENSEMBL_FILE_NUM_FIELDS ) {
            // sanity check.
            throw new IllegalStateException( "NCBI geneEnsembl file has unexpected column count. Expected "
                    + NcbiGeneEnsemblFileParser.GENE_ENSEMBL_FILE_NUM_FIELDS + ", got " + fields.length + " in line="
                    + line );
        }

        // String taxonId = fields[0];
        String geneId = fields[1];
        String ensemblId = fields[2];

        if ( StringUtils.isBlank( geneId ) || geneId.equals( "-" ) ) {
            return null;
        }

        // String discontinuedSymbol = fields[3];

        id2ensembl.put( geneId, ensemblId );

        return ensemblId;
    }

    @Override
    protected String getKey( String newItem ) {
        return newItem;
    }

    @Override
    protected void put( String key, String value ) {
        id2ensembl.put( key, value );
    }

}
