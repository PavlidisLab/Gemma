#!/usr/bin/groovy
package ubic.gemma.script.example
import ubic.gemma.script.framework.SpringSupport

/* Parse arguments */
def cli = new CliBuilder(usage: 'groovy CreateMeanVariance [options] -u *** -p *** -e ***')
cli.p(longOpt: 'password', args:1, 'password', required: true)
cli.u(longOpt: 'username', args:1, 'username', required: true)
cli.e(args:1, 'file that contains EE shortNames, one per line', required:true)
cli.r(longOpt: 'recompute', 'recompute mean-variance?', required: false)
cli.h(longOpt: 'help'  , 'usage information', required: false)
def opt = cli.parse(args)

if (!opt) {
    return
}

if (opt.h) {
    cli.usage()
    return
}

username = opt.u
password = opt.p
forceRecompute = opt.r
infile = opt.e
eeIds = new File(infile).readLines()

/* Do the actual work */
sx = new SpringSupport(username, password)
ees = sx.getBean("expressionExperimentService")
mvs = sx.getBean("meanVarianceService")

pass = 0
fail = []
for (id in eeIds) {
    ee = ees.findByShortName(id)
    
    if (ee == null) {
        System.err.println('ERROR: Could not find experiment '+id)
        fail.add(id)
        continue
    }
    
    println "Processing mean-variance for experiment " + ee.getShortName() + ' ...'
    
    try {
        mvs.create(ee, forceRecompute)
        pass++
    } catch(e) {
        System.err.println('ERROR: Error computing mean-variance')
        e.printStackTrace()
        fail.add(id)
    }
}

sx.shutdown()

println 'Finished processing ' + pass + ' experiments.'

if (fail.size() > 0) {
    println 'Failed to process these experiments ' + fail
}

