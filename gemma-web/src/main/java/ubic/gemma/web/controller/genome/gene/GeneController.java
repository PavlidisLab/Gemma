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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.analysis.service.ExpressionDataFileService;
import ubic.gemma.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.genome.gene.DatabaseBackedGeneSetValueObject;
import ubic.gemma.genome.gene.GeneDetailsValueObject;
import ubic.gemma.genome.gene.GeneSetValueObject;
import ubic.gemma.image.aba.AllenBrainAtlasService;
import ubic.gemma.image.aba.Image;
import ubic.gemma.image.aba.ImageSeries;
import ubic.gemma.loader.genome.gene.ncbi.homology.HomologeneService;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneAlias;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetMember;
import ubic.gemma.model.genome.gene.GeneSetService;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;
import ubic.gemma.ontology.providers.GeneOntologyService;
import ubic.gemma.search.GeneSetSearch;
import ubic.gemma.web.controller.BaseController;
import ubic.gemma.web.controller.WebConstants;
import ubic.gemma.web.image.aba.ImageValueObject;
import ubic.gemma.web.view.TextView;

/**
 * @author daq2101
 * @author pavlidis
 * @author joseph
 * @version $Id$
 */
@Controller
@RequestMapping("/gene")
public class GeneController extends BaseController {

    private static Log log = LogFactory.getLog( GeneController.class );

    @Autowired
    private AllenBrainAtlasService allenBrainAtlasService = null;

    @Autowired
    private Gene2GOAssociationService gene2GOAssociationService = null;

    @Autowired
    private GeneOntologyService geneOntologyService;

    @Autowired
    private HomologeneService homologeneService = null;

    @Autowired
    private TaxonService taxonService = null;

    @Autowired
    private GeneService geneService = null;

    @Autowired
    private GeneSetService geneSetService = null;

    @Autowired
    private GeneSetSearch geneSetSearch;

    @Autowired
    private PhenotypeAssociationManagerService phenotypeAssociationManagerService = null;

