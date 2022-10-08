package ubic.gemma.groovy.framework

import gemma.gsec.authentication.ManualAuthenticationService
import org.apache.commons.lang3.time.StopWatch
import ubic.gemma.persistence.util.SpringContextUtil

import java.util.concurrent.atomic.AtomicBoolean

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

    private ctx

    SpringSupport() {
        this(null, null)
    }

    SpringSupport(String userName, String password) {

        def b = new AtomicBoolean(false)
        System.err.print "Loading Spring context "
        //noinspection GroovyAssignabilityCheck
        def t = Thread.start {
            def timer = new StopWatch()
            timer.start()
            while (!b.get()) {
                sleep 1000
                System.err.print '.'
            }
            System.err.println "Ready in ${timer.getTime()}ms"
        }

        // scan for beans, but exclude jms, web
        ctx = SpringContextUtil.getApplicationContext(false)
        b.set(true)
        t.join()

        ManualAuthenticationService manAuthentication = (ManualAuthenticationService) ctx.getBean("manualAuthenticationService")

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

    def shutdown() {
        ctx.close()
    }
}

