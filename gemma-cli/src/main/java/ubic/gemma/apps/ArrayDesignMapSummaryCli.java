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

import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.cli.util.CLI;
import ubic.gemma.core.analysis.sequence.ArrayDesignMapResultService;
import ubic.gemma.core.analysis.sequence.CompositeSequenceMapSummary;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;

import java.util.Collection;

/**
 * CLI for ArrayDesignMapSummaryService
 *
 * @author Paul
 */
public class ArrayDesignMapSummaryCli extends ArrayDesignSequenceManipulatingCli {

    @Autowired
    private ArrayDesignMapResultService arrayDesignMapResultService;

    @Override
    public String getCommandName() {
        return "platformMapSummary";
    }

    @Override
    protected void processArrayDesigns( Collection<ArrayDesign> arrayDesigns ) {
        Collection<ArrayDesign> ads = arrayDesignService.thaw( arrayDesigns );
        for ( ArrayDesign thawed : ads ) {
            Collection<CompositeSequenceMapSummary> results = arrayDesignMapResultService.summarizeMapResults( thawed );
            getCliContext().getOutputStream().println( CompositeSequenceMapSummary.HEADER );
            for ( CompositeSequenceMapSummary summary : results ) {
                getCliContext().getOutputStream().println( summary );
            }
        }
    }
}
