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

import java.lang.reflect.Method;
import java.util.Collection;

import org.easymock.classextension.MockClassControl;

import ubic.gemma.analysis.service.AnalysisHelperService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Tests the {@link AnalyzerHelper}.
 * 
 * @author keshav
 * @version $Id$
 */
public class AnalyzerHelperTest extends BaseAnalyzerConfigurationTest {

    private AnalyzerHelper analyzerHelper = null;

    private AnalysisHelperService analysisHelperService = null;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.diff.BaseAnalyzerConfigurationTest#onSetUpInTransaction()
     */
    @Override
    public void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

        this.analyzerHelper = ( AnalyzerHelper ) this.getBean( "analyzerHelper" );

        // TODO replace with non-deprecated metods

        MockClassControl control = MockClassControl.createControl( AnalysisHelperService.class,
                new Method[] { AnalysisHelperService.class.getMethod( "getVectors", ExpressionExperiment.class ) } );

        analysisHelperService = ( AnalysisHelperService ) control.getMock();

        analysisHelperService.getVectors( expressionExperiment );

        Collection<DesignElementDataVector> vectorsToReturn = expressionExperiment.getDesignElementDataVectors();
        control.setReturnValue( vectorsToReturn );

        control.replay();

        analyzerHelper.setAnalysisHelperService( analysisHelperService );

    }

    /**
     * Tests the AnalyzerHelper.checkBiologicalReplicates method.
     * <p>
     * Expected result: null exception
     */
    public void testCheckBiologicalReplicates() {

        Exception ex = null;
        try {
            analyzerHelper.checkBiologicalReplicates( expressionExperiment );
        } catch ( Exception e ) {
            ex = e;
            e.printStackTrace();
        } finally {
            assertNull( ex );
        }
    }

    /**
     * Tests the AnalyzerHelper.checkBlockDesign method.
     * <p>
     * Expected result: null exception
     */
    public void testCheckBlockDesign() {
        Exception ex = null;
        try {
            analyzerHelper.checkBlockDesign( expressionExperiment );
        } catch ( Exception e ) {
            ex = e;
            e.printStackTrace();
        } finally {
            assertNull( ex );
        }
    }

}
