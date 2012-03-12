/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
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

package ubic.gemma.model.expression.experiment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.expression.experiment.ExpressionExperimentSetValueObjectHelper;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentSetService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.security.authentication.UserManager;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Tests for methods that create ExpressionExperimentSetValueObjects from expressionExperiment entities
 * 
 * @author tvrossum
 * @version $Id$
 */
public class ExpressionExperimentSetValueObjectHelperTest extends BaseSpringContextTest {

    @Autowired
    ExpressionExperimentService expressionExperimentService;

    @Autowired
    ExpressionExperimentSetService expressionExperimentSetService;

    @Autowired
    UserManager userManager;

    @Autowired
    ExpressionExperimentSetValueObjectHelper expressionExperimentSetValueObjectHelper;

    private static final String EE_NAME = RandomStringUtils.randomAlphanumeric( 20 );
    private Taxon tax1;
    private ExpressionExperiment ee = null;
    private ExpressionExperimentSet eeSet = null;
    private BioAssay ba = null;
    private BioMaterial bm1 = null;
    private BioMaterial bm2 = null;
    private ArrayDesign ad = null;

    @Before
    public void setUp() throws Exception {

        tax1 = this.getTaxon( "human" );

        bm1 = this.getTestPersistentBioMaterial();
        bm2 = this.getTestPersistentBioMaterial();
        bm1.setSourceTaxon( tax1 );
        bm2.setSourceTaxon( tax1 );
        Collection<BioMaterial> bms = new ArrayList<BioMaterial>();
        bms.add( bm1 );
        bms.add( bm2 );

        ad = this.getTestPersistentArrayDesign( 4, true );
        ba = this.getTestPersistentBioAssay( ad );
        ba.setSamplesUsed( bms );
        Collection<BioAssay> bas = new ArrayList<BioAssay>();
        bas.add( ba );

        ee = this.getTestPersistentExpressionExperiment();
        ee.setName( EE_NAME );
        ee.setBioAssays( bas );

        // needs to be a set
        Collection<BioAssaySet> ees = new HashSet<BioAssaySet>();
        ees.add( ee );

        eeSet = ExpressionExperimentSet.Factory.newInstance();
        eeSet.setName( "CreateTest" );
        eeSet.setDescription( "CreateDesc" );
        eeSet.setExperiments( ees );

        eeSet = expressionExperimentSetService.create( eeSet );

    }

    @Test
    public void testConvertToValueObject() {

        Long id = eeSet.getId();
        assertNotNull( id );

        ExpressionExperimentSetValueObject eesvo = expressionExperimentSetValueObjectHelper
                .convertToValueObject( eeSet );

        assertEquals( eeSet.getId(), eesvo.getId() );
        assertEquals( eeSet.getExperiments().size(), eesvo.getNumExperiments().intValue() );
        assertEquals( eeSet.getName(), eesvo.getName() );
        assertEquals( eeSet.getDescription(), eesvo.getDescription() );
    }

    @Test
    public void testConvertToLightValueObject() {

        Long id = eeSet.getId();
        assertNotNull( id );

        ExpressionExperimentSetValueObject eesvo = expressionExperimentSetValueObjectHelper
                .convertToLightValueObject( eeSet );

        assertEquals( 0, eesvo.getExpressionExperimentIds().size() );

        assertEquals( eeSet.getId(), eesvo.getId() );
        assertEquals( eeSet.getExperiments().size(), eesvo.getNumExperiments().intValue() );
        assertEquals( eeSet.getName(), eesvo.getName() );
        assertEquals( eeSet.getDescription(), eesvo.getDescription() );

    }

}
