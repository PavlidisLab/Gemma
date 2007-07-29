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
package ubic.gemma.security;

import java.util.HashSet;

import ubic.gemma.model.association.RelationshipImpl;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailImpl;
import ubic.gemma.model.common.description.DatabaseEntryImpl;
import ubic.gemma.model.common.description.LocalFileImpl;
import ubic.gemma.model.common.description.MedicalSubjectHeadingImpl;
import ubic.gemma.model.common.description.PublicationTypeImpl;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeImpl;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorImpl;
import ubic.gemma.model.expression.designElement.CompositeSequenceImpl;
import ubic.gemma.model.expression.experiment.FactorValueImpl;
import ubic.gemma.model.genome.GeneImpl;
import ubic.gemma.model.genome.TaxonImpl;
import ubic.gemma.model.genome.biosequence.BioSequenceImpl;
import ubic.gemma.model.genome.gene.GeneAliasImpl;
import ubic.gemma.model.genome.gene.GeneProductImpl;

/**
 * A datastructure to hold unsecured classes. These classes either do not implement Serializable or do not have acl
 * permissions applied to them directly.
 * <p>
 * Example: reporter - we secure the ArrayDesign, but not the CompositeSequence.
 * 
 * @author keshav
 * @version $Id$
 */
public class UnsecuredSet extends HashSet {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * For some types of objects, we don't put permissions on them directly, but on the containing object.
     * 
     * @param additionalClasses Additional classes to add to the initial set of unsecured classes.
     */
    @SuppressWarnings("unchecked")
    public UnsecuredSet( Class[] additionalClasses ) {
        // these are not of type Securable
        this.add( AuditTrailImpl.class );
        this.add( DatabaseEntryImpl.class );
        this.add( LocalFileImpl.class );
        this.add( TechnologyType.class );
        this.add( FactorValueImpl.class );
        this.add( MedicalSubjectHeadingImpl.class );
        this.add( PublicationTypeImpl.class );
        // these are securable but we don't use acls on them directly
        this.add( DesignElementDataVectorImpl.class );
        this.add( DatabaseEntryImpl.class );
        this.add( BioSequenceImpl.class );
        this.add( RelationshipImpl.class );
        this.add( CompositeSequenceImpl.class );
        this.add( TaxonImpl.class );
        this.add( GeneImpl.class );
        this.add( GeneProductImpl.class );
        this.add( GeneAliasImpl.class );
        this.add( QuantitationTypeImpl.class );

        for ( Class clazz : additionalClasses ) {
            this.add( clazz );
        }
    }
}
