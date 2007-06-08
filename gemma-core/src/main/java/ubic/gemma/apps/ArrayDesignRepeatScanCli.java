/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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

import ubic.gemma.analysis.sequence.RepeatScan;
import ubic.gemma.loader.expression.arrayDesign.ArrayDesignSequenceAlignmentService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.BioSequenceService;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignRepeatScanCli extends ArrayDesignSequenceManipulatingCli {

    /**
     * @param args
     */
    public static void main( String[] args ) {
        ArrayDesignRepeatScanCli p = new ArrayDesignRepeatScanCli();
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
        Exception exception = processCommandLine( "repeatScan", args );
        if ( exception != null ) return exception;

        BioSequenceService bsService = ( BioSequenceService ) this.getBean( "bioSequenceService" );

        ArrayDesign design = this.locateArrayDesign( arrayDesignName );
        unlazifyArrayDesign( design );

        Collection<BioSequence> sequences = ArrayDesignSequenceAlignmentService.getSequences( design );

        RepeatScan scanner = new RepeatScan();

        scanner.repeatScan( sequences );

        for ( BioSequence sequence : sequences ) {
            log.info( sequence.getFractionRepeats() + " " + sequence.getSequence() );
        }

        // bsService.update( sequences );

        return null;
    }

}
