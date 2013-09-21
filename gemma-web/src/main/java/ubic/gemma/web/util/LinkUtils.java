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

import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.model.genome.gene.GeneProductType;
import ubic.gemma.model.genome.gene.GeneProductValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResultValueObject;
import ubic.gemma.util.Settings;

/**
 * Methods to generate links and/or urls to common resources.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class LinkUtils {

    public static final String UCSC_ICON = "/Gemma/images/logo/ucsc.gif";

    public static final String NCBI_ICON = "/Gemma/images/logo/ncbi.gif";

    public static String getGemmaGeneLink( GeneValueObject gene ) {
        return "<a target='_blank' href='/Gemma/gene/showGene.html?id=" + gene.getId()
                + "'><img height=10 width=10 src='/Gemma/images/logo/gemmaTiny.gif'></a>";
    }

    /**
     * @param blatResult
     * @return URL to the genome browser for the given blat result, or null if the URL cannot be formed correctly.
     */
    public static String getGenomeBrowserLink( BlatResultValueObject blatResult ) {

        if ( ( blatResult.getQuerySequence() == null ) || ( blatResult.getQuerySequence().getTaxon() == null ) )
            return null;

        TaxonValueObject taxon = blatResult.getQuerySequence().getTaxon();
        String organism = taxon.getCommonName();

        String database = "";
        if ( organism.equalsIgnoreCase( "Human" ) ) {
            database = Settings.getString( "gemma.goldenpath.db.human" );
        } else if ( organism.equalsIgnoreCase( "Rat" ) ) {
            database = Settings.getString( "gemma.goldenpath.db.rat" );
        } else if ( organism.equalsIgnoreCase( "Mouse" ) ) {
            database = Settings.getString( "gemma.goldenpath.db.mouse" );
        } else {
            return null;
        }

        String link = "http://genome.ucsc.edu/cgi-bin/hgTracks?org=" + organism + "&pix=850" + "&db=" + database
                + "&hgt.customText=" + Settings.getBaseUrl() + "blatTrack.html?id=";
        link += blatResult.getId();

        return link;
    }

    /**
     * @param product
     * @return
     */
    public static String getNcbiUrl( GeneProductValueObject product ) {
        String ncbiLink = "";
        if ( product.getType().equals( GeneProductType.RNA.getValue() ) ) {
            ncbiLink = "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=Nucleotide&cmd=search&term=";
        } else {
            // assume protein
            ncbiLink = "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=Protein&cmd=search&term=";

        }
        return ncbiLink + product.getNcbiId();
    }

    public static String getNcbiUrl( GeneValueObject gene ) {
        return "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene&cmd=Retrieve&dopt=full_report&list_uids="
                + gene.getNcbiId();
    }

}
