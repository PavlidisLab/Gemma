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
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

/**
 * @author paul
 * @version $Id$
 */
public interface Gene2GenePopulationService {

    /**
     * @param expressionExperiments
     * @param toUseGenes
     * @param useDB
     */
    public void nodeDegreeAnalysis( Collection<ExpressionExperiment> expressionExperiments,
            Collection<Gene> toUseGenes, boolean useDB );

    /**
     * @param expressionExperiments
     * @param toUseGenes
     * @param stringency
     * @param analysisName
     * @param useDB
     */
    public void analyze( Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> toUseGenes,
            int stringency, String analysisName, boolean useDB );

    /**
     * @param expressionExperiments
     * @param taxon
     * @param toUseGenes
     * @param analysisName
     * @param stringency
     * @return
     */
    public GeneCoexpressionAnalysis intializeNewAnalysis( Collection<ExpressionExperiment> expressionExperiments,
            Taxon taxon, Collection<Gene> toUseGenes, String analysisName, int stringency );

    /**
     * @param taxon
     * @param toUseGenes
     * @param toUseStringency
     * @param analysisName
     * @param useDB
     */
    public void analyze( Taxon taxon, Collection<Gene> toUseGenes, int toUseStringency, String analysisName,
            boolean useDB );

}