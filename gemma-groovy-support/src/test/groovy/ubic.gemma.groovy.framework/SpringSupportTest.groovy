package ubic.gemma.groovy.framework

import org.junit.Test
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService
import ubic.gemma.persistence.util.SpringProfiles

import static org.junit.Assert.assertNotNull

class SpringSupportTest {

    @Test
    void test() {
        var springSupport = new SpringSupport(null, null, [SpringProfiles.TEST])
        assertNotNull(springSupport.getBean("expressionExperimentService"))
        assertNotNull(springSupport.getBean(ExpressionExperimentService.class))
    }
}
