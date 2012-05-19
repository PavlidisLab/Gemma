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
package ubic.gemma.model.expression.experiment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author kkeshav
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionExperimentServiceTest extends BaseSpringContextTest {

    @Autowired
    ExpressionExperimentService expressionExperimentService;

    private static final String EE_NAME = RandomStringUtils.randomAlphanumeric( 20 );
    ExpressionExperiment ee = null;
    ExternalDatabase ed;
    String accession;
    String contactName;
    boolean persisted = false;

    /**
     * @exception Exception
     */
    @Before
    public void setup() throws Exception {

        if ( !persisted ) {
            ee = this.getTestPersistentCompleteExpressionExperiment( false );
            ee.setName( EE_NAME );

            DatabaseEntry accessionEntry = this.getTestPersistentDatabaseEntry();
            accession = accessionEntry.getAccession();
            ed = accessionEntry.getExternalDatabase();
            ee.setAccession( accessionEntry );

            Contact c = this.getTestPersistentContact();
            ee.setOwner( c );

            expressionExperimentService.update( ee );
            ee = expressionExperimentService.thaw( ee );

            persisted = true;
        } else {
            log.debug( "Skipping making new ee for test" );
        }
    }

    @Test
    public final void testFindByAccession() throws Exception {
        DatabaseEntry accessionEntry = DatabaseEntry.Factory.newInstance( ed );
        accessionEntry.setAccession( accession );

        Collection<ExpressionExperiment> expressionExperiment = expressionExperimentService
                .findByAccession( accessionEntry );
        assertTrue( expressionExperiment.size() > 0 );
    }

    @Test
    public void testGetByTaxon() throws Exception {
        ExpressionExperimentService eeService = ( ExpressionExperimentService ) this
                .getBean( "expressionExperimentService" );

        Taxon taxon = taxonService.findByCommonName( "mouse" );
        Collection<ExpressionExperiment> list = expressionExperimentService.findByTaxon( taxon );
        assertNotNull( list );
        Taxon checkTaxon = eeService.getTaxon( list.iterator().next() );
        assertEquals( taxon, checkTaxon );

    }

    @Test
    public final void testGetDesignElementDataVectorsByQt() throws Exception {
        QuantitationType quantitationType = ee.getRawExpressionDataVectors().iterator().next().getQuantitationType();
        Collection<QuantitationType> quantitationTypes = new HashSet<QuantitationType>();
        quantitationTypes.add( quantitationType );
        Collection<DesignElementDataVector> vectors = expressionExperimentService
                .getDesignElementDataVectors( quantitationTypes );
        assertEquals( 12, vectors.size() );

    }

    @Test
    public final void testGetPerTaxonCount() throws Exception {
        Map<Taxon, Long> counts = expressionExperimentService.getPerTaxonCount();
        assertNotNull( counts );
    }

    @Test
    public final void testGetQuantitationTypes() throws Exception {
        Collection<QuantitationType> types = expressionExperimentService.getQuantitationTypes( ee );
        assertEquals( 2, types.size() );
    }

    @Test
    public final void testGetQuantitationTypesForArrayDesign() throws Exception {
        ArrayDesign ad = ee.getRawExpressionDataVectors().iterator().next().getDesignElement().getArrayDesign();
        Collection<QuantitationType> types = expressionExperimentService.getQuantitationTypes( ee, ad );
        assertEquals( 2, types.size() );
    }

    /**
     * @throws Exception
     */
    @Test
    public final void testgetRawExpressionDataVectors() throws Exception {
        ExpressionExperiment eel = this.getTestPersistentCompleteExpressionExperiment( false );
        Collection<CompositeSequence> designElements = new HashSet<CompositeSequence>();
        QuantitationType quantitationType = eel.getRawExpressionDataVectors().iterator().next().getQuantitationType();
        Collection<RawExpressionDataVector> allv = eel.getRawExpressionDataVectors();

        assertNotNull( quantitationType );

        assertTrue( allv.size() > 1 );

        for ( Iterator<RawExpressionDataVector> it = allv.iterator(); it.hasNext(); ) {
            CompositeSequence designElement = it.next().getDesignElement();
            assertNotNull( designElement );

            designElements.add( designElement );
            if ( designElements.size() == 2 ) break;
        }

        assertEquals( 2, designElements.size() );

        Collection<DesignElementDataVector> vectors = expressionExperimentService.getDesignElementDataVectors(
                designElements, quantitationType );

        assertEquals( 2, vectors.size() );

    }

    @Test
    public final void testLoadValueObjects() throws Exception {
        Collection<Long> ids = new HashSet<Long>();
        Long id = ee.getId();
        ids.add( id );
        Collection<ExpressionExperimentValueObject> list = expressionExperimentService.loadValueObjects( ids, false );
        assertNotNull( list );
        assertEquals( 1, list.size() );
    }

}
