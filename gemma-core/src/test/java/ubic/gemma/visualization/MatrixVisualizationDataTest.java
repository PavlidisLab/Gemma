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
package ubic.gemma.visualization;

import java.util.Collection;
import java.util.HashSet;

import junit.framework.TestCase;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author keshav
 * @version $Id$
 */
public class MatrixVisualizationDataTest extends TestCase {

    MatrixVisualizationData vizualizationData = null;

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void setUp() throws Exception {

        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();

        Collection<DesignElement> designElements = new HashSet();

        CompositeSequence cs0 = CompositeSequence.Factory.newInstance();
        cs0.setName( "0_at" );
        CompositeSequence cs1 = CompositeSequence.Factory.newInstance();
        cs0.setName( "1_at" );
        CompositeSequence cs2 = CompositeSequence.Factory.newInstance();
        cs0.setName( "2_at" );

        designElements.add( cs0 );
        designElements.add( cs1 );
        designElements.add( cs2 );

        vizualizationData = new MatrixVisualizationData( ee, designElements );
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {

    }

    /**
     * 
     *
     */
    public void testMatrixVisualizationData() {

    }

}
