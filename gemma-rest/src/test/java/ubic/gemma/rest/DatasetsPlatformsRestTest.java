package ubic.gemma.rest;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ubic.gemma.core.util.test.PersistentDummyObjectHelper;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.rest.util.BaseJerseyIntegrationTest5;
import ubic.gemma.rest.util.args.DatasetArg;

import javax.annotation.Nullable;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.rest.util.Assertions.assertThat;

/**
 * Integration tests for {@code GET /datasets/{dataset}/platforms}, in both modes, against a real
 * (gemdtest) database.
 * <p>
 * 🛑 The point of this class is the transaction boundary, which neither of the two existing test layers
 * crosses. {@code ArrayDesignDaoTest} covers the same three cases but runs inside {@code BaseDatabaseTest5}'s
 * own transaction, so a Hibernate session is always bound to the thread; {@code DatasetsWebServiceTest} stubs
 * {@code ArrayDesignService}, so the real service method never executes. A read-service method missing its
 * {@code @Transactional} is green in both and 500s in production — which is what
 * {@code ArrayDesignReadServiceImpl.loadOriginalPlatformValueObjectsForEE} did until {@code 69c1c11b5f}:
 * {@code Could not obtain transaction-synchronized Session for current thread}.
 * <p>
 * These tests call the handler with no transaction open on the test thread (the Jersey base class registers
 * no {@code TransactionalTestExecutionListener} and the in-memory container has no open-session-in-view
 * filter), so the service must open its own. Fixture setup is the one part that needs a session, and it takes
 * one explicitly through a {@link TransactionTemplate}.
 * <p>
 * Cases, mirroring the endpoint's documented contract:
 * <ul>
 *     <li>a switched dataset answers with the platform it was submitted on, not the one in use;</li>
 *     <li>a dataset that was never switched answers with an empty list, not its current platform;</li>
 *     <li>a no-op switch (recorded original == the platform in use) is excluded.</li>
 * </ul>
 *
 * @author gemma
 */
@Tag("integration")
public class DatasetsPlatformsRestTest extends BaseJerseyIntegrationTest5 {

    @Autowired
    private DatasetsWebService datasetsWebService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private PersistentDummyObjectHelper testHelper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /** What every fixture dataset's assays are on now. */
    private ArrayDesign usedPlatform;
    /** What {@link #eeSwitched} came in on. */
    private ArrayDesign submittedPlatform;

    private ExpressionExperiment eeSwitched;
    private ExpressionExperiment eeNeverSwitched;
    private ExpressionExperiment eeNoopSwitch;

    @BeforeEach
    public void setUpFixtures() {
        testHelper.resetSeed();
        usedPlatform = testHelper.getTestPersistentArrayDesign( 0, true, false );
        submittedPlatform = testHelper.getTestPersistentArrayDesign( 0, true, false );

        eeSwitched = testHelper.getTestPersistentBasicExpressionExperiment( usedPlatform, false );
        recordOriginalPlatform( eeSwitched, submittedPlatform );

        eeNeverSwitched = testHelper.getTestPersistentBasicExpressionExperiment( usedPlatform, false );

        eeNoopSwitch = testHelper.getTestPersistentBasicExpressionExperiment( usedPlatform, false );
        recordOriginalPlatform( eeNoopSwitch, usedPlatform );
    }

    @AfterEach
    public void tearDownFixtures() {
        if ( eeSwitched != null ) {
            expressionExperimentService.remove( eeSwitched );
        }
        if ( eeNeverSwitched != null ) {
            expressionExperimentService.remove( eeNeverSwitched );
        }
        if ( eeNoopSwitch != null ) {
            expressionExperimentService.remove( eeNoopSwitch );
        }
    }

