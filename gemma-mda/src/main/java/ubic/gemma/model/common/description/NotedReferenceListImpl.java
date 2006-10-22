/*
 * The Gemma project.
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
package ubic.gemma.model.common.description;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.common.description.NotedReferenceList
 */
public class NotedReferenceListImpl extends ubic.gemma.model.common.description.NotedReferenceList {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 4626465693739196455L;

    /**
     * @see ubic.gemma.model.common.description.NotedReferenceList#addReference(ubic.gemma.model.common.description.BibliographicReference)
     */
    @Override
    public void addReference( NotedReference bibliographicReference ) {
        this.getReferences().add( bibliographicReference );
    }

    /**
     * @see ubic.gemma.model.common.description.NotedReferenceList#removeReference(ubic.gemma.model.common.description.BibliographicReference)
     */
    @Override
    public void removeReference( NotedReference bibliographicReference ) {
        if ( bibliographicReference != null ) {
            this.getReferences().remove( bibliographicReference );
        } else {
            throw new UnsupportedOperationException(
                    "Removing a reference from a list is only supported for persistent instances" );
        }
    }

}