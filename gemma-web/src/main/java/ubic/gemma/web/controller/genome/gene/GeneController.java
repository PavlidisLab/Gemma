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
package ubic.gemma.web.controller.genome.gene;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.analysis.sequence.ArrayDesignMapResultService;
import ubic.gemma.analysis.sequence.CompositeSequenceMapValueObject;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.ontology.GeneOntologyService;
import ubic.gemma.web.controller.BaseMultiActionController;
import ubic.gemma.web.controller.expression.experiment.AnnotationValueObject;
import ubic.gemma.web.remote.EntityDelegator;

/**
 * @author daq2101
 * @author pavlidis
 * @author joseph
 * @version $Id$
 * @spring.bean id="geneController"
 * @spring.property name="geneService" ref="geneService"
 * @spring.property name="geneOntologyService" ref="geneOntologyService"
 * @spring.property name="gene2GOAssociationService" ref="gene2GOAssociationService"
 * @spring.property name="compositeSequenceService" ref="compositeSequenceService"
 * @spring.property name="arrayDesignMapResultService" ref="arrayDesignMapResultService"
 * @spring.property name="methodNameResolver" ref="geneActions"
 */
public class GeneController extends BaseMultiActionController {
    /**
     * Informs submit methods not to populate model with data that is going to be fetched with AJAX call. We'll
     * configure this externally, so this is temporary.
     */
    private static final boolean AJAX = true;
    private GeneService geneService = null;
    private Gene2GOAssociationService gene2GOAssociationService = null;
    private ArrayDesignMapResultService arrayDesignMapResultService = null;
    private CompositeSequenceService compositeSequenceService = null;

    private GeneOntologyService geneOntologyService;

    /**
     * For ajax
     * 
     * @param geneDelegator
     * @return
     * @spring.webremote include="true"
     */
    @SuppressWarnings("unchecked")
    public Collection findGOTerms( EntityDelegator geneDelegator ) {
        Gene g = Gene.Factory.newInstance();
        g.setId( geneDelegator.getId() );
        Collection<Gene2GOAssociation> associations = gene2GOAssociationService.findAssociationByGene( g );
        Collection<AnnotationValueObject> ontos = new HashSet<AnnotationValueObject>();

        for ( Gene2GOAssociation assoc : associations ) {

            if ( assoc.getOntologyEntry() == null ) continue;

            AnnotationValueObject annot = new AnnotationValueObject();

            annot.setId( assoc.getOntologyEntry().getId() );
            annot.setTermName( geneOntologyService.getTermName( assoc.getOntologyEntry().getValue() ) );
            annot.setTermUri( assoc.getOntologyEntry().getValue() );
            annot.setEvidenceCode( assoc.getEvidenceCode().getValue() );
            annot.setDescription( assoc.getOntologyEntry().getDescription() );
            annot.setClassUri( assoc.getOntologyEntry().getCategoryUri() );
            annot.setClassName( assoc.getOntologyEntry().getCategory() );

            ontos.add( annot );
        }
        cleanup( ontos );
        return ontos;
    }

    /**
     * For ajax.
     * 
     * @param geneDelegator
     * @return
     * @spring.webremote include="true"
     */
    public Collection getProducts( EntityDelegator geneDelegator ) {
        Long id = geneDelegator.getId();
        Gene gene = geneService.load( id );
        return gene.getProducts();
    }

    /**
     * @param arrayDesignMapResultService the arrayDesignMapResultService to set
     */
    public void setArrayDesignMapResultService( ArrayDesignMapResultService arrayDesignMapResultService ) {
        this.arrayDesignMapResultService = arrayDesignMapResultService;
    }

    /**
     * @param compositeSequenceService the compositeSequenceService to set
     */
    public void setCompositeSequenceService( CompositeSequenceService compositeSequenceService ) {
        this.compositeSequenceService = compositeSequenceService;
    }

    /**
     * @param gene2GOAssociationService the gene2GOAssociationService to set
     */
    public void setGene2GOAssociationService( Gene2GOAssociationService gene2GOAssociationService ) {
        this.gene2GOAssociationService = gene2GOAssociationService;
    }

    public void setGeneOntologyService( GeneOntologyService geneOntologyService ) {
        this.geneOntologyService = geneOntologyService;
    }

