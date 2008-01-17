/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.web.controller.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

import ubic.gemma.analysis.diff.DifferentialExpressionAnalyzerService;
import ubic.gemma.model.analysis.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;

/**
 * @author keshav
 * @version $Id$ *
 * @spring.bean id="diffExpressionSearchController"
 * @spring.property name = "commandName" value="diffExpressionSearchCommand"
 * @spring.property name = "commandClass" value="ubic.gemma.web.controller.diff.DiffExpressionSearchCommand"
 * @spring.property name = "formView" value="searchDiffExpression"
 * @spring.property name = "successView" value="searchDiffExpression"
 * @spring.property name = "differentialExpressionAnalyzerService" ref="differentialExpressionAnalyzerService"
 * @spring.property name = "geneService" ref="geneService"
 */
public class DiffExpressionSearchController extends SimpleFormController {

    private DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService = null;

    private GeneService geneService = null;

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
     */
    @SuppressWarnings("unchecked")
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        DiffExpressionSearchCommand diffCommand = ( ( DiffExpressionSearchCommand ) command );

        String officialSymbol = diffCommand.getSearchString();

        /* multiple genes can have the same symbol */
        Collection<Gene> genes = geneService.findByOfficialSymbol( officialSymbol );

        ModelAndView mav = new ModelAndView();
        for ( Gene g : genes ) {
            Collection<DifferentialExpressionAnalysis> analyses = differentialExpressionAnalyzerService
                    .getDiffAnalysesForGene( g );

            for ( DifferentialExpressionAnalysis d : analyses ) {
                Collection<ExpressionExperiment> experimentsAnalyzed = d.getExperimentsAnalyzed();

                mav.addObject( g.getOfficialSymbol(), experimentsAnalyzed );
            }
        }

        return mav;
    }

    /**
     * @param differentialExpressionAnalyzerService
     */
    public void setDifferentialExpressionAnalyzerService(
            DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService ) {
        this.differentialExpressionAnalyzerService = differentialExpressionAnalyzerService;
    }

    /**
     * @param geneService
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }
}
