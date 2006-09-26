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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import ubic.basecode.util.StringUtil;
import ubic.gemma.loader.genome.gene.ncbi.model.NCBIGeneInfo;
import ubic.gemma.loader.genome.gene.ncbi.model.NCBIGeneInfo.NomenclatureStatus;
import ubic.gemma.loader.util.parser.BasicLineMapParser;
import ubic.gemma.loader.util.parser.FileFormatException;

/**
 * Class to parse the gene_info file from NCBI Gene. See {@link ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/README} for details
 * of the format.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class NcbiGeneInfoParser extends BasicLineMapParser {

    /**
     * 
     */
    private static final int NCBI_GENEINFO_FIELDS_PER_ROW = 13;

    private Map<String, NCBIGeneInfo> results = new HashMap<String, NCBIGeneInfo>();
    
    
    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.loaderutils.LineParser#parseOneLine(java.lang.String)
     */
    public Object parseOneLine( String line ) {
        String[] fields = StringUtil.splitPreserveAllTokens( line, '\t' );

        if ( fields.length != NCBI_GENEINFO_FIELDS_PER_ROW ) {
            throw new FileFormatException( "Line is not in the right format: has " + fields.length
                    + " fields, expected " + NCBI_GENEINFO_FIELDS_PER_ROW );
        }
        NCBIGeneInfo geneInfo = new NCBIGeneInfo();
        try {
            geneInfo.setTaxId( Integer.parseInt( fields[0] ) );
            geneInfo.setGeneId( fields[1] );
            geneInfo.setDefaultSymbol( fields[2] );
            geneInfo.setLocusTag( fields[3] );
            String[] synonyms = StringUtil.splitPreserveAllTokens( fields[4], '|' );
            for ( int i = 0; i < synonyms.length; i++ ) {
                geneInfo.addToSynonyms( synonyms[i] );
            }

            if ( !fields[5].equals( "-" ) ) {
                String[] dbXRefs = StringUtil.splitPreserveAllTokens( fields[5], '|' );
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
    protected Object getKey( Object newItem ) {
        return ( ( NCBIGeneInfo ) newItem ).getGeneId();
    }

    @Override
    public Collection<NCBIGeneInfo> getResults() {
        return results.values();
    }

    @Override
    public NCBIGeneInfo get( Object key ) {
        return results.get( key );
    }

    @Override
    protected void put( Object key, Object value ) {
        results.put( ( String ) key, ( NCBIGeneInfo ) value );
    }

    @Override
    public boolean containsKey( Object key ) {
        return results.containsKey( key );
    }

}