    /**
     * @param geneService The geneService to set.
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    /**
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @SuppressWarnings( { "unused", "unchecked" })
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {
        Long id = Long.parseLong( request.getParameter( "id" ) );
        Gene gene = geneService.load( id );
        if ( gene == null ) {
            addMessage( request, "object.notfound", new Object[] { "Gene " + id } );
            return new ModelAndView( "mainMenu.html" );
        }

        ModelAndView mav = new ModelAndView( "gene.detail" );
        mav.addObject( "gene", gene );

        if ( !AJAX ) {
            Collection<VocabCharacteristic> ontos = gene2GOAssociationService.findByGene( gene );
            if ( ontos.size() != 0 ) {
                fillInTermNames( ontos );
                cleanupVcs( ontos );
                mav.addObject( "ontologyEntries", ontos );
            }
            mav.addObject( "numOntologyEntries", ontos.size() );
        }

        // Get the composite sequences
        Long compositeSequenceCount = geneService.getCompositeSequenceCountById( id );
        mav.addObject( "compositeSequenceCount", compositeSequenceCount );
        return mav;
    }

    /**
     * @param request
     * @param response
     * @return ModelAndView
     */
    public ModelAndView showAll( HttpServletRequest request, HttpServletResponse response ) {

        String sId = request.getParameter( "id" );
        Collection<Gene> genes = new ArrayList<Gene>();
        // if no IDs are specified, then show an error message
        if ( sId == null ) {
            addMessage( request, "object.notfound", new Object[] { "All genes cannot be listed. Genes " } );
        }

        // if ids are specified, then display only those genes
        else {
            String[] idList = StringUtils.split( sId, ',' );

            for ( int i = 0; i < idList.length; i++ ) {
                Long id = Long.parseLong( idList[i] );
                Gene gene = geneService.load( id );
                if ( gene == null ) {
                    addMessage( request, "object.notfound", new Object[] { "Gene " + id } );
                }
                genes.add( gene );
            }
        }
        return new ModelAndView( "genes" ).addObject( "genes", genes );

    }

    /**
     * @param request
     * @param response
     * @return ModelAndView
     */
    @SuppressWarnings("unchecked")
    public ModelAndView showCompositeSequences( HttpServletRequest request, HttpServletResponse response ) {

        // gene id.
        Long id = Long.parseLong( request.getParameter( "id" ) );
        Gene gene = geneService.load( id );
        if ( gene == null ) {
            addMessage( request, "object.notfound", new Object[] { "Gene " + id } );
            StringBuffer requestURL = request.getRequestURL();
            log.info( requestURL );
            return new ModelAndView( "mainMenu.html" );
        }
        Collection<CompositeSequence> compositeSequences = geneService.getCompositeSequencesById( id );

        ModelAndView mav = new ModelAndView( "compositeSequences.geneMap" );
        mav.addObject( "numCompositeSequences", compositeSequences.size() );

        if ( !AJAX ) {
            Collection<Object[]> rawSummaries = compositeSequenceService.getRawSummary( compositeSequences, 0 );
            Collection<CompositeSequenceMapValueObject> summaries = arrayDesignMapResultService
                    .getSummaryMapValueObjects( rawSummaries );

            if ( summaries == null || summaries.size() == 0 ) {
                // / FIXME, return error or do something else intelligent.
            }
            mav.addObject( "sequenceData", summaries );

        } else {
            StringBuilder buf = new StringBuilder();
            for ( CompositeSequence sequence : compositeSequences ) {
                buf.append( sequence.getId() );
                buf.append( "," );
            }
            mav.addObject( "compositeSequenceIdList", buf.toString().replaceAll( ",$", "" ) );
        }
        mav.addObject( "gene", gene );

        return mav;
    }

    /**
     * Remove root terms.
     * 
     * @param ontos
     */
    private void cleanupVcs( Collection<VocabCharacteristic> ontos ) {
        for ( Iterator<VocabCharacteristic> it = ontos.iterator(); it.hasNext(); ) {
            VocabCharacteristic v = it.next();
            String term = v.getDescription();
            if ( term.equals( "molecular_function" ) || term.equals( "biological_process" )
                    || term.equals( "cellular_component" ) ) {
                it.remove();
            }
        }
    }

    /**
     * Remove root terms.
     * 
     * @param associations
     */
    private void cleanup( Collection<AnnotationValueObject> associations ) {
        for ( Iterator<AnnotationValueObject> it = associations.iterator(); it.hasNext(); ) {
            String term = it.next().getTermName();
            if ( term == null ) continue;
            if ( term.equals( "molecular_function" ) || term.equals( "biological_process" )
                    || term.equals( "cellular_component" ) ) {
                it.remove();
            }
        }
    }

    /**
     * Provide the human-readable text for each GO term.
     * 
     * @param ontos
     */
    private void fillInTermNames( Collection<VocabCharacteristic> ontos ) {
        for ( VocabCharacteristic v : ontos ) {
            String desc = geneOntologyService.getTermName( v.getValue() );
            v.setDescription( desc ); // we're just using this as a convenient spot.
        }
    }

}