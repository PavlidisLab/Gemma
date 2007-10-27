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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.association.coexpression;

import ubic.gemma.model.analysis.Analysis;

/**
 * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService
 */
public class Gene2GeneCoexpressionServiceImpl extends
        ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionServiceBase {

    /**
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService#create(ubic.gemma.model.association.coexpression.Gene2GeneCoexpression)
     */
    @Override
    protected ubic.gemma.model.association.coexpression.Gene2GeneCoexpression handleCreate(
            ubic.gemma.model.association.coexpression.Gene2GeneCoexpression gene2gene ) throws java.lang.Exception {

        if ( gene2gene instanceof RatGeneCoExpression )
            return ( RatGeneCoExpression ) this.getRatGeneCoExpressionDao().create( ( RatGeneCoExpression ) gene2gene );
        else if ( gene2gene instanceof MouseGeneCoExpression )
            return ( MouseGeneCoExpression ) this.getMouseGeneCoExpressionDao().create(
                    ( MouseGeneCoExpression ) gene2gene );
        else if ( gene2gene instanceof HumanGeneCoExpression )
            return ( HumanGeneCoExpression ) this.getHumanGeneCoExpressionDao().create(
                    ( HumanGeneCoExpression ) gene2gene );
        else if ( gene2gene instanceof OtherGeneCoExpression )
            return ( OtherGeneCoExpression ) this.getOtherGeneCoExpressionDao().create(
                    ( OtherGeneCoExpression ) gene2gene );
        else
            throw new java.lang.Exception( "gene2gene coexpression isn't of a known type. don't know how to create."
                    + gene2gene );
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService#create(java.util.Collection)
     */
    @Override
    protected java.util.Collection handleCreate( java.util.Collection genes ) throws java.lang.Exception {

        if ( !this.validCollection( genes ) ) return null;
        Object check = genes.iterator().next();

        if ( check instanceof RatGeneCoExpression )
            return this.getRatGeneCoExpressionDao().create( genes );
        else if ( check instanceof MouseGeneCoExpression )
            return this.getMouseGeneCoExpressionDao().create( genes );
        else if ( check instanceof HumanGeneCoExpression )
            return this.getHumanGeneCoExpressionDao().create( genes );
        else if ( check instanceof OtherGeneCoExpression )
            return this.getOtherGeneCoExpressionDao().create( genes );
        else
            throw new java.lang.Exception( "gene2gene coexpression isn't of a known type. don't know how to create."
                    + check );

    }

    /**
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService#delete(ubic.gemma.model.association.coexpression.Gene2GeneCoexpression)
     */
    @Override
    protected void handleDelete( ubic.gemma.model.association.coexpression.Gene2GeneCoexpression toDelete )
            throws java.lang.Exception {

        if ( toDelete instanceof RatGeneCoExpression )
            this.getRatGeneCoExpressionDao().remove( toDelete );
        else if ( toDelete instanceof MouseGeneCoExpression )
            this.getMouseGeneCoExpressionDao().remove( toDelete );
        else if ( toDelete instanceof HumanGeneCoExpression )
            this.getHumanGeneCoExpressionDao().remove( toDelete );
        else if ( toDelete instanceof OtherGeneCoExpression )
            this.getOtherGeneCoExpressionDao().remove( toDelete );
        else
            throw new IllegalArgumentException( "Collection contains objects that it can't persist:"
                    + toDelete.getClass() + " no service method for persisting." );

        return;
    }

    /**
     * Performs the core logic for {@link #delete(java.util.Collection)}
     */
    protected void handleDelete( java.util.Collection deletes ) throws java.lang.Exception {

        if ( !this.validCollection( deletes ) ) return;

        Object check = deletes.iterator().next();

        if ( check instanceof RatGeneCoExpression )
            this.getRatGeneCoExpressionDao().remove( deletes );
        else if ( check instanceof MouseGeneCoExpression )
            this.getMouseGeneCoExpressionDao().remove( deletes );
        else if ( check instanceof HumanGeneCoExpression )
            this.getHumanGeneCoExpressionDao().remove( deletes );
        else if ( check instanceof OtherGeneCoExpression )
            this.getOtherGeneCoExpressionDao().remove( deletes );
        else
            throw new IllegalArgumentException( "Collection contains objects that it can't persist:" + check.getClass()
                    + " no service method for persisting." );

        return;

    }

    /**
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService#findCoexpressionRelationships(ubic.gemma.model.genome.Gene,
     *      java.util.Collection)
     */
    @Override
    protected java.util.Collection handleFindCoexpressionRelationships( ubic.gemma.model.genome.Gene gene,
            Analysis analysis, int stringency ) throws java.lang.Exception {
        return this.getGene2GeneCoexpressionDao().findCoexpressionRelationships( gene, analysis, stringency );
    }

    private Boolean validCollection( java.util.Collection g2gExpressions ) throws IllegalArgumentException {
        // sanity check.
        if ( ( g2gExpressions == null ) || ( g2gExpressions.size() == 0 ) ) return false;

        // Make sure that the collections passed in is all of the same Class

        Object last = g2gExpressions.iterator().next();

        for ( Object next : g2gExpressions ) {

            if ( last.getClass() != next.getClass() ) {
                throw new IllegalArgumentException(
                        "Given collection doesn't contain objects of uniform type. Contains an object of type "
                                + last.getClass() + " and another of type " + next.getClass() );
            }

            last = next;
        }

        return true;
    }
}