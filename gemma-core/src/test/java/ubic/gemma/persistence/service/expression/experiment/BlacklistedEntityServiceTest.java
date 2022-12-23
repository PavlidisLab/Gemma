package ubic.gemma.persistence.service.expression.experiment;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

import static org.junit.Assert.assertTrue;

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
        ee = getTestPersistentExpressionExperiment();
        blacklistedEntityService.blacklistExpressionExperiment( ee, "Don't feel bad, you'll get another chance." );
        assertTrue( blacklistedEntityService.isBlacklisted( ee ) );
    }
}