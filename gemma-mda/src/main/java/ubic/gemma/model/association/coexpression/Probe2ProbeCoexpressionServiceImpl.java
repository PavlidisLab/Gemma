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

package ubic.gemma.model.association.coexpression;

import java.util.Collection;
import java.util.Map;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

/**
 * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService
 * @versio n$Id$
 * @author paul
 */
public class Probe2ProbeCoexpressionServiceImpl extends
        ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionServiceBase {

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService#create(ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression)
     */
    @Override
    protected ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression handleCreate(
            ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression p2pCoexpression )
            throws java.lang.Exception {

        if ( p2pCoexpression instanceof RatProbeCoExpression )
            return ( RatProbeCoExpression ) this.getRatProbeCoExpressionDao().create(
                    ( RatProbeCoExpression ) p2pCoexpression );
        else if ( p2pCoexpression instanceof MouseProbeCoExpression )
            return ( MouseProbeCoExpression ) this.getMouseProbeCoExpressionDao().create(
                    ( MouseProbeCoExpression ) p2pCoexpression );
        else if ( p2pCoexpression instanceof HumanProbeCoExpression )
            return ( HumanProbeCoExpression ) this.getHumanProbeCoExpressionDao().create(
                    ( HumanProbeCoExpression ) p2pCoexpression );
        else if ( p2pCoexpression instanceof OtherProbeCoExpression )
            return ( OtherProbeCoExpression ) this.getOtherProbeCoExpressionDao().create(
                    ( OtherProbeCoExpression ) p2pCoexpression );
        else
            throw new java.lang.Exception( "p2pCoexpression isn't of a known type. don't know how to create."
                    + p2pCoexpression );

    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionServiceBase#handleCreate(java.util.Collection)
     */
    @Override
    protected java.util.List handleCreate( java.util.List p2pExpressions ) throws java.lang.Exception {
        return this.getProbe2ProbeCoexpressionDao().create( p2pExpressions );
        // if ( !this.validCollection( p2pExpressions ) ) return null;
        //
        // Object check = p2pExpressions.iterator().next();
        // if ( check instanceof RatProbeCoExpression )
        // return this.getRatProbeCoExpressionDao().create( p2pExpressions );
        // else if ( check instanceof MouseProbeCoExpression )
        // return this.getMouseProbeCoExpressionDao().create( p2pExpressions );
        // else if ( check instanceof HumanProbeCoExpression )
        // return this.getHumanProbeCoExpressionDao().create( p2pExpressions );
        // else if ( check instanceof OtherProbeCoExpression )
        // return this.getOtherProbeCoExpressionDao().create( p2pExpressions );
        // else
        // throw new IllegalArgumentException( "Collection contains objects that it can't persist:" + check.getClass()
        // + " no service method for persisting." );

    }

    /**
     * Performs the core logic for {@link #delete(ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression)}
     */
    @Override
    protected void handleDelete( ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression toDelete )
            throws java.lang.Exception {

        if ( toDelete instanceof RatProbeCoExpression )
            this.getRatProbeCoExpressionDao().remove( ( RatProbeCoExpression ) toDelete );
        else if ( toDelete instanceof MouseProbeCoExpression )
            this.getMouseProbeCoExpressionDao().remove( ( MouseProbeCoExpression ) toDelete );
        else if ( toDelete instanceof HumanProbeCoExpression )
            this.getHumanProbeCoExpressionDao().remove( ( HumanProbeCoExpression ) toDelete );
        else if ( toDelete instanceof OtherProbeCoExpression )
            this.getOtherProbeCoExpressionDao().remove( ( OtherProbeCoExpression ) toDelete );
        else
            throw new IllegalArgumentException( "Collection contains objects that it can't persist:"
                    + toDelete.getClass() + " no service method for persisting." );

        return;

    }

    /**
     * Performs the core logic for {@link #delete(java.util.Collection)}
     */
    @Override
    protected void handleDelete( java.util.Collection deletes ) throws java.lang.Exception {

        if ( !this.validCollection( deletes ) ) return;

        Object check = deletes.iterator().next();

        if ( check instanceof RatProbeCoExpression )
            this.getRatProbeCoExpressionDao().remove( deletes );
        else if ( check instanceof MouseProbeCoExpression )
            this.getMouseProbeCoExpressionDao().remove( deletes );
        else if ( check instanceof HumanProbeCoExpression )
            this.getHumanProbeCoExpressionDao().remove( deletes );
        else if ( check instanceof OtherProbeCoExpression )
            this.getOtherProbeCoExpressionDao().remove( deletes );
        else
            throw new IllegalArgumentException( "Collection contains objects that it can't persist:" + check.getClass()
                    + " no service method for persisting." );

        return;

    }

    private Boolean validCollection( java.util.Collection p2pExpressions ) throws IllegalArgumentException {
        // sanity check.
        if ( ( p2pExpressions == null ) || ( p2pExpressions.size() == 0 ) ) return false;

        // Make sure that the collections passed in is all of the same Class

        Object last = p2pExpressions.iterator().next();

        for ( Object next : p2pExpressions ) {

            if ( last.getClass() != next.getClass() ) {
                throw new IllegalArgumentException(
                        "Given collection doesn't contain objects of uniform type. Contains an object of type "
                                + last.getClass() + " and another of type " + next.getClass() );
            }

            last = next;
        }

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionServiceBase#handleDeleteLinks(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    protected void handleDeleteLinks( ExpressionExperiment ee ) throws Exception {
        this.getProbe2ProbeCoexpressionDao().deleteLinks( ee );

    }

    @Override
    protected Integer handleCountLinks( ExpressionExperiment expressionExperiment ) throws Exception {
        return this.getProbe2ProbeCoexpressionDao().countLinks( expressionExperiment );
    }

    @Override
    protected Collection handleGetProbeCoExpression( ExpressionExperiment expressionExperiment, String taxon,
            boolean cleaned ) throws Exception {
        // cleaned: a temporary table is created.s
        return this.getProbe2ProbeCoexpressionDao().getProbeCoExpression( expressionExperiment, taxon, cleaned );
    }

    @Override
    protected void handlePrepareForShuffling( Collection ees, String taxon, boolean filterNonSpecific )
            throws Exception {
        this.getProbe2ProbeCoexpressionDao().prepareForShuffling( ees, taxon, filterNonSpecific );
    }

    @Override
    protected Map handleGetExpressionExperimentsLinkTestedIn( Gene geneA, Collection genesB,
            Collection expressionExperiments, boolean filterNonSpecific ) throws Exception {
        return this.getProbe2ProbeCoexpressionDao().getExpressionExperimentsLinkTestedIn( geneA, genesB,
                expressionExperiments, filterNonSpecific );
    }

    @Override
    protected Map handleGetExpressionExperimentsTestedIn( Collection genes, Collection expressionExperiments,
            boolean filterNonSpecific ) throws Exception {
        return this.getProbe2ProbeCoexpressionDao().getExpressionExperimentsTestedIn( genes, expressionExperiments,
                filterNonSpecific );
    }

    @Override
    protected Collection handleGetExpressionExperimentsLinkTestedIn( Gene gene, Collection expressionExperiments,
            boolean filterNonSpecific ) throws Exception {
        return this.getProbe2ProbeCoexpressionDao().getExpressionExperimentsLinkTestedIn( gene, expressionExperiments,
                filterNonSpecific );
    }

    @Override
    protected Collection handleGetVectorsForLinks( Gene gene, Collection ees ) throws Exception {
        return this.getProbe2ProbeCoexpressionDao().getVectorsForLinks( gene, ees );
    }

    @Override
    protected Map handleGetVectorsForLinks( Collection genes, Collection ees ) throws Exception {
        return this.getProbe2ProbeCoexpressionDao().getVectorsForLinks( genes, ees );
    }

    @Override
    protected Collection handleGetGenesTestedBy( ExpressionExperiment expressionExperiment, boolean filterNonSpecific )
            throws Exception {
        return this.getProbe2ProbeCoexpressionDao().getGenesTestedBy( expressionExperiment, filterNonSpecific );
    }

}