/*
 * Print out the locations of the alignments for probes on a given platform (basically in PSL format)
 */
package ubic.gemma.script.example
import ubic.gemma.analysis.sequence.BlatResult2Psl
import ubic.gemma.script.framework.GemmaCliBuilder
import ubic.gemma.script.framework.SpringSupport

def cli = new GemmaCliBuilder(usage : "groovy ProbeLocations [opts] <platform short name>")

def opt = cli.parse(args)

def sx = new SpringSupport()

def ars = sx.getBean("arrayDesignService")
def bas = sx.getBean("blatResultService")
def bss = sx.getBean("bioSequenceService")

def array = ars.findByShortName(opt.arguments()[0])

println("# ${array}")

ars.getBioSequences(array).each{
    def cs = it.key
    def bs = bss.thaw(it.value)
    bs.getBioSequence2GeneProduct().each{
        def bl = bas.thaw(it.getBlatResult())
        def s = BlatResult2Psl.blatResult2Psl(bl)
        print("${cs.name}\t${cs.id}\t${s}")
    }
}

sx.shutdown()
