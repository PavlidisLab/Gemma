/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.expression.designElement;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * @author keshav
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.expression.designElement.CompositeSequenceService
 */
@Service
public class CompositeSequenceServiceImpl extends
        ubic.gemma.model.expression.designElement.CompositeSequenceServiceBase {

    Log log = LogFactory.getLog( this.getClass() );

    @Override
    protected Integer handleCountAll() throws Exception {
        return this.getCompositeSequenceDao().countAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceServiceBase#handleCreate(java.util.Collection)
     */
    @Override
    @SuppressWarnings("unchecked")
    protected Collection<CompositeSequence> handleCreate( Collection compositeSequences ) throws Exception {
        return ( Collection<CompositeSequence> ) this.getCompositeSequenceDao().create( compositeSequences );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.designElement.CompositeSequenceServiceBase#handleCreate(ubic.gemma.model.expression
     * .designElement.CompositeSequence)
     */
    @Override
    protected CompositeSequence handleCreate( CompositeSequence compositeSequence ) throws Exception {
        return this.getCompositeSequenceDao().create( compositeSequence );
    }

    @Override
    protected CompositeSequence handleFind( CompositeSequence compositeSequence ) throws Exception {
        return this.getCompositeSequenceDao().find( compositeSequence );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.designElement.CompositeSequenceServiceBase#handleFindByBioSequence(ubic.gemma.model
     * .genome.biosequence.BioSequence)
     */
    @Override
    protected Collection handleFindByBioSequence( BioSequence bioSequence ) throws Exception {
        return this.getCompositeSequenceDao().findByBioSequence( bioSequence );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.designElement.CompositeSequenceServiceBase#handleFindByBioSequenceName(java.lang.
     * String)
     */
    @Override
    protected Collection handleFindByBioSequenceName( String name ) throws Exception {
        return this.getCompositeSequenceDao().findByBioSequenceName( name );
    }

    @Override
    protected Collection handleFindByGene( Gene gene ) throws Exception {
        return this.getCompositeSequenceDao().findByGene( gene );
    }

    @Override
    protected Collection handleFindByGene( Gene gene, ArrayDesign arrayDesign ) throws Exception {
        return this.getCompositeSequenceDao().findByGene( gene, arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.designElement.CompositeSequenceServiceBase#handleFindByName(ubic.gemma.model.expression
     * .arrayDesign.ArrayDesign, java.lang.String)
     */
    @Override
    protected CompositeSequence handleFindByName( ArrayDesign arrayDesign, String name ) throws Exception {
        return this.getCompositeSequenceDao().findByName( arrayDesign, name );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceServiceBase#handleFindByName(java.lang.String)
     */
    @Override
    protected Collection<CompositeSequence> handleFindByName( String name ) throws Exception {
        return this.getCompositeSequenceDao().findByName( name );
    }

    /*
     * Checks to see if the CompositeSequence exists in any of the array designs. If so, it is internally stored in the
     * collection of composite sequences as a {@link LinkedHashSet), preserving order based on insertion. (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.designElement.CompositeSequenceServiceBase#handleGetMatchingCompositeSequences(java
     * .lang.String[], java.util.Collection)
     */
    @Override
    protected Collection handleFindByNamesInArrayDesigns( Collection compositeSequenceNames, Collection arrayDesigns )
            throws Exception {
        LinkedHashMap<String, CompositeSequence> compositeSequencesMap = new LinkedHashMap<String, CompositeSequence>();

        Iterator iter = arrayDesigns.iterator();

        while ( iter.hasNext() ) {
            ArrayDesign arrayDesign = ( ArrayDesign ) iter.next();

            for ( Object obj : compositeSequenceNames ) {
                String name = ( String ) obj;
                name = StringUtils.trim( name );
                log.debug( "entered: " + name );
                CompositeSequence cs = this.findByName( arrayDesign, name );
                if ( cs != null && !compositeSequencesMap.containsKey( cs.getName() ) ) {
                    compositeSequencesMap.put( cs.getName(), cs );
                } else {
                    log.warn( "Composite sequence " + name + " does not exist.  Discarding ... " );
                }
            }
        }

        if ( compositeSequencesMap.isEmpty() ) return null;

        return compositeSequencesMap.values();
    }

    @Override
    protected CompositeSequence handleFindOrCreate( CompositeSequence compositeSequence ) throws Exception {
        return this.getCompositeSequenceDao().findOrCreate( compositeSequence );
    }

    @Override
    protected Map handleGetGenes( Collection sequences ) throws Exception {
        return this.getCompositeSequenceDao().getGenes( sequences );
    }

    @Override
    protected Collection handleGetGenes( CompositeSequence compositeSequence ) throws Exception {
        return this.getCompositeSequenceDao().getGenes( compositeSequence );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Map<CompositeSequence, Collection<BioSequence2GeneProduct>> handleGetGenesWithSpecificity(
            Collection compositeSequences ) throws Exception {
        return this.getCompositeSequenceDao().getGenesWithSpecificity( compositeSequences );
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.expression.designElement.CompositeSequenceServiceBase#handleGetRawSummary(ubic.gemma.model.
     * expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected Collection handleGetRawSummary( ArrayDesign arrayDesign, Integer numResults ) throws Exception {
        return this.getCompositeSequenceDao().getRawSummary( arrayDesign, numResults );
    }

    @Override
    protected Collection handleGetRawSummary( Collection compositeSequences, Integer numResults ) throws Exception {
        return this.getCompositeSequenceDao().getRawSummary( compositeSequences, numResults );
    }

    @Override
    protected Collection handleGetRawSummary( CompositeSequence compositeSequence, Integer numResults )
            throws Exception {
        return this.getCompositeSequenceDao().getRawSummary( compositeSequence, numResults );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceServiceBase#handleLoad(java.lang.Long)
     */
    @Override
    protected CompositeSequence handleLoad( Long id ) throws Exception {
        return this.getCompositeSequenceDao().load( id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.designElement.CompositeSequenceServiceBase#handleLoadMultiple(java.util.Collection)
     */
    @Override
    protected Collection<CompositeSequence> handleLoadMultiple( Collection ids ) throws Exception {
        return ( Collection<CompositeSequence> ) this.getCompositeSequenceDao().load( ids );
    }

    @Override
    protected void handleRemove( CompositeSequence compositeSequence ) throws Exception {
        this.getCompositeSequenceDao().remove( compositeSequence );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceServiceBase#handleRemove(Collection)
     */
    @Override
    protected void handleRemove( java.util.Collection sequencesToDelete ) throws java.lang.Exception {

        // check the collection to make sure it contains no transitive entities (just check the id and make sure its
        // non-null

        Collection<CompositeSequence> filteredSequence = new Vector<CompositeSequence>();
        for ( Object sequence : sequencesToDelete ) {
            if ( ( ( CompositeSequence ) sequence ).getId() != null )
                filteredSequence.add( ( CompositeSequence ) sequence );
        }

        this.getCompositeSequenceDao().remove( filteredSequence );
        return;
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#saveCompositeSequence(ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    protected void handleSaveCompositeSequence(
            ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) throws java.lang.Exception {
        this.getCompositeSequenceDao().create( compositeSequence );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceServiceBase#handleThaw(java.util.Collection)
     */
    @Override
    protected void handleThaw( Collection compositeSequences ) throws Exception {
        this.getCompositeSequenceDao().thaw( compositeSequences );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.designElement.CompositeSequenceServiceBase#handleUpdate(ubic.gemma.model.expression
     * .designElement.CompositeSequence)
     */
    @Override
    protected void handleUpdate( CompositeSequence compositeSequence ) throws Exception {
        this.getCompositeSequenceDao().update( compositeSequence );
    }

    @Override
    public CompositeSequence thaw( CompositeSequence compositeSequence ) {
        return this.getCompositeSequenceDao().thaw( compositeSequence );
    }

}