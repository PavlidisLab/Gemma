package ubic.gemma.core.loader.genome.gene.ncbi;

import lombok.Getter;
import lombok.Setter;
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
@Getter
@Setter
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

}
