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
import java.util.LinkedHashSet;
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

    ExpressionExperimentDao expressionExperimentDao;

    /**
     * 
     */
    private static final String EE_NAME = RandomStringUtils.randomAlphanumeric( 20 );
    ContactService cs = null;
    ExpressionExperiment ee = null;
    ExternalDatabase ed;
    String accession;
    String contactName;
    BibliographicReference primary;
    BibliographicReference other;

    /**
     * @exception Exception
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {

        super.onSetUpInTransaction();

        endTransaction();
        ee = this.getTestPersistentCompleteExpressionExperiment();
        ee.setName( EE_NAME );

        DatabaseEntry accessionEntry = this.getTestPersistentDatabaseEntry();
        accession = accessionEntry.getAccession();
        ed = accessionEntry.getExternalDatabase();
        ee.setAccession( accessionEntry );

        Contact c = this.getTestPersistentContact();
        this.contactName = c.getName();
        ee.setOwner( c );

        // Creating the bibReference association doesn't work like this
        // BibliographicReferenceService bibRefService = ( BibliographicReferenceService ) this
        // .getBean( "bibliographicReferenceService" );
        // primary = BibliographicReference.Factory.newInstance();
        // primary.setPubAccession( accessionEntry );
        // primary.setTitle( "Primary" );
        // primary = bibRefService.create( primary );
        //        
        // ee.setPrimaryPublication( primary );
        //        
        // other = BibliographicReference.Factory.newInstance();
        // other.setPubAccession( accessionEntry );
        // other.setTitle( "other" );
        // other = bibRefService.create( other );
        //        
        // Collection<BibliographicReference> others = new HashSet<BibliographicReference>();
        // others.add( other );
        // ee.setOtherRelevantPublications( others ) ;

        expressionExperimentDao.update( ee );
        expressionExperimentDao.thaw( ee );

    }

    @Override
    protected void onTearDownInTransaction() throws Exception {
        super.onTearDownInTransaction();
        if ( ee != null ) {
            expressionExperimentDao.remove( ee );
        }
    }

    /**
     * @throws Exception
     */
    public void testGetOwner() throws Exception {
        // what is this testing exactly?
        ExpressionExperiment expressionExperiment = expressionExperimentDao.findByName( EE_NAME );
        assertNotNull( expressionExperiment );
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

        Collection<DesignElementDataVector> vectors = expressionExperimentDao.getDesignElementDataVectors(
                designElements, quantitationType );

        assertEquals( 2, vectors.size() );

    }

    @SuppressWarnings("unchecked")
    public final void testGetDesignElementDataVectorsByQt() throws Exception {
        QuantitationType quantitationType = ee.getDesignElementDataVectors().iterator().next().getQuantitationType();
        Collection<QuantitationType> quantitationTypes = new HashSet<QuantitationType>();
        quantitationTypes.add( quantitationType );

        log.info( "***********************" );
        Collection<DesignElementDataVector> vectors = expressionExperimentDao.getDesignElementDataVectors( ee,
                quantitationTypes );
        log.info( "***********************" );
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
    
    //Test needs to be run against the production db. Comment out the onsetup and on tear down before running on production. 
    //The test db is just to trivial a db for this test to ever fail. 
    //there were issues with loadValueObjects not returning all the specified value objects
    //because of join issues (difference between left join and inner join).  Made this test to quickly test if it was working or not. 
    // public final void testVerifyLoadValueObjects() throws Exception {
    //               
    // Collection<ExpressionExperiment> eeAll = expressionExperimentDao.loadAll();
    //        
    // Collection<Long> ids = new LinkedHashSet<Long>();
    // for ( ExpressionExperiment ee : eeAll ) {
    // ids.add( ee.getId() );
    // }
    // log.debug( "loadAll: " + ids.toString() );
    //
    // Collection<ExpressionExperimentValueObject> valueObjs = expressionExperimentDao.loadValueObjects( ids );
    //        
    // Collection<Long> idsAfter = new LinkedHashSet<Long>();
    // for (ExpressionExperimentValueObject ee : valueObjs){
    // idsAfter.add( ee.getId());
    // }
    //        
    // log.debug( "loadValueObjects: " + idsAfter.toString() );
    //        
    // Collection<Long> removedIds = new LinkedHashSet<Long>(ids);
    // removedIds.removeAll( idsAfter );
    //        
    // log.debug( "Intersection of EEs: " + removedIds.toString() );
    //        assertEquals(idsAfter.size(), ids.size());
    //    }

    /**
     * @param expressionExperimentDao the expressionExperimentDao to set
     */
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

    // Creating test data for this was difficult. Needed to use a current data base for this test to work.
    // public void testFindByGene() throws Exception {
    // GeneService geneS = (GeneService) this.getBean( "geneService" );
    // Collection<Gene> genes = geneS.findByOfficialSymbol( "grin1" );
    // ExpressionExperimentService eeService = ( ExpressionExperimentService ) this
    // .getBean( "expressionExperimentService" );
    // Collection<Long> results = eeService.findByGene( genes.iterator().next());
    // log.info( results );
    // assertEquals(89, results.size() );
    //        
    // }

    // This test uses the DB
    // public void testFindByBibliographicReference(){
    // ExpressionExperimentService eeService = ( ExpressionExperimentService ) this
    // .getBean( "expressionExperimentService" );
    // BibliographicReferenceService bibRefService = ( BibliographicReferenceService ) this
    // .getBean( "bibliographicReferenceService" );
    //        
    // BibliographicReference bibRef = bibRefService.load( new Long(111 ));
    //
    // Collection<ExpressionExperiment> foundEEs = eeService.findByBibliographicReference( bibRef );
    // assertEquals(1,foundEEs.size());
    // assertEquals(new Long(8), (Long) foundEEs.iterator().next().getId());
    //       
    // }

}
