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

import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Tests the {@link AnalyzerHelper}.
 * 
 * @author keshav
 * @version $Id$
 */
public class AnalyzerHelperTest extends BaseSpringContextTest {

    ExperimentalFactor experimentalFactorA = null;

    ExperimentalFactor experimentalFactorB = null;

    Collection<BioMaterial> biomaterials = null;

    @Override
    public void onSetUpInTransaction() {

        /* experimental factor "area" */
        experimentalFactorA = ExperimentalFactor.Factory.newInstance();
        experimentalFactorA.setName( "area" );

        Collection<FactorValue> factorValuesA = new HashSet<FactorValue>();

        FactorValue factorValueA1 = FactorValue.Factory.newInstance();
        factorValueA1.setValue( "cerebellum" );
        factorValueA1.setExperimentalFactor( experimentalFactorA );

        FactorValue factorValueA2 = FactorValue.Factory.newInstance();
        factorValueA2.setValue( "amygdala" );
        factorValueA2.setExperimentalFactor( experimentalFactorA );

        factorValuesA.add( factorValueA1 );
        factorValuesA.add( factorValueA2 );

        experimentalFactorA.setFactorValues( factorValuesA );

        /* experimental factor "treat" */
        experimentalFactorB = ExperimentalFactor.Factory.newInstance();
        experimentalFactorB.setName( "treat" );

        Collection<FactorValue> factorValuesB = new HashSet<FactorValue>();

        FactorValue factorValueB1 = FactorValue.Factory.newInstance();
        factorValueB1.setValue( "no pcp" );
        factorValueB1.setExperimentalFactor( experimentalFactorB );

        FactorValue factorValueB2 = FactorValue.Factory.newInstance();
        factorValueB2.setValue( "pcp" );
        factorValueB2.setExperimentalFactor( experimentalFactorB );

        factorValuesB.add( factorValueB1 );
        factorValuesB.add( factorValueB2 );

        experimentalFactorB.setFactorValues( factorValuesB );

        /* set up the biomaterials */
        biomaterials = new HashSet<BioMaterial>();

        BioMaterial biomaterial0 = BioMaterial.Factory.newInstance();
        Collection<FactorValue> factorValuesForBioMaterial0 = new HashSet<FactorValue>();
        factorValuesForBioMaterial0.add( factorValueA1 );
        factorValuesForBioMaterial0.add( factorValueB1 );
        biomaterial0.setFactorValues( factorValuesForBioMaterial0 );

        BioMaterial biomaterial1 = BioMaterial.Factory.newInstance();
        Collection<FactorValue> factorValuesForBioMaterial1 = new HashSet<FactorValue>();
        factorValuesForBioMaterial1.add( factorValueA1 );
        factorValuesForBioMaterial1.add( factorValueB2 );
        biomaterial1.setFactorValues( factorValuesForBioMaterial1 );

        BioMaterial biomaterial2 = BioMaterial.Factory.newInstance();
        Collection<FactorValue> factorValuesForBioMaterial2 = new HashSet<FactorValue>();
        factorValuesForBioMaterial2.add( factorValueA2 );
        factorValuesForBioMaterial2.add( factorValueB1 );
        biomaterial2.setFactorValues( factorValuesForBioMaterial2 );

        BioMaterial biomaterial3 = BioMaterial.Factory.newInstance();
        Collection<FactorValue> factorValuesForBioMaterial3 = new HashSet<FactorValue>();
        factorValuesForBioMaterial3.add( factorValueA2 );
        factorValuesForBioMaterial3.add( factorValueB2 );
        biomaterial3.setFactorValues( factorValuesForBioMaterial3 );

        biomaterials.add( biomaterial0 );
        biomaterials.add( biomaterial1 );
        biomaterials.add( biomaterial2 );
        biomaterials.add( biomaterial3 );

    }

    /**
     * 
     *
     */
    public void testCheckBlockDesign() {

        Exception ex = null;
        try {
            AnalyzerHelper.checkBlockDesign( biomaterials, experimentalFactorA.getFactorValues() );

            AnalyzerHelper.checkBlockDesign( biomaterials, experimentalFactorB.getFactorValues() );
        } catch ( Exception e ) {
            ex = e;
            e.printStackTrace();
        } finally {
            assertNull( ex );
        }
    }

}
