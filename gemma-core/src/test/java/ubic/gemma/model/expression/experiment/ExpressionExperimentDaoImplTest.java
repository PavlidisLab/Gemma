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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;

import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.ContactService;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author kkeshav
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionExperimentDaoImplTest extends BaseSpringContextTest {

    static ExpressionExperimentDao expressionExperimentDao;

    private static final String EE_NAME = RandomStringUtils.randomAlphanumeric( 20 );
    static ContactService cs = null;
    static ExpressionExperiment ee = null;
    static ExternalDatabase ed;
    static String accession;
    static String contactName;
    static BibliographicReference primary;
    static BibliographicReference other;
    static boolean persisted = false;

    /**
     * @exception Exception
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {

        super.onSetUpInTransaction();

        endTransaction();
        if ( !persisted ) {
            ee = this.getTestPersistentCompleteExpressionExperiment( false );
            ee.setName( EE_NAME );

            DatabaseEntry accessionEntry = this.getTestPersistentDatabaseEntry();
            accession = accessionEntry.getAccession();
            ed = accessionEntry.getExternalDatabase();
            ee.setAccession( accessionEntry );

            Contact c = this.getTestPersistentContact();
            contactName = c.getName();
            ee.setOwner( c );

            expressionExperimentDao.update( ee );
            expressionExperimentDao.thaw( ee );
            persisted = true;
        } else {
            log.debug( "Skipping making new ee for test" );
        }
    }

  

    public final void testFindByAccession() throws Exception {
        DatabaseEntry accessionEntry = DatabaseEntry.Factory.newInstance( ed );
        accessionEntry.setAccession( accession );

        ExpressionExperiment expressionExperiment = expressionExperimentDao.findByAccession( accessionEntry );
        assertNotNull( expressionExperiment );
    }

    @SuppressWarnings("unchecked")
    public final void testGetDesignElementDataVectors() throws Exception {
        Collection<DesignElement> designElements = new HashSet<DesignElement>();
        QuantitationType quantitationType = ee.getDesignElementDataVectors().iterator().next().getQuantitationType();
        Collection<DesignElementDataVector> allv = ee.getDesignElementDataVectors();
        Iterator<DesignElementDataVector> it = allv.iterator();
        for ( int i = 0; i < 2; i++ ) {
            designElements.add( it.next().getDesignElement() );
        }

        assert ( designElements.size() == 2 );

        Collection<DesignElementDataVector> vectors = expressionExperimentDao.getDesignElementDataVectors(
                designElements, quantitationType );

        assertEquals( 2, vectors.size() );

    }

    @SuppressWarnings("unchecked")
    public final void testGetDesignElementDataVectorsByQt() throws Exception {
        QuantitationType quantitationType = ee.getDesignElementDataVectors().iterator().next().getQuantitationType();
        Collection<QuantitationType> quantitationTypes = new HashSet<QuantitationType>();
        quantitationTypes.add( quantitationType );
        Collection<DesignElementDataVector> vectors = expressionExperimentDao
                .getDesignElementDataVectors( quantitationTypes );
        assertEquals( 12, vectors.size() );

    }

    @SuppressWarnings("unchecked")
    public final void testGetSamplingOfVectors() throws Exception {
        QuantitationType quantitationType = ee.getDesignElementDataVectors().iterator().next().getQuantitationType();
        Collection<DesignElementDataVector> vectors = expressionExperimentDao
                .getSamplingOfVectors( quantitationType, 2 );

        assertEquals( 2, vectors.size() );

    }

    @SuppressWarnings("unchecked")
    public final void testGetQuantitationTypes() throws Exception {
        Collection<QuantitationType> types = expressionExperimentDao.getQuantitationTypes( ee );
        assertEquals( 2, types.size() );
    }

    @SuppressWarnings("unchecked")
    public final void testGetQuantitationTypesForArrayDesign() throws Exception {
        ArrayDesign ad = ee.getDesignElementDataVectors().iterator().next().getDesignElement().getArrayDesign();
        Collection<QuantitationType> types = expressionExperimentDao.getQuantitationTypes( ee, ad );
        assertEquals( 2, types.size() );
    }

    @SuppressWarnings("unchecked")
    public final void testGetPerTaxonCount() throws Exception {
        Map<String, Long> counts = expressionExperimentDao.getPerTaxonCount();
        assertNotNull( counts );
    }

    public final void testLoadAllValueObjects() throws Exception {
        Collection list = expressionExperimentDao.loadAllValueObjects();
        assertNotNull( list );
    }

    /**
     * @param expressionExperimentDao the expressionExperimentDao to set
     */
    @SuppressWarnings("static-access")
    public void setExpressionExperimentDao( ExpressionExperimentDao expressionExperimentDao ) {
        this.expressionExperimentDao = expressionExperimentDao;
    }

    @SuppressWarnings("unchecked")
    public void testGetByTaxon() throws Exception {
        TaxonService taxonService = ( TaxonService ) this.getBean( "taxonService" );
        ExpressionExperimentService eeService = ( ExpressionExperimentService ) this
                .getBean( "expressionExperimentService" );

        Taxon taxon = taxonService.findByCommonName( "mouse" );
        Collection<ExpressionExperiment> list = expressionExperimentDao.findByTaxon( taxon );
        assertNotNull( list );
        Taxon checkTaxon = eeService.getTaxon( list.iterator().next().getId() );
        assertEquals( taxon, checkTaxon );

    }

}
