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
package ubic.gemma.loader.expression.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.loader.expression.simple.model.SimpleExpressionExperimentMetaData;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.ontology.providers.MgedOntologyService;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Test for import that results in multiple factor values for the same factor on a single biomaterial.
 * 
 * @author paul
 * @version $Id$
 */
public class ExperimentalDesignImportDuplicateValueTest extends BaseSpringContextTest {

    @Autowired
    private OntologyService os;

    private MgedOntologyService mos;

    private ExpressionExperiment ee;

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private SimpleExpressionDataLoaderService s;

    @Autowired
    private ExperimentalDesignImporter experimentalDesignImporter;

    @Before
    public void setup() throws Exception {
        InputStream data = this.getClass().getResourceAsStream(
                "/data/loader/expression/expdesign.import.testfull.data.txt" );

        SimpleExpressionExperimentMetaData metaData = new SimpleExpressionExperimentMetaData();

        mos = os.getMgedOntologyService();
        if ( !mos.isOntologyLoaded() ) {
            mos.startInitializationThread( true );
            while ( !mos.isOntologyLoaded() ) {
                Thread.sleep( 5000 );
                log.info( "Waiting for mgedontology to load" );
            }
        }

        Taxon human = taxonService.findByCommonName( "human" );

        metaData.setShortName( randomName() );
        metaData.setDescription( "bar" );
        metaData.setIsRatio( false );
        metaData.setTaxon( human );
        metaData.setQuantitationTypeName( "rma" );
        metaData.setScale( ScaleType.LOG2 );
        metaData.setType( StandardQuantitationType.AMOUNT );

        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setShortName( "gfoobly_" + randomName() );
        ad.setName( "foobly doo loo" );
        ad.setPrimaryTaxon( human );
        ad.setTechnologyType( TechnologyType.ONECOLOR );
        metaData.getArrayDesigns().add( ad );

        ee = s.create( metaData, data );
        ee = eeService.thawLite( ee );
    }

    /**
     * Note that this test will fail if you run it again on a dirty DB. Sorry!
     */
    @Test
    public final void testParse() throws Exception {

        InputStream is = this.getClass().getResourceAsStream( "/data/loader/expression/expdesign.import.testfull.txt" );

        experimentalDesignImporter.importDesign( ee, is, false );

        Collection<BioMaterial> bms = new HashSet<BioMaterial>();
        for ( BioAssay ba : ee.getBioAssays() ) {
            BioMaterial bm = ba.getSampleUsed();
            bms.add( bm );
        }

        checkResults( bms );
    }

    /**
     * @param bms
     */
    private void checkResults( Collection<BioMaterial> bms ) {

        assertEquals( 17, ee.getExperimentalDesign().getExperimentalFactors().size() );

        for ( BioMaterial bm : bms ) {
            Collection<ExperimentalFactor> seenExperimentalFactors = new HashSet<ExperimentalFactor>();
            for ( FactorValue fv : bm.getFactorValues() ) {

                if ( seenExperimentalFactors.contains( fv.getExperimentalFactor() ) ) {
                    for ( FactorValue ff : bm.getFactorValues() ) {
                        assertNotNull( ff.getId() );
                        if ( ff.getExperimentalFactor().equals( fv.getExperimentalFactor() ) ) {
                            log.info( bm + " : " + ff );
                        }
                    }

                    fail( fv.getExperimentalFactor() + " has more than one value for " + bm );
                }
                seenExperimentalFactors.add( fv.getExperimentalFactor() );
            }

        }
    }

}
