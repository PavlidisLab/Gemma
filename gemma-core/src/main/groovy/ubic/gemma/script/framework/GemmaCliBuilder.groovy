package ubic.gemma.script.framework
import groovy.transform.InheritConstructors

import org.apache.log4j.*

@InheritConstructors
class GemmaCliBuilder extends CliBuilder {

    GemmaCliBuilder(String u) {
        usage = u
        super.v(argName: 'verbosity', longOpt:'verbosity', args:1, 'verbosity [0=silent;5=debug]')
        super.h(longOpt: 'help', 'usage information')
        super.u(argName: 'username', longOpt:'username', args:1, required:false)
        super.p(argName: 'password', longOpt:'password', args:1, required:false)
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

    def parse() {
        def opts = super.parse()
        if (opts.h) {
            super.usage()
        }
        if (opts.v) {
            setGemmaLogging(opts.v)
        }
        return opts
    }
}

