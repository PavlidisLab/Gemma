/**
 * 
 */
package ubic.gemma.web.taglib.displaytag.expression.designElement;

import java.text.NumberFormat;
import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.displaytag.decorator.TableDecorator;

import ubic.gemma.analysis.sequence.BlatResultGeneSummary;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneProductType;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;

/**
 * Used to generate hyperlinks in displaytag tables.
 * <p>
 * See http://displaytag.sourceforge.net/10/tut_decorators.html and http://displaytag.sourceforge.net/10/tut_links.html
 * for explanation of how this works.
 * 
 * @author joseph
 */
public class CompositeSequenceWrapper extends TableDecorator {

    /*
     * FIXME This should be factored out.
     */
    private static final String GEMMA_BASE_URL = "http://www.bioinformatics.ubc.ca/Gemma/";

    Log log = LogFactory.getLog( this.getClass() );
    static NumberFormat nf = NumberFormat.getNumberInstance();

    static {
        nf.setMaximumFractionDigits( 3 );
    }

    public String getBlatResult() {
        BlatResultGeneSummary object = ( BlatResultGeneSummary ) getCurrentRowObject();
        BlatResult blatResult = object.getBlatResult();
        String retVal = "Chr. ";
        retVal += blatResult.getTargetChromosome().getName();
        retVal += " : ";
        retVal += blatResult.getTargetStart().toString() + "-";
        retVal += blatResult.getTargetEnd().toString();
        retVal += "<a target='_blank' href='" + getGenomeBrowserLink( blatResult ) + "'>(view in genome browser)</a>";
        return retVal;
    }

    public String getBlatScore() {
        BlatResultGeneSummary object = ( BlatResultGeneSummary ) getCurrentRowObject();
        BlatResult blatResult = object.getBlatResult();
        String retVal = "";
        if ( blatResult.score() != null ) {
            retVal += nf.format( blatResult.score() );
        }
        return retVal;
    }

    public String getBlatIdentity() {
        BlatResultGeneSummary object = ( BlatResultGeneSummary ) getCurrentRowObject();
        BlatResult blatResult = object.getBlatResult();
        String retVal = "";
        if ( blatResult.identity() != null ) {
            retVal += nf.format( blatResult.identity() );
        }
        return retVal;
    }

    /**
     * @param blatResult
     * @return URL to the genome browser for the given blat result, or null if the URL cannot be formed correctly.
     */
    private String getGenomeBrowserLink( BlatResult blatResult ) {

        if ( ( blatResult.getQuerySequence() == null ) || ( blatResult.getQuerySequence().getTaxon() == null ) )
            return null;

        Taxon taxon = blatResult.getQuerySequence().getTaxon();
        String organism = taxon.getCommonName();

        // String database = taxon.getExternalDatabase().getName();

        String database = "hg18";
        if ( organism.equalsIgnoreCase( "Human" ) ) {
            database = "hg18";
        } else if ( organism.equalsIgnoreCase( "Rat" ) ) {
            database = "rn4";
        } else if ( organism.equalsIgnoreCase( "Mouse" ) ) {
            database = "mm8";
        }

        // build position if the biosequence has an accession
        // otherwise point to location
        // String position = "Chr";
        // position += blatResult.getTargetChromosome().getName();
        // position += ":";
        // position += blatResult.getTargetStart().toString() + "-";
        // position += blatResult.getTargetEnd().toString();
        //
        // // }
        // String link = "http://genome.ucsc.edu/cgi-bin/hgTracks?clade=vertebrate&org=" + organism + "&db=" + database
        // + "&position=" + position + "&pix=620";

        String link = "http://genome.ucsc.edu/cgi-bin/hgTracks?org=" + organism + "&pix=850" + "&db=" + database
                + "&hgt.customText=" + GEMMA_BASE_URL + "blatTrack.html?id=";
        link += blatResult.getId();

        return link;
    }

    public String getGeneProducts() {
        BlatResultGeneSummary object = ( BlatResultGeneSummary ) getCurrentRowObject();
        Collection<GeneProduct> geneProducts = object.getGeneProducts();
        String retVal = "";
        for ( GeneProduct product : geneProducts ) {
            String ncbiLink = "";
            if ( product.getType() == GeneProductType.RNA ) {
                ncbiLink = "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=Nucleotide&cmd=search&term=";
            } else {
                // assume protein
                ncbiLink = "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=Protein&cmd=search&term=";

            }
            String fullName = product.getName();
            String shortName = StringUtils.abbreviate( fullName, 20 );
            if ( product.getNcbiId() != null ) {
                retVal += "&nbsp;&nbsp;<span title='" + fullName + "'>" + shortName
                        + "</span><a target='_blank' href='" + ncbiLink + product.getNcbiId()
                        + "'><img height=10 width=10 src='/Gemma/images/logo/ncbi.gif' /></a><br />";
            } else {
                retVal += "&nbsp;&nbsp;<span title='" + fullName + "'>" + shortName + "</span><br />";
            }
        }
        return retVal;
    }

    public String getGenes() {
        BlatResultGeneSummary object = ( BlatResultGeneSummary ) getCurrentRowObject();
        Collection<GeneProduct> geneProducts = object.getGeneProducts();
        String retVal = "";
        for ( GeneProduct product : geneProducts ) {
            Collection<Gene> genes = object.getGenes( product );
            for ( Gene gene : genes ) {
                String shortName = StringUtils.abbreviate( gene.getOfficialSymbol(), 20 );
                if ( gene.getNcbiId() != null ) {
                    retVal += "<span title='"
                            + gene.getOfficialSymbol()
                            + "'>"
                            + shortName
                            + "</span><a target='_blank' href='http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene&cmd=Retrieve&dopt=full_report&list_uids="
                            + gene.getNcbiId() + "'><img height=10 width=10 src='/Gemma/images/logo/ncbi.gif' /></a>"
                            + "<a target='_blank' href='/Gemma/gene/showGene.html?id=" + gene.getId()
                            + "'><img height=10 width=10 src='/Gemma/images/logo/gemmaTiny.gif'></a><br />";
                } else {
                    retVal += "<span title='" + gene.getOfficialSymbol() + "'>" + shortName + "</span>"
                            + "<a target='_blank' href='/Gemma/gene/showGene.html?id=" + gene.getId()
                            + "'><img height=10 width=10 src='/Gemma/images/logo/gemmaTiny.gif'></a><br />";
                }
            }
        }
        return retVal;
    }
}