    /**
     * Stamp {@code originalPlatform} on every assay of {@code ee}. Runs in its own transaction so the assays
     * are managed and the change flushes on commit — the fixture needs a session, the test bodies must not
     * have one.
     */
    private void recordOriginalPlatform( ExpressionExperiment ee, @Nullable ArrayDesign original ) {
        new TransactionTemplate( transactionManager ).executeWithoutResult( status -> {
            ExpressionExperiment attached = expressionExperimentService
                    .thawBioAssays( expressionExperimentService.load( ee.getId() ) );
            ArrayDesign op = original != null ? arrayDesignService.load( original.getId() ) : null;
            for ( BioAssay ba : attached.getBioAssays() ) {
                ba.setOriginalPlatform( op );
            }
        } );
    }

    /**
     * A real switch: {@code ?original=true} names the submitted platform, and the default mode still names
     * the one in use.
     */
    @Test
    public void getPlatformsOriginal_namesTheSubmittedPlatform() {
        List<ArrayDesignValueObject> original = datasetsWebService
                .getDatasetPlatforms( DatasetArg.valueOf( eeSwitched.getId().toString() ), true )
                .getData();
        assertThat( original )
                .singleElement()
                .hasFieldOrPropertyWithValue( "shortName", submittedPlatform.getShortName() );

        List<ArrayDesignValueObject> current = datasetsWebService
                .getDatasetPlatforms( DatasetArg.valueOf( eeSwitched.getId().toString() ), false )
                .getData();
        assertThat( current )
                .singleElement()
                .hasFieldOrPropertyWithValue( "shortName", usedPlatform.getShortName() );
    }

    /**
     * The wire check, and the reason this class exists: the route answers 200 with the submitted platform in
     * the body. With the read service's {@code @Transactional} absent this is a 500 and the body carries
     * {@code Could not obtain transaction-synchronized Session for current thread}.
     */
    @Test
    public void getPlatformsOriginal_isTwoHundredOverHttp() {
        try ( Response r = target( "/datasets/" + eeSwitched.getId() + "/platforms" )
                .queryParam( "original", "true" )
                .request()
                .get() ) {
            assertThat( r )
                    .hasStatus( Response.Status.OK )
                    .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                    .entityAsString()
                    .contains( submittedPlatform.getShortName() )
                    .doesNotContain( usedPlatform.getShortName() )
                    .doesNotContain( "Session for current thread" );
        }
    }

    /**
     * Never switched: an empty list, NOT the platform the dataset is on. Returning the current platform here
     * would render "as originally submitted" on every dataset in the corpus.
     */
    @Test
    public void getPlatformsOriginal_isEmptyWhenNeverSwitched() {
        assertThat( datasetsWebService
                .getDatasetPlatforms( DatasetArg.valueOf( eeNeverSwitched.getId().toString() ), true )
                .getData() )
                .isEmpty();

        // the dataset does have a current platform — the empty answer above is the absence of a switch,
        // not an absent platform.
        assertThat( datasetsWebService
                .getDatasetPlatforms( DatasetArg.valueOf( eeNeverSwitched.getId().toString() ), false )
                .getData() )
                .singleElement()
                .hasFieldOrPropertyWithValue( "shortName", usedPlatform.getShortName() );

        try ( Response r = target( "/datasets/" + eeNeverSwitched.getId() + "/platforms" )
                .queryParam( "original", "true" )
                .request()
                .get() ) {
            assertThat( r )
                    .hasStatus( Response.Status.OK )
                    .entityAsString()
                    .doesNotContain( usedPlatform.getShortName() );
        }
    }

    /**
     * A no-op switch — the original recorded as the platform already in use — is excluded, so a non-empty
     * answer always names something that actually changed.
     */
    @Test
    public void getPlatformsOriginal_excludesANoopSwitch() {
        assertThat( datasetsWebService
                .getDatasetPlatforms( DatasetArg.valueOf( eeNoopSwitch.getId().toString() ), true )
                .getData() )
                .isEmpty();

        try ( Response r = target( "/datasets/" + eeNoopSwitch.getId() + "/platforms" )
                .queryParam( "original", "true" )
                .request()
                .get() ) {
            assertThat( r )
                    .hasStatus( Response.Status.OK )
                    .entityAsString()
                    .doesNotContain( usedPlatform.getShortName() );
        }
    }
}
