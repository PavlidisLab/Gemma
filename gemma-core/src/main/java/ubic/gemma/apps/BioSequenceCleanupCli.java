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
package ubic.gemma.apps;

import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Goes through the biosequences in the database and removes duplicates. This should be much faster than doing
 * findOrCreate at loadtime.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class BioSequenceCleanupCli extends AbstractSpringAwareCLI {

    @Override
    protected void buildOptions() {
        // TODO Auto-generated method stub

    }

    public static void main( String[] args ) {
        BioSequenceCleanupCli p = new BioSequenceCleanupCli();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    protected Exception doWork( String[] args ) {

        Exception err = processCommandLine( "Sequence cleanup", args );
        if ( err != null ) return err;

        // Strategy: do a 'find' on all the biosequences in the db (one at a time). If two are retrieved the odd one
        // will be deleted.

        BioSequenceService bss = ( BioSequenceService ) this.getBean( "bioSequenceService" );

        BioSequence template = BioSequence.Factory.newInstance();

        // maybe a better way to do this?
        Integer total = bss.countAll();
        log.info( "Have " + total + " to process." );
        for ( int i = 1; i <= total; i++ ) {

            BioSequence bs = bss.load( i );
            if ( bs != null ) {
                template.setName( bs.getName() );
                template.setTaxon( bs.getTaxon() );
                bss.find( template ); // this will clobber the extras.
            }

            if ( ++i % 2000 == 0 ) {
                log.info( "Processed " + i );
            }

        }

        return null;

    }

}
