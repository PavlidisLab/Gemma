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
package ubic.gemma.core.loader.association;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import ubic.basecode.util.FileTools;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.persistence.persister.Persister;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.*;

/**
 * @author keshav
 * @author pavlidis
 */
public class NCBIGene2GOAssociationLoader {

    private static final Log log = LogFactory.getLog( NCBIGene2GOAssociationLoader.class );
    private static final int QUEUE_SIZE = 60000;
    private static final int BATCH_SIZE = 12000;
    private final Persister persisterHelper;
    private final NCBIGene2GOAssociationParser parser;

    // two pools are needed here because having two consumers in a 2-threads pool would result in starvation
    // also this nicely ensures that no two consumer can run simultaneously
    private final ExecutorService producerExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService consumerExecutor = Executors.newSingleThreadExecutor();

    public NCBIGene2GOAssociationLoader( Persister persisterHelper, NCBIGene2GOAssociationParser parser ) {
        this.persisterHelper = persisterHelper;
        this.parser = parser;
    }

    /**
     * Load from a stream.
     *
     * @param inputStream
     * @return
     */
    public synchronized Future<Long> loadAsync( final InputStream inputStream ) {
        final BlockingQueue<Gene2GOAssociation> queue = new ArrayBlockingQueue<>(
                NCBIGene2GOAssociationLoader.QUEUE_SIZE );
        final SecurityContext context = SecurityContextHolder.getContext();
        final Authentication authentication = context.getAuthentication();

        final Future<Integer> producerFuture = producerExecutor.submit( () -> {
            SecurityContextHolder.getContext().setAuthentication( authentication );
            try {
                // potential race condition between the parsing and the counting of results
                synchronized ( parser ) {
                    parser.parse( inputStream, queue );
                    return parser.getCount();
                }
            } catch ( IOException e ) {
                NCBIGene2GOAssociationLoader.log.error( "Error while parsing NCBI gene2go associations.", e );
                throw new RuntimeException( e );
            } finally {
                NCBIGene2GOAssociationLoader.log.info( "Done parsing" );
            }
        } );

        return consumerExecutor.submit( () -> {
            long count = 0;
            NCBIGene2GOAssociationLoader.log.info( "Starting loading" );
            SecurityContextHolder.setContext( context );

            NCBIGene2GOAssociationLoader.log.debug( "Entering 'load' " );

            long millis = System.currentTimeMillis();
            int cpt = 0;
            double secspt = 0.0;

            Collection<Gene2GOAssociation> itemsToPersist = new ArrayList<>();
            try {
                while ( !( producerFuture.isDone() && queue.isEmpty() ) ) {
                    Gene2GOAssociation associations = queue.poll();

                    if ( associations == null ) {
                        continue;
                    }

                    itemsToPersist.add( associations );
                    if ( ++count % NCBIGene2GOAssociationLoader.BATCH_SIZE == 0 ) {
                        persisterHelper.persist( itemsToPersist );
                        itemsToPersist.clear();
                    }

                    // just some timing information.
                    if ( count % 10000 == 0 ) {
                        cpt++;
                        double secsperthousand = ( System.currentTimeMillis() - millis ) / 1000.0;
                        secspt += secsperthousand;
                        double meanspt = secspt / cpt;

                        String progString = "Processed and loaded " + count + " (" + secsperthousand
                                + " seconds elapsed, average per thousand=" + String.format( "%.2f", meanspt ) + ")";
                        NCBIGene2GOAssociationLoader.log.info( progString );
                        millis = System.currentTimeMillis();
                    }

                }
            } catch ( Exception e ) {
                NCBIGene2GOAssociationLoader.log.fatal( e, e );
                throw new RuntimeException( e );
            }

            // finish up.
            persisterHelper.persist( itemsToPersist );

            NCBIGene2GOAssociationLoader.log.info( "Finished, loaded total of " + count + " GO associations" );

            return count;
        } );
    }

    public Future<Long> loadAsync( LocalFile ncbiFile ) {
        try ( InputStream inputStream = FileTools
                .getInputStreamFromPlainOrCompressedFile( ncbiFile.asFile().getAbsolutePath() ) ) {
            return this.loadAsync( inputStream );
        } catch ( IOException e ) {
            NCBIGene2GOAssociationLoader.log.error( e, e );
            throw new RuntimeException( e );
        }
    }
}
