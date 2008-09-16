/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.displaytag.decorator.TableDecorator;

import ubic.gemma.model.analysis.expression.coexpression.CoexpressionValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.util.AnchorTagUtil;

/**
 * Displaytag decorator for Link analysis results display.
 * 
 * @author jsantos, klc, luke
 * @version $Id$
 */
public class CoexpressionWrapper extends TableDecorator {

    private static final String GEMMA_ICON = "<img src=\"/Gemma/images/logo/gemmaTiny.gif\" />";
    Log log = LogFactory.getLog( this.getClass() );

    /**
     * Function to build a GET coexpression link
     * 
     * @return
     */
    public String getCoexpressionLink() {
        CoexpressionValueObject object = ( CoexpressionValueObject ) getCurrentRowObject();
        return getCoexpressionLink( object, GEMMA_ICON );
    }

    /**
     * Function to build a GET coexpression link, given a coexpressionValueObect
     * 
     * @param object the CoexpressionValueObject to build the link for
     * @param innerHTML The html to show as a link. This is typically an image link or a gene name.
     * @return
     */
    public String getCoexpressionLink( CoexpressionValueObject object, String innerHTML ) {
        StringBuffer link = new StringBuffer();
        Collection<String> paramList = new ArrayList<String>();
        Collection<String> includeList = new ArrayList<String>();
        // include just taxon params
        includeList.add( "stringency" );
        includeList.add( "eeSearchString" );
        extractParameters( paramList, includeList );
        // add in the current gene with exactSearch
        // paramList.add( "searchString=" + object.getGeneName() );
        paramList.add( "id=" + object.getGeneId() );
        paramList.add( "taxon=" + object.getTaxonId() );
        paramList.add( "exactSearch=on" );

        // put in the tmm link
        link.append( "&nbsp;" );
        link.append( "<a href=\"/Gemma/searchCoexpression.html?" );
        link.append( StringUtils.join( paramList.toArray(), "&" ) );
        link.append( "\">" + innerHTML + "</a>" );

        return link.toString();
    }

    /**
     * Function to return the number of data sets for a coexpression match
     * 
     * @return the data set count column
     */
    public String getDataSetCount() {
        String count;
        CoexpressionValueObject object = ( CoexpressionValueObject ) getCurrentRowObject();
        count = ( new Integer( object.getExpressionExperimentValueObjects().size() ) ).toString();
        return count;
    }

    /**
     * Function to return the data sets for a coexpression match
     * 
     * @return the data set column
     */
    @SuppressWarnings("unchecked")
    public String getDataSets() {
        CoexpressionValueObject object = ( CoexpressionValueObject ) getCurrentRowObject();
        Collection<ExpressionExperimentValueObject> ees = object.getExpressionExperimentValueObjects();
        Collection<String> dsLinks = new ArrayList<String>();
        for ( ExpressionExperimentValueObject ee : ees ) {
            String link = "<a " + "title=\"" + ee.getName() + "\" "
                    + "href=\"/Gemma/expressionExperiment/showExpressionExperiment.html?id=" + ee.getId() + "\">"
                    + ee.getShortName() + "</a>";
            dsLinks.add( link );
        }
        return StringUtils.join( dsLinks.toArray(), "," );
    }

    /**
     * Function to return the data sets for a coexpression match
     * 
     * @return the data set column
     */
    @SuppressWarnings("unchecked")
    public String getDataSetsExport() {
        CoexpressionValueObject object = ( CoexpressionValueObject ) getCurrentRowObject();
        Collection<ExpressionExperimentValueObject> ees = object.getExpressionExperimentValueObjects();
        Collection<String> dsNames = new ArrayList<String>();
        for ( ExpressionExperimentValueObject ee : ees ) {
            dsNames.add( ee.getShortName() );
        }
        return StringUtils.join( dsNames.toArray(), "," );
    }

