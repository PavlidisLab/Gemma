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
package ubic.gemma.core.datastructure.matrix.io;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.EntityUrlBuilder;

import java.io.StringWriter;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeNoException;
import static org.junit.Assume.assumeNotNull;

/**
 * @author keshav
 */
public class ExperimentalDesignWriterTest extends AbstractGeoServiceTest {

    @Autowired
    private ExpressionExperimentService eeService = null;
    @Autowired
    private GeoService geoService;
    @Autowired
    private EntityUrlBuilder entityUrlBuilder;
    @Autowired
    private BuildInfo buildInfo;

    private ExpressionExperiment ee = null;

    @Before
    public void setUp() throws Exception {
        try {
            geoService.setGeoDomainObjectGenerator(
                    new GeoDomainObjectGeneratorLocal( this.getTestFileBasePath( "gds994Medium" ) ) );
            Collection<?> results = geoService.fetchAndLoad( "GSE1611", false, true, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            ee = ( ( Collection<ExpressionExperiment> ) e.getData() ).iterator().next();
            assumeNoException( e );
        }
        ee = eeService.thaw( ee );
    }

    @After
    public void tearDown() {
        if ( ee != null ) {
            this.eeService.remove( ee );
        }
    }

    @Test
    @Category(SlowTest.class)
    public void testGSE1611() throws Exception {
        assumeNotNull( ee, "Could not find experiment GSE1611." );
        ExperimentalDesignWriter edWriter = new ExperimentalDesignWriter( entityUrlBuilder, buildInfo, false );
        StringWriter writer = new StringWriter();
        edWriter.write( ee, writer );
        assertThat( writer.toString() )
                .hasLineCount( 26 )
                .contains( "#$strain : Category=strain Type=Categorical\n" )
                .contains( "#$age : Category=age Type=Categorical\n" )
                .contains( "Bioassay\tExternalID\tage\tstrain\n" )
                .contains( "GSE1611_Biomat_1___Pool.Ts1Cje_P30_hyb1\tGSM27482\tP30\tTs1Cje\n" );
    }
}
