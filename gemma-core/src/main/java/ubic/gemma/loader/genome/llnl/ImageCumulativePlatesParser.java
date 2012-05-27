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
package ubic.gemma.loader.genome.llnl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.lang.StringUtils;

import ubic.gemma.loader.util.QueuingParser;
import ubic.gemma.loader.util.parser.BasicLineParser;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.SequenceType;

/**
 * <code>
 *  100     LLAM    5753    a       7       381     human   AA17697
 * </code>
 * 
 * @author pavlidis
 * @version $Id$
 * @deprecated as we don't import sequences this way any more.
 */
@Deprecated
public class ImageCumulativePlatesParser extends BasicLineParser<BioSequence> implements QueuingParser<BioSequence> {

    BlockingQueue<BioSequence> results = new ArrayBlockingQueue<BioSequence>( 10000 );
    private ExternalDatabase genbank;

    Collection<Integer> seen = new HashSet<Integer>();

    public ImageCumulativePlatesParser() {
        super();
        initGenbank();
    }

    @Override
    protected void addResult( BioSequence obj ) {
        try {
            results.put( obj );
        } catch ( InterruptedException e ) {
            // ;
        }
    }

    @Override
    public Collection<BioSequence> getResults() {
        return results;
    }

    private void initGenbank() {
        // if ( externalDatabaseService != null ) {
        // genbank = externalDatabaseService.find( "Genbank" );
        // } else {
        genbank = ExternalDatabase.Factory.newInstance();
        genbank.setName( "Genbank" );
        genbank.setType( DatabaseType.SEQUENCE );
        // }
    }

    @Override
    public BioSequence parseOneLine( String line ) {
        String[] fields = StringUtils.splitPreserveAllTokens( line, '\t' );

        if ( StringUtils.isBlank( fields[7] ) ) {
            return null;
        }

        // detect duplicates
        if ( seen.contains( fields[0].hashCode() ) ) {
            return null;
        }

        BioSequence seq = BioSequence.Factory.newInstance();
        String[] accessions = StringUtils.split( fields[7] );
        assert accessions.length > 0;
        seq.setName( accessions[0] );

        seq.setType( SequenceType.EST );

        Taxon t = Taxon.Factory.newInstance();
        t.setCommonName( fields[6] );

        seq.setTaxon( t );

        DatabaseEntry acc = DatabaseEntry.Factory.newInstance();

        acc.setAccession( accessions[0] );
        acc.setExternalDatabase( genbank );
        seq.setSequenceDatabaseEntry( acc );

        StringBuilder buf = new StringBuilder();
        buf.append( "IMAGE clone, " );
        buf.append( accessions[0] + "; " );
        if ( accessions.length > 1 ) {
            buf.append( "Other accessions:" );
            for ( int i = 1; i < accessions.length; i++ ) {
                buf.append( " " + accessions[i] );
            }
        }
        seq.setDescription( "IMAGE:" + fields[0] + "; accessions:" + buf.toString() );

        seen.add( fields[0].hashCode() );
        return seq;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.util.QueuingParser#parse(java.io.InputStream, java.util.concurrent.BlockingQueue)
     */
    @Override
    public void parse( InputStream inputStream, BlockingQueue<BioSequence> queue ) throws IOException {
        this.results = queue;
        seen.clear();
        parse( inputStream );

    }
}
