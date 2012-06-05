/*
 * The Gemma project
 * 
 * Copyright (c) 2006 Columbia University
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

import ubic.gemma.analysis.sequence.ArrayDesignMapResultService;
import ubic.gemma.analysis.sequence.CompositeSequenceMapSummary;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;

/**
 * CLI for ArrayDesignMapSummaryService
 * 
 * @author Paul
 * @version $Id$
 */
public class ArrayDesignMapSummaryCli extends ArrayDesignSequenceManipulatingCli {

    public static void main( String[] args ) {
        ArrayDesignMapSummaryCli p = new ArrayDesignMapSummaryCli();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "Array design mapping summary", args );
        if ( err != null ) return err;
        ArrayDesignMapResultService arrayDesignMapResultService = this.getBean( ArrayDesignMapResultService.class );

        for ( ArrayDesign arrayDesign : this.arrayDesignsToProcess ) {

            ArrayDesign thawed = unlazifyArrayDesign( arrayDesign );

            Collection<CompositeSequenceMapSummary> results = arrayDesignMapResultService.summarizeMapResults( thawed );

            System.out.println( CompositeSequenceMapSummary.header() );
            for ( CompositeSequenceMapSummary summary : results ) {
                System.out.println( summary );
            }
        }
        return null;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        // FIXME: add HTML output option.
    }

}
