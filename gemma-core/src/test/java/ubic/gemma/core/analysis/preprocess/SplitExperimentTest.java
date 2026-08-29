/*
 * The gemma-core project
 *
 * Copyright (c) 2018 University of British Columbia
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

package ubic.gemma.core.analysis.preprocess;

import ubic.gemma.core.security.SecurityService;
import ubic.gemma.core.security.acl.domain.AclObjectIdentity;
import ubic.gemma.core.security.acl.domain.AclService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.NotFoundException;
import ubic.gemma.core.util.FileTools;
import ubic.gemma.core.loader.entrez.EntrezUtils;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.loader.expression.simple.ExperimentalDesignImporter;
import ubic.gemma.core.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.core.util.test.BaseSpringContextTest5;
import ubic.gemma.core.util.test.NetworkAvailable;
import ubic.gemma.core.util.test.NetworkAvailableExtension;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.abort;

/**
 *
 *
 * @author paul
 */
@ExtendWith(NetworkAvailableExtension.class)
public class SplitExperimentTest extends BaseSpringContextTest5 {

    @Autowired
    private SplitExperimentService splitService;

    @Autowired
    private GeoService geoService;

    @Autowired
    private ExperimentalDesignImporter experimentalDesignImporter;

    @Autowired
    private PreprocessorService preprocessor;

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private AclService aclService;

    /* fixtures */
    private Collection<ExpressionExperiment> ees;
    private ExpressionExperimentSet results;

    @Test
    @Tag("slow")
    @NetworkAvailable(url = EntrezUtils.ESEARCH)
    public void testSplitGSE17183ByOrganismPart() throws Exception {

        String geoId = "GSE17183";

        geoService.setGeoDomainObjectGenerator(
                new GeoDomainObjectGeneratorLocal( FileTools.resourceToPath( "/data/analysis/preprocess" ) ) );

        try {
            //noinspection unchecked
            ees = ( Collection<ExpressionExperiment> ) geoService.fetchAndLoad( geoId, false, false, false );
        } catch ( AlreadyExistsInSystemException e ) {
            //noinspection unchecked
            ees = ( Collection<ExpressionExperiment> ) e.getData();
            abort( e.getMessage() );
        }

        ExpressionExperiment ee = ees.iterator().next();
        assertNotNull( ee );

        ee = eeService.thaw( ee );

        securityService.makePublic( ee );

        try ( InputStream is = this.getClass()
                .getResourceAsStream( "/data/analysis/preprocess/2877_GSE17183_expdesign.data.txt" ) ) {
            assertNotNull( is );
            experimentalDesignImporter.importDesign( ee, is );
        }

        preprocessor.process( ee, true, true ); // to mimic real life better

        ExperimentalFactor splitOn = null;
        for ( ExperimentalFactor ef : ee.getExperimentalDesign().getExperimentalFactors() ) {
            if ( ef.getName().toLowerCase().startsWith( "organism.part" ) ) {
                splitOn = ef;
            }
        }

        assertNotNull( splitOn );

        results = splitService.split( ee, splitOn, true );

        assertEquals( splitOn.getFactorValues().size(), results.getExperiments().size() );

        for ( ExpressionExperiment b : results.getExperiments() ) {
            ExpressionExperiment e = eeService.thaw( b );

            // sanity checks for the clones
            assertNotNull( ee.getAccession() );
            assertNotNull( e.getAccession() );
            assertNotEquals( ee.getAccession().getId(), e.getAccession().getId() );
            assertEquals( ee.getTaxon(), e.getTaxon() );
            assertNotEquals( ee.getAuditTrail().getId(), e.getAuditTrail().getId() );
            assertNotEquals( ee.getCurationDetails().getId(), e.getCurationDetails().getId() );
            assertNotEquals( ee.getExperimentalDesign().getId(), e.getExperimentalDesign().getId() );
            assertEquals( ee.getPrimaryPublication(), e.getPrimaryPublication() );

            // make sure that clones are used for BAs and BMs
            Set<BioMaterial> bms = ee.getBioAssays().stream().map( BioAssay::getSampleUsed ).collect( Collectors.toSet() );
            for ( BioAssay ba : e.getBioAssays() ) {
                assertFalse( ee.getBioAssays().contains( ba ) );
                assertFalse( bms.contains( ba.getSampleUsed() ) );
            }

            Collection<RawExpressionDataVector> rvs = e.getRawExpressionDataVectors();
            assertEquals( 100, rvs.size() );

            Collection<ProcessedExpressionDataVector> pvs = e.getProcessedExpressionDataVectors();
            assertEquals( 100, pvs.size() );
            assertEquals( 100, e.getNumberOfDataVectors().intValue() );

            RawExpressionDataVector rv = rvs.iterator().next();
            assertTrue( rv.getQuantitationType().getIsPreferred() );
            assertEquals( 2, e.getOtherParts().size() );

        }
    }

