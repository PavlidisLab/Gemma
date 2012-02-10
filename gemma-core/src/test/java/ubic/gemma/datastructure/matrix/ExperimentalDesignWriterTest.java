/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.datastructure.matrix;

import static org.junit.Assert.assertFalse;

import java.io.PrintWriter;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author keshav
 * @version $Id$
 */
public class ExperimentalDesignWriterTest extends BaseSpringContextTest {
    @Autowired
    ExpressionExperimentService eeService = null;

    ExpressionExperiment ee = null;

    String shortName = "GSE1997";

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.testing.BaseSpringContextTest#onSetUpInTransaction()
     */
    @Before
    public void setup() throws Exception {
        ee = eeService.findByShortName( shortName );
    }

    /**
     * Tests writing out the experimental design
     */
    @Test
    public void testWrite() {
        if ( ee == null ) {
            log.error( "Could not find experiment " + shortName + ".  Skipping test ..." );
            return;
        }

        boolean fail = false;
        try {
            ExperimentalDesignWriter edWriter = new ExperimentalDesignWriter();

            PrintWriter writer = new PrintWriter( "test_writer_" + ee.getShortName().replaceAll( "\\s", "" ) + ".txt" );

            edWriter.write( writer, ee, true, true );
        } catch ( Exception e ) {
            e.printStackTrace();
            fail = true;
        } finally {
            assertFalse( fail );
        }
    }
}
