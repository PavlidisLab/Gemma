/*
 * The gemma-core project
 * 
 * Copyright (c) 2013 University of British Columbia
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

package ubic.gemma.apps;

import java.util.Collection;

import org.apache.commons.lang.math.RandomUtils;

import ubic.gemma.annotation.reference.BibliographicReferenceService;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.util.AbstractCLIContextCLI;

/**
 * Refreshes the information in all the bibliographic references in the system.
 * 
 * @author Paul
 * @version $Id$
 */
public class BibRefUpdaterCli extends AbstractCLIContextCLI {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @Override
    protected void buildOptions() {
        super.addUserNameAndPasswordOptions( true );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        Exception ex = super.processCommandLine( "", args );
        if ( ex != null ) return ex;
        BibliographicReferenceService bibliographicReferenceService = this
                .getBean( BibliographicReferenceService.class );

        Collection<Long> bibrefIds = bibliographicReferenceService.listAll();
        log.info( "There are " + bibrefIds.size() + " to update" );
        for ( Long id : bibrefIds ) {
            BibliographicReference bibref = bibliographicReferenceService.load( id );
            bibref = bibliographicReferenceService.thaw( bibref );
            log.info( bibref );
            try {
                bibliographicReferenceService.refresh( bibref.getPubAccession().getAccession() );
            } catch ( Exception e ) {
                log.info( "Failed to upate: " + bibref + " (" + e.getMessage() + ")" );
            }
            try {
                Thread.sleep( RandomUtils.nextInt( 1000 ) );
            } catch ( InterruptedException e ) {
                return e;
            }
        }

        return null;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        BibRefUpdaterCli e = new BibRefUpdaterCli();
        Exception ex = e.doWork( args );
        if ( ex != null ) {
            log.fatal( ex, ex );
        }

    }

}
