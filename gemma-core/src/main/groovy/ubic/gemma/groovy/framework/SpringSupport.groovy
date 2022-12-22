package ubic.gemma.groovy.framework

import gemma.gsec.authentication.ManualAuthenticationService
import org.springframework.context.ApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.security.core.context.SecurityContextHolder
import ubic.gemma.persistence.util.SpringContextUtil

/**
 * Class for creation of a spring context. Example usage:
 * <pre>
 * sx = new SpringSupport();
 * gs = sx.getBean("geneService");
 * listogenes = gs.findByOfficialSymbol("Grin1");
 * listogenes.each{println(it)}* sx.shutdown();
 * </pre>
 * With authentication use constructor that takes user name and password.
 *
 * @author Paul
 *
 */
class SpringSupport {

    private final ApplicationContext ctx

    SpringSupport() {
        this(null, null, [])
    }

    SpringSupport(String userName, String password) {
        this(userName, password, [])
    }

    SpringSupport(String userName, String password, List<String> activeProfiles) {
        System.err.print "Loading Spring context"
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL)
        ctx = SpringContextUtil.getApplicationContext(activeProfiles as String[])
        authenticate(userName, password);
    }

    private void authenticate(String userName, String password) {
        ManualAuthenticationService manAuthentication = ctx.getBean(ManualAuthenticationService.class)
        if (userName == null && password == null) {
            manAuthentication.authenticateAnonymously()
        } else {
            def success = manAuthentication.validateRequest(userName, password)
            if (!success) {
                //noinspection GroovyAssignabilityCheck
                throw new Exception("Not authenticated. Make sure you entered a valid username (got '" + userName + "') and password")
            } else {
                println("Logged in as " + userName)
            }
        }
    }

    def getBean(String beanName) {
        return ctx.getBean(beanName)
    }

    def <T> T getBean(Class<T> requiredType) {
        return ctx.getBean(requiredType)
    }

    void shutdown() {
        if (ctx instanceof ConfigurableApplicationContext) {
            ctx.close();
        }
    }
}

