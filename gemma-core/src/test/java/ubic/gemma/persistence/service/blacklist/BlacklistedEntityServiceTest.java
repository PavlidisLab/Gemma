package ubic.gemma.persistence.service.blacklist;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.blacklist.BlacklistedExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.*;

public class BlacklistedEntityServiceTest extends BaseSpringContextTest {


    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private BlacklistedEntityService blacklistedEntityService;

    /* fixtures */
    private ArrayDesign ad;
    private ExpressionExperiment ee;

    @After
    public void tearDown() {
        if ( ee != null )
            expressionExperimentService.remove( ee );
        if ( ad != null )
            arrayDesignService.remove( ad );
        blacklistedEntityService.removeAll();
    }

    @Test
    public void testBlacklistPlatformProperlyCascadeThroughAllExperiments() {
        ad = getTestPersistentArrayDesign( 10, true );
        ee = getTestPersistentBasicExpressionExperiment( ad );
        blacklistedEntityService.blacklistPlatform( ad, "Don't feel bad, you'll get another chance." );
        assertTrue( blacklistedEntityService.isBlacklisted( ad ) );
        assertTrue( blacklistedEntityService.isBlacklisted( ee ) );
    }

    @Test
    public void testBlacklistExperiment() {
        ee = getTestPersistentBasicExpressionExperiment();
        BlacklistedExperiment be = blacklistedEntityService.blacklistExpressionExperiment( ee, "Don't feel bad, you'll get another chance." );
        assertEquals( ee.getShortName(), be.getShortName() );
        assertNotNull( ee.getAccession() );
        assertEquals( ee.getAccession().getAccession(), be.getExternalAccession().getAccession() );
        assertNull( be.getExternalAccession().getAccessionVersion() );
        assertTrue( blacklistedEntityService.isBlacklisted( ee ) );
    }

    @Test
    public void testBlacklistedExperimentAsNonAdmin() {
        ee = getTestPersistentBasicExpressionExperiment();
        try {
            runAsUser( "bob" );
            assertThatThrownBy( () -> blacklistedEntityService.blacklistExpressionExperiment( ee, "Don't feel bad, you'll get another chance." ) )
                    .isInstanceOf( AccessDeniedException.class );
        } finally {
            runAsAdmin();
        }
    }
}