/*
 * The Gemma project
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
package ubic.gemma.persistence.service.analysis.expression.diff;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysis;

import java.util.List;
import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Paul
 */
public class GeneDiffExMetaAnalysisServiceTest extends BaseSpringContextTest {

    @Autowired
    private GeneDiffExMetaAnalysisService geneDiffExMetaAnalysisService;

    @Test
    public void test() {

        /*
         * create the analysis object
         */
        GeneDifferentialExpressionMetaAnalysis metaAnalysis = GeneDifferentialExpressionMetaAnalysis.Factory
                .newInstance();

        metaAnalysis.setNumGenesAnalyzed( 100 );
        metaAnalysis.setQvalueThresholdForStorage( 0.05 );
        geneDiffExMetaAnalysisService.create( metaAnalysis );

        List<Long> ids = new Vector<>();
        ids.add( metaAnalysis.getId() );

        assertTrue( geneDiffExMetaAnalysisService.loadAll().size() > 0 );

        assertEquals( 1, geneDiffExMetaAnalysisService.findMetaAnalyses( ids ).size() );

    }

}
