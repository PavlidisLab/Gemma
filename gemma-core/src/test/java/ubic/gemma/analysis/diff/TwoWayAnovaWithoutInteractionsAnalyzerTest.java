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
package ubic.gemma.analysis.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * Tests the two way anova analyzer.
 * 
 * @author keshav
 * @version $Id$
 */
public class TwoWayAnovaWithoutInteractionsAnalyzerTest extends AbstractAnalyzerTest {

    private Log log = LogFactory.getLog( this.getClass() );

    TwoWayAnovaWithoutInteractionsAnalyzer analyzer = new TwoWayAnovaWithoutInteractionsAnalyzer();

    /**
     * Tests the TwoWayAnova method.
     */
    public void testTwoWayAnova() {

        Iterator iter = this.efs.iterator();
        ExperimentalFactor experimentalFactorA = ( ExperimentalFactor ) iter.next();
        ExperimentalFactor experimentalFactorB = ( ExperimentalFactor ) iter.next();

        List<BioMaterial> alteredBiomaterials = new ArrayList<BioMaterial>();

        /* is just one fv per biomaterial in the test experiment so we'll add another for the two way anova */
        for ( BioMaterial m : biomaterials ) {

            Collection<FactorValue> factorValuesFromBioMaterial = m.getFactorValues();

            List<FactorValue> alteredFactorValues = new ArrayList<FactorValue>();

            for ( FactorValue fv : factorValuesFromBioMaterial ) {

                alteredFactorValues.add( fv );

                Collection<FactorValue> fvs = null;

                if ( fv.getExperimentalFactor() == experimentalFactorA ) {
                    fvs = experimentalFactorB.getFactorValues();
                } else {
                    fvs = experimentalFactorA.getFactorValues();
                }
                FactorValue anotherFactorValue = fvs.iterator().next();
                alteredFactorValues.add( anotherFactorValue );
                break;
            }

            m.setFactorValues( alteredFactorValues );

            alteredBiomaterials.add( m );
        }

        analyzer.twoWayAnova( this.matrix, experimentalFactorA, experimentalFactorB, alteredBiomaterials );

        // analyzer.twoWayAnova( this.matrix, experimentalFactorA, experimentalFactorB, biomaterials );
    }

}
