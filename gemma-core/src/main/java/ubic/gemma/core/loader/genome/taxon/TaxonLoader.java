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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.basecode.util.FileTools;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.persister.Persister;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

/**
 * Load taxa into the system.
 *
 * @author pavlidis
 */
public class TaxonLoader {

    private static final Log log = LogFactory.getLog( TaxonLoader.class.getName() );

    private Persister<Taxon> taxonPersister;

    public void setTaxonPersister( Persister taxonPersister ) {
        this.taxonPersister = taxonPersister;
    }

    public int load( File file ) throws IOException {
        if ( file == null ) {
            throw new IllegalArgumentException( "File cannot be null" );
        }
        if ( !file.exists() || !file.canRead() ) {
            throw new IOException( "Could not read from file " + file.getPath() );
        }
        try (InputStream stream = FileTools.getInputStreamFromPlainOrCompressedFile( file.getAbsolutePath() )) {
            return this.load( stream );
        }
    }

    @SuppressWarnings("unused") // Possible external use
    public int load( String filename ) throws IOException {
        if ( StringUtils.isBlank( filename ) ) {
            throw new IllegalArgumentException( "No filename provided" );
        }
        TaxonLoader.log.info( "Parsing " + filename );
        File infile = new File( filename );
        return this.load( infile );
    }

    public int load( final InputStream inputStream ) throws IOException {
        TaxonParser parser = new TaxonParser();
        parser.parse( inputStream );
        Collection<Taxon> results = parser.getResults();

        int count = 0;

        for ( Taxon taxon : results ) {

            if ( TaxonLoader.log.isDebugEnabled() )
                TaxonLoader.log.debug( "Loading " + taxon );
            taxonPersister.persist( taxon );
            count++;

        }

        TaxonLoader.log.info( "Persisted " + count + " taxa" );
        return count;
    }

}
