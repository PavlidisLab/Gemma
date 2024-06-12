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

package ubic.gemma.persistence.service.expression.experiment;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNull;

/**
 * @author kelsey
 */
public class ExpressionExperimentDeleteTest extends BaseSpringContextTest {

    @Autowired
    private ExpressionExperimentService svc;

    @Autowired
    private BioAssayService bioAssayService;

    @Test
    public final void testRemove() {
        ExpressionExperiment ee = getTestPersistentCompleteExpressionExperiment( false );
        List<Long> ids = new ArrayList<>();
        for ( BioAssay ba : ee.getBioAssays() ) {
            ids.add( ba.getId() );
        }

        svc.remove( ee );
        assertNull( svc.load( ee.getId() ) );

        // sure bioassays are gone.
        for ( Long id : ids ) {
            BioAssay ba = bioAssayService.load( id );
            assertNull( ba );
        }
    }

}
