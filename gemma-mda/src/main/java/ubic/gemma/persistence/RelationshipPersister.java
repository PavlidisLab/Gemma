/*
 * The Gemma project
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
package ubic.gemma.persistence;

import java.util.Collection;
import java.util.HashSet;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSetService;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisService;
import ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.association.Gene2GeneProteinAssociation;
import ubic.gemma.model.association.Gene2GeneProteinAssociationService;
import ubic.gemma.model.association.TfGeneAssociation;
import ubic.gemma.model.association.TfGeneAssociationService;
import ubic.gemma.model.expression.experiment.BioAssaySet;

/**
 * Persist objects like Gene2GOAssociation.
 * 
 * @author pavlidis
 * @version $Id$
 */
public abstract class RelationshipPersister extends ExpressionPersister {

    @Autowired
    private Gene2GOAssociationService gene2GoAssociationService;

    @Autowired
    private ProbeCoexpressionAnalysisService probeCoexpressionAnalysisService;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Autowired
    private GeneCoexpressionAnalysisService geneCoexpressionAnalysisService;

    @Autowired
    private TfGeneAssociationService tfGeneAssociationService;

    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;

    @Autowired
    private Gene2GeneProteinAssociationService gene2GeneProteinAssociationService;

    public RelationshipPersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.util.persister.Persister#persist(java.lang.Object)
     */
    @Override
    public Object persist( Object entity ) {
        if ( entity == null ) return null;

        if ( entity instanceof Gene2GOAssociation ) {
            return persistGene2GOAssociation( ( Gene2GOAssociation ) entity );
        } else if ( entity instanceof ProbeCoexpressionAnalysis ) {
            return persistProbeCoexpressionAnalysis( ( ProbeCoexpressionAnalysis ) entity );
        } else if ( entity instanceof DifferentialExpressionAnalysis ) {
            return persistDifferentialExpressionAnalysis( ( DifferentialExpressionAnalysis ) entity );
        } else if ( entity instanceof GeneCoexpressionAnalysis ) {
            return persistGeneCoexpressionAnalysis( ( GeneCoexpressionAnalysis ) entity );
        } else if ( entity instanceof ExpressionExperimentSet ) {
            return persistExpressionExperimentSet( ( ExpressionExperimentSet ) entity );
        } else if ( entity instanceof Gene2GeneProteinAssociation ) {
            return persistGene2GeneProteinAssociation( ( Gene2GeneProteinAssociation ) entity );
        } else if ( entity instanceof TfGeneAssociation ) {
            return persistTfGeneAssociation( ( TfGeneAssociation ) entity );
        }
        return super.persist( entity );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.CommonPersister#persistOrUpdate(java.lang.Object)
     */
    @Override
    public Object persistOrUpdate( Object entity ) {
        if ( entity == null ) return null;
        return super.persistOrUpdate( entity );
    }

    public void setDifferentialExpressionAnalysisService(
            DifferentialExpressionAnalysisService differentialExpressionAnalysisService ) {
        this.differentialExpressionAnalysisService = differentialExpressionAnalysisService;
    }

    public void setExpressionExperimentSetService( ExpressionExperimentSetService expressionExperimentSetService ) {
        this.expressionExperimentSetService = expressionExperimentSetService;
    }

    /**
     * @param gene2GoAssociationService the gene2GoAssociationService to set
     */
    public void setGene2GoAssociationService( Gene2GOAssociationService gene2GoAssociationService ) {
        this.gene2GoAssociationService = gene2GoAssociationService;
    }

    public void setGeneCoexpressionAnalysisService( GeneCoexpressionAnalysisService geneCoexpressionAnalysisService ) {
        this.geneCoexpressionAnalysisService = geneCoexpressionAnalysisService;
    }

    public void setProbeCoexpressionAnalysisService( ProbeCoexpressionAnalysisService probeCoexpressionAnalysisService ) {
        this.probeCoexpressionAnalysisService = probeCoexpressionAnalysisService;
    }

    public void setGene2GeneProteinAssociationService(
            Gene2GeneProteinAssociationService gene2GeneProteinAssociationService ) {
        this.gene2GeneProteinAssociationService = gene2GeneProteinAssociationService;
    }

    /**
     * @param entity
     * @return
     */
    protected DifferentialExpressionAnalysis persistDifferentialExpressionAnalysis(
            DifferentialExpressionAnalysis entity ) {
        if ( entity == null ) return null;
        if ( !isTransient( entity ) ) return entity;
        entity.setProtocol( persistProtocol( entity.getProtocol() ) );
        entity.setExpressionExperimentSetAnalyzed( persistExpressionExperimentSet( entity
                .getExpressionExperimentSetAnalyzed() ) );
        return differentialExpressionAnalysisService.create( entity );
    }

    protected ExpressionExperimentSet persistExpressionExperimentSet( ExpressionExperimentSet entity ) {
        if ( !isTransient( entity ) ) return entity;
        if ( entity.getExperiments().size() == 0 ) {
            throw new IllegalArgumentException( "Attempt to create an empty ExpressionExperimentSet." );
        }

        Collection<BioAssaySet> setMembers = new HashSet<BioAssaySet>();

        for ( BioAssaySet baSet : entity.getExperiments() ) {
            if ( isTransient( baSet ) ) {
                baSet = ( BioAssaySet ) persist( baSet );
            }
            setMembers.add( baSet );
        }
        entity.getExperiments().clear();
        entity.getExperiments().addAll( setMembers );

        return expressionExperimentSetService.create( entity );
    }

    /**
     * @param association
     * @return
     */
    protected Gene2GOAssociation persistGene2GOAssociation( Gene2GOAssociation association ) {
        if ( association == null ) return null;
        if ( !isTransient( association ) ) return association;

        association.setGene( persistGene( association.getGene() ) );
        return gene2GoAssociationService.findOrCreate( association );
    }

    private TfGeneAssociation persistTfGeneAssociation( TfGeneAssociation entity ) {
        if ( entity == null ) return null;
        if ( !isTransient( entity ) ) return entity;

        if ( isTransient( entity.getFirstGene() ) || isTransient( entity.getSecondGene() ) ) {
            throw new IllegalArgumentException(
                    "Associations can only be made between genes that already exist in the system" );
        }

        return tfGeneAssociationService.create( entity );

    }

    /**
     * @param entity
     * @return
     */
    protected GeneCoexpressionAnalysis persistGeneCoexpressionAnalysis( GeneCoexpressionAnalysis entity ) {
        if ( entity == null ) return null;
        if ( !isTransient( entity ) ) return entity;
        entity.setProtocol( persistProtocol( entity.getProtocol() ) );
        entity.setExpressionExperimentSetAnalyzed( persistExpressionExperimentSet( entity
                .getExpressionExperimentSetAnalyzed() ) );

        return geneCoexpressionAnalysisService.create( entity );
    }

    /**
     * @param entity
     * @return
     */
    protected ProbeCoexpressionAnalysis persistProbeCoexpressionAnalysis( ProbeCoexpressionAnalysis entity ) {
        if ( entity == null ) return null;
        if ( !isTransient( entity ) ) return entity;
        entity.setProtocol( persistProtocol( entity.getProtocol() ) );

        entity.setExpressionExperimentSetAnalyzed( persistExpressionExperimentSet( entity
                .getExpressionExperimentSetAnalyzed() ) );

        return probeCoexpressionAnalysisService.create( entity );
    }

    /**
     * The persisting method for Gene2GeneProteinAssociation which validates the the Gene2GeneProteinAssociation does
     * not already exist in the system. If it does then the persisted object is returned
     * 
     * @param entity Gene2GeneProteinAssociation the object to persist
     * @return Gene2GeneProteinAssociation the persisted object
     */
    protected Gene2GeneProteinAssociation persistGene2GeneProteinAssociation(
            Gene2GeneProteinAssociation gene2GeneProteinAssociation ) {
        if ( gene2GeneProteinAssociation == null ) return null;
        if ( !isTransient( gene2GeneProteinAssociation ) ) return gene2GeneProteinAssociation;

        return gene2GeneProteinAssociationService.createOrUpdate( gene2GeneProteinAssociation );
    }

}
