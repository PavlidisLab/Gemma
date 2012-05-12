/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
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
package ubic.gemma.analysis.expression.coexpression;

import java.util.Collection;

import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

/**
 * @author paul
 * @version $Id$
 */
public interface Gene2GenePopulationService {

    public void nodeDegreeAnalysis( Collection<BioAssaySet> expressionExperiments, Collection<Gene> toUseGenes,
            boolean useDB );

    public void analyze( Collection<BioAssaySet> expressionExperiments, Collection<Gene> toUseGenes, int stringency,
            String analysisName, boolean useDB );

    /*
     * Defined here so that we can make it transactional.
     */
    public GeneCoexpressionAnalysis intializeNewAnalysis( Collection<BioAssaySet> expressionExperiments, Taxon taxon,
            Collection<Gene> toUseGenes, String analysisName, int stringency );

}