package ubic.gemma.web.taglib.displaytag.coexpressionSearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.displaytag.decorator.TableDecorator;

import ubic.gemma.model.coexpression.CoexpressionValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;

/**
 * Used to generate hyperlinks in displaytag tables.
 * <p>
 * See http://displaytag.sourceforge.net/10/tut_decorators.html and http://displaytag.sourceforge.net/10/tut_links.html
 * for explanation of how this works.
 *
 * @author jsantos
 * @version $Id $
 *  
 */
public class CoexpressionWrapper extends TableDecorator {

    Log log = LogFactory.getLog( this.getClass() );
    
    /**
     * @return String
     */
    public String getNameLink() {
        CoexpressionValueObject object = ( CoexpressionValueObject ) getCurrentRowObject();
        StringBuffer link = new StringBuffer();
        
        // gene link to gemma
        link.append( "<a href=\"/Gemma/gene/showGene.html?id=" );
        link.append( object.getGeneId() );
        link.append( "\">" );
        link.append( object.getGeneName() );
        link.append( "</a>" );
        
        // tmm-style coexpression search link
        
        // build the GET parameters

        Collection<String> paramList = new ArrayList<String>();
        Collection<String> excludeList = new ArrayList<String>();
        // exclude the submit, searchString, and exactSearch params
        excludeList.add( "submit" );
        excludeList.add( "searchString" );
        excludeList.add( "exactSearch" );
        extractParameters( paramList, excludeList );
        // add in the current gene with exactSearch
        paramList.add( "searchString=" + object.getGeneName() );
        paramList.add( "exactSearch=on" );
        
        // put in the tmm link
        link.append( "&nbsp;" );
        link.append( "<a href=\"/Gemma/searchCoexpression.html?" );
        link.append( StringUtils.join( paramList.toArray(), "&" ) );
        link.append( "\"><img src=\"/Gemma/images/logo/gemmaTiny.gif\" /></a>" );
        
        return link.toString();
    }
    
    /**
     * Function to return the number of data sets for a coexpression match
     * @return the data set count column
     */
    public String getDataSetCount() {
        String count;
        CoexpressionValueObject object = ( CoexpressionValueObject ) getCurrentRowObject();
        count = (new Integer(object.getExpressionExperimentValueObjects().size())).toString();
        return count; 
    }
    
    /**
     * Function to return the data sets for a coexpression match
     * @return the data set column
     */
    @SuppressWarnings("unchecked")
    public String getDataSets() {
        CoexpressionValueObject object = ( CoexpressionValueObject ) getCurrentRowObject();
        Collection<ExpressionExperimentValueObject> ees = object.getExpressionExperimentValueObjects();
        Collection<String> dsLinks = new ArrayList<String>();
        for ( ExpressionExperimentValueObject ee : ees ) {
            String link = "<a " +
                    "title=\"" +
                    ee.getName() +
                    "\" " +
                    "href=\"/Gemma/expressionExperiment/showExpressionExperiment.html?id=" +
                    ee.getId() + 
                    "\">" + 
                    ee.getShortName() + 
                    "</a>";
            dsLinks.add(link);
        }
        return StringUtils.join( dsLinks.toArray(), "," ); 
    }

    /**
     * Function for extracting the GET parameter list, excluding the parameters in excludeList
     * @param paramList
     * @param excludeList
     */
    private void extractParameters( Collection<String> paramList, Collection<String> excludeList ) {
        Set params = this.getPageContext().getRequest().getParameterMap().entrySet();
        for ( Object param : params ) {
            Map.Entry entry =  (Map.Entry) param;
            if (isInExcludeList( excludeList, entry )) {
                continue;
            }
            String[] values = (String[]) entry.getValue();
            for ( String string : values ) {
                paramList.add( entry.getKey() + "=" + string ); 
                // just use one parameter value
                break;
            }
        }
    }

    /**
     * Helper function. Checks to see if the entry is in the given exclude list
     * @param excludeList
     * @param entry
     * @return
     */
    private boolean isInExcludeList( Collection<String> excludeList, Map.Entry entry ) {
        for ( String exclude : excludeList ) {
            if ( ((String)entry.getKey()).equals( exclude ) ) {
                return true;
            }
        }
        return false;
    }
}
