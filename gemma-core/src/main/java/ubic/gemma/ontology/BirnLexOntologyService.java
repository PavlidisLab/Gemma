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

package ubic.gemma.ontology;

import java.util.Collection;

import org.springframework.stereotype.Service;

import ubic.gemma.util.ConfigUtils;

import com.hp.hpl.jena.ontology.OntModel;

/**
 * Holds a complete copy of the BirnLex Ontology. This gets loaded on startup. BirnLex is small, only about 1500 terms
 * (as of 12/2007) so we can hold it in memory.
 * 
 * @author klc
 * @version $Id: BirnLexOntologyService.java
 */
@Service
public class BirnLexOntologyService extends AbstractOntologyService {

    private static final String BIRNLEX_ONTOLOGY_URL = "url.birnlexOntology";

    // FIXME: This is a hack to fix a problem with the birnlex ontology returning Organ as a child of liver.
    // See BUG: http://www.chibi.ubc.ca/faculty/pavlidis/bugs/show_bug.cgi?id=1550
    // Might be better to remove organ from all search results
    @Override
    public Collection<OntologyTerm> findTerm( String search ) {

        Collection<OntologyTerm> results = super.findTerm( search );

        if ( search.equalsIgnoreCase( "liver" ) ) {
            for ( OntologyTerm ont : results ) {
                if ( ont.getTerm().equalsIgnoreCase( "Organ" ) ) {
                    log.info( "Liver condition met. Removing organ from results. Exact Term is: " + ont );
                    results.remove( ont );
                    break;
                }
            }
        }

        return results;

    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.ontology.AbstractOntologyService#getOntologyName()
     */
    @Override
    protected String getOntologyName() {
        return "birnlexOntology";
    }

    @Override
    protected String getOntologyUrl() {
        return ConfigUtils.getString( BIRNLEX_ONTOLOGY_URL );
    }

    @Override
    protected OntModel loadModel( String url ) {
        return OntologyLoader.loadMemoryModel( url );
    }
}
