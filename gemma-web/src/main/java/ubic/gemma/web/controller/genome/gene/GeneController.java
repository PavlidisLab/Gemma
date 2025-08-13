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

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ubic.basecode.util.FileTools;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.TsvUtils;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProductValueObject;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetMember;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.genome.gene.GeneSetService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.web.controller.util.ControllerUtils;
import ubic.gemma.web.controller.util.EntityNotFoundException;
import ubic.gemma.web.controller.util.MessageUtil;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

import static ubic.gemma.core.util.TsvUtils.appendBaseHeader;

/**
 * @author daq2101
 * @author pavlidis
 * @author joseph
 */
@Controller
@RequestMapping(value = { "/gene", "/g" })
public class GeneController {

    protected final Log log = LogFactory.getLog( getClass().getName() );

    @Autowired
    protected MessageSource messageSource;
    @Autowired
    protected MessageUtil messageUtil;
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
    public ModelAndView show( @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "ncbiid", required = false) Integer ncbiId,
            @RequestParam(value = "name", required = false) String geneName,
            @RequestParam(value = "taxon", required = false) String taxonName,
            @RequestParam(value = "ensemblId", required = false) String ensemblId ) {

        String idDesc;
        GeneValueObject geneVO = null;
        if ( id != null ) {
            idDesc = "ID " + id;
            geneVO = geneService.loadValueObjectById( id );
        } else if ( ncbiId != null ) {
            idDesc = "NCBI ID " + ncbiId;
            geneVO = geneService.findByNCBIIdValueObject( ncbiId );
        } else if ( ensemblId != null ) {
            idDesc = "Ensembl ID " + ensemblId;
            Collection<Gene> foundGenes = Collections
                    .singleton( geneService.findByEnsemblId( ensemblId ) );
            Gene gene = foundGenes.iterator().next();
            if ( gene != null ) {
                geneVO = geneService.loadValueObjectById( gene.getId() );
            }
        } else if ( geneName != null && taxonName != null ) {
            Taxon taxon = taxonService.findByCommonName( taxonName );
            if ( taxon == null ) {
                throw new EntityNotFoundException( "No taxon found with common name " + taxonName + "." );
            }
            idDesc = "official symbol " + geneName + " (" + taxon.getCommonName() + ")";
            Gene gene = geneService.findByOfficialSymbol( geneName, taxon );
            if ( gene != null ) {
                geneVO = geneService.loadValueObjectById( gene.getId() );
            }
        } else {
            throw new IllegalArgumentException( "No valid parameters provided to identify gene." );
        }

        if ( geneVO == null ) {
            throw new EntityNotFoundException( "No gene found with " + idDesc );
        }

        return new ModelAndView( "gene.detail" )
                .addObject( "gene", geneVO );
    }

    @RequestMapping(value = "/downloadGeneList.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public void downloadGeneList(
            @RequestParam(value = "g", required = false) @Nullable String geneIdsStr,
            @RequestParam(value = "gs", required = false) @Nullable String geneSetIdsStr,
            HttpServletResponse response ) throws IOException {
        if ( geneIdsStr == null && geneSetIdsStr == null ) {
            throw new IllegalArgumentException( "No gene IDs or gene set IDs provided." );
        }

        StopWatch watch = new StopWatch();
        watch.start();

        Collection<Long> geneIds = ControllerUtils.extractIds( geneIdsStr ); // might not be any
        Collection<Long> geneSetIds = ControllerUtils.extractIds( geneSetIdsStr ); // might not be there

        Collection<Gene> genes = geneService.load( geneIds );
        Collection<GeneSet> geneSets = geneSetService.loadWithMembers( geneSetIds );

        if ( genes.isEmpty() && geneSets.isEmpty() ) {
            throw new EntityNotFoundException( String.format( "Could not find genes to match gene IDs: %s or gene set IDs: %s.",
                    geneIds.stream().sorted().map( String::valueOf ).collect( Collectors.joining( ", " ) ),
                    geneSetIds.stream().sorted().map( String::valueOf ).collect( Collectors.joining( ", " ) ) ) );
        }

        if ( geneIds.isEmpty() && geneSetIds.size() == 1 ) {
            // requesting a single gene set
            GeneSet geneSet = geneSets.iterator().next();
            response.setContentType( "text/tab-separated-values" );
            response.setHeader( "Content-Disposition", "attachment; filename=\"" + geneSet.getId() + "_" + FileTools.cleanForFileName( geneSet.getName() ) + ".tsv\"" );
            format4File( geneSet.getMembers().stream().map( GeneSetMember::getGene ).collect( Collectors.toSet() ),
                    "Gene Set", geneSet.getName(), response.getWriter() );
        } else {
            // requesting multiple gene sets and/or genes
            Set<Gene> allGenes = new HashSet<>( genes );
            for ( GeneSet gs : geneSets ) {
                for ( GeneSetMember gsm : gs.getMembers() ) {
                    allGenes.add( gsm.getGene() );
                }
            }
            response.setContentType( "text/tab-separated-values" );
            response.setHeader( "Content-Disposition", "attachment; filename=\"genes.tsv\"" );
            format4File( allGenes, "Gene List", null, response.getWriter() );
        }

        watch.stop();
        long time = watch.getTime();

        if ( time > 100 ) {
            log.info( "Retrieved and Formated" + genes.size() + " genes in : " + time + " ms." );
        }
    }

    private void format4File( Collection<Gene> genes, String what, @Nullable String geneSetName, Writer strBuff ) throws IOException {
        appendBaseHeader( what, buildInfo, new Date(), strBuff );
        if ( geneSetName != null ) {
            strBuff.append( "#\n" )
                    .append( "# Gene Set: " ).append( geneSetName )
                    .append( "\n" );
        }
        strBuff.append( "#\n" )
                .append( "# " ).append( String.valueOf( genes.size() ) ).append( ( genes.size() > 1 ) ? " genes" : " gene" )
                .append( "\n" );

        // add header
        strBuff.append( "Gene Symbol\tGene Name\tNCBI ID\n" );
        for ( Gene gene : genes ) {
            strBuff.append( TsvUtils.format( gene.getOfficialSymbol() ) )
                    .append( "\t" ).append( TsvUtils.format( gene.getOfficialName() ) )
                    .append( "\t" ).append( TsvUtils.format( gene.getNcbiGeneId() ) )
                    .append( "\n" );
        }
    }

}