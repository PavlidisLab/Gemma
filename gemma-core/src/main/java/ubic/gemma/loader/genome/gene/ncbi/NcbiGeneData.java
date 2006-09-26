/**
 * 
 */
package ubic.gemma.loader.genome.gene.ncbi;

import java.util.ArrayList;
import java.util.Collection;

import ubic.gemma.loader.genome.gene.ncbi.model.NCBIGene2Accession;
import ubic.gemma.loader.genome.gene.ncbi.model.NCBIGeneInfo;

/**
 * Simple helper data structure that stores an NcbiGeneInfo and its associated
 * NcbiGene2Accession elements.
 * 
 * @author jsantos
 *
 */
public class NcbiGeneData {
    private NCBIGeneInfo geneInfo;
    private Collection<NCBIGene2Accession> accessions;
    
    public NcbiGeneData () {
        geneInfo = null;
        accessions = new ArrayList<NCBIGene2Accession>();
    }
    
    public void setAccessions(Collection<NCBIGene2Accession> accessions) {
        this.accessions = accessions;
    }
    
    public void addAccession(NCBIGene2Accession accession) {
        this.accessions.add( accession );
    }
    
    public void setGeneInfo(NCBIGeneInfo geneInfo) {
        this.geneInfo = geneInfo;
    }
    
    public NCBIGeneInfo getGeneInfo() {
        return this.geneInfo;
    }
    
    public Collection<NCBIGene2Accession> getAccessions() {
        return this.accessions;
    }
    
}
