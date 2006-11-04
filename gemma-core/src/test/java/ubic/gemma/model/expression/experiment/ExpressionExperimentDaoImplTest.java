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

import org.apache.commons.lang.RandomStringUtils;

import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.ContactService;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.DesignElement;
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

    /**
     * @exception Exception
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {

        super.onSetUpInTransaction();

        ee = this.getTestPersistentCompleteExpressionExperiment();
        ee.setName( EE_NAME );

        DatabaseEntry accessionEntry = this.getTestPersistentDatabaseEntry();
        accession = accessionEntry.getAccession();
        ed = accessionEntry.getExternalDatabase();
        ee.setAccession( accessionEntry );

        Contact c = this.getTestPersistentContact();
        this.contactName = c.getName();

        ee.setOwner( c );

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

        Collection<DesignElementDataVector> vectors = expressionExperimentDao.getDesignElementDataVectors( ee,
                designElements, quantitationType );

        assertEquals( 2, vectors.size() );

    }

    @SuppressWarnings("unchecked")
    public final void testGetSamplingOfVectors() throws Exception {
        QuantitationType quantitationType = ee.getDesignElementDataVectors().iterator().next().getQuantitationType();
        Collection<DesignElementDataVector> vectors = expressionExperimentDao.getSamplingOfVectors( ee,
                quantitationType, 2 );

        assertEquals( 2, vectors.size() );

    }

    /**
     * @param expressionExperimentDao the expressionExperimentDao to set
     */
    public void setExpressionExperimentDao( ExpressionExperimentDao expressionExperimentDao ) {
        this.expressionExperimentDao = expressionExperimentDao;
    }

}
