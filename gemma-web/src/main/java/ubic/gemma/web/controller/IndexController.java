package ubic.gemma.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ubic.gemma.search.indexer.IndexService;
import ubic.gemma.tasks.maintenance.IndexerTaskCommand;

@Controller
public class IndexController {

    @Autowired
    IndexService indexService;
    
    public String index (IndexerTaskCommand command) {        
        return this.indexService.index( command );
    }
}
