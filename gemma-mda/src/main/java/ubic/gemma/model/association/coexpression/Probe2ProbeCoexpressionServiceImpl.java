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

/**
 * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService
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
    protected java.util.Collection handleCreate( java.util.Collection p2pExpressions ) throws java.lang.Exception {

        // sanity check.
        if ( ( p2pExpressions == null ) || ( p2pExpressions.size() == 0 ) ) return null;

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

        if ( last instanceof RatProbeCoExpression )
            return this.getRatProbeCoExpressionDao().create( p2pExpressions );
        else if ( last instanceof MouseProbeCoExpression )
            return this.getMouseProbeCoExpressionDao().create( p2pExpressions );
        else if ( last instanceof HumanProbeCoExpression )
            return this.getHumanProbeCoExpressionDao().create( p2pExpressions );
        else if ( last instanceof OtherProbeCoExpression )
            return this.getOtherProbeCoExpressionDao().create( p2pExpressions );
        else
            throw new IllegalArgumentException( "Collection contains objects that it can't persist:" + last.getClass()
                    + " no service method for persisting." );

    }

}