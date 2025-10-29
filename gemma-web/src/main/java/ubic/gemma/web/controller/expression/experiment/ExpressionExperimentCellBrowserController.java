package ubic.gemma.web.controller.expression.experiment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ubic.gemma.core.visualization.cellbrowser.CellBrowserService;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.controller.util.EntityNotFoundException;

import javax.annotation.Nullable;
import java.util.stream.Collectors;

/**
 * Provide a cell
 */
@Controller
public class ExpressionExperimentCellBrowserController {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private CellBrowserService cellBrowserService;

    @RequestMapping(value = "/expressionExperiment/showCellBrowser.html", method = { RequestMethod.GET, RequestMethod.HEAD },
            params = { "id" })
    public ModelAndView showCellBrowserById( @RequestParam("id") Long id, @RequestParam(value = "meta", required = false) String meta ) {
        ExpressionExperiment ee = expressionExperimentService.loadOrFail( id, EntityNotFoundException::new, "Could not load dataset with ID " + id + "." );
        return showCellBrowser( ee, meta );
    }

    @RequestMapping(value = "/expressionExperiment/showCellBrowser.html", method = { RequestMethod.GET, RequestMethod.HEAD }, params = { "shortName" })
    public ModelAndView showCellBrowserByShortName( @RequestParam("shortName") String shortName, @RequestParam(value = "meta", required = false) String meta ) {
        ExpressionExperiment ee = expressionExperimentService.findByShortName( shortName );
        if ( ee == null ) {
            throw new EntityNotFoundException( "Could not load dataset with " + "short name" + " " + shortName + "." );
        }
        return showCellBrowser( ee, meta );
    }

    private ModelAndView showCellBrowser( ExpressionExperiment ee, @Nullable String meta ) {
        if ( !cellBrowserService.hasBrowser( ee ) ) {
            throw new EntityNotFoundException( ee.getShortName() + " does not have a cell browser." );
        }
        return new ModelAndView( "expressionExperiment.cellBrowser" )
                .addObject( "ee", ee )
                .addObject( "keywords", getKeywords( ee ) )
                .addObject( "cellBrowserUrl", cellBrowserService.getBrowserUrl( ee, meta ) );
    }

    private String getKeywords( ExpressionExperiment ee ) {
        return expressionExperimentService.getAnnotations( ee ).stream()
                .map( AnnotationValueObject::getTermName )
                .collect( Collectors.joining( "," ) );
    }
}
