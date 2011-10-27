/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.web.taglib.displaytag.gene;

import java.util.ArrayList;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.displaytag.decorator.TableDecorator;

import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.search.SearchResult;

/**
 * Used to generate hyperlinks in displaytag tables.
 * <p>
 * See http://displaytag.sourceforge.net/10/tut_decorators.html and http://displaytag.sourceforge.net/10/tut_links.html
 * for explanation of how this works.
 * 
 * @author jsantos
 * @version $Id $
 */
public class GeneFinderWrapper extends TableDecorator {

    /**
     * @return String
     */
    public String getTaxon() {
        SearchResult object = ( SearchResult ) getCurrentRowObject();
        Gene g = ( Gene ) object.getResultObject();
        return g.getTaxon().getScientificName();
    }

    public String getAccession() {
        DatabaseEntry object = ( DatabaseEntry ) getCurrentRowObject();
        return object.getAccession() + "." + object.getAccessionVersion();
    }

    public String getNcbiLink() {
        SearchResult object = ( SearchResult ) getCurrentRowObject();
        Gene g = ( Gene ) object.getResultObject();
        if ( g.getNcbiGeneId() == null ) return "";

        String ncbiLink = "<a target='_blank' href='http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene&cmd=Retrieve&dopt=full_report&list_uids="
                + g.getNcbiGeneId() + "'>  <img hight='10' width='10%' src=\"/Gemma/images/logo/ncbi.gif\" /></a>";
        return ncbiLink;
    }

    public String getGemmaLink() {
        SearchResult object = ( SearchResult ) getCurrentRowObject();
        String gemmaLink = "<a href='/Gemma/gene/showGene.html?id=" + object.getId()
                + "'><img src=\"/Gemma/images/logo/gemmaTiny.gif\" /> </a>";
        return gemmaLink;
    }

    public String getNameLink() {
        SearchResult object = ( SearchResult ) getCurrentRowObject();
        Gene g = ( Gene ) object.getResultObject();
        String nameLink = "<a href='/Gemma/gene/showGene.html?id=" + object.getId() + "'>" + g.getName()
                + "</a> &nbsp; &nbsp;" + getNcbiLink();
        return nameLink;
    }

    public String getOfficialName() {
        SearchResult object = ( SearchResult ) getCurrentRowObject();
        Gene g = ( Gene ) object.getResultObject();
        return g.getOfficialName();
    }

    public String getOfficialSymbol() {
        SearchResult object = ( SearchResult ) getCurrentRowObject();
        Gene g = ( Gene ) object.getResultObject();
        return g.getOfficialSymbol();
    }

    @SuppressWarnings("unchecked")
    public String getMatchesView() {
        Gene object = ( Gene ) getCurrentRowObject();
        Set<Gene> geneMatch = ( Set ) this.getPageContext().findAttribute( "geneMatch" );
        Set<Gene> aliasMatch = ( Set ) this.getPageContext().findAttribute( "aliasMatch" );
        Set<Gene> geneProductMatch = ( Set ) this.getPageContext().findAttribute( "geneProductMatch" );
        Set<Gene> bioSequenceMatch = ( Set ) this.getPageContext().findAttribute( "bioSequenceMatch" );
        ArrayList<String> matches = new ArrayList<String>();
        if ( geneMatch.contains( object ) ) {
            matches.add( "Symbol" );
            
        }
        if ( aliasMatch.contains( object ) ) {
            matches.add( "Alias" );
            
        }
        if ( geneProductMatch.contains( object ) ) {
            matches.add( "GeneProduct" );
            
        }
        if ( bioSequenceMatch.contains( object ) ) {
            matches.add( "BioSequence" );
            
        }
        return StringUtils.join( matches.toArray(), "," );
    }

}
