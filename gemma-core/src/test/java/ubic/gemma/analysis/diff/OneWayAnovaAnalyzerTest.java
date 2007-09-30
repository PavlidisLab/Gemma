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
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * Tests the one way anova analyzer.
 * 
 * @author keshav
 * @version $Id$
 */
public class OneWayAnovaAnalyzerTest extends BaseAnalyzerTest {
    private Log log = LogFactory.getLog( this.getClass() );

    OneWayAnovaAnalyzer analyzer = new OneWayAnovaAnalyzer();

    /**
     * Tests the OneWayAnova method.
     */
    public void testOneWayAnova() {

        List<BioMaterial> alteredBiomaterials = new ArrayList<BioMaterial>();

        /*
         * Need to make sure all the biomaterials have factor values from the same experimental factor for one way
         * anova. Also, make sure we only have one factor value per biomaterial.
         */
        for ( BioMaterial m : biomaterials ) {
            log.debug( "Biomaterial: " + m.getName() );

            Collection<FactorValue> factorValuesFromBioMaterial = m.getFactorValues();

            List<FactorValue> alteredFactorValuesFromBioMaterial = new ArrayList<FactorValue>();

            FactorValue f = factorValuesFromBioMaterial.iterator().next();
            log.debug( "Experimental factor from factor value: " + f.getExperimentalFactor() );

            if ( f.getExperimentalFactor() != ef ) {
                f.setExperimentalFactor( ef );
            }
            alteredFactorValuesFromBioMaterial.add( f );

            m.setFactorValues( alteredFactorValuesFromBioMaterial );

            alteredBiomaterials.add( m );
        }

        Map pvaluesMap = analyzer.oneWayAnova( matrix, ef.getFactorValues(), alteredBiomaterials );

        log.info( pvaluesMap );

        assertEquals( pvaluesMap.size(), 6 );

    }

}
