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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.GeoService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author keshav
 * @version $Id$
 */
public class ExperimentalDesignWriterTest extends AbstractGeoServiceTest {

    private ExpressionExperiment ee = null;

    @Autowired
    private ExpressionExperimentService eeService = null;

    @Autowired
    private GeoService geoService;

    private String shortName = "GSE1611";

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.testing.BaseSpringContextTest#onSetUpInTransaction()
     */
    @Before
    public void setup() throws Exception {
        ee = eeService.findByShortName( shortName );

        if ( ee == null ) {
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal(
                    getTestFileBasePath( "gds994Medium" ) ) );
            Collection<?> results = geoService.fetchAndLoad( shortName, false, true, false, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        }
        ee = eeService.thaw( ee );
    }

    @After
    public void tearDown() throws Exception {
        if ( ee != null ) {
            this.eeService.delete( ee );
        }
    }

    /**
     * Tests writing out the experimental design
     */
    @Test
    public void testWrite() throws Exception {
        if ( ee == null ) {
            log.error( "Could not find experiment " + shortName + ".  Skipping test ..." );
            return;
        }

        ExperimentalDesignWriter edWriter = new ExperimentalDesignWriter();

        File f = File.createTempFile( "test_writer_" + shortName + ".", ".txt" );
        PrintWriter writer = new PrintWriter( f );

        edWriter.write( writer, ee, true, true );

        writer.flush();
        writer.close();

        log.info( f );
        FileReader fr = new FileReader( f );

        char[] b = new char[( int ) f.length()];
        fr.read( b );
        fr.close();

        String in = new String( b );

        assertTrue( in.contains( "PoolTs1Cje_P0_hyb1" ) );
        assertTrue( in.contains( "#$strain : Category=StrainOrLine Type=Categorical" ) );

    }
}
