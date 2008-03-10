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
package ubic.gemma.web.taglib.displaytag.coexpressionSearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.displaytag.decorator.TableDecorator;

import ubic.gemma.model.analysis.expression.coexpression.CoexpressionValueObject;
import ubic.gemma.model.analysis.expression.coexpression.CommonCoexpressionValueObject;
import ubic.gemma.model.genome.Gene;

/**
 * Used to generate hyperlinks in displaytag tables.
 * <p>
 * See http://displaytag.sourceforge.net/10/tut_decorators.html and http://displaytag.sourceforge.net/10/tut_links.html
 * for explanation of how this works.
 * 
 * @author luke
 * @version $Id $
 */
public class MultipleCoexpressionWrapper extends TableDecorator {

    Log log = LogFactory.getLog( this.getClass() );

    /**
     * @return String
     */
    public String getNameLink() {
        CommonCoexpressionValueObject object = ( CommonCoexpressionValueObject ) getCurrentRowObject();
        
        return getNameLink( null, true, object.getGeneId(), object.getGeneName() );
    }
    
    public String getNameLink(String linkClass, boolean coexpressionLink, Long geneId, String geneName) {
        CommonCoexpressionValueObject object = ( CommonCoexpressionValueObject ) getCurrentRowObject();
        StringBuffer link = new StringBuffer();

        // tmm-style coexpression search link

        // gene link to gemma
        link.append( "<a " );
        if ( linkClass != null ) {
            link.append( "class=\"" );
            link.append( linkClass );
            link.append( "\" " );
        }
        link.append( "href=\"/Gemma/gene/showGene.html?id=" );
        link.append( geneId );
        link.append( "\">" );
        link.append( StringUtils.abbreviate( geneName, 20 ) );
        link.append( "</a>" );

        // build the GET parameters
        if ( coexpressionLink )
            link.append( getCoexpressionLink( geneId, "<img src=\"/Gemma/images/logo/gemmaTiny.gif\" />" ) );
        
        return link.toString();
    }

    /**
     * Function to build a GET coexpression link, given a coexpressionValueObect
     * 
     * @param object the CoexpressionValueObject to build the link for
     * @param innerHTML The html to show as a link. This is typically an image link or a gene name.
     * @return
     */
    public String getCoexpressionLink( Long geneId, String innerHTML ) {
        StringBuffer link = new StringBuffer();
        Collection<String> paramList = new ArrayList<String>();
        Collection<String> includeList = new ArrayList<String>();
        // include just taxon params
        includeList.add( "taxon" );
        includeList.add( "stringency" );
        includeList.add( "eeSearchString" );
        extractParameters( paramList, includeList );
        // add in the current gene with exactSearch
        // paramList.add( "searchString=" + object.getGeneName() );
        paramList.add( "id=" + geneId );
        paramList.add( "exactSearch=on" );

        // put in the tmm link
        link.append( "&nbsp;" );
        link.append( "<a href=\"/Gemma/searchCoexpression.html?" );
        link.append( StringUtils.join( paramList.toArray(), "&" ) );
        link.append( "\">" + innerHTML + "</a>" );

        return link.toString();
    }

    public String getLinkCount() {
        String count = "";
        CommonCoexpressionValueObject object = ( CommonCoexpressionValueObject ) getCurrentRowObject();
        Integer positiveLinks = object.getEEContributing2PositiveLinks().size();
        Integer negativeLinks = object.getEEContributing2NegativeLinks().size();

        if ( positiveLinks != null && positiveLinks != 0 ) {
            count += "<span class='positiveLink' >";
            count += positiveLinks.toString();
//            if ( !object.getEEContributing2PositiveLinks().isEmpty() )
//                count += getNonSpecificString( object, object.getEEContributing2PositiveLinks(), positiveLinks );
            count += "</span>";
        }

        if ( negativeLinks != null && negativeLinks != 0 ) {
            if ( count.length() > 0 ) {
                count += "/";
            }
            count += "<span class='negativeLink' >";
            count += negativeLinks.toString();
//            if ( !object.getEEContributing2NegativeLinks().isEmpty() )
//                count += getNonSpecificString( object, object.getEEContributing2NegativeLinks(), negativeLinks );
            count += "</span>";
        }
        return count;
    }
    
