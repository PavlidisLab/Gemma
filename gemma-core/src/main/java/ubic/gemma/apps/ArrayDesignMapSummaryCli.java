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

import ubic.gemma.analysis.sequence.ArrayDesignMapResultService;
import ubic.gemma.analysis.sequence.CompositeSequenceMapSummary;
import ubic.gemma.apps.GemmaCLI.CommandGroup;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;

import java.util.Collection;

/**
 * CLI for ArrayDesignMapSummaryService
 *
 * @author Paul
 */
public class ArrayDesignMapSummaryCli extends ArrayDesignSequenceManipulatingCli {

    public static void main( String[] args ) {
        ArrayDesignMapSummaryCli p = new ArrayDesignMapSummaryCli();
        tryDoWorkNoExit( p, args );
    }

    @Override
    public String getCommandName() {
        return "platformMapSummary";
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.ANALYSIS;
    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( args );
        if ( err != null )
            return err;
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

}
