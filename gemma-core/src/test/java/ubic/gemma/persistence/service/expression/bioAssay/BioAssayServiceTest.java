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
package ubic.gemma.persistence.service.expression.bioAssay;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Collection;

import static org.junit.Assert.*;

/**
 * @author pavlidis
 */
public class BioAssayServiceTest extends BaseSpringContextTest {

    private BioAssay ba;

    @Autowired
    private BioAssayService bioAssayService;

    @Before
    public void setUp() throws Exception {

        ExpressionExperiment ee = super.getTestPersistentCompleteExpressionExperiment( false );

        ba = ee.getBioAssays().iterator().next();

    }

    @Test
    public void testFindByAccession() {
        assertEquals( 1, bioAssayService.findByAccession( ba.getAccession().getAccession() ).size() );
    }

    /**
     * Tests HQL
     */
    @Test
    public void testFindBioAssayDimensionsLong() {
        assertTrue( ba.getId() != null );
        Collection<BioAssayDimension> result = bioAssayService.findBioAssayDimensions( ba );
        assertEquals( 1, result.size() );
    }

    @Test
    public void testGetCount() {
        long count = bioAssayService.countAll();
        assertTrue( count > 0 );
    }

}
