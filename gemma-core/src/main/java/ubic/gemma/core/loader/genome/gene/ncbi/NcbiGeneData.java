package ubic.gemma.core.loader.genome.gene.ncbi;

import ubic.gemma.core.loader.genome.gene.ncbi.model.NCBIGene2Accession;
import ubic.gemma.core.loader.genome.gene.ncbi.model.NCBIGeneInfo;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Simple helper data structure that stores an NcbiGeneInfo and its associated
 * NcbiGene2Accession elements.
 *
 * @author jsantos
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class NcbiGeneData {
    private NCBIGeneInfo geneInfo;
    private Collection<NCBIGene2Accession> accessions;

    public NcbiGeneData() {
        geneInfo = null;
        accessions = new ArrayList<>();
    }

    public void addAccession( NCBIGene2Accession accession ) {
        this.accessions.add( accession );
    }

    public NCBIGeneInfo getGeneInfo() {
        return this.geneInfo;
    }

    public void setGeneInfo( NCBIGeneInfo geneInfo ) {
        this.geneInfo = geneInfo;
    }

    public Collection<NCBIGene2Accession> getAccessions() {
        return this.accessions;
    }

    @SuppressWarnings("unused") // Possible external use
    public void setAccessions( Collection<NCBIGene2Accession> accessions ) {
        this.accessions = accessions;
    }

}
