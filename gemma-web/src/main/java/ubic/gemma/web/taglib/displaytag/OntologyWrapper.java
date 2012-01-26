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
package ubic.gemma.web.taglib.displaytag;

import org.displaytag.decorator.TableDecorator;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.ontology.providers.GeneOntologyServiceImpl;

/**
 * This is for Gene Ontology.
 * <p>
 * See http://displaytag.sourceforge.net/10/tut_decorators.html and http://displaytag.sourceforge.net/10/tut_links.html
 * for explanation of how this works.
 * 
 * @author klc
 * @version $Id$
 */
public class OntologyWrapper extends TableDecorator {

    public String getAccession() {
        Characteristic oe = ( Characteristic ) getCurrentRowObject();

        return "<a target='_blank' href='http://amigo.geneontology.org/cgi-bin/amigo/go.cgi?view=details&query="
                + GeneOntologyServiceImpl.asRegularGoId( oe ) + "'>" + GeneOntologyServiceImpl.asRegularGoId( oe ) + "</a>";

    }

    public String getAspect() {
        VocabCharacteristic oe = ( VocabCharacteristic ) getCurrentRowObject();
        return GeneOntologyServiceImpl.getTermAspect( oe ).toString().toLowerCase();
    }

}
