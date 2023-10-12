/*
 * The Gemma project
 *
 * Copyright (c) 2013 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.loader.association.phenotype;

import ubic.basecode.ontology.providers.HumanPhenotypeOntologyService;
import ubic.basecode.ontology.providers.MedicOntologyService;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.ontology.providers.MondoOntologyService;
import ubic.gemma.core.util.AbstractAuthenticatedCLI;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

/**
 * @author nicolas
 */
@Deprecated
public abstract class ExternalDatabaseEvidenceImporterAbstractCLI extends AbstractAuthenticatedCLI {

    protected String writeFolder = null;

    protected PhenotypeProcessingUtil ppUtil;

    protected TaxonService taxonService;
    protected GeneService geneService;

    // the init is in the constructor, we always need those
    public ExternalDatabaseEvidenceImporterAbstractCLI() {
        super();
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.PHENOTYPES;
    }

    /**
     * load all needed services and initialize data structures
     */
    protected void init() throws Exception {
        this.geneService = this.getBean( GeneService.class );
        this.taxonService = getBean( TaxonService.class );
        this.ppUtil = new PhenotypeProcessingUtil( geneService, getBean( MedicOntologyService.class ),
                getBean( MondoOntologyService.class ), getBean( HumanPhenotypeOntologyService.class ) );
    }

}
