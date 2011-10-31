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
package ubic.gemma.loader.genome.goldenpath;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.lang.StringUtils;

import ubic.gemma.loader.util.QueuingParser;
import ubic.gemma.loader.util.parser.BasicLineParser;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * Parse a dump of a Goldenpath table. The input is expected to have just two columns: sequence identifier (accession)
 * and sequence length. This assumes that all sequences are Genbank entries, so be careful using this with Ensembl (we
 * prefer to use GoldenPathDumper instead)
 * 
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.loader.genome.goldenpath.GoldenPathDumper
 */
public class GoldenPathBioSequenceParser extends BasicLineParser<BioSequence> implements QueuingParser<BioSequence> {

    private BlockingQueue<BioSequence> results = new ArrayBlockingQueue<BioSequence>( 10000 );

    private ExternalDatabase genbank;

    public GoldenPathBioSequenceParser() {
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
        genbank = ExternalDatabase.Factory.newInstance();
        genbank.setName( "Genbank" );
        genbank.setType( DatabaseType.SEQUENCE );
    }

    public void parse( InputStream inputStream, BlockingQueue<BioSequence> queue ) throws IOException {
        this.results = queue;
        parse( inputStream );
    }

    public BioSequence parseOneLine( String line ) {
        String[] fields = StringUtils.split( line );
        BioSequence bioSequence = BioSequence.Factory.newInstance();

        DatabaseEntry de = DatabaseEntry.Factory.newInstance();

        String name = fields[0];
        Long length = Long.parseLong( fields[1] );
        bioSequence.setName( name );
        bioSequence.setLength( length );

        de.setAccession( name );
        de.setExternalDatabase( genbank );

        bioSequence.setSequenceDatabaseEntry( de );

        return bioSequence;
    }

}
