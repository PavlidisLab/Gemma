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

import ubic.gemma.model.common.Securable;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeImpl;
import ubic.gemma.model.expression.bioAssayData.DataVector;
import ubic.gemma.model.expression.designElement.CompositeSequenceImpl;
import ubic.gemma.model.genome.GeneImpl;
import ubic.gemma.model.genome.biosequence.BioSequenceImpl;
import ubic.gemma.model.genome.gene.GeneProductImpl;

/**
 * A datastructure to hold {@link Securable} classes that are not to be secured directly.
 * <p>
 * Example: We secure the ArrayDesign, but not the CompositeSequence.
 * 
 * @author keshav
 * @version $Id$
 */
public class UnsecuredSecurableSet extends HashSet {

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
    public UnsecuredSecurableSet( Class[] additionalClasses ) {
        // these are Securable but we don't use acls on them directly
        this.add( BioSequenceImpl.class );
        this.add( CompositeSequenceImpl.class );
        this.add( GeneImpl.class );
        this.add( GeneProductImpl.class );
        this.add( QuantitationTypeImpl.class );
        this.add( DataVector.class );

        if ( additionalClasses != null ) {
            for ( Class clazz : additionalClasses ) {
                this.add( clazz );
            }
        }
    }
}
