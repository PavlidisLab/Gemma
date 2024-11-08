package ubic.gemma.web.controller.expression.experiment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.web.util.EntityNotFoundException;

import javax.annotation.Nullable;

@Controller
@RequestMapping("/taxon")
public class TaxonController {

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @RequestMapping("/showAllTaxa.html")
    public ModelAndView showAllTxa() {
        return new ModelAndView( "taxa" )
                .addObject( "taxa", taxonService.loadAll() );
    }

    @RequestMapping(value = "/showTaxon.html", params = { "id" })
    public ModelAndView showTaxonById( @RequestParam("id") Long id ) {
        return showTaxon( taxonService.load( id ), "ID", id.toString() );
    }

    @RequestMapping(value = "/showTaxon.html", params = { "ncbiId" })
    public ModelAndView showTaxonByNcbiId( @RequestParam("ncbiId") Integer ncbiId ) {
        return showTaxon( taxonService.findByNcbiId( ncbiId ), "NCBI ID", ncbiId.toString() );
    }

    @RequestMapping(value = "/showTaxon.html", params = { "commonName" })
    public ModelAndView showTaxonByCommonName( @RequestParam("commonName") String name ) {
        return showTaxon( taxonService.findByCommonName( name ), "common name", name );
    }

    @RequestMapping(value = "/showTaxon.html", params = { "scientificName" })
    public ModelAndView showTaxonByScientificName( @RequestParam("scientificName") String name ) {
        return showTaxon( taxonService.findByCommonName( name ), "scientific name", name );
    }

    private ModelAndView showTaxon( @Nullable Taxon taxon, String idType, String val ) {
        if ( taxon == null ) {
            throw new EntityNotFoundException( "No taxon found with " + idType + " " + val );
        }
        return new ModelAndView( "taxon.detail" )
                .addObject( "taxon", taxon )
                .addObject( "numberOfExperiments", expressionExperimentService.getPerTaxonCount().getOrDefault( taxon, 0L ) );
    }
}
