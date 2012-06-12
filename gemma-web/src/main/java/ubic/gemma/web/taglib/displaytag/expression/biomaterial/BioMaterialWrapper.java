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
package ubic.gemma.web.taglib.displaytag.expression.biomaterial;

import java.util.Collection;
import org.displaytag.decorator.TableDecorator;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * Used to generate hyperlinks in displaytag tables.
 * <p>
 * See http://displaytag.sourceforge.net/10/tut_decorators.html and http://displaytag.sourceforge.net/10/tut_links.html
 * for explanation of how this works.
 * 
 * @author keshav
 * @version $Id$
 */
@Deprecated
public class BioMaterialWrapper extends TableDecorator {

    /**
     * @return String
     */
    public String getTreatmentsLink() {
        BioMaterial object = ( BioMaterial ) getCurrentRowObject();
        if ( object.getTreatments() != null ) {
            return "<a href=\"/Gemma/biomaterial/showBioMaterial.html?id=" + object.getId() + "\">"
                    + object.getTreatments().size() + "</a>";
        }
        return "No treatments";
    }

    public String getFactorList() {
        String factorValueString = "";
        BioMaterial object = ( BioMaterial ) getCurrentRowObject();
        Collection<FactorValue> factorValues = object.getFactorValues();

        for ( FactorValue value : factorValues ) {
            if ( value.getCharacteristics().size() > 0 ) {
                for ( Characteristic c : value.getCharacteristics() ) {
                    factorValueString += c.getValue() + "<br>";
                }

            } else {
                factorValueString += value.getValue() + "<br>";
            }
        }
        return factorValueString;
    }

    public String getNameLink() {
        BioMaterial object = ( BioMaterial ) getCurrentRowObject();

        String nameLink = "<a href='/Gemma/bioMaterial/showBioMaterial.html?id=" + object.getId() + "'>"
                + object.getName() + "</a>";
        return nameLink;
    }
}
