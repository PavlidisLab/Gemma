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
package ubic.gemma.loader.expression.arrayDesign;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang.RandomStringUtils;
import org.hibernate.LockMode;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.loader.expression.geo.service.AbstractGeoService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignSequenceProcessorTest extends BaseSpringContextTest {

    Collection<CompositeSequence> designElements = new HashSet<CompositeSequence>();
    InputStream seqFile;
    InputStream probeFile;
    InputStream designElementStream;
    Taxon taxon;
    ArrayDesign result;
    ArrayDesignSequenceProcessingService app;

    @Override
    protected void onSetUp() throws Exception {
        super.onSetUp();

        // note that the name MG-U74A is not used by the result.
        designElementStream = this.getClass().getResourceAsStream( "/data/loader/expression/arrayDesign/MG-U74A.txt" );
        app = ( ArrayDesignSequenceProcessingService ) getBean( "arrayDesignSequenceProcessingService" );
        seqFile = this.getClass().getResourceAsStream( "/data/loader/expression/arrayDesign/MG-U74A_target" );

        probeFile = this.getClass().getResourceAsStream( "/data/loader/expression/arrayDesign/MG-U74A_probe" );

        taxon = ( ( TaxonService ) getBean( "taxonService" ) ).findByScientificName( "Mus musculus" );
        assert taxon != null;
    }

    @Override
    protected void onTearDown() throws Exception {
        if ( result != null ) {
            ArrayDesignService svc = ( ArrayDesignService ) this.getBean( "arrayDesignService" );
            svc.remove( result );
        }
    }

    public void testAssignSequencesToDesignElements() throws Exception {
        app.assignSequencesToDesignElements( designElements, seqFile );
        CompositeSequenceParser parser = new CompositeSequenceParser();
        parser.parse( designElementStream );
        designElements = parser.getResults();
        for ( DesignElement de : designElements ) {
            assertTrue( ( ( CompositeSequence ) de ).getBiologicalCharacteristic() != null );
        }
    }

    public void testAssignSequencesToDesignElementsMissingSequence() throws Exception {

        CompositeSequenceParser parser = new CompositeSequenceParser();
        parser.parse( designElementStream );
        designElements = parser.getResults();

        CompositeSequence doesntExist = CompositeSequence.Factory.newInstance();
        String fakeName = "I'm not real";
        doesntExist.setName( fakeName );
        designElements.add( doesntExist );

        app.assignSequencesToDesignElements( designElements, seqFile );

        boolean found = false;
        assertEquals( 34, designElements.size() ); // 33 from file plus one fake.

        for ( DesignElement de : designElements ) {

            if ( de.getName().equals( fakeName ) ) {
                found = true;
                if ( ( ( CompositeSequence ) de ).getBiologicalCharacteristic() != null ) {
                    fail( "Shouldn't have found a biological characteristic for this sequence" );

                    continue;
                }
            } else {
                assertTrue( de.getName() + " biological sequence not found", ( ( CompositeSequence ) de )
                        .getBiologicalCharacteristic() != null );
            }

        }

        assertTrue( found ); // sanity check.

    }

    public void testProcessAffymetrixDesign() throws Exception {
        result = app.processAffymetrixDesign( RandomStringUtils.randomAlphabetic( 10 ) + "_arraydesign",
                designElementStream, probeFile, taxon );

        assertEquals( "composite sequence count", 33, result.getCompositeSequences().size() );
        // assertEquals( "reporter count", 528, result.getReporters().size() );
        // assertEquals( "reporter per composite sequence", 16, result.getCompositeSequences().iterator().next()
        // .getComponentReporters().size() );
        assertTrue( result.getCompositeSequences().iterator().next().getArrayDesign() == result );

    }

    @SuppressWarnings("unchecked")
    public void testBig() throws Exception {
        // first load the GPL88 - small
        AbstractGeoService geoService = ( AbstractGeoService ) this.getBean( "geoDatasetService" );
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );
        geoService.setLoadPlatformOnly( true );
        final Collection<ArrayDesign> ads = ( Collection<ArrayDesign> ) geoService.fetchAndLoad( "GPL88" );

        final ArrayDesign ad = ads.iterator().next();

        HibernateDaoSupport hds = new HibernateDaoSupport() {
        };

        hds.setSessionFactory( ( SessionFactory ) this.getBean( "sessionFactory" ) );

        HibernateTemplate templ = hds.getHibernateTemplate();

        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                session.lock( ad, LockMode.READ );
                ad.getCompositeSequences().size();
                return null;
            }
        }, true );

        // now do the sequences.
        ZipInputStream z = new ZipInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/arrayDesign/RN-U34_probe_tab.zip" ) );

        z.getNextEntry();

        taxon = ( ( TaxonService ) getBean( "taxonService" ) ).findByScientificName( "Rattus norvegicus" );
        assertNotNull( taxon );
        Collection<BioSequence> res = app.processArrayDesign( ad, z, SequenceType.AFFY_PROBE, taxon );
        assertEquals( 1322, res.size() );
    }

    public void testProcessNonAffyDesign() throws Exception {
        result = app.processAffymetrixDesign( RandomStringUtils.randomAlphabetic( 10 ) + "_arraydesign",
                designElementStream, probeFile, taxon );

        assertNotNull( result.getId() );

        app.processArrayDesign( result, seqFile, SequenceType.EST, taxon );

        assertEquals( "composite sequence count", 33, result.getCompositeSequences().size() );

        // assertEquals( "reporter per composite sequence", 17, result.getCompositeSequences().iterator().next()
        // .getComponentReporters().size() );
        assertTrue( result.getCompositeSequences().iterator().next().getArrayDesign() == result );
    }

}