    public String getCoexpressedQueryGenes() {
        StringBuffer buf = new StringBuffer();
        CommonCoexpressionValueObject object = ( CommonCoexpressionValueObject ) getCurrentRowObject();
        Collection<Gene> positiveGenes = object.getCommonPositiveCoexpressedQueryGenes();
        Collection<Gene> negativeGenes = object.getCommonNegativeCoexpressedQueryGenes();
        
        if ( ! positiveGenes.isEmpty() ) {
            for ( Iterator iter = positiveGenes.iterator(); iter.hasNext(); ) {
                Gene gene = (Gene)iter.next();
                buf.append( getNameLink( "positiveLink", false, gene.getId(), gene.getName() ) );
                if ( iter.hasNext() )
                    buf.append( ", " );
            }
        }
        
        if ( ! negativeGenes.isEmpty() ) {
            for ( Iterator iter = negativeGenes.iterator(); iter.hasNext(); ) {
                Gene gene = (Gene)iter.next();
                buf.append( getNameLink( "negativeLink", false, gene.getId(), gene.getName() ) );
                if ( iter.hasNext() )
                    buf.append( ", " );
            }
        }
        
        return buf.toString();
    }

    /**
     * Function to build a GET coexpression link
     * 
     * @return
     */
    private String getCoexpressionLink() {
        CommonCoexpressionValueObject object = ( CommonCoexpressionValueObject ) getCurrentRowObject();
        return getCoexpressionLink( object.getGeneId(), "<img src=\"/Gemma/images/logo/gemmaTiny.gif\" />" );
    }

    public String getSimpleLinkCount() {
        return getLinkCount();
    }

    public String getExperimentBitList() {
        CoexpressionValueObject object = ( CoexpressionValueObject ) getCurrentRowObject();
        return object.getExperimentBitList();
    }

    public String getExperimentBitImage() {
        CommonCoexpressionValueObject object = ( CommonCoexpressionValueObject ) getCurrentRowObject();

        // wanted to put the EE short names in the title but sadly all the short names are null at this point.
        // Not worth the effort to put it in as we have to edit some hql queries. ;(
        // String eeList = "";
        // for(ExpressionExperimentValueObject evo : object.getExpressionExperimentValueObjects())
        // eeList += StringUtils.abbreviate(evo.getShortName(),6) + ", ";
        //        
        // if ( eeList.length() > 1 )
        // eeList= eeList.substring( 0, eeList.length() - 2 ); // remove trailing ' ,'
        // title='" + eeList + "'

        int width = object.getExperimentBitList().length() - 1; // probably okay
        return "<span style=\"background-color:#DDDDDD;\"><img src=\"/Gemma/spark?type=bar&width=" + width
                + "&height=10&color=black&spacing=0&data=" + object.getExperimentBitList() + "\" /></span>";
    }

//    private String getNonSpecificString( CommonCoexpressionValueObject object, Collection<Long> contributingEE,
//            Integer numTotalLinks ) {
//
//        Collection<Long> allNonSpecificEE = object.getNonspecificEE();
//        Collection<String> nonSpecificGenes = object.getNonSpecificGenes();
//        boolean hybridizesWithQueryGene = object.isHybridizesWithQueryGene();
//        String coexpressedGeneName = object.getGeneName();
//
//        int nonSpecific = 0;
//        String nonSpecificList = "";
//        String hybridizes = "";
//
//        for ( Long id : contributingEE ) {
//            if ( allNonSpecificEE.contains( id ) ) {
//                nonSpecific++;
//            }
//        }
//
//        for ( String gene : nonSpecificGenes ) {
//            if ( !gene.equalsIgnoreCase( coexpressedGeneName ) ) // no point in displaying itself
//                nonSpecificList += StringUtils.abbreviate( gene, 8 ) + " ,";
//        }
//        if ( nonSpecificList.length() > 1 )
//            nonSpecificList = nonSpecificList.substring( 0, nonSpecificList.length() - 2 ); // remove trailing ' ,'
//
//        if ( hybridizesWithQueryGene ) hybridizes = "*";
//
//        if ( nonSpecific == 0 ) return "";
//
//        return "<i> <span style = 'font-size:smaller' title='" + nonSpecificList + "' >  ( "
//                + ( numTotalLinks - nonSpecific ) + hybridizes + " )  </span> </i>";
//    }

    /**
     * Function for extracting the GET parameter list, excluding the parameters in excludeList
     * 
     * @param paramList
     * @param excludeList
     */
    private void extractParameters( Collection<String> paramList, Collection<String> excludeList ) {
        Set params = this.getPageContext().getRequest().getParameterMap().entrySet();
        for ( Object param : params ) {
            Map.Entry entry = ( Map.Entry ) param;
            if ( !isInIncludeList( excludeList, entry ) ) {
                continue;
            }
            String value = ( ( String[] ) entry.getValue() )[0];
            paramList.add( entry.getKey() + "=" + value );
        }
    }

    /**
     * Helper function. Checks to see if the entry is in the given include list
     * 
     * @param excludeList
     * @param entry
     * @return
     */
    private boolean isInIncludeList( Collection<String> includeList, Map.Entry entry ) {
        for ( String include : includeList ) {
            if ( ( ( String ) entry.getKey() ).equals( include ) ) {
                return true;
            }
        }
        return false;
    }
}