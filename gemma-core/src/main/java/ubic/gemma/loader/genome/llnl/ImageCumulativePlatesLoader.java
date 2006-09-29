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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.util.FileTools;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.util.progress.ProgressData;
import ubic.gemma.util.progress.ProgressManager;

/**
 * Persist biosequences generated from the IMAGE clone library. IMAGE contains ESTs from human, mouse, rat, rhesus and a
 * few others. This class assumes that the sequences are new.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ImageCumulativePlatesLoader {

    private static Log log = LogFactory.getLog( ImageCumulativePlatesLoader.class.getName() );

    private static final int BATCH_SIZE = 2000;

    PersisterHelper persisterHelper;

    ExternalDatabaseService externalDatabaseService;
    BioSequenceService bioSequenceService;
    ExternalDatabase genbank;

    // private TaxonService taxonService;

    public void setExternalDatabaseService( ExternalDatabaseService externalDatabaseService ) {
        this.externalDatabaseService = externalDatabaseService;
        genbank = externalDatabaseService.find( "Genbank" );
        assert ( genbank != null && genbank.getId() != null );
    }

    public void setBioSequenceService( BioSequenceService bioSequenceService ) {
        this.bioSequenceService = bioSequenceService;
    }

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
        ImageCumulativePlatesParser parser = new ImageCumulativePlatesParser();
        parser.parse( inputStream );
        Collection<BioSequence> results = parser.getResults();

        Collection<BioSequence> bioSequencesToPersist = new ArrayList<BioSequence>();
        int count = 0;
        int cpt = 0;
        double secspt = 0.0;
        long millis = System.currentTimeMillis();

        for ( BioSequence sequence : results ) {
            sequence.getSequenceDatabaseEntry().setExternalDatabase( genbank );
            sequence.setTaxon( ( Taxon ) persisterHelper.persist( sequence.getTaxon() ) );

            bioSequencesToPersist.add( sequence );
            if ( ++count % BATCH_SIZE == 0 ) {
                bioSequenceService.create( bioSequencesToPersist );
                bioSequencesToPersist.clear();
            }

            // just some timing information.
            if ( count % 1000 == 0 ) {
                cpt++;
                double secsperthousand = ( System.currentTimeMillis() - millis ) / 1000.0;
                secspt += secsperthousand;
                double meanspt = secspt / cpt;

                String progString = "Processed and loaded " + count + " sequences, last one was " + sequence.getName()
                        + " (" + secsperthousand + " seconds elapsed, average per thousand=" + meanspt + ")";
                ProgressManager.updateCurrentThreadsProgressJob( new ProgressData( count, progString ) );
                log.info( progString );
                millis = System.currentTimeMillis();
            }
        }

        // finish up.
        bioSequenceService.create( bioSequencesToPersist );
        log.info( "Loaded total of " + count + " sequences" );
        return count;
    }

}
