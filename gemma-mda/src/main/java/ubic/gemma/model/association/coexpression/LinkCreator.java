/*
 * The gemma-mda project
 * 
 * Copyright (c) 2014 University of British Columbia
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import ubic.gemma.model.analysis.expression.coexpression.HumanCoexpressionSupportDetailsImpl;
import ubic.gemma.model.analysis.expression.coexpression.MouseCoexpressionSupportDetailsImpl;
import ubic.gemma.model.analysis.expression.coexpression.OtherCoexpressionSupportDetailsImpl;
import ubic.gemma.model.analysis.expression.coexpression.RatCoexpressionSupportDetailsImpl;
import ubic.gemma.model.analysis.expression.coexpression.SupportDetails;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.TaxonUtility;

/**
 * Helper class to use for generating the link objects for persistence.
 * 
 * @author Paul
 * @version $Id$
 */
public class LinkCreator {

    private Class<?> clazz;
    private Class<? extends ExperimentCoexpressionLink> eeclazz;
    private Constructor<ExperimentCoexpressionLink> eeFactoryMethod;
    private Method factorymethod;
    private Class<? extends SupportDetails> supportclazz;
    private Constructor<? extends SupportDetails> supportDetailsFactoryMethod;
    private Constructor<? extends SupportDetails> supportDetailsFactoryMethodByIds;

    public LinkCreator( Taxon taxon ) {

        if ( TaxonUtility.isMouse( taxon ) ) {
            clazz = MouseGeneCoExpression.Factory.class;
            eeclazz = MouseExperimentCoexpressionLinkImpl.class;
            supportclazz = MouseCoexpressionSupportDetailsImpl.class;
        } else if ( TaxonUtility.isRat( taxon ) ) {
            clazz = RatGeneCoExpression.Factory.class;
            eeclazz = RatExperimentCoexpressionLinkImpl.class;
            supportclazz = RatCoexpressionSupportDetailsImpl.class;

        } else if ( TaxonUtility.isHuman( taxon ) ) {
            clazz = HumanGeneCoExpression.Factory.class;
            eeclazz = HumanExperimentCoexpressionLinkImpl.class;
            supportclazz = HumanCoexpressionSupportDetailsImpl.class;

        } else {
            clazz = OtherGeneCoExpression.Factory.class;
            eeclazz = OtherExperimentCoexpressionLinkImpl.class;
            supportclazz = OtherCoexpressionSupportDetailsImpl.class;
        }

        try {
            factorymethod = clazz.getMethod( "newInstance", new Class[] { Double.class, Long.class, Long.class } );
            eeFactoryMethod = ( Constructor<ExperimentCoexpressionLink> ) eeclazz.getConstructor( BioAssaySet.class,
                    Long.class, Long.class, Long.class );
            supportDetailsFactoryMethod = supportclazz.getConstructor( Gene.class, Gene.class, Boolean.class );
            supportDetailsFactoryMethodByIds = supportclazz.getConstructor( Long.class, Long.class, Boolean.class );
        } catch ( SecurityException | NoSuchMethodException e ) {
            throw new RuntimeException( e );
        }
    }

    // public Gene2GeneCoexpression create( double w, Gene firstGene, Gene secondGene ) {
    //
    // try {
    // return ( Gene2GeneCoexpression ) factorymethod.invoke( clazz, new Object[] { w, firstGene, secondGene } );
    // } catch ( IllegalAccessException | IllegalArgumentException | InvocationTargetException e ) {
    // throw new RuntimeException( e );
    // }
    // }
    
    public Gene2GeneCoexpression create( double w, Long firstGene, Long secondGene ) {

        try {
            return ( Gene2GeneCoexpression ) factorymethod.invoke( clazz, new Object[] { w, firstGene, secondGene } );
        } catch ( IllegalAccessException | IllegalArgumentException | InvocationTargetException e ) {
            throw new RuntimeException( e );
        }
    }

    public ExperimentCoexpressionLink createEELink( BioAssaySet bas, Long linkid, Long firstGene, Long secondGene ) {
        try {
            return eeFactoryMethod.newInstance( bas, linkid, firstGene, secondGene );
        } catch ( InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e ) {
            throw new RuntimeException( e );
        }
    }

    public SupportDetails createSupportDetails( Gene g1, Gene g2, Boolean isPositive ) {
        try {
            return supportDetailsFactoryMethod.newInstance( g1, g2, isPositive );
        } catch ( InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * @param g1
     * @param g2
     * @param isPositive
     * @return
     */
    public SupportDetails createSupportDetails( Long g1, Long g2, boolean isPositive ) {
        try {
            return supportDetailsFactoryMethodByIds.newInstance( g1, g2, isPositive );
        } catch ( InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e ) {
            throw new RuntimeException( e );
        }
    }
}
