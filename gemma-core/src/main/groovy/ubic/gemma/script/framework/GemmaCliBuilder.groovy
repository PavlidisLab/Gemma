package ubic.gemma.script.framework
import  org.apache.log4j.*
import groovy.transform.InheritConstructors
import org.apache.commons.cli.Option

/**
 * To use this in a way that permits omitting the user name and password, use
 * <pre>
 * def u = opt.u ?: ""
 * def p = opt.p ?: ""
 * sx = new SpringSupport(u,p)
 * </pre>
 * Otherwise you will get a "no matching constructor" error when the username and password are not provided.
 */
@InheritConstructors
class GemmaCliBuilder extends CliBuilder {

    GemmaCliBuilder(String u) {
        super()
        usage = u
        this.v([argName: 'verbosity', longOpt:'verbosity', args:1], 'verbosity [0=silent;5=debug]')
        this.h([longOpt: 'help'], 'usage information')
        this.u([argName: 'username', longOpt:'username', args:1, required:false], "")
        this.p([argName: 'password', longOpt:'password', args:1, required:false], "")
    }

    def setGemmaLogging(int verbosity) {
        Logger log4jLogger = LogManager.exists( "ubic.gemma" )
        switch ( verbosity ) {
            case 0:
                log4jLogger.setLevel( Level.OFF )
                break
            case 1:
                log4jLogger.setLevel( Level.FATAL )
                break
            case 2:
                log4jLogger.setLevel( Level.ERROR )
                break
            case 3:
                log4jLogger.setLevel( Level.WARN )
                break
            case 4:
                log4jLogger.setLevel( Level.INFO )
                break
            case 5:
                log4jLogger.setLevel( Level.DEBUG )
                break
            default:
                throw new RuntimeException( "Verbosity must be from 0 to 5" )
        }
    }

    @Override
    def OptionAccessor parse(args) {
        def opts = super.parse(args)

        if (!opts) {
            return opts
        }

        if (opts.h) {
            super.usage()
        }
        if (opts.v) {
            setGemmaLogging(opts.v)
        }
    }
}