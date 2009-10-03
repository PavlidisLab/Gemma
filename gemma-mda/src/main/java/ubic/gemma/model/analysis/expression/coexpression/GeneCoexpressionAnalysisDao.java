/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.model.analysis.expression.coexpression;

/**
 * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis
 */
public interface GeneCoexpressionAnalysisDao extends
        ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysisDao<GeneCoexpressionAnalysis> {
    /**
     * <p>
     * This is needed to allow security filtering of the EE collection
     * </p>
     */
    public java.util.Collection getDatasetsAnalyzed(
            ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis analysis );

    /**
     * <p>
     * Get the number of data sets analyzed .
     * </p>
     */
    public int getNumDatasetsAnalyzed(
            ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis analysis );

    /**
     * 
     */
    public void thaw(
            ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis geneCoexpressionAnalysis );

}
