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
package ubic.gemma.web.taglib.displaytag.expression.arrayDesign;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.displaytag.decorator.TableDecorator;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;

/**
 * Used to generate hyperlinks in displaytag tables.
 * <p>
 * See http://displaytag.sourceforge.net/10/tut_decorators.html and http://displaytag.sourceforge.net/10/tut_links.html
 * for explanation of how this works.
 * 
 * @author joseph
 * @version $Id$
 */
public class ArrayDesignWrapper extends TableDecorator {

    Log log = LogFactory.getLog( this.getClass() );

     
     public String getExpressionExperimentCountLink() {
         ArrayDesignValueObject object = ( ArrayDesignValueObject ) getCurrentRowObject();
         if (object.getExpressionExperimentCount() != null && object.getExpressionExperimentCount() > 0) {
             long id = object.getId();
         
             return object.getExpressionExperimentCount() + " <a href=\"showExpressionExperimentsFromArrayDesign.html?id=" + id + "\">"
                     + "<img src=\"/Gemma/images/magnifier.png\" height=10 width=10/></a>";
         }
        
         return "0";

     }
     
     public String getDelete() {
         ArrayDesignValueObject object = ( ArrayDesignValueObject ) getCurrentRowObject();

         if ( object == null ) {
             return "Array Design unavailable";
         }

         return "<form action=\"deleteArrayDesign.html?id=" + object.getId()
                 + "\" onSubmit=\"return confirmDelete('Array Design " + object.getName()
                 + "')\" method=\"post\"><input type=\"submit\"  value=\"Delete\" /></form>";

     }
     
     public String getColor() {
         ArrayDesignValueObject object = ( ArrayDesignValueObject ) getCurrentRowObject();
         
         if ( object == null ) {
             return "Array Design unavailable";
         }
         
         String colorString = "";
         if (object.getColor().equalsIgnoreCase( "ONECOLOR" )) {
             colorString = "one-color";
         }
         else if (object.getColor().equalsIgnoreCase( "TWOCOLOR" )) {
             colorString = "two-color";   
         }
         else if (object.getColor().equalsIgnoreCase( "DUALMODE" )) {
             colorString = "dual mode"; 
         }
         else {
             colorString = "No color";
         }
         return colorString;         
     }
     
}
