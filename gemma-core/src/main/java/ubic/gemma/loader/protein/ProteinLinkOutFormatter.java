/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.loader.protein;

import ubic.gemma.model.common.description.DatabaseEntry;

/**
 * This class concentrates functionality for formating url links
 * to external websites which provide protein protein interaction data.
 * <p>
 * For example the string url can be appended with:
 * &limit=20 which increases the number of links dispayed on the string page.  
 * 
 * @author ldonnison
 * @version $Id$
 */
public class ProteinLinkOutFormatter {
    
    /**  Default number of interactions displayed by string. */
    private Integer numberOflinksToDisplayOnStringPage = 10;
    
    /**Name of parameter to pass in link to string */
    private static String limitParameter= "&limit=";
    
    /**
     * Method that creates a string url. The url is stored in the db as two parts
     * which need merging together that it url and accession id.
     * For a protein protein interaction there is no id so instead the id
     * has been strored as two ensembl protein ids merged together. The url has been stored 
     * in db as if only protein id is being passed as such to actually pass the two ids a s has to be added to the url.
     * identifier >identifiers. Thus s is added to the url.    
     * 
     * @param entry Database entry representing protein protein interaction
     * @return String formated url.
     */
    public String getStringProteinProteinInteractionLink(DatabaseEntry entry){  
        if(entry.getUri().endsWith( "=" )){
             return (entry.getUri().replaceFirst( "=", "s=" )).concat(entry.getAccession());
        }else{
            return entry.getUri().concat("s").concat(entry.getAccession());  
        }             
    }
    
    /**
     * Method to format url for string protein protein interaction.
     * Different parameters can be queried for, such as increasing number of links displayed on string page.
     * This method allows that number to be changed.
     * 
     * @param baseUrl reprsesenting base string url 
     * @param Number of links to display on page
     * @return String appended with extra value
     */
    public String addStringProteinProteinInteractionsLinkedNumberDefined(String baseURL, Integer numberOfLinksToDisplayOnString){      
        return baseURL.concat(limitParameter).concat( Integer.toString( numberOfLinksToDisplayOnString) );       
    }
        
    
    /**
     * @return the numberOflinksToDisplayOnStringPage
     */
    public Integer getNumberOflinksToDisplayOnStringPage() {
        return numberOflinksToDisplayOnStringPage;
    }

    /**
     *  
     * @param numberOflinksToDisplayOnStringPage the numberOflinksToDisplayOnStringPage to set
     */
    public void setNumberOflinksToDisplayOnStringPage( Integer numberOflinksToDisplayOnStringPage ) {
        this.numberOflinksToDisplayOnStringPage = numberOflinksToDisplayOnStringPage;
    }    

}
