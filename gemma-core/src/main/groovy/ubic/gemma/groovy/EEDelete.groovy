package ubic.gemma.groovy

import ubic.gemma.groovy.framework.GemmaCliBuilder
import ubic.gemma.groovy.framework.SpringSupport

//noinspection GroovyAssignabilityCheck
GemmaCliBuilder cli = new GemmaCliBuilder(usage: 'groovy ubic.gemma.groovy.EEDelete [opts] <eeid>')

def opt = cli.parse(args) as Object
if (!opt || opt.h) {
    cli.usage()
    return
}
def u = opt.u ?: ""
def p = opt.p ?: ""
sx = new SpringSupport(u, p)

ees = sx.getBean("expressionExperimentService")

for (id in opt.arguments()) {
    ee = ees.load(ee)
    ees.remove(ee)
}


sx.shutdown()
