/*
 * The gemma-core project
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

package ubic.gemma.analysis.expression.coexpression;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.model.association.Gene2GeneProteinAssociation;
import ubic.gemma.model.association.Gene2GeneProteinAssociationService;
import ubic.gemma.model.association.TfGeneAssociation;
import ubic.gemma.model.association.TfGeneAssociationService;
import ubic.gemma.model.genome.Gene;

/**
 * Not used yet...
 * 
 * @author Paul
 * @version $Id$
 */
@Component
public class CoexpressionAddons {

    private static Logger log = LoggerFactory.getLogger( CoexpressionAddons.class );

    @Autowired
    private Gene2GeneProteinAssociationService gene2GeneProteinAssociationService = null;

    @Autowired
    private TfGeneAssociationService tfGeneAssociationService;

    /**
     * For a given query gene retrieve it's protein protein interactions. Iterating through those interactions create a
     * map keyed on the gene association that was retreived for that given gene. E.g. query gene 'AB' has interactions
     * with 'BB' and 'CC' then create a map using the ids as keys from BB and CC. and the value using the String url for
     * that interaction
     * 
     * @param gene The gene to find associations for
     * @return Map of gene ids and their protein protein interactions
     */
    protected Map<Long, Gene2GeneProteinAssociation> getGene2GeneProteinAssociationForQueryGene( Gene gene ) {
        Map<Long, Gene2GeneProteinAssociation> stringUrlsMappedByGeneID = new HashMap<Long, Gene2GeneProteinAssociation>();
        Collection<Gene2GeneProteinAssociation> proteinInteractions = this.gene2GeneProteinAssociationService
                .findProteinInteractionsForGene( gene );
        // check if found any interactions
        if ( proteinInteractions != null && !proteinInteractions.isEmpty() ) {

            for ( Gene2GeneProteinAssociation proteinInteraction : proteinInteractions ) {
                gene2GeneProteinAssociationService.thaw( proteinInteraction );
                if ( log.isDebugEnabled() ) {
                    log.debug( "found interaction for gene " + proteinInteraction.getFirstGene() + " and "
                            + proteinInteraction.getSecondGene() );
                }

                if ( proteinInteraction.getDatabaseEntry() != null && proteinInteraction.getSecondGene().getId() != null
                        && proteinInteraction.getFirstGene().getId() != null ) {
                    // can append extra details to link if required this
                    // formating code should be somewhere else?

                    if ( proteinInteraction.getFirstGene().getId().equals( gene.getId() ) ) {
                        stringUrlsMappedByGeneID.put( proteinInteraction.getSecondGene().getId(), proteinInteraction );
                    } else {
                        stringUrlsMappedByGeneID.put( proteinInteraction.getFirstGene().getId(), proteinInteraction );
                    }
                }
            }
        }
        return stringUrlsMappedByGeneID;

    }

    /**
     * @param gene which is to be treated as a "target"
     * @return map of the transcription factor to the interaction details
     */
    protected Map<Long, TfGeneAssociation> getTfGeneAssociationsforTargetGene( Gene gene ) {
        Map<Long, TfGeneAssociation> associationsMappedByGeneId = new HashMap<Long, TfGeneAssociation>();
        Collection<? extends TfGeneAssociation> interactions = this.tfGeneAssociationService.findByTargetGene( gene );

        if ( interactions != null && !interactions.isEmpty() ) {

            for ( TfGeneAssociation interaction : interactions ) {

                if ( log.isDebugEnabled() ) {
                    log.debug( "found interaction for gene " + interaction.getFirstGene() + " and "
                            + interaction.getSecondGene() );
                }

                associationsMappedByGeneId.put( interaction.getFirstGene().getId(), interaction );

            }
        }
        return associationsMappedByGeneId;

    }

