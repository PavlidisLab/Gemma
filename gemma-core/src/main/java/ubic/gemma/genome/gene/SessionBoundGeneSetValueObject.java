
/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
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

package ubic.gemma.genome.gene;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.session.GemmaSessionBackedValueObject;
/**
 * TODO Document Me
 * 
 * @author tvrossum
 * @version $Id$
 */
public class SessionBoundGeneSetValueObject extends GeneSetValueObject implements GemmaSessionBackedValueObject {

    private static final long serialVersionUID = 5073203626044664184L;
    private boolean modified;
    
    /**
     * @param genesets
     * @param includeOnesWithoutGenes if true, even gene sets that lack genes will be included.
     * @return
     */
    public static Collection<GeneSetValueObject> convert2ValueObjects( Collection<GeneSet> genesets,
            boolean includeOnesWithoutGenes ) {
        List<GeneSetValueObject> results = new ArrayList<GeneSetValueObject>();

        for ( GeneSet gs : genesets ) {
            if ( !includeOnesWithoutGenes && gs.getMembers().isEmpty() ) {
                continue;
            }

            results.add( new SessionBoundGeneSetValueObject( gs ) );
        }

        Collections.sort( results, new Comparator<GeneSetValueObject>() {
            @Override
            public int compare( GeneSetValueObject o1, GeneSetValueObject o2 ) {
                return -o1.getSize().compareTo( o2.getSize() );
            }
        } );
        return results;
    }
    /**
     * default constructor to satisfy java bean contract
     */
    public SessionBoundGeneSetValueObject() {
        super();
        this.setModified( false );
    }

    /**
     * Constructor to build value object from GeneSet
     * 
     * @param gs
     */
    public SessionBoundGeneSetValueObject( GeneSet gs ) {
        super(gs);
        this.setModified( false );
    }
    
    /* (non-Javadoc)
     * @see ubic.gemma.web.genome.gene.GeneSetValueObject#isSessionBound()
     */
    public boolean isSessionBound() {
        return true;
    }
    /**
     * @param modified the modified to set
     */
    @Override
    public void setModified( boolean modified ) {
        this.modified = modified;
    }
    /**
     * @return the modified
     */
    @Override
    public boolean isModified() {
        return modified;
    }

    @Override
    public boolean equals( GemmaSessionBackedValueObject ervo ) {
        if(ervo.getClass().equals( this.getClass() ) && ervo.getId().equals( this.getId() )){
            return true;
        }
       return false;
    }
    @Override
    public Collection<Long> getMemberIds() {
        return this.getGeneIds();
    } 

}
