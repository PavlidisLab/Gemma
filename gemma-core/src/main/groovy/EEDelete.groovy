#!/usr/bin/groovy
import ubic.gemma.script.SpringSupport

def cli = new CliBuilder(usage: 'groovy EEDelete [opts] <eeid>')

cli.h(longOpt: 'help', 'usage information')
cli.u(argName: 'username', longOpt:'username', args:1, required:true, 'username')
cli.p(argName: 'password', longOpt:'password', args:1, required:true, 'password')

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