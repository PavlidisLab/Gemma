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
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.genome.CompositeSequenceGeneMapperService;
import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.common.description.BibliographicReferenceService;
import ubic.gemma.model.common.description.OntologyEntryService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.web.controller.BaseMultiActionController;

/**
 * @author daq2101
 * @author pavlidis
 * @author joseph
 * @version $Id$
 * @spring.bean id="geneController"
 * @spring.property name="geneService" ref="geneService"
 * @spring.property name="compositeSequenceGeneMapperService" ref="compositeSequenceGeneMapperService"
 * @spring.property name="bibliographicReferenceService" ref="bibliographicReferenceService"
 * @spring.property name="gene2GOAssociationService" ref="gene2GOAssociationService"
 * @spring.property name="methodNameResolver" ref="geneActions"
 * @spring.property name="ontologyEntryService" ref="ontologyEntryService"
 */
public class GeneController extends BaseMultiActionController {
    private GeneService geneService = null;
    private BibliographicReferenceService bibliographicReferenceService = null;
    private CompositeSequenceGeneMapperService compositeSequenceGeneMapperService = null;
    private Gene2GOAssociationService gene2GOAssociationService = null;
    private OntologyEntryService ontologyEntryService = null;

    /**
     * @return Returns the geneService.
     */
    public GeneService getGeneService() {
        return geneService;
    }

    /**
     * @param geneService The geneService to set.
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    /**
     * @return Returns the bibliographicReferenceService.
     */
    public BibliographicReferenceService getBibliographicReferenceService() {
        return bibliographicReferenceService;
    }

    /**
     * @param bibliographicReferenceService The bibliographicReferenceService to set.
     */
    public void setBibliographicReferenceService( BibliographicReferenceService bibliographicReferenceService ) {
        this.bibliographicReferenceService = bibliographicReferenceService;
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
     * @param errors
     * @return ModelAndView
     */
    @SuppressWarnings("unused")
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {
        Long id = Long.parseLong( request.getParameter( "id" ) );
        Gene gene = geneService.load( id );
        if ( gene == null ) {
            addMessage( request, "object.notfound", new Object[] { "Gene " + id } );
            return new ModelAndView( "mainMenu.html" );
        }

        ModelAndView mav = new ModelAndView( "gene.detail" );

        Collection ontos = gene2GOAssociationService.findByGene( gene );
        mav.addObject( "gene", gene );

        // get the ontolgy terms and their parents
        if ( ontos.size() != 0 ) {
            mav.addObject( "ontologyEntries", ontos );
        }

        mav.addObject( "numOntologyEntries", ontos.size() );

        // Get the composite sequences
        Long compositeSequenceCount = compositeSequenceGeneMapperService.getCompositeSequenceCountByGeneId( id );
        mav.addObject( "compositeSequenceCount", compositeSequenceCount );
        return mav;
    }

    /**
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @SuppressWarnings( { "unused", "unchecked" })
    public ModelAndView showCompositeSequences( HttpServletRequest request, HttpServletResponse response ) {
        Long id = Long.parseLong( request.getParameter( "id" ) );
        Gene gene = geneService.load( id );
        if ( gene == null ) {
            addMessage( request, "object.notfound", new Object[] { "Gene " + id } );
            return new ModelAndView( "mainMenu.html" );
        }
        ModelAndView mav = new ModelAndView( "compositeSequences" );
        mav.addObject( "gene", gene );
        Collection<CompositeSequence> compositeSequences = compositeSequenceGeneMapperService
                .getCompositeSequencesByGeneId( id );
        mav.addObject( "compositeSequences", compositeSequences );
        return mav;
    }

    /**
     * @param compositeSequenceGeneMapperService The compositeSequenceGeneMapperService to set.
     */
    public void setCompositeSequenceGeneMapperService(
            CompositeSequenceGeneMapperService compositeSequenceGeneMapperService ) {
        this.compositeSequenceGeneMapperService = compositeSequenceGeneMapperService;
    }

    /**
     * @param gene2GOAssociationService the gene2GOAssociationService to set
     */
    public void setGene2GOAssociationService( Gene2GOAssociationService gene2GOAssociationService ) {
        this.gene2GOAssociationService = gene2GOAssociationService;
    }

    /**
     * @param ontologyEntryService the ontologyEntryService to set
     */
    public void setOntologyEntryService( OntologyEntryService ontologyEntryService ) {
        this.ontologyEntryService = ontologyEntryService;
    }

}