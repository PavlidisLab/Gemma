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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProductValueObject;
import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.genome.gene.GeneSetService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.web.controller.BaseController;
import ubic.gemma.web.controller.ControllerUtils;
import ubic.gemma.web.view.TextView;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import static ubic.gemma.core.util.Constants.GEMMA_CITATION_NOTICE;
import static ubic.gemma.core.util.Constants.GEMMA_LICENSE_NOTICE;
import static ubic.gemma.core.util.TsvUtils.format;

/**
 * @author daq2101
 * @author pavlidis
 * @author joseph
 */
@Controller
@RequestMapping(value = { "/gene", "/g" })
public class GeneController extends BaseController {

    @Autowired
    private GeneService geneService;
    @Autowired
    private TaxonService taxonService;
    @Autowired
    private GeneSetService geneSetService;
    @Autowired
    private BuildInfo buildInfo;

    @SuppressWarnings("WeakerAccess") // Frontend ajax access
    public Collection<AnnotationValueObject> findGOTerms( Long geneId ) {
        return geneService.findGOTerms( geneId );
    }

    @SuppressWarnings({ "WeakerAccess", "unused" }) // Frontend ajax access
    public Collection<GeneProductValueObject> getProducts( Long geneId ) {
        if ( geneId == null )
            throw new IllegalArgumentException( "Null id for gene" );
        return geneService.getProducts( geneId );
    }


    @SuppressWarnings("unused") // Frontend ajax use, gene page
    public GeneValueObject loadGeneDetails( Long geneId ) {
        GeneValueObject gvo = geneService.loadFullyPopulatedValueObject( geneId );
        gvo.setNumGoTerms( this.findGOTerms( geneId ).size() );
        return gvo;
    }

    @SuppressWarnings("unused") // Required
    @RequestMapping(value = { "/showGene.html", "/" }, method = { RequestMethod.GET, RequestMethod.HEAD })
    public ModelAndView show( HttpServletRequest request ) {

        String idString = request.getParameter( "id" );
        String ncbiId = request.getParameter( "ncbiid" );
        String geneName = request.getParameter( "name" );
        String taxonName = request.getParameter( "taxon" );
        String ensemblId = request.getParameter( "ensemblId" );

        GeneValueObject geneVO = null;

        try {
            if ( StringUtils.isNotBlank( idString ) ) {
                Long id = Long.parseLong( idString );

                geneVO = geneService.loadValueObjectById( id );

                if ( geneVO == null ) {
                    addMessage( request, "object.notfound", new Object[] { "Gene " + id } );
                    return new ModelAndView( "index" );
                }
            } else if ( StringUtils.isNotBlank( ncbiId ) ) {

                geneVO = geneService.findByNCBIIdValueObject( Integer.parseInt( ncbiId ) );

            } else if ( StringUtils.isNotBlank( ensemblId ) ) {
                Collection<Gene> foundGenes = Collections
                        .singleton( geneService.findByEnsemblId( ensemblId ) );

                Gene gene = foundGenes.iterator().next();
                if ( gene != null ) {
                    geneVO = geneService.loadValueObjectById( gene.getId() );
                }

            } else if ( StringUtils.isNotBlank( geneName ) && StringUtils.isNotBlank( taxonName ) ) {
                Taxon taxon = taxonService.findByCommonName( taxonName );
                if ( taxon != null ) {
                    Gene gene = geneService.findByOfficialSymbol( geneName, taxon );
                    if ( gene != null ) {
                        geneVO = geneService.loadValueObjectById( gene.getId() );
                    }
                }
            }

        } catch ( NumberFormatException e ) {
            addMessage( request, "object.notfound", new Object[] { "Gene" } );
            return new ModelAndView( "index" );
        }

        if ( geneVO == null ) {
            addMessage( request, "object.notfound", new Object[] { "Gene" } );
            return new ModelAndView( "index" );
        }

        Long id = geneVO.getId();

        assert id != null;
        ModelAndView mav = new ModelAndView( "gene.detail" );
        mav.addObject( "geneId", id );
        mav.addObject( "geneOfficialSymbol", geneVO.getOfficialSymbol() );
        mav.addObject( "geneOfficialName", geneVO.getOfficialName() );
        mav.addObject( "geneNcbiId", geneVO.getNcbiId() );
        mav.addObject( "geneTaxonCommonName", geneVO.getTaxonCommonName() );
        mav.addObject( "geneTaxonId", geneVO.getTaxonId() );

        return mav;
    }

    @RequestMapping(value = "/downloadGeneList.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public ModelAndView handleRequestInternal( HttpServletRequest request ) {

        StopWatch watch = new StopWatch();
        watch.start();

        Collection<Long> geneIds = ControllerUtils.extractIds( request.getParameter( "g" ) ); // might not be any
        Collection<Long> geneSetIds = ControllerUtils.extractIds( request.getParameter( "gs" ) ); // might not be there
        String geneSetName = request.getParameter( "gsn" ); // might not be there

        ModelAndView mav = new ModelAndView( new TextView() );
        if ( geneIds.isEmpty() && geneSetIds.isEmpty() ) {
            mav.addObject( "text",
                    "Could not find genes to match gene ids: {" + geneIds + "} or gene set ids {" + geneSetIds + "}" );
            return mav;
        }
        Collection<GeneValueObject> genes = new ArrayList<>();
        for ( Long id : geneIds ) {
            GeneValueObject vo = geneService.loadValueObjectById( id );
            if ( vo != null ) {
                genes.add( vo );
            }
        }
        for ( Long id : geneSetIds ) {
            genes.addAll( geneSetService.getGenesInGroup( new GeneSetValueObject( id ) ) );
        }

        mav.addObject( "text", format4File( genes, geneSetName ) );
        watch.stop();
        long time = watch.getTime();

        if ( time > 100 ) {
            log.info( "Retrieved and Formated" + genes.size() + " genes in : " + time + " ms." );
        }
        return mav;

    }

    private String format4File( Collection<GeneValueObject> genes, String geneSetName ) {
        StringBuilder strBuff = new StringBuilder();
        strBuff.append( "# Generated by Gemma " ).append( buildInfo.getVersion() ).append( " on " ).append( format( new Date() ) ).append( "\n" );
        strBuff.append( "#\n" );
        for ( String line : GEMMA_CITATION_NOTICE ) {
            strBuff.append( "# " ).append( line ).append( "\n" );
        }
        strBuff.append( "#\n" );
        strBuff.append( "# " ).append( GEMMA_LICENSE_NOTICE ).append( "\n" );
        strBuff.append( "#\n" );

        if ( geneSetName != null && !geneSetName.isEmpty() )
            strBuff.append( "# Gene Set: " ).append( geneSetName ).append( "\n" );
        strBuff.append( "# " ).append( genes.size() ).append( ( genes.size() > 1 ) ? " genes" : " gene" )
                .append( "\n" );

        // add header
        strBuff.append( "Gene Symbol\tGene Name\tNCBI ID\n" );
        for ( GeneValueObject gene : genes ) {
            strBuff.append( gene.getOfficialSymbol() ).append( "\t" ).append( gene.getOfficialName() ).append( "\t" )
                    .append( gene.getNcbiId() );
            strBuff.append( "\n" );
        }

        return strBuff.toString();
    }

}