/*
 * The gemma project
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

package ubic.gemma.persistence.service.association.coexpression;

import ubic.gemma.model.analysis.expression.coexpression.*;
import ubic.gemma.model.association.coexpression.*;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.taxon.TaxonUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Helper class to use for generating the link objects for persistence.
 *
 * @author Paul
 */
public class LinkCreator {

    private final Class<?> clazz;
    private Constructor<ExperimentCoexpressionLink> eeFactoryMethod;
    private Method factoryMethod;
    private Constructor<? extends SupportDetails> supportDetailsFactoryMethod;
    private Constructor<? extends SupportDetails> supportDetailsFactoryMethodByIds;

    @SuppressWarnings("unchecked")
    public LinkCreator( Taxon taxon ) {

        Class<? extends ExperimentCoexpressionLink> eeClazz;
        Class<? extends SupportDetails> supportClazz;
        if ( TaxonUtils.isMouse( taxon ) ) {
            clazz = MouseGeneCoExpression.Factory.class;
            eeClazz = MouseExperimentCoexpressionLinkImpl.class;
            supportClazz = MouseCoexpressionSupportDetailsImpl.class;
        } else if ( TaxonUtils.isRat( taxon ) ) {
            clazz = RatGeneCoExpression.Factory.class;
            eeClazz = RatExperimentCoexpressionLinkImpl.class;
            supportClazz = RatCoexpressionSupportDetailsImpl.class;

        } else if ( TaxonUtils.isHuman( taxon ) ) {
            clazz = HumanGeneCoExpression.Factory.class;
            eeClazz = HumanExperimentCoexpressionLinkImpl.class;
            supportClazz = HumanCoexpressionSupportDetailsImpl.class;

        } else {
            clazz = OtherGeneCoExpression.Factory.class;
            eeClazz = OtherExperimentCoexpressionLinkImpl.class;
            supportClazz = OtherCoexpressionSupportDetailsImpl.class;
        }

        try {
            factoryMethod = clazz.getMethod( "newInstance", Double.class, Long.class, Long.class );
            eeFactoryMethod = ( Constructor<ExperimentCoexpressionLink> ) eeClazz
                    .getConstructor( BioAssaySet.class, Long.class, Long.class, Long.class );
            supportDetailsFactoryMethod = supportClazz.getConstructor( Gene.class, Gene.class, Boolean.class );
            supportDetailsFactoryMethodByIds = supportClazz.getConstructor( Long.class, Long.class, Boolean.class );
        } catch ( SecurityException | NoSuchMethodException e ) {
            throw new RuntimeException( e );
        }
    }

    public Gene2GeneCoexpression create( double w, Long firstGene, Long secondGene ) {

        try {
            return ( Gene2GeneCoexpression ) factoryMethod.invoke( clazz, new Object[] { w, firstGene, secondGene } );
        } catch ( IllegalAccessException | IllegalArgumentException | InvocationTargetException e ) {
            throw new RuntimeException( e );
        }
    }

    public ExperimentCoexpressionLink createEELink( BioAssaySet bas, Long linkid, Long firstGene, Long secondGene ) {
        try {
            return eeFactoryMethod.newInstance( bas, linkid, firstGene, secondGene );
        } catch ( InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e ) {
            throw new RuntimeException( e );
        }
    }

    public SupportDetails createSupportDetails( Gene g1, Gene g2, Boolean isPositive ) {
        try {
            return supportDetailsFactoryMethod.newInstance( g1, g2, isPositive );
        } catch ( InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e ) {
            throw new RuntimeException( e );
        }
    }

    public SupportDetails createSupportDetails( Long g1, Long g2, boolean isPositive ) {
        try {
            return supportDetailsFactoryMethodByIds.newInstance( g1, g2, isPositive );
        } catch ( InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e ) {
            throw new RuntimeException( e );
        }
    }
}
