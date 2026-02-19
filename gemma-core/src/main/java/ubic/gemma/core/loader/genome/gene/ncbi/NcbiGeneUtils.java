package ubic.gemma.core.loader.genome.gene.ncbi;

import static ubic.gemma.core.util.StringUtils.urlEncode;

/**
 * @author poirigui
 */
public class NcbiGeneUtils {

    public static String getUri( String geneId ) {
        return "https://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene&cmd=Retrieve&dopt=full_report&list_uids=" + urlEncode( geneId );
    }
}
