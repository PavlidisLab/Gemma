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
package ubic.gemma.persistence;

import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.association.Gene2GOAssociationService;

/**
 * Persist objects like Gene2GOAssociation.
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.property name="gene2GOAssociationService" ref="gene2GOAssociationService"
 */
public class RelationshipPersister extends ExpressionPersister {

    private Gene2GOAssociationService gene2GOAssociationService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.util.persister.Persister#persist(java.lang.Object)
     */
    public Object persist( Object entity ) {
        if ( entity == null ) return null;

        if ( entity instanceof Gene2GOAssociation ) {
            return persistGene2GOAssociation( ( Gene2GOAssociation ) entity );
        }
        return super.persist( entity );

    }

    /**
     * @param gene2GOAssociationService the gene2GOAssociationService to set
     */
    public void setGene2GOAssociationService( Gene2GOAssociationService gene2GOAssociationService ) {
        this.gene2GOAssociationService = gene2GOAssociationService;
    }

    /**
     * @param association
     * @return
     */
    protected Gene2GOAssociation persistGene2GOAssociation( Gene2GOAssociation association ) {
        if ( association == null ) return null;
        if ( !isTransient( association ) ) return association;

        Gene2GOAssociation existing = gene2GOAssociationService.find( association );

        if ( existing != null ) {
            return existing;
        }

        association.setGene( persistGene( association.getGene() ) );
        association.setOntologyEntry( persistOntologyEntry( association.getOntologyEntry() ) );
        return gene2GOAssociationService.create( association );
    }

}
