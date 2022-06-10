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
package ubic.gemma.core.loader.genome.gene.ncbi;

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.loader.genome.gene.ncbi.model.NCBIGeneInfo;
import ubic.gemma.core.loader.genome.gene.ncbi.model.NCBIGeneInfo.NomenclatureStatus;
import ubic.gemma.core.loader.util.QueuingParser;
import ubic.gemma.core.loader.util.parser.BasicLineMapParser;
import ubic.gemma.core.loader.util.parser.FileFormatException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/**
 * Class to parse the gene_info file from NCBI Gene. See <a href="ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/README">readme</a> for details
 * of the format.
 *
 * @author pavlidis
 */
public class NcbiGeneInfoParser extends BasicLineMapParser<String, NCBIGeneInfo> implements QueuingParser<String> {

    private static final int NCBI_GENEINFO_FIELDS_PER_ROW = 16;
    private final Map<String, NCBIGeneInfo> results = new HashMap<>();
    private BlockingQueue<String> resultsKeys;
    private boolean filter = true;
    private Collection<Integer> ncbiTaxonIds;

    @Override
    public boolean containsKey( String key ) {
        return results.containsKey( key );
    }

    @Override
    public NCBIGeneInfo get( String key ) {
        return results.get( key );
    }

    @Override
    public Collection<String> getKeySet() {
        return results.keySet();
    }

    @Override
    public Collection<NCBIGeneInfo> getResults() {
        return results.values();
    }

    @Override
    public NCBIGeneInfo parseOneLine( String line ) {
        String[] fields = StringUtils.splitPreserveAllTokens( line, '\t' );

        if ( fields.length != NcbiGeneInfoParser.NCBI_GENEINFO_FIELDS_PER_ROW ) {
            //noinspection StatementWithEmptyBody // backwards compatibility, old format, hopefully okay
            if ( fields.length == 13 || fields.length == 14 || fields.length == 15 ) {
                // They keep adding fields at the end...we only need the first few.
            } else {
                throw new FileFormatException(
                        "Line + " + line + " is not in the right format: has " + fields.length + " fields, expected "
                                + NcbiGeneInfoParser.NCBI_GENEINFO_FIELDS_PER_ROW );
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
            
            // See ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/README
            // #Format:
            
            // tax_id
            // GeneID
            // Symbol
            // LocusTag
            // Synonyms
            // dbXrefs, separated by |
            // chromosome
            // map_location
            // description
            // type_of_gene
            // Symbol_from_nomenclature_authority
            // Full_name_from_nomenclature_authority
            // Nomenclature_status
            // Other_designations
            // Modification_date
            // Feature type

            geneInfo.setTaxId( taxonId );
            geneInfo.setGeneId( fields[1] );
            geneInfo.setDefaultSymbol( fields[2] );
            geneInfo.setLocusTag( fields[3] );
            String[] synonyms = StringUtils.splitPreserveAllTokens( fields[4], '|' );
            for ( String synonym : synonyms ) {
                if ( synonym.equals( "-" ) )
                    continue;
                geneInfo.addToSynonyms( synonym );
            }

            if ( !fields[5].equals( "-" ) ) {
                String[] dbXRefs = StringUtils.splitPreserveAllTokens( fields[5], '|' );
                for ( String dbXr : dbXRefs ) {
                    String[] dbF = StringUtils.split( dbXr, ':' );
                    if ( dbF.length != 2 ) {
                        /*
                         * Annoyingly, HGCN identifiers now have the format HGNC:X where X is an integer. This is
                         * apparent from downloading files from HGCN (http://www.genenames.org/cgi-bin/statistics). Same
                         * situation for MGI
                         *
                         * 2022: There are also identifiers with the format AllianceGenome:WB:WBGene00022277.
                         *
                         * Therefore we have a special case.
                         */
                        if ( dbF.length == 3 && ( dbF[1].equals( "HGNC" ) || dbF[1].equals( "MGI" ) ) ) {
                            dbF[1] = dbF[1] + ":" + dbF[2];
                        } else if (dbF.length == 3 && (dbF[0].equals("AllianceGenome"))) {
                            // we aren't going to do anything with these.
                            continue;
                        } else {
                            // we're very stringent to avoid data corruption.
                            throw new FileFormatException(
                                    "Expected 2 fields, got " + dbF.length + " from '" + dbXr + "'" );
                        }
                    }
                    geneInfo.addToDbXRefs( dbF[0], dbF[1] );
                }
            }

            geneInfo.setChromosome( fields[6] );
            geneInfo.setMapLocation( fields[7] );
            geneInfo.setDescription( fields[8] );
            geneInfo.setGeneType( NCBIGeneInfo.typeStringToGeneType( fields[9] ) );
            geneInfo.setSymbolIsFromAuthority( !fields[10].equals( "-" ) );
            geneInfo.setNameIsFromAuthority( !fields[11].equals( "-" ) );
            geneInfo.setNomenclatureStatus( fields[12].equals( "-" ) ?
                    NomenclatureStatus.UNKNOWN :
                    fields[11].equals( "O" ) ? NomenclatureStatus.OFFICIAL : NomenclatureStatus.INTERIM );
            // ignore 14th field for now - it stores alternate protein names
            // ignore 15th, modification date
        } catch ( NumberFormatException e ) {
            throw new FileFormatException( e );
        }
        return geneInfo;
    }

    @Override
    public String getKey( NCBIGeneInfo newItem ) {
        return newItem.getGeneId();
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
    public void parse( InputStream inputStream, BlockingQueue<String> queue ) throws IOException {
        this.resultsKeys = queue;
        this.parse( inputStream );
    }

    public void setFilter( boolean filter ) {
        this.filter = filter;
    }

    /**
     * @param ncbiTaxonIds Taxon IDs (NCBI, not Gemma ids) e.g. 9606 for H. sapiens
     */
    public void setSupportedTaxa( Collection<Integer> ncbiTaxonIds ) {
        this.ncbiTaxonIds = ncbiTaxonIds;
    }

}
