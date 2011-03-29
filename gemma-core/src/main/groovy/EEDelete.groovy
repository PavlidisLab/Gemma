#!/usr/bin/groovy
import ubic.gemma.script.SpringSupport

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