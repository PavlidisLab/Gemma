/*
 * The gemma project
 *
 * Copyright (c) 2013 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.analysis.expression.coexpression.links;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

/**
 * Handles the persistence phase of a Link analysis.
 *
 * @author Paul
 */
public interface LinkAnalysisPersister {

    /**
     * Remove any links and coexpression analyses for the given experiment. It gets called
     * automatically by saveLinksToDb();
     *
     * @param ee the experiment
     * @return true if anything was deleted.
     */
    boolean deleteAnalyses( ExpressionExperiment ee );

    /**
     * Temporary method.
     *
     * @param t taxon
     */
    void initializeLinksFromOldData( Taxon t );

    /**
     * Persist links to the database. This takes care of saving a 'flipped' version of the links.
     *
     * @param la analysis
     */
    void saveLinksToDb( LinkAnalysis la );
}