    /**
     * For ajax
     * 
     * @param geneDelegator
     * @return
     */
    public Collection<AnnotationValueObject> findGOTerms( Long geneId ) {
        if ( geneId == null ) throw new IllegalArgumentException( "Null id for gene" );
        Collection<AnnotationValueObject> ontos = new HashSet<AnnotationValueObject>();
        Gene g = geneService.load( geneId );

        if ( g == null ) {
            throw new IllegalArgumentException( "No such gene could be loaded with id=" + geneId );
        }

        Collection<Gene2GOAssociation> associations = gene2GOAssociationService.findAssociationByGene( g );

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
     */
    public Collection<GeneProduct> getProducts( Long geneId ) {
        if ( geneId == null ) throw new IllegalArgumentException( "Null id for gene" );
        Gene gene = geneService.load( geneId );

        if ( gene == null ) throw new IllegalArgumentException( "No gene with id " + geneId );

        gene = geneService.thaw( gene );
        return gene.getProducts();
    }

    /**
     * AJAX used for gene page
     * 
     * @param geneId
     * @return
     */
    public GeneDetailsValueObject loadGeneDetails( Long geneId ) {

        Gene gene = geneService.load( geneId );
        // need to thaw for aliases (at least)
        gene = geneService.thaw( gene );

        Collection<Long> ids = new HashSet<Long>();
        ids.add( gene.getId() );
        Collection<GeneValueObject> initialResults = geneService.loadValueObjects( ids );

        if ( initialResults.size() == 0 ) {
            return null;
        }

        GeneValueObject initialResult = initialResults.iterator().next();
        GeneDetailsValueObject details = new GeneDetailsValueObject( initialResult );

        Collection<GeneAlias> aliasObjs = gene.getAliases();
        Collection<String> aliasStrs = new ArrayList<String>();
        for ( GeneAlias ga : aliasObjs ) {
            aliasStrs.add( ga.getAlias() );
        }
        details.setAliases( aliasStrs );

        Long compositeSequenceCount = geneService.getCompositeSequenceCountById( geneId );
        details.setCompositeSequenceCount( compositeSequenceCount );

        Collection<GeneSet> genesets = geneSetSearch.findByGene( gene );
        Collection<GeneSetValueObject> gsvos = new ArrayList<GeneSetValueObject>();
        gsvos.addAll( DatabaseBackedGeneSetValueObject.convert2ValueObjects( genesets, false ) );
        details.setGeneSets( gsvos );

        Collection<Gene> geneHomologues = homologeneService.getHomologues( gene );
        Collection<GeneValueObject> homologues = GeneValueObject.convert2ValueObjects( geneHomologues );
        details.setHomologues( homologues );

        return details;

    }

    /** used to show gene info in the phenotype tab */
    public Collection<EvidenceValueObject> loadGeneEvidences( Long geneId ) {
        return phenotypeAssociationManagerService.findEvidences( geneId );
    }

    public GeneDetailsValueObject loadGenePhenotypes( Long geneId ) {
        return geneService.loadGeneDetails( geneId );
    }

    /**
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @RequestMapping(value = "/showGene.html", method = RequestMethod.GET)
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {

        if ( request.getParameter( "id" ) == null ) {
            addMessage( request, "object.notfound", new Object[] { "Gene" } );
            return new ModelAndView( "index" );
        }

        Long id = null;

        String ncbiId = null;

        Gene gene = null;

        try {
            id = Long.parseLong( request.getParameter( "id" ) );
            gene = geneService.load( id );
            if ( id == null || gene == null ) {
                addMessage( request, "object.notfound", new Object[] { "Gene " + id } );
                return new ModelAndView( "index" );
            }
        } catch ( NumberFormatException e ) {
            ncbiId = request.getParameter( "ncbiid" );

            if ( StringUtils.isNotBlank( ncbiId ) ) {
                gene = geneService.findByNCBIId( ncbiId );
            } else {
                addMessage( request, "object.notfound", new Object[] { "Gene" } );
                return new ModelAndView( "index" );
            }
        }

        if ( gene == null ) {
            addMessage( request, "object.notfound", new Object[] { "Gene " + id } );
            return new ModelAndView( "index" );
        }

        id = gene.getId();

        assert id != null;
        ModelAndView mav = new ModelAndView( "gene.detail" );
        mav.addObject( "geneId", id );
        mav.addObject( "geneOfficialSymbol", gene.getOfficialSymbol() );

        return mav;
    }

    /**
     * @param request
     * @param response
     * @return ModelAndView
     */
    @RequestMapping(value = "/showGenes.html", method = RequestMethod.GET)
    public ModelAndView showMultiple( HttpServletRequest request, HttpServletResponse response ) {

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
        /*
         * FIXME this view doesn't exist!
         */
        return new ModelAndView( "genes" ).addObject( "genes", genes );

    }

    /**
     * Used to display the probe browser, when starting by gene.
     * 
     * @param request
     * @param response
     * @return ModelAndView
     */
    @RequestMapping("/showCompositeSequences.html")
    public ModelAndView showCompositeSequences( HttpServletRequest request, HttpServletResponse response ) {

        // gene id.
        Long id = Long.parseLong( request.getParameter( "id" ) );
        Gene gene = geneService.load( id );
        if ( gene == null ) {
            addMessage( request, "object.notfound", new Object[] { "Gene with id: " + request.getParameter( "id" ) } );
            StringBuffer requestURL = request.getRequestURL();
            log.info( requestURL );
            return new ModelAndView( WebConstants.HOME_PAGE );
        }
        Collection<CompositeSequence> compositeSequences = geneService.getCompositeSequencesById( id );

        ModelAndView mav = new ModelAndView( "compositeSequences.geneMap" );
        mav.addObject( "numCompositeSequences", compositeSequences.size() );

        // fill in by ajax instead.
        // Collection<Object[]> rawSummaries = compositeSequenceService.getRawSummary( compositeSequences, 0 );
        // Collection<CompositeSequenceMapValueObject> summaries = arrayDesignMapResultService
        // .getSummaryMapValueObjects( rawSummaries );
        //
        // if ( summaries == null || summaries.size() == 0 ) {
        // // / FIXME, return error or do something else intelligent.
        // }
        // mav.addObject( "sequenceData", summaries );

        StringBuilder buf = new StringBuilder();
        for ( CompositeSequence sequence : compositeSequences ) {
            buf.append( sequence.getId() );
            buf.append( "," );
        }
        mav.addObject( "compositeSequenceIdList", buf.toString().replaceAll( ",$", "" ) );

        mav.addObject( "gene", gene );

        return mav;
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
     * @param gene
     * @param mav
     */
    private void getAllenBrainImages( Gene gene, ModelAndView mav ) {
        final Taxon mouse = this.taxonService.findByCommonName( "mouse" );
        Gene mouseGene = gene;
        if ( !gene.getTaxon().equals( mouse ) ) {
            mouseGene = this.homologeneService.getHomologue( gene, mouse );
        }

        if ( mouseGene != null ) {
            Collection<ImageSeries> imageSeries = null;

            try {
                imageSeries = allenBrainAtlasService.getRepresentativeSaggitalImages( mouseGene.getOfficialSymbol() );
                String abaGeneUrl = allenBrainAtlasService.getGeneUrl( mouseGene.getOfficialSymbol() );

                Collection<Image> representativeImages = allenBrainAtlasService.getImagesFromImageSeries( imageSeries );

                if ( !representativeImages.isEmpty() ) {
                    mav.addObject( "abaImages", representativeImages );
                    mav.addObject( "abaGeneUrl", abaGeneUrl );
                    mav.addObject( "homologousMouseGene", mouseGene );
                }
            } catch ( IOException e ) {
                log.warn( "Could not get ABA data: " + e );
            }

        }
    }

    /**
     * AJAX NOTE: this method updates the value object passed in
     * 
     * @param gene
     * @param GeneDetailsValueObject gdvo the details object to set the values for
     */
    public Collection<ImageValueObject> loadAllenBrainImages( Long geneId ) {
        Collection<ImageValueObject> images = new ArrayList<ImageValueObject>();
        Gene gene = geneService.load( geneId );
        // need to thaw for aliases (at least)
        gene = geneService.thaw( gene );
        String queryGeneSymbol = gene.getOfficialSymbol();
        final Taxon mouse = this.taxonService.findByCommonName( "mouse" );
        Gene mouseGene = gene;
        boolean usingHomologue = false;
        if ( !gene.getTaxon().equals( mouse ) ) {
            mouseGene = this.homologeneService.getHomologue( gene, mouse );
            usingHomologue = true;
        }

        if ( mouseGene != null ) {
            Collection<ImageSeries> imageSeries = null;

            try {
                imageSeries = allenBrainAtlasService.getRepresentativeSaggitalImages( mouseGene.getOfficialSymbol() );
                String abaGeneUrl = allenBrainAtlasService.getGeneUrl( mouseGene.getOfficialSymbol() );

                Collection<Image> representativeImages = allenBrainAtlasService.getImagesFromImageSeries( imageSeries );
                images = ImageValueObject.convert2ValueObjects( representativeImages, abaGeneUrl, new GeneValueObject(
                        mouseGene ), queryGeneSymbol, usingHomologue );

            } catch ( IOException e ) {
                log.warn( "Could not get ABA data: " + e );
            }
        }
        return images;
    }

    /**
     * Returns a collection of {@link Long} ids from strings.
     * 
     * @param idString
     * @return
     */
    protected Collection<Long> extractIds( String idString ) {
        Collection<Long> ids = new ArrayList<Long>();
        if ( idString != null ) {
            for ( String s : idString.split( "," ) ) {
                try {
                    ids.add( Long.parseLong( s.trim() ) );
                } catch ( NumberFormatException e ) {
                    log.warn( "invalid id " + s );
                }
            }
        }
        return ids;
    }

    /*
     * Handle case of text export of a list of genes
     * 
     * @see org.springframework.web.servlet.mvc.AbstractFormController#handleRequestInternal(javax.servlet.http.
     * HttpServletRequest, javax.servlet.http.HttpServletResponse) Called by /Gemma/gene/downloadGeneList.html
     */
    @RequestMapping("/downloadGeneList.html")
    protected ModelAndView handleRequestInternal( HttpServletRequest request ) throws Exception {

        StopWatch watch = new StopWatch();
        watch.start();

        Collection<Long> geneIds = extractIds( request.getParameter( "g" ) ); // might not be any
        Collection<Long> geneSetIds = extractIds( request.getParameter( "gs" ) ); // might not be there
        String geneSetName = request.getParameter( "gsn" ); // might not be there

        ModelAndView mav = new ModelAndView( new TextView() );
        if ( ( geneIds == null || geneIds.isEmpty() ) && ( geneSetIds == null || geneSetIds.isEmpty() ) ) {
            mav.addObject( "text", "Could not find genes to match gene ids: {" + geneIds + "} or gene set ids {"
                    + geneSetIds + "}" );
            return mav;
        }
        Collection<Gene> genes = new ArrayList<Gene>();
        for ( Long id : geneIds ) {
            genes.add( geneService.load( id ) );
        }
        for ( Long id : geneSetIds ) {
            for ( GeneSetMember gsm : geneSetService.load( id ).getMembers() ) {
                genes.add( gsm.getGene() );
            }
        }

        mav.addObject( "text", format4File( genes, geneSetName ) );
        watch.stop();
        Long time = watch.getTime();

        if ( time > 100 ) {
            log.info( "Retrieved and Formated" + genes.size() + " genes in : " + time + " ms." );
        }
        return mav;

    }

    /**
     * @param vectors
     * @return
     */
    private String format4File( Collection<Gene> genes, String geneSetName ) {
        StringBuffer strBuff = new StringBuffer();
        strBuff.append( "# Generated by Gemma\n# " + ( new Date() ) + "\n" );
        strBuff.append( ExpressionDataFileService.DISCLAIMER + "#\n" );

        if ( geneSetName != null && geneSetName.length() != 0 ) strBuff.append( "# Gene Set: " + geneSetName + "\n" );
        strBuff.append( "# " + genes.size() + ( ( genes.size() > 1 ) ? " genes" : " gene" ) + "\n" );

        // add header
        strBuff.append( "Gene Symbol\tGene Name\tNCBI ID\n" );
        for ( Gene gene : genes ) {
            strBuff.append( gene.getOfficialSymbol() + "\t" + gene.getOfficialName() + "\t" + gene.getNcbiId() );
            strBuff.append( "\n" );
        }

        return strBuff.toString();
    }

}