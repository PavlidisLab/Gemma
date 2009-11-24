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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.description.ExternalDatabase;

/**
 * @author pavlidis
 * @version $Id$
 */
public abstract class AbstractOntologyResource implements OntologyResource {

    private static final long serialVersionUID = 1L;

    protected static Log log = LogFactory.getLog( OntologyPropertyImpl.class.getName() );

    protected ExternalDatabase sourceOntology;

    public int compareTo( Object other ) {
        OntologyResource r = ( OntologyResource ) other;
        return this.getUri().compareTo( r.getUri() );

    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        final AbstractOntologyResource other = ( AbstractOntologyResource ) obj;
        if ( getLabel() == null ) {
            if ( other.getLabel() != null ) return false;
        } else if ( !getLabel().equals( other.getLabel() ) ) return false;
        if ( getUri() == null ) {
            if ( other.getUri() != null ) return false;
        } else if ( !getUri().equals( other.getUri() ) ) return false;
        return true;
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.analysis.ontology.OntologyTerm#getSourceOntology()
     */
    public ExternalDatabase getSourceOntology() {
        return sourceOntology;
    }

    public abstract String getUri();

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ( ( getLabel() == null ) ? 0 : getLabel().hashCode() );
        result = PRIME * result + ( ( getUri() == null ) ? 0 : getUri().hashCode() );
        return result;
    }

}
