/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.core.analysis.expression.diff;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseSpringContextTest5;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Which existing analyses a newly persisted one replaces.
 * <p>
 * Paul's ruling, 2026-09-13: a non-subset analysis is replaced by a non-subset run that covers every one of its
 * factors, not only by a run on the identical factor set. GSE107259 kept 264375 on {@code genotype} beside 432412 on
 * {@code genotype} + {@code treatment}.
 *
 * @author gembro
 */
public class DifferentialExpressionAnalysisReplacementTest extends BaseSpringContextTest5 {

    @Autowired
    private DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    private ExpressionExperiment ee;
    private ExperimentalFactor genotype;
    private ExperimentalFactor treatment;

    @BeforeEach
    public void setUp() {
        ee = testHelper.getTestExpressionExperimentWithAllDependencies( false );
        Iterator<ExperimentalFactor> factors = ee.getExperimentalDesign().getExperimentalFactors().iterator();
        genotype = factors.next();
        treatment = factors.next();
    }

    @Test
    public void testARunOnMoreFactorsReplacesTheNarrowerAnalysis() {
        DifferentialExpressionAnalysis narrower = persist( Collections.singletonList( genotype ) );
        DifferentialExpressionAnalysis wider = persist( Arrays.asList( genotype, treatment ) );

        assertThat( differentialExpressionAnalysisService.load( narrower.getId() ) ).isNull();
        assertThat( differentialExpressionAnalysisService.load( wider.getId() ) ).isNotNull();
    }

    /** The rule runs one way: a narrower run leaves a wider analysis in place, since it does not cover it. */
    @Test
    public void testARunOnFewerFactorsKeepsTheWiderAnalysis() {
        DifferentialExpressionAnalysis wider = persist( Arrays.asList( genotype, treatment ) );
        DifferentialExpressionAnalysis narrower = persist( Collections.singletonList( genotype ) );

        assertThat( differentialExpressionAnalysisService.load( wider.getId() ) ).isNotNull();
        assertThat( differentialExpressionAnalysisService.load( narrower.getId() ) ).isNotNull();
    }

    /** The identical factor set is still replaced, as it always was. */
    @Test
    public void testARunOnTheSameFactorsReplacesTheAnalysis() {
        DifferentialExpressionAnalysis first = persist( Collections.singletonList( treatment ) );
        DifferentialExpressionAnalysis second = persist( Collections.singletonList( treatment ) );

        assertThat( differentialExpressionAnalysisService.load( first.getId() ) ).isNull();
        assertThat( differentialExpressionAnalysisService.load( second.getId() ) ).isNotNull();
    }

    private DifferentialExpressionAnalysis persist( Collection<ExperimentalFactor> factors ) {
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.addFactorsToInclude( factors );
        DifferentialExpressionAnalysis analysis = DifferentialExpressionAnalysis.Factory.newInstance();
        analysis.setProtocol( DiffExAnalyzerUtils.createProtocolForConfig( config, Collections.emptyMap() ) );
        ExpressionAnalysisResultSet resultSet = ExpressionAnalysisResultSet.Factory.newInstance();
        resultSet.setAnalysis( analysis );
        resultSet.getExperimentalFactors().addAll( factors );
        analysis.getResultSets().add( resultSet );
        analysis.setExperimentAnalyzed( ee );
        return differentialExpressionAnalyzerService.persistAnalysis( ee, analysis, config );
    }
}
