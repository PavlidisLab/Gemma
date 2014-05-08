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
package ubic.gemma.loader.genome.taxon;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.util.FileTools;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.Persister;

/**
 * Load taxa into the system.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class TaxonLoader {

    private static Log log = LogFactory.getLog( TaxonLoader.class.getName() );

    Persister persisterHelper;

    public void setPersisterHelper( Persister persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    /**
     * @param file
     * @return
     * @throws IOException
     */
    public int load( File file ) throws IOException {
        if ( file == null ) {
            throw new IllegalArgumentException( "File cannot be null" );
        }
        if ( !file.exists() || !file.canRead() ) {
            throw new IOException( "Could not read from file " + file.getPath() );
        }
        try (InputStream stream = FileTools.getInputStreamFromPlainOrCompressedFile( file.getAbsolutePath() );) {
            int count = load( stream );
            return count;
        }
    }

    /**
     * @param file
     * @return
     * @throws IOException
     */
    public int load( String filename ) throws IOException {
        if ( StringUtils.isBlank( filename ) ) {
            throw new IllegalArgumentException( "No filename provided" );
        }
        log.info( "Parsing " + filename );
        File infile = new File( filename );
        return load( infile );
    }

    /**
     * @param inputStream
     * @throws IOException
     */
    public int load( final InputStream inputStream ) throws IOException {
        TaxonParser parser = new TaxonParser();
        parser.parse( inputStream );
        Collection<Taxon> results = parser.getResults();

        int count = 0;

        for ( Taxon taxon : results ) {

            if ( log.isDebugEnabled() ) log.debug( "Loading " + taxon );
            taxon = ( Taxon ) persisterHelper.persist( taxon );
            count++;

        }

        log.info( "Persisted " + count + " taxa" );
        return count;
    }

}
