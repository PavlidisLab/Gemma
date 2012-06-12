/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.web.taglib.displaytag.expression.designElement;

import java.text.NumberFormat;
import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.displaytag.decorator.TableDecorator;

import ubic.gemma.analysis.sequence.GeneMappingSummary;
import ubic.gemma.model.genome.gene.GeneProductValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResultValueObject;
import ubic.gemma.web.util.LinkUtils;

/**
 * Used to generate hyperlinks in displaytag tables.
 * <p>
 * See http://displaytag.sourceforge.net/10/tut_decorators.html and http://displaytag.sourceforge.net/10/tut_links.html
 * for explanation of how this works.
 * 
 * @author joseph
 * @version $Id$
 */
@Deprecated
public class CompositeSequenceWrapper extends TableDecorator {

    static NumberFormat nf = NumberFormat.getNumberInstance();
    static {
        nf.setMaximumFractionDigits( 3 );
    }

    /**
     * @return
     */
    public String getBlatIdentity() {
        GeneMappingSummary object = ( GeneMappingSummary ) getCurrentRowObject();
        BlatResultValueObject blatResult = object.getBlatResult();
        String retVal = "";
        if ( blatResult.getIdentity() != null ) {
            retVal += nf.format( blatResult.getIdentity() );
        }
        return retVal;
    }

    /**
     * @return
     */
    public String getBlatResult() {
        GeneMappingSummary object = ( GeneMappingSummary ) getCurrentRowObject();
        BlatResultValueObject blatResult = object.getBlatResult();
        String retVal = "Chr. ";
        retVal += blatResult.getTargetChromosomeName();
        retVal += " : ";
        retVal += blatResult.getTargetStart().toString() + "-";
        retVal += blatResult.getTargetEnd().toString();
        retVal += "<a target='_blank' href='" + LinkUtils.getGenomeBrowserLink( blatResult ) + "'><img src=\""
                + LinkUtils.UCSC_ICON + "\" alt=\"Genome browser view (opens in new window)\" /></a>";
        return retVal;
    }

    /**
     * @return
     */
    public String getBlatScore() {
        GeneMappingSummary object = ( GeneMappingSummary ) getCurrentRowObject();
        BlatResultValueObject blatResult = object.getBlatResult();
        String retVal = "";
        if ( blatResult.getScore() != null ) {
            retVal += nf.format( blatResult.getScore() );
        }
        return retVal;
    }

    /**
     * @return
     */
    public String getGeneProducts() {
        GeneMappingSummary object = ( GeneMappingSummary ) getCurrentRowObject();
        Collection<GeneProductValueObject> geneProducts = object.getGeneProducts();

        if ( geneProducts == null || geneProducts.size() == 0 ) {
            return "[none]";
        }

        String retVal = "";
        for ( GeneProductValueObject product : geneProducts ) {
            String ncbiLink = LinkUtils.getNcbiUrl( product );
            String fullName = product.getName();
            String shortName = StringUtils.abbreviate( fullName, 20 );
            if ( product.getNcbiId() != null ) {
                retVal += "&nbsp;&nbsp;<span title='" + fullName + "'>" + shortName
                        + "</span>&nbsp;<a target='_blank' href='" + ncbiLink + "'><img height=10 width=10 src=\""
                        + LinkUtils.NCBI_ICON + "\" alt=\"NCBI\" /></a><br />";
            } else {
                retVal += "&nbsp;&nbsp;<span title='" + fullName + "'>" + shortName + "</span><br />";
            }
        }
        return retVal;
    }

    /**
     * @return
     */
    public String getGenes() {
        GeneMappingSummary object = ( GeneMappingSummary ) getCurrentRowObject();
        Collection<GeneProductValueObject> geneProducts = object.getGeneProducts();

        if ( geneProducts == null || geneProducts.size() == 0 ) {
            return "[none]";
        }

        String retVal = "";
        for ( GeneProductValueObject product : geneProducts ) {
            GeneValueObject gene = object.getGene( product );
            String shortName = StringUtils.abbreviate( gene.getOfficialSymbol(), 20 );
            if ( gene.getNcbiId() != null ) {
                retVal += "<span title='" + gene.getOfficialSymbol() + "'>" + shortName
                        + "</span>&nbsp;<a target='_blank' href=\"" + LinkUtils.getNcbiUrl( gene )
                        + "\"><img height=10 width=10 src=\"" + LinkUtils.NCBI_ICON + "\" alt=\"NCBI\" /></a>&nbsp;"
                        + LinkUtils.getGemmaGeneLink( gene ) + "<br />";
            } else {
                retVal += "<span title='" + gene.getOfficialSymbol() + "'>" + shortName + "</span>" + "&nbsp;"
                        + LinkUtils.getGemmaGeneLink( gene ) + "<br />";
            }

        }
        return retVal;
    }

}
