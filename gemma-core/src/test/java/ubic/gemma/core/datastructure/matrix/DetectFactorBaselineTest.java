/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
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
package ubic.gemma.core.datastructure.matrix;

import org.junit.Test;
import ubic.gemma.core.analysis.expression.diff.BaselineSelection;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.Statement;

import static org.junit.Assert.assertTrue;

/**
 * @author paul
 */
public class DetectFactorBaselineTest {

    @Test
    public void testIsBaselineA() {

        FactorValue fv = FactorValue.Factory.newInstance();
        fv.setValue( "fv" );

        Statement c = Statement.Factory.newInstance();
        c.setValue( "control_group" );
        fv.getCharacteristics().add( c );

        boolean actual = BaselineSelection.isBaselineCondition( fv );
        assertTrue( actual );

    }

    @Test
    public void testIsBaselineB() {

        FactorValue fv = FactorValue.Factory.newInstance();
        fv.setValue( "fv" );

        Statement c = Statement.Factory.newInstance();
        c.setValueUri( "http://purl.org/nbirn/birnlex/ontology/BIRNLex-Investigation.owl#birnlex_2201" );
        fv.getCharacteristics().add( c );

        boolean actual = BaselineSelection.isBaselineCondition( fv );
        assertTrue( actual );

    }

    @Test
    public void testIsNotBaselineA() {

        FactorValue fv = FactorValue.Factory.newInstance();
        fv.setValue( "fv" );

        Statement c = Statement.Factory.newInstance();
        c.setValueUri( "http://purl.org/obo/owl/CHEBI#CHEBI_16236" );
        fv.getCharacteristics().add( c );

        boolean actual = BaselineSelection.isBaselineCondition( fv );
        assertTrue( !actual );

    }
}
