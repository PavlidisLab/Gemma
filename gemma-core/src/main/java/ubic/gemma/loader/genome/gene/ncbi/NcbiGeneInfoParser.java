/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.lang.StringUtils;

import ubic.gemma.loader.genome.gene.ncbi.model.NCBIGeneInfo;
import ubic.gemma.loader.genome.gene.ncbi.model.NCBIGeneInfo.NomenclatureStatus;
import ubic.gemma.loader.util.QueuingParser;
import ubic.gemma.loader.util.parser.BasicLineMapParser;
import ubic.gemma.loader.util.parser.FileFormatException;

/**
 * Class to parse the gene_info file from NCBI Gene. See {@link ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/README} for details
 * of the format.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class NcbiGeneInfoParser extends BasicLineMapParser<String, NCBIGeneInfo> implements QueuingParser {

    /**
     * 
     */
    private static final int NCBI_GENEINFO_FIELDS_PER_ROW = 15;

    private Map<String, NCBIGeneInfo> results = new HashMap<String, NCBIGeneInfo>();

    private BlockingQueue<String> resultsKeys;

    private boolean filter = true;

    private Collection<Integer> ncbiTaxonIds;

    public void setFilter( boolean filter ) {
        this.filter = filter;
    }

    /**
     * @param ncbiTaxonIds Taxon IDs (NCBI, not Gemma ids) e.g. 9606 for H. sapiens
     */
    public void setSupportedTaxa( Collection<Integer> ncbiTaxonIds ) {
        this.ncbiTaxonIds = ncbiTaxonIds;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.loaderutils.LineParser#parseOneLine(java.lang.String)
     */
    @Override
    public NCBIGeneInfo parseOneLine( String line ) {
        String[] fields = StringUtils.splitPreserveAllTokens( line, '\t' );

        if ( fields.length != NCBI_GENEINFO_FIELDS_PER_ROW ) {
            if ( fields.length == 13 || fields.length == 14 ) {
                // backwards compatibility
                // old format, hopefully okay
            } else {
                throw new FileFormatException( "Line + " + line + " is not in the right format: has " + fields.length
                        + " fields, expected " + NCBI_GENEINFO_FIELDS_PER_ROW );
            }
        }
        NCBIGeneInfo geneInfo = new NCBIGeneInfo();
        try {

            // Skip taxa that we don't support.
            int taxonId = Integer.parseInt( fields[0] );
            if ( filter && ncbiTaxonIds != null ) {
                if ( !ncbiTaxonIds.contains( taxonId ) ) {
                    return null;
                }
            }

            geneInfo.setTaxId( taxonId );
            geneInfo.setGeneId( fields[1] );
            geneInfo.setDefaultSymbol( fields[2] );
            geneInfo.setLocusTag( fields[3] );
            String[] synonyms = StringUtils.splitPreserveAllTokens( fields[4], '|' );
            for ( int i = 0; i < synonyms.length; i++ ) {
                if ( synonyms[i].equals( "-" ) ) continue;
                geneInfo.addToSynonyms( synonyms[i] );
            }

            if ( !fields[5].equals( "-" ) ) {
                String[] dbXRefs = StringUtils.splitPreserveAllTokens( fields[5], '|' );
                for ( int i = 0; i < dbXRefs.length; i++ ) {
                    String dbXr = dbXRefs[i];
                    String[] dbF = StringUtils.split( dbXr, ':' );
                    if ( dbF.length != 2 ) {
                        throw new FileFormatException( "Expected 2 fields, got " + dbF.length + " from '" + dbXr + "'" );
                    }
                    geneInfo.addToDbXRefs( dbF[0], dbF[1] );
                }
            }

            geneInfo.setChromosome( fields[6] );
            geneInfo.setMapLocation( fields[7] );
            geneInfo.setDescription( fields[8] );
            geneInfo.setGeneType( NCBIGeneInfo.typeStringToGeneType( fields[9] ) );
            geneInfo.setSymbolIsFromAuthority( fields[10].equals( "-" ) ? false : true );
            geneInfo.setNameIsFromAuthority( fields[11].equals( "-" ) ? false : true );
            geneInfo.setNomenclatureStatus( fields[12].equals( "-" ) ? NomenclatureStatus.UNKNOWN : fields[11]
                    .equals( "O" ) ? NomenclatureStatus.OFFICIAL : NomenclatureStatus.INTERIM );
            // ignore 14th field for now - it stores alternate protein names
        } catch ( NumberFormatException e ) {
            throw new FileFormatException( e );
        }
        return geneInfo;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.loaderutils.BasicLineMapParser#getKey(java.lang.Object)
     */
    @Override
    public String getKey( NCBIGeneInfo newItem ) {
        return newItem.getGeneId();
    }

    @Override
    public Collection<NCBIGeneInfo> getResults() {
        return results.values();
    }

    @Override
    public NCBIGeneInfo get( String key ) {
        return results.get( key );
    }

    @Override
    protected void put( String key, NCBIGeneInfo value ) {
        try {
            if ( resultsKeys != null ) {
                resultsKeys.put( key );
            }
            results.put( key, value );
        } catch ( InterruptedException e ) {
            log.error( e );
            throw new RuntimeException( e );
        }
    }

    @Override
    public boolean containsKey( String key ) {
        return results.containsKey( key );
    }

    @Override
    public void parse( InputStream inputStream, BlockingQueue queue ) throws IOException {
        this.resultsKeys = queue;
        this.parse( inputStream );
    }

    @Override
    public Collection<String> getKeySet() {
        return results.keySet();
    }

}
