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

import org.displaytag.decorator.TableDecorator;

import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.genome.Gene;

/**
 * Used to generate hyperlinks in displaytag tables.
 * <p>
 * See http://displaytag.sourceforge.net/10/tut_decorators.html and http://displaytag.sourceforge.net/10/tut_links.html
 * for explanation of how this works.
 * 
 * @author jsantos
 * @version $Id $
 */
public class GeneWrapper extends TableDecorator {
    /**
     * @return String
     */
    public String getTaxon() {
        Gene object = ( Gene ) getCurrentRowObject();
        return object.getTaxon().getScientificName();
    }

    public String getAccession() {
        DatabaseEntry object = ( DatabaseEntry ) getCurrentRowObject();
        String accession = "";
        if ( object.getAccession() != null ) {
            accession += object.getAccession();
            if ( object.getAccessionVersion() != null ) {
                accession += object.getAccessionVersion();
            }
        }
        return accession;
    }

    public String getNcbiLink() {
        Gene object = ( Gene ) getCurrentRowObject();

        if ( object.getNcbiGeneId() == null ) return "";

        String ncbiLink = "<a href='http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene&cmd=Retrieve&dopt=full_report&list_uids="
                + object.getNcbiGeneId() + "'>(ncbi)</a>";
        return ncbiLink;
    }

    public String getGemmaLink() {
        Gene object = ( Gene ) getCurrentRowObject();
        String gemmaLink = "<a href='/Gemma/gene/showGene.html?id=" + object.getId() + "'>(gemma)</a>";
        return gemmaLink;
    }

    public String getNameLink() {
        Gene object = ( Gene ) getCurrentRowObject();
        String nameLink = object.getName() + getGemmaLink() + getNcbiLink();

        return nameLink;
    }

}
