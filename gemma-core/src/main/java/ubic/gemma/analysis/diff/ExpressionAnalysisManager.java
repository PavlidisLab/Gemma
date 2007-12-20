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
package ubic.gemma.analysis.diff;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.analysis.Analysis;
import ubic.gemma.model.analysis.AnalysisResult;
import ubic.gemma.model.analysis.AnalysisService;
import ubic.gemma.model.expression.analysis.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.expression.analysis.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.analysis.GeneAnalysisResult;

/**
 * This class contains the methods needed for different analyses.
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="expressionAnalysisManager"
 * @spring.property name="analysisService" ref="analysisService"
 */
public class ExpressionAnalysisManager {

    // TODO consolidate with methods from ExpressionDataManager. Move methods from there to a
    // SimpleExpressionAnalysisManager for reading in files with p-values and probe/gene names.

    protected static final Log log = LogFactory.getLog( ExpressionAnalysisManager.class );

    private AnalysisService analysisService = null;

    /**
     * @param analysisName
     * @return {@link Collection} Results of an analysis
     */
    private Collection<DifferentialExpressionAnalysisResult> getAnalysisResults( String analysisName ) {

        Analysis analysis = analysisService.findByName( analysisName );

        Collection<ExpressionAnalysisResultSet> resultSets = analysis.getResultSets();

        // FIXME manager should work with result sets now
        ExpressionAnalysisResultSet resultSet = resultSets.iterator().next();

        return resultSet.getResults();
    }

    /**
     * @param analysisName
     * @return
     */
    public Collection<GeneAnalysisResult> getGeneAnalysisResults( String analysisName ) {

        Collection<DifferentialExpressionAnalysisResult> results = getAnalysisResults( analysisName );

        Collection<GeneAnalysisResult> geneAnalysisResults = new HashSet<GeneAnalysisResult>();

        for ( AnalysisResult result : results ) {
            if ( result instanceof GeneAnalysisResult ) {
                GeneAnalysisResult geneAnalysisResult = ( GeneAnalysisResult ) result;
                geneAnalysisResults.add( geneAnalysisResult );
            } else {
                log.warn( "Analysis results are not gene analysis results." );
                break;
            }
        }
        return geneAnalysisResults;
    }

    /**
     * @param analysisName
     * @param threshold
     * @return Collection
     */
    @SuppressWarnings("unchecked")
    public Collection<GeneAnalysisResult> getSignificantGenes( String analysisName, double threshold ) {

        Collection<GeneAnalysisResult> results = getGeneAnalysisResults( analysisName );

        if ( results == null || results.isEmpty() ) return results;

        for ( GeneAnalysisResult result : results ) {
            if ( result.getPvalue() >= threshold ) {
                results.remove( result );
            }
        }

        List resultsAsList = Arrays.asList( results.toArray() );
        // FIXME use a GeneAnalysisResultValueObject that implements Comparator and compare by rank
        Collections.sort( resultsAsList );

        return resultsAsList;
    }

    /**
     * @param analysisService the analysisService to set
     */
    public void setAnalysisService( AnalysisService analysisService ) {
        this.analysisService = analysisService;
    }

}
