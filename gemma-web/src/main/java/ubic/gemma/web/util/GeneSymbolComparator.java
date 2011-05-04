package ubic.gemma.web.util;

import java.util.Comparator;

import ubic.gemma.model.genome.Gene;

public class GeneSymbolComparator implements Comparator<Gene> {

    @Override
    public int compare( Gene o1, Gene o2 ) {
        return o1.getOfficialSymbol().compareToIgnoreCase( o2.getOfficialSymbol() );
    }

}