    public String getExperimentBitImage() {
        CoexpressionValueObject object = ( CoexpressionValueObject ) getCurrentRowObject();

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
        int height = 10;

        StringBuffer buf = new StringBuffer();
        buf.append( "<span style=\"background-color:#DDDDDD;\"><img src=\"/Gemma/spark?type=bar&width=" );
        buf.append( width );
        buf.append( "&height=" );
        buf.append( height );
        buf.append( "&color=black&spacing=0&data=" );
        buf.append( object.getExperimentBitList() );
        buf.append( "\" usemap=\"" );
        buf.append( object.getImageMapName() );
        buf.append( "\" /></span>" );
        buf.append( "<map name=\"" );
        buf.append( object.getImageMapName() );
        buf.append( "\">" );
        int x = 0, y = 0, barWidth = 2;
        for ( Long eeId : object.getExperimentBitIds() ) {
            if ( eeId != 0 ) {
                ExpressionExperimentValueObject eevo = object.getExpressionExperimentValueObject( eeId );
                buf.append( String.format(
                        "<area shape=\"rect\" coords=\"%d,%d,%d,%d\" href=\"%s\" alt=\"%s\" title=\"%s\" />", x, y, x
                                + barWidth, height - 1, AnchorTagUtil.getExpressionExperimentUrl( eeId ), eevo
                                .getName(), eevo.getName() ) );
            }
            x += barWidth;
        }
        buf.append( "</map>" );

        return buf.toString();
        // return "<span style=\"background-color:#DDDDDD;\"><img src=\"/Gemma/spark?type=bar&width=" + width
        // + "&height=10&color=black&spacing=0&data=" + object.getExperimentBitList() + "\" /></span>";
    }

    /**
     * @return
     */
    public String getExperimentBitList() {
        CoexpressionValueObject object = ( CoexpressionValueObject ) getCurrentRowObject();
        return object.getExperimentBitList();
    }

    /**
     * @return
     */
    public String getGemmaLink() {
        CoexpressionValueObject object = ( CoexpressionValueObject ) getCurrentRowObject();
        return getCoexpressionLink( object, GEMMA_ICON );
    }

    /**
     * @return
     */
    public String getGoOverlap() {
        CoexpressionValueObject cvo = ( CoexpressionValueObject ) getCurrentRowObject();
        if ( ( cvo.getGoOverlap() == null ) || cvo.getPossibleOverlap() == 0 ) return "-";

        String overlap = "" + cvo.getGoOverlap().size() + "/" + cvo.getPossibleOverlap() + "";
        return overlap;
    }

    /**
     * Function to return the number of data sets for a coexpression match
     * 
     * @return the data set count column
     */
    public String getLinkCount() {
        String count = "";
        CoexpressionValueObject object = ( CoexpressionValueObject ) getCurrentRowObject();
        int positiveLinks = object.getPositiveLinkSupport();
        int negativeLinks = object.getNegativeLinkSupport();
        int numEesTestedIn = object.getNumDatasetsTestedIn();

        if ( positiveLinks > 0 ) {
            count += "<span class='positiveLink' >";
            count += positiveLinks;

            if ( !object.getExpressionExperiments().isEmpty() )
                count += getNonSpecificString( object, object.getEEContributing2PositiveLinks(), positiveLinks );

            count += "</span>";
        }

        if ( negativeLinks > 0 ) {
            if ( count.length() > 0 ) {
                count += " ";
            }
            count += "<span class='negativeLink' >";
            count += negativeLinks;
            if ( !object.getExpressionExperiments().isEmpty() )
                count += getNonSpecificString( object, object.getEEContributing2NegativeLinks(), negativeLinks );

            count += "</span>";
        }

        count += " / " + numEesTestedIn;

        return count;
    }

    /**
     * Function to return the number of data sets for a coexpression match
     * 
     * @return the data set count column
     */
    public String getLinkCountExport() {
        StringBuffer buf = new StringBuffer();
        CoexpressionValueObject object = ( CoexpressionValueObject ) getCurrentRowObject();
        int positiveLinks = object.getPositiveLinkSupport();
        int negativeLinks = object.getNegativeLinkSupport();

        if ( positiveLinks > 0 ) {
            buf.append( positiveLinks );
            if ( !object.getExpressionExperiments().isEmpty() )
                buf
                        .append( getNonSpecificStringExport( object, object.getEEContributing2PositiveLinks(),
                                positiveLinks ) );
        }

        if ( negativeLinks > 0 ) {
            if ( buf.length() > 0 ) buf.append( " / " );
            buf.append( negativeLinks );
            if ( !object.getExpressionExperiments().isEmpty() )
                buf
                        .append( getNonSpecificStringExport( object, object.getEEContributing2NegativeLinks(),
                                negativeLinks ) );
        }

        return buf.toString();
    }

