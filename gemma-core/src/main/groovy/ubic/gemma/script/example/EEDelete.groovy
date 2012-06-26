#!/usr/bin/groovy

package ubic.gemma.script.example

import ubic.gemma.script.framework.GemmaCliBuilder;
import ubic.gemma.script.framework.SpringSupport;

def cli = new GemmaCliBuilder(usage: 'groovy EEDelete [opts] <eeid>')

def opt = cli.parse(args)
if (!opt) return

    if (opt.h) cli.usage()

sx = new SpringSupport(opt.u, opt.p)
ees = sx.getBean("expressionExperimentService")

for (id in opt.arguments()) {
    ee = ees.load(ee)
    ees.remove(ee)
}


sx.shutdown()