    @Test
    @Tag("slow")
    @NetworkAvailable(url = EntrezUtils.ESEARCH)
    public void testSplitGSE123753ByCollectionOfMaterial() throws Exception {

        String geoId = "GSE123753";

        geoService.setGeoDomainObjectGenerator(
                new GeoDomainObjectGeneratorLocal( FileTools.resourceToPath( "/data/analysis/preprocess" ) ) );

        try {
            //noinspection unchecked
            ees = ( Collection<ExpressionExperiment> ) geoService.fetchAndLoad( geoId, false, false, false );
        } catch ( AlreadyExistsInSystemException e ) {
            //noinspection unchecked
            ees = ( ( Collection<ExpressionExperiment> ) e.getData() );
            abort( e.getMessage() );
        }

        ExpressionExperiment ee = ees.iterator().next();
        assertNotNull( ee );

        ee = eeService.thaw( ee );

        securityService.makePublic( ee );

        try ( InputStream is = this.getClass()
                .getResourceAsStream( "/data/analysis/preprocess/17525_GSE123753_expdesign.data.txt" ) ) {
            assertNotNull( is );
            experimentalDesignImporter.importDesign( ee, is );
        }

        // we can't really process the data since there are no attached datasets to the GEO series

        ExperimentalFactor splitOn = null;
        for ( ExperimentalFactor ef : ee.getExperimentalDesign().getExperimentalFactors() ) {
            if ( ef.getName().toLowerCase().startsWith( "collection.of.material" ) ) {
                splitOn = ef;
            }
        }

        assertNotNull( splitOn );

        results = splitService.split( ee, splitOn, false );
        assertEquals( splitOn.getFactorValues().size(), results.getExperiments().size() );
    }

    /**
     * Every experiment a split produces must come out editable.
     * <p>
     * A split child is created through {@code eeWriteService.create}, so its ACL is the
     * responsibility of {@code AclEventListener.onPostInsert} — nothing in
     * {@link SplitExperimentService} sets one up itself, and {@code split} is
     * {@code Propagation.NEVER} so the insert happens in a transaction the method does not own.
     * ExpressionExperiments 93287, 93288, 93289, 93433 and 93434 — splits made under Gemma 1.0,
     * which had no such listener — reached production with no ACL at all and answer 403 on every
     * update. This pins the 2.0 path: the ACL exists and grants edit.
     * <p>
     * Uses a synthetic single-platform experiment rather than a GEO series so it stays in the
     * default suite; the two tests above are {@code slow} and need the network.
     */
    @Test
    public void testSplitGivesEachNewExperimentAnAcl() {
        ArrayDesign ad = getTestPersistentArrayDesign( 0, true, false, false );
        ExpressionExperiment ee = getTestPersistentBasicExpressionExperiment( ad );
        ees = Collections.singleton( ee );

        // Deliberately NOT thawed: split() is Propagation.NEVER and reads the deprecated lazy
        // ExperimentalFactor.annotations while cloning, which no thaw initializes. Use the graph
        // the helper built in memory.
        ExperimentalFactor splitOn = ee.getExperimentalDesign().getExperimentalFactors().iterator().next();
        assertFalse( splitOn.getFactorValues().isEmpty() );

        results = splitService.split( ee, splitOn, false );
        assertEquals( splitOn.getFactorValues().size(), results.getExperiments().size() );

        for ( ExpressionExperiment split : results.getExperiments() ) {
            Acl acl;
            try {
                acl = aclService.readAclById( new AclObjectIdentity( ExpressionExperiment.class, split.getId() ) );
            } catch ( NotFoundException e ) {
                acl = null;
            }
            assertNotNull( acl, "Split " + split.getShortName() + " was created without an ACL." );
            assertFalse( acl.getEntries().isEmpty(),
                    "Split " + split.getShortName() + " has an ACL with no entries, so nothing grants edit." );
            // isEditableByUser reads the ACL for WRITE or ADMINISTRATION; that is the permission
            // the 1.0-era splits lack, and the reason they answer 403 on update.
            assertTrue( securityService.isEditableByCurrentUser( split ),
                    "Split " + split.getShortName() + " is not editable by an administrator." );
        }
    }

    @AfterEach
    public void teardown() throws Exception {
        // remove original dataset
        if ( ees != null ) {
            eeService.remove( ees );
        }
        // remove any created experiments
        if ( results != null ) {
            for ( ExpressionExperiment b : results.getExperiments() ) {
                eeService.remove( b );
            }
        }
    }
}
