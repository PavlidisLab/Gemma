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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.util.FileTools;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.util.progress.ProgressData;
import ubic.gemma.util.progress.ProgressManager;

/**
 * Load taxa into the system.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class TaxonLoader {

    private static Log log = LogFactory.getLog( TaxonLoader.class.getName() );

    PersisterHelper persisterHelper;

    public void setPersisterHelper( PersisterHelper persisterHelper ) {
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
        InputStream stream = FileTools.getInputStreamFromPlainOrCompressedFile( file.getAbsolutePath() );
        int count = load( stream );
        stream.close();
        return count;
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
    @SuppressWarnings("unchecked")
    public int load( final InputStream inputStream ) throws IOException {
        TaxonParser parser = new TaxonParser();
        parser.parse( inputStream );
        Collection<Taxon> results = parser.getResults();

        int count = 0;
        int cpt = 0;
        double secspt = 0.0;
        long millis = System.currentTimeMillis();

        for ( Taxon taxon : results ) {

            taxon = ( Taxon ) persisterHelper.persist( taxon );

            // just some timing information.
            if ( ++count % 1000 == 0 ) {
                cpt++;
                double secsperthousand = ( System.currentTimeMillis() - millis ) / 1000.0;
                secspt += secsperthousand;
                double meanspt = secspt / cpt;

                String progString = "Processed and loaded " + count + " taxa, last one was " + taxon.getCommonName()
                        + " (" + secsperthousand + " seconds elapsed, average per thousand=" + meanspt + ")";
                ProgressManager.updateCurrentThreadsProgressJob( new ProgressData( count, progString ) );
                log.info( progString );
                millis = System.currentTimeMillis();
            }
        }
        return count;
    }

}
