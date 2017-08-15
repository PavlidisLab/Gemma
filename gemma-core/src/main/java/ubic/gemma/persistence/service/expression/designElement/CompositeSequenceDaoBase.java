/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.persistence.service.expression.designElement;

import org.hibernate.SessionFactory;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.VoEnabledDao;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>CompositeSequence</code>.
 * </p>
 *
 * @see CompositeSequence
 */
public abstract class CompositeSequenceDaoBase extends VoEnabledDao<CompositeSequence, CompositeSequenceValueObject>
        implements CompositeSequenceDao {

    public CompositeSequenceDaoBase( SessionFactory sessionFactory ) {
        super( CompositeSequence.class, sessionFactory );
    }

    /**
     * @see CompositeSequenceDao#findByBioSequence(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @Override
    public Collection<CompositeSequence> findByBioSequence(
            final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        return this.handleFindByBioSequence( bioSequence );
    }

    /**
     * @see CompositeSequenceDao#findByBioSequenceName(String)
     */
    @Override
    public Collection<CompositeSequence> findByBioSequenceName( final String name ) {
        return this.handleFindByBioSequenceName( name );
    }

    @Override
    public Collection<CompositeSequence> findByName( final String name ) {
        return this.findByStringProperty( "name", name );
    }

    @Override
    public CompositeSequence findByName( ArrayDesign arrayDesign, final String name ) {
        List results = this.getSessionFactory().getCurrentSession().createQuery(
                "from CompositeSequence as compositeSequence where compositeSequence.arrayDesign = :arrayDesign and compositeSequence.name = :name" )
                .setParameter( "arrayDesign", arrayDesign ).setParameter( "name", name ).list();

        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'CompositeSequence" + "' was found for name '" + name
                            + "' and array design '" + arrayDesign.getId() + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }
        return ( CompositeSequence ) result;
    }

    /**
     * @see CompositeSequenceDao#getGenes(Collection)
     */
    @Override
    public Map<CompositeSequence, Collection<Gene>> getGenes( final Collection<CompositeSequence> compositeSequences ) {
        return this.handleGetGenes( compositeSequences );
    }

    /**
     * @see CompositeSequenceDao#getGenesWithSpecificity(Collection)
     */
    @Override
    public Map<CompositeSequence, Collection<BioSequence2GeneProduct>> getGenesWithSpecificity(
            final Collection<CompositeSequence> compositeSequences ) {
        return this.handleGetGenesWithSpecificity( compositeSequences );
    }

    @Override
    public Collection<Object[]> getRawSummary( final Collection<CompositeSequence> compositeSequences,
            final Integer numResults ) {
        return this.handleGetRawSummary( compositeSequences, numResults );
    }

    @Override
    public Collection<Object[]> getRawSummary( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign,
            final Integer numResults ) {
        return this.handleGetRawSummary( arrayDesign, numResults );
    }

    @Override
    public Collection<Object[]> getRawSummary( final CompositeSequence compositeSequence, final Integer numResults ) {
        return this.handleGetRawSummary( compositeSequence, numResults );
    }

    /**
     * @see CompositeSequenceDao#load(Collection)
     */
    @Override
    public Collection<CompositeSequence> load( final Collection<Long> ids ) {
        return this.handleLoad( ids );
    }

    /**
     * @see CompositeSequenceDao#thaw(Collection)
     */
    @Override
    public void thaw( final Collection<CompositeSequence> compositeSequences ) {
        this.handleThaw( compositeSequences );
    }

    /**
     * Performs the core logic for {@link #findByBioSequence(ubic.gemma.model.genome.biosequence.BioSequence)}
     */
    protected abstract Collection<CompositeSequence> handleFindByBioSequence(
            ubic.gemma.model.genome.biosequence.BioSequence bioSequence );

    /**
     * Performs the core logic for {@link #findByBioSequenceName(String)}
     */
    protected abstract Collection<CompositeSequence> handleFindByBioSequenceName( String name );

    /**
     * Performs the core logic for {@link #getGenes(Collection)}
     */
    protected abstract Map<CompositeSequence, Collection<Gene>> handleGetGenes(
            Collection<CompositeSequence> compositeSequences );

    /**
     * Performs the core logic for {@link #getGenesWithSpecificity(Collection)}
     */
    protected abstract Map<CompositeSequence, Collection<BioSequence2GeneProduct>> handleGetGenesWithSpecificity(
            Collection<CompositeSequence> compositeSequences );

    /**
     * Performs the core logic for {@link #getRawSummary(Collection, Integer)}
     */
    protected abstract Collection<Object[]> handleGetRawSummary( Collection<CompositeSequence> compositeSequences,
            Integer numResults );

    /**
     * Performs the core logic for
     * {@link #getRawSummary(ubic.gemma.model.expression.arrayDesign.ArrayDesign, Integer)}
     */
    protected abstract Collection<Object[]> handleGetRawSummary(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign, Integer numResults );

    /**
     * Performs the core logic for
     * {@link #getRawSummary(CompositeSequence, Integer)}
     */
    protected abstract Collection<Object[]> handleGetRawSummary( CompositeSequence compositeSequence,
            Integer numResults );

    /**
     * Performs the core logic for {@link #load(Collection)}
     */
    protected abstract Collection<CompositeSequence> handleLoad( Collection<Long> ids );

    /**
     * Performs the core logic for {@link #thaw(Collection)}
     */
    protected abstract void handleThaw( Collection<CompositeSequence> compositeSequences );

}