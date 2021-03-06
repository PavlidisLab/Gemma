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
package ubic.gemma.core.loader.genome.taxon;

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.loader.util.parser.BasicLineMapParser;
import ubic.gemma.model.genome.Taxon;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Parse the "names.dmp" file from NCBI, ftp://ftp.ncbi.nih.gov/pub/taxonomy/.
 *
 * @author pavlidis
 */
public class TaxonParser extends BasicLineMapParser<Integer, Taxon> {

    private final Map<Integer, Taxon> results = new HashMap<>();

    @Override
    public boolean containsKey( Integer key ) {
        return results.containsKey( key );
    }

    @Override
    public Taxon get( Integer key ) {
        return results.get( key );
    }

    @Override
    public Collection<Integer> getKeySet() {
        return results.keySet();
    }

    @Override
    public Collection<Taxon> getResults() {
        return results.values();
    }

    @Override
    public Taxon parseOneLine( String line ) {
        String[] fields = StringUtils.splitPreserveAllTokens( line, '|' );

        int ncbiid = Integer.parseInt( StringUtils.strip( fields[0] ) );

        if ( !results.containsKey( ncbiid ) ) {
            Taxon t = Taxon.Factory.newInstance();
            t.setNcbiId( ncbiid );
            t.setIsGenesUsable( false );
            results.put( ncbiid, t );
        }

        String tag = StringUtils.strip( fields[3] );
        if ( tag.equals( "scientific name" ) ) {
            results.get( ncbiid ).setScientificName( StringUtils.strip( fields[1] ) );
        } else if ( tag.equals( "genbank common name" ) ) {
            results.get( ncbiid ).setCommonName( fields[1] );
        }

        return results.get( ncbiid );

    }

    @Override
    protected Integer getKey( Taxon newItem ) {
        return newItem.getNcbiId();
    }

    @Override
    protected void put( Integer key, Taxon value ) {
        results.put( key, value );
    }

}