    /**
     * @param gene which is to be treated as a "transcription factor"
     * @return map of the target genes to the interaction details.
     */
    protected Map<Long, TfGeneAssociation> getTfGeneAssociationsforTf( Gene gene ) {
        Map<Long, TfGeneAssociation> associationsMappedByGeneId = new HashMap<Long, TfGeneAssociation>();
        Collection<? extends TfGeneAssociation> interactions = this.tfGeneAssociationService.findByTf( gene );
        if ( interactions != null && !interactions.isEmpty() ) {

            for ( TfGeneAssociation interaction : interactions ) {

                if ( log.isDebugEnabled() ) {
                    log.debug( "found interaction for gene " + interaction.getFirstGene() + " and "
                            + interaction.getSecondGene() );
                }
                associationsMappedByGeneId.put( interaction.getSecondGene().getId(), interaction );
            }
        }
        return associationsMappedByGeneId;
    }

    /**
     * Adds the protein protein interaction data to the value object, that is the url link for string the evidence for
     * that interaction and the confidence score.
     * 
     * @param proteinProteinInteraction Protein Protein interaction for the coexpression link
     * @param cvo The value object used to display coexpression data
     */
    private void addProteinDetailsToValueObject( Gene2GeneProteinAssociation proteinProteinInteraction,
            CoexpressionValueObjectExt cvo ) {

        if ( proteinProteinInteraction == null ) return;

        // ProteinLinkOutFormatter proteinFormatter = new ProteinLinkOutFormatter();
        // String proteinProteinIdUrl = proteinFormatter
        // .getStringProteinProteinInteractionLinkGemmaDefault( proteinProteinInteraction.getDatabaseEntry() );
        //
        // String evidenceText = proteinFormatter.getEvidenceDisplayText( proteinProteinInteraction.getEvidenceVector()
        // );
        //
        // String confidenceText = proteinFormatter.getConfidenceScoreAsPercentage( proteinProteinInteraction
        // .getConfidenceScore() );

        // log.debug( "A coexpression link in GEMMA has a interaction in STRING " + proteinProteinIdUrl +
        // " evidence of "
        // + evidenceText );
        //
        // cvo.setGene2GeneProteinAssociationStringUrl( proteinProteinIdUrl );
        // cvo.setGene2GeneProteinInteractionConfidenceScore( confidenceText );
        // cvo.setGene2GeneProteinInteractionEvidence( evidenceText );

    }

    /**
     * @param tfGeneAssociation
     * @param cvo
     */
    private void addTfInteractionToValueObject( TfGeneAssociation tfGeneAssociation, CoexpressionValueObjectExt cvo ) {
        if ( tfGeneAssociation == null ) return;
        //
        // if ( tfGeneAssociation.getFirstGene().getId().equals( cvo.getQueryGene().getId() ) ) {
        // cvo.setQueryRegulatesFound( true );
        // } else if ( tfGeneAssociation.getFirstGene().getId().equals( cvo.getFoundGene().getId() ) ) {
        // cvo.setFoundRegulatesQuery( true );
        // } else {
        // throw new IllegalStateException();
        // }
    }

    /**
     * @param proteinInteractionMap
     * @param regulatedBy
     * @param regulates
     * @param foundGene
     * @param cvo
     */
    @SuppressWarnings("unused")
    private void populateInteractions( Map<Long, Gene2GeneProteinAssociation> proteinInteractionMap,
            Map<Long, TfGeneAssociation> regulatedBy, Map<Long, TfGeneAssociation> regulates, Gene foundGene,
            CoexpressionValueObjectExt cvo ) {

        StopWatch timer = new StopWatch();
        timer.start();

        // set the interaction if none null will be put
        if ( proteinInteractionMap != null && !( proteinInteractionMap.isEmpty() ) ) {
            Gene2GeneProteinAssociation association = proteinInteractionMap.get( foundGene.getId() );
            if ( association != null ) this.addProteinDetailsToValueObject( association, cvo );
        }

        if ( regulatedBy != null && !regulatedBy.isEmpty() ) {
            TfGeneAssociation tfGeneAssociation = regulatedBy.get( foundGene.getId() );
            if ( tfGeneAssociation != null ) this.addTfInteractionToValueObject( tfGeneAssociation, cvo );
        }

        if ( regulates != null && !regulates.isEmpty() ) {
            TfGeneAssociation tfGeneAssociation = regulates.get( foundGene.getId() );
            if ( tfGeneAssociation != null ) this.addTfInteractionToValueObject( tfGeneAssociation, cvo );
        }
        if ( timer.getTime() > 10 ) log.info( "Iteraction population:" + timer.getTime() + "ms" );

    }

}
