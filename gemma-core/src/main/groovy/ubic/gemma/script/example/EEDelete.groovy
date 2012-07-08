package ubic.gemma.script.example

import ubic.gemma.script.framework.GemmaCliBuilder
import ubic.gemma.script.framework.SpringSupport

def cli = new GemmaCliBuilder(usage: 'groovy EEDelete [opts] <eeid>')

def opt = cli.parse(args)
if (!opt || opt.h ) {
    cli.usage()
    return
}


sx = new SpringSupport(userName : opt.u, password : opt.p)
ees = sx.getBean("expressionExperimentService")

for (id in opt.arguments()) {
    ee = ees.load(ee)
    ees.remove(ee)
}


sx.shutdown()
