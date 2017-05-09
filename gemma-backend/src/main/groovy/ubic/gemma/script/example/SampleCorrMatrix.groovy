package ubic.gemma.script.example
import ubic.gemma.script.framework.GemmaCliBuilder
import ubic.gemma.script.framework.SpringSupport

def cli = new GemmaCliBuilder("groovy Samplecorrmatrix [opts] <eeid>")
sx = new SpringSupport( )
s = sx.getBean("sampleCoexpressionMatrixService")

for (id in opt.arguments()) {
    ee = ees.load(ee)
    s.getSampleCorrelationMatrix(ee, true)
}


sx.shutdown()
