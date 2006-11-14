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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.util.TaxonUtility;

/**
 * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression
 */
public class Probe2ProbeCoexpressionDaoImpl
    extends ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoBase
{
    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#findCoexpressionRelationships(ubic.gemma.model.genome.Gene, java.util.Collection, ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    protected java.util.Collection handleFindCoexpressionRelationships(ubic.gemma.model.genome.Gene givenG, java.util.Collection ees, ubic.gemma.model.common.quantitationtype.QuantitationType qt)
    {       
        
            String p2pClassName;
            if (TaxonUtility.isHuman(givenG.getTaxon()))
                p2pClassName = "HumanProbeCoExpressionImpl";
            else if (TaxonUtility.isMouse(givenG.getTaxon()))
                p2pClassName = "MouseProbeCoExpressionImpl";       
            else if (TaxonUtility.isRat(givenG.getTaxon()))
                p2pClassName = "RatProbeCoExpressionImpl";        
            else //must be other
                p2pClassName = "OtherProbeCoExpressionImpl";
                
            
            final String queryStringFirstVector =
            // source tables
            "select distinct p2pc.secondVector from GeneImpl as gene, BioSequence2GeneProductImpl as bs2gp, CompositeSequenceImpl as compositeSequence,"
                    // join table
                    + p2pClassName + " as p2pc,"
                    // target tables
                    + " GeneImpl as coGene,BioSequence2GeneProductImpl as coBs2gp, CompositeSequenceImpl as coCompositeSequence"
                    + " where gene.products.id=bs2gp.geneProduct.id "
                    + " and compositeSequence.biologicalCharacteristic=bs2gp.bioSequence "
                    + " and compositeSequence.designElementDataVectors.id=p2pc.firstVector.id "
                    + " and p2pc.firstVector.expressionExperiment.id in (:collectionOfEE)"
                    + " and p2pc.quantitationType.id = :givenQtId"                    
                    + " and gene.id = :givenGId";
            
            final String queryStringSecondVector =
            // source tables
            "select distinct p2pc.firstVector from GeneImpl as gene, BioSequence2GeneProductImpl as bs2gp, CompositeSequenceImpl as compositeSequence,"
                    // join table
                    + "HumanProbeCoExpressionImpl as p2pc,"
                    // target tables
                    + " where gene.products.id=bs2gp.geneProduct.id "
                    + " and compositeSequence.biologicalCharacteristic=bs2gp.bioSequence "
                    + " and compositeSequence.designElementDataVectors.id=p2pc.secondVector.id "
                    + " and p2pc.secondVector.expressionExperiment.id in (:collectionOfEE)"
                    + " and p2pc.quantitationType.id = :givenQtId"
                    + " and gene.id = :givenGId";
         
            Collection<DesignElementDataVector> dedvs = new HashSet<DesignElementDataVector>();
            
            try {
                // do query joining coexpressed genes through the firstVector to the secondVector
                Collection<Long> eeIds= new ArrayList<Long>();
                for ( Iterator iter = ees.iterator(); iter.hasNext(); ) {
                    ExpressionExperiment e = ( ExpressionExperiment ) iter.next();
                    eeIds.add( e.getId() );                
                }            

                
                org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryStringFirstVector );
                queryObject.setParameterList( "collectionOfEE", eeIds);
                queryObject.setLong( "givenQtId", qt.getId() );
                queryObject.setLong( "givenGId", givenG.getId() );
                dedvs.addAll( queryObject.list() );
                // do query joining coexpressed genes through the secondVector to the firstVector
                queryObject = super.getSession( false ).createQuery( queryStringSecondVector );
                queryObject.setParameterList( "collectionOfEE", eeIds);
                queryObject.setLong( "givenQtId", qt.getId() );
                queryObject.setLong( "givenGId", givenG.getId() );
                dedvs.addAll( queryObject.list() );

            } catch ( org.hibernate.HibernateException ex ) {
                throw super.convertHibernateAccessException( ex );
            }
            return dedvs;
        }


}