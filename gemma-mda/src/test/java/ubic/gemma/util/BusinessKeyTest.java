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
package ubic.gemma.util;

import org.apache.commons.lang.RandomStringUtils;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueDao;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author paul
 * @version $Id$
 */
public class BusinessKeyTest extends BaseSpringContextTest {

    public void testCreateQueryObjectCriteriaFactorValue() throws Exception {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setShortName( "foo" + RandomStringUtils.randomAlphanumeric( 10 ) );
        ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance();
        ef.setName( "oogly" + RandomStringUtils.randomAlphanumeric( 10 ) );
        FactorValue fv = FactorValue.Factory.newInstance();
        Characteristic c1 = Characteristic.Factory.newInstance();
        c1.setValue( "foo" );
        c1.setCategory( "bar" );
        fv.getCharacteristics().add( c1 );

        Characteristic c2 = Characteristic.Factory.newInstance();
        c2.setValue( "fool" );
        c2.setCategory( "barf" );
        fv.getCharacteristics().add( c2 );

        ef.getFactorValues().add( fv );

        ee.setExperimentalDesign( ExperimentalDesign.Factory.newInstance() );

        ee.getExperimentalDesign().getExperimentalFactors().add( ef );

        ee = ( ExpressionExperiment ) this.persisterHelper.persist( ee );

        ef = ee.getExperimentalDesign().getExperimentalFactors().iterator().next();

        FactorValueDao fvd = ( FactorValueDao ) this.getBean( "factorValueDao" );

        // Test that find basically works.
        FactorValue fvf = FactorValue.Factory.newInstance();
        Characteristic cf1 = Characteristic.Factory.newInstance();
        cf1.setValue( "foo" );
        cf1.setCategory( "bar" );
        fvf.getCharacteristics().add( cf1 );
        fvf.setExperimentalFactor( ef );
        FactorValue found = fvd.find( fvf );

        // should not find it, because the one in the db should have two characteristics.
        if ( found != null ) {
            fail( "Should not have found" );
        }

        Characteristic cf2 = Characteristic.Factory.newInstance();
        cf2.setValue( "fool" );
        cf2.setCategory( "barf" );
        fvf.getCharacteristics().add( cf2 );
        found = fvd.find( fvf );
        assertNotNull( "Should have found", found );

        // Test that the EF must not be null.
        fvf.setExperimentalFactor( null );
        try {
            fvd.find( fvf );
            fail( "Should have an exception here" );
        } catch ( IllegalArgumentException e ) {
            //
        }

    }

}
