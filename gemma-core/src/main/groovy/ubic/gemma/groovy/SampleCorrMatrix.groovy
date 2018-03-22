package ubic.gemma.groovy

import ubic.gemma.groovy.framework.GemmaCliBuilder
import ubic.gemma.groovy.framework.SpringSupport

//noinspection GroovyUnusedAssignment
def cli = new GemmaCliBuilder("groovy Samplecorrmatrix [opts] <eeid>")
sx = new SpringSupport()
s = sx.getBean("sampleCoexpressionMatrixService")

for (id in opt.arguments()) {
    ee = ees.load(ee)
    s.getSampleCorrelationMatrix(ee, true)
}


sx.shutdown()
