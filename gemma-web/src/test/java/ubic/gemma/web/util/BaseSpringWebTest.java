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
package ubic.gemma.web.util;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.PersistentDummyObjectHelper;
import ubic.gemma.core.util.test.TestAuthenticationUtils;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Class to extend for tests of controllers et al. that need a spring context. Provides convenience methods for dealing
 * with mock requests and responses. Also provides a safe port to send email on for testing (for example, using
 * dumbster)
 * @author pavlidis
 * @deprecated favour the simpler {@link BaseWebIntegrationTest} for new tests
 */
@Deprecated
public abstract class BaseSpringWebTest extends BaseWebIntegrationTest {

    protected final Log log = LogFactory.getLog( this.getClass() );

    /* shared fixtures */
    private static ExpressionExperiment readOnlyEe = null;

    @Autowired
    protected ExternalDatabaseService externalDatabaseService;
    @Autowired
    protected TaxonService taxonService;
    @Autowired
    protected PersistentDummyObjectHelper testHelper;
    @Autowired
    private TestAuthenticationUtils testAuthenticationUtils;

    /**
     * Obtain a taxon by its common name.
     * <p>
     * See {@code sql/init-data.sql} for a list of available taxa to use in tests.
     */
    public Taxon getTaxon( String commonName ) {
        return requireNonNull( this.taxonService.findByCommonName( commonName ),
                String.format( "Unknown taxa with common name %s, has the test data been loaded?", commonName ) );
    }

    public Gene getTestPersistentGene( Taxon taxon ) {
        return testHelper.getTestPersistentGene( taxon );
    }

    public Set<BioSequence2GeneProduct> getTestPersistentBioSequence2GeneProducts( BioSequence bioSequence ) {
        return testHelper.getTestPersistentBioSequence2GeneProducts( bioSequence );
    }

    /**
     * Convenience shortcut for RandomStringUtils.insecure().nextAlphabetic( 10 ) (or something similar to that)
     *
     * @return random alphabetic string
     */
    public String randomName() {
        return RandomStringUtils.insecure().nextAlphabetic( 10 );
    }

    /**
     * @return EE with no data; just bioassays, biomaterials, quantitation types and (minimal) array designs.
     */
    protected ExpressionExperiment getTestPersistentBasicExpressionExperiment() {
        return testHelper.getTestPersistentBasicExpressionExperiment();
    }

    protected BibliographicReference getTestPersistentBibliographicReference( String accession ) {
        return testHelper.getTestPersistentBibliographicReference( accession );
    }

    /**
     * Convenience method to get a (fairly) complete randomly generated persisted expression experiment.
     *
     * @param readOnly If the test only needs to read, a new data set might not be created.
     * @return EE
     */
    protected ExpressionExperiment getTestPersistentCompleteExpressionExperiment( boolean readOnly ) {
        if ( readOnly ) {
            if ( BaseSpringWebTest.readOnlyEe == null ) {
                log.info( "Initializing test expression experiment (one-time for read-only tests)" );
                BaseSpringWebTest.readOnlyEe = testHelper.getTestExpressionExperimentWithAllDependencies();
            }
            return BaseSpringWebTest.readOnlyEe;
        }

        return testHelper.getTestExpressionExperimentWithAllDependencies();
    }

    /**
     * Convenience method to provide a DatabaseEntry that can be used to fill non-nullable associations in test objects.
     * The accession and ExternalDatabase name are set to random strings.
     *
     * @param ed external Db
     * @return Db entry
     */
    protected DatabaseEntry getTestPersistentDatabaseEntry( ExternalDatabase ed ) {
        return this.getTestPersistentDatabaseEntry( null, ed );
    }

    /**
     * Convenience method to provide a DatabaseEntry that can be used to fill non-nullable associations in test objects.
     * The accession and ExternalDatabase name are set to random strings.
     *
     * @param accession accession
     * @param ed        database
     * @return Db entry
     */
    protected DatabaseEntry getTestPersistentDatabaseEntry( String accession, ExternalDatabase ed ) {
        return testHelper.getTestPersistentDatabaseEntry( accession, ed );
    }

    /**
     * Convenience method to provide an ExpressionExperiment that can be used to fill non-nullable associations in test
     * objects. This implementation does NOT fill in associations of the created object.
     *
     * @return EE
     */
    protected ExpressionExperiment getTestPersistentExpressionExperiment() {
        return testHelper.getTestPersistentExpressionExperiment();
    }

    /**
     * @see TestAuthenticationUtils#runAsUser(String, boolean)
     */
    protected void runAsUser( String userName ) {
        testAuthenticationUtils.runAsUser( userName, true );
    }
}
