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
package ubic.gemma.web.util;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneProductType;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.util.ConfigUtils;

/**
 * Methods to generate links and/or urls to common resources.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class LinkUtils {

    public static final String UCSC_ICON = "/Gemma/images/logo/ucsc.gif";

    public static final String NCBI_ICON = "/Gemma/images/logo/ncbi.gif";

    /**
     * @param blatResult
     * @return URL to the genome browser for the given blat result, or null if the URL cannot be formed correctly.
     */
    public static String getGenomeBrowserLink( BlatResult blatResult ) {

        if ( ( blatResult.getQuerySequence() == null ) || ( blatResult.getQuerySequence().getTaxon() == null ) )
            return null;

        Taxon taxon = blatResult.getQuerySequence().getTaxon();
        String organism = taxon.getCommonName();

        // String database = taxon.getExternalDatabase().getName();
        // FIXME get this from the taxon.
        String database = "hg18";
        if ( organism.equalsIgnoreCase( "Human" ) ) {
            database = "hg18";
        } else if ( organism.equalsIgnoreCase( "Rat" ) ) {
            database = "rn4";
        } else if ( organism.equalsIgnoreCase( "Mouse" ) ) {
            database = "mm8";
        }

        String link = "http://genome.ucsc.edu/cgi-bin/hgTracks?org=" + organism + "&pix=850" + "&db=" + database
                + "&hgt.customText=" + ConfigUtils.getBaseUrl() + "blatTrack.html?id=";
        link += blatResult.getId();

        return link;
    }

    /**
     * @param gene
     * @return
     */
    public static String getGemmaGeneLink( Gene gene ) {
        return "<a target='_blank' href='/Gemma/gene/showGene.html?id=" + gene.getId()
                + "'><img height=10 width=10 src='/Gemma/images/logo/gemmaTiny.gif'></a>";
    }

    /**
     * @param gene
     * @return
     */
    public static String getNcbiUrl( Gene gene ) {
        return "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene&cmd=Retrieve&dopt=full_report&list_uids="
                + gene.getNcbiId();
    }

    /**
     * @param product
     * @return
     */
    public static String getNcbiUrl( GeneProduct product ) {
        String ncbiLink = "";
        if ( product.getType() == GeneProductType.RNA ) {
            ncbiLink = "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=Nucleotide&cmd=search&term=";
        } else {
            // assume protein
            ncbiLink = "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=Protein&cmd=search&term=";

        }
        return ncbiLink + product.getNcbiId();
    }

}