    /**
     * @return String
     */
    public String getNameLink() {
        CoexpressionValueObject object = ( CoexpressionValueObject ) getCurrentRowObject();
        StringBuffer link = new StringBuffer();

        // tmm-style coexpression search link

        // gene link to gemma
        link.append( "<a href=\"/Gemma/gene/showGene.html?id=" );
        link.append( object.getGeneId() );
        link.append( "\">" );
        link.append( StringUtils.abbreviate( object.getGeneName(), 20 ) );
        link.append( "</a>" );

        return link.toString();
    }

    public String getSimpleLinkCount() {
        String count = "";
        CoexpressionValueObject object = ( CoexpressionValueObject ) getCurrentRowObject();
        int positiveLinks = object.getPositiveLinkSupport();
        int negativeLinks = object.getNegativeLinkSupport();

        if ( positiveLinks > 0 ) {
            count += "<span class='positiveLink' >";
            count += positiveLinks;

            count += "</span>";
        }

        if ( negativeLinks > 0 ) {
            if ( count.length() > 0 ) {
                count += "/";
            }
            count += "<span class='negativeLink' >";
            count += negativeLinks;
            count += "</span>";
        }
        return count;
    }

    public String getSimpleLinkCountExport() {
        StringBuffer buf = new StringBuffer();
        CoexpressionValueObject object = ( CoexpressionValueObject ) getCurrentRowObject();
        Integer positiveLinks = object.getPositiveLinkSupport();
        Integer negativeLinks = object.getNegativeLinkSupport();

        if ( positiveLinks != null && positiveLinks != 0 ) {
            buf.append( positiveLinks );
        }

        if ( negativeLinks != null && negativeLinks != 0 ) {
            if ( buf.length() > 0 ) buf.append( " / " );
            buf.append( negativeLinks );
        }

        return buf.toString();
    }

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
     * @param cvo
     * @param contributingEE
     * @param numTotalLinks
     * @return
     */
    private String getNonSpecificString( CoexpressionValueObject cvo, Collection<Long> contributingEE, int numTotalLinks ) {

        Collection<Long> allNonSpecificEE = cvo.getNonspecificEE();
        Collection<Long> nonSpecificGenes = cvo.getCrossHybridizingGenes();
        boolean hybridizesWithQueryGene = cvo.isHybridizesWithQueryGene();

        int nonSpecific = 0;
        String nonSpecificList = "";
        String hybridizes = "";

        for ( Long id : contributingEE ) {
            if ( allNonSpecificEE.contains( id ) ) {
                nonSpecific++;
            }
        }

        if ( nonSpecificList.length() > 1 )
            nonSpecificList = nonSpecificList.substring( 0, nonSpecificList.length() - 2 ); // remove trailing ' ,'

        if ( hybridizesWithQueryGene ) hybridizes = "*";

        if ( nonSpecific == 0 ) return "";

        return "<i> <span style = 'font-size:smaller' title='" + nonSpecificList + "' >  ( "
                + ( numTotalLinks - nonSpecific ) + hybridizes + " )  </span> </i>";
    }

    /**
     * @param cvo
     * @param contributingEE
     * @param numTotalLinks
     * @return
     */
    private String getNonSpecificStringExport( CoexpressionValueObject cvo, Collection<Long> contributingEE,
            Integer numTotalLinks ) {

        Collection<Long> allNonSpecificEE = cvo.getNonspecificEE();
        boolean hybridizesWithQueryGene = cvo.isHybridizesWithQueryGene();

        int nonSpecific = 0;
        for ( Long id : contributingEE ) {
            if ( allNonSpecificEE.contains( id ) ) {
                nonSpecific++;
            }
        }

        StringBuffer buf = new StringBuffer();
        if ( nonSpecific > 0 ) {
            buf.append( " ( " );
            buf.append( numTotalLinks - nonSpecific );
            if ( hybridizesWithQueryGene ) buf.append( "*" );
            buf.append( " )" );
        }
        return buf.toString();
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