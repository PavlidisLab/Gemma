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
package ubic.gemma.core.apps;

import ubic.gemma.core.analysis.sequence.ArrayDesignMapResultService;
import ubic.gemma.core.analysis.sequence.CompositeSequenceMapSummary;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * CLI for ArrayDesignMapSummaryService
 *
 * @author Paul
 */
public class ArrayDesignMapSummaryCli extends ArrayDesignSequenceManipulatingCli {

    @Override
    public String getCommandName() {
        return "platformMapSummary";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return null;
    }

    @Override
    protected void doWork() throws Exception {
        ArrayDesignMapResultService arrayDesignMapResultService = this.getBean( ArrayDesignMapResultService.class );

        Collection<ArrayDesign> ads = getArrayDesignService().thaw( getArrayDesignsToProcess() );
        for ( ArrayDesign thawed : ads ) {
            Collection<CompositeSequenceMapSummary> results = arrayDesignMapResultService.summarizeMapResults( thawed );
            System.out.println( CompositeSequenceMapSummary.HEADER );
            for ( CompositeSequenceMapSummary summary : results ) {
                System.out.println( summary );
            }
        }
    }

    @Override
    public GemmaCLI.CommandGroup getCommandGroup() {
        return GemmaCLI.CommandGroup.ANALYSIS;
    }

}
