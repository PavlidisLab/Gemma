/*
 * The Gemma project
 *
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.basecode.ontology.providers.ExperimentalFactorOntologyService;
import ubic.gemma.core.loader.expression.simple.ExperimentalDesignImporter;
import ubic.gemma.core.ontology.OntologyUtils;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Paul
 * @see ExperimentalDesignImporter
 */
public class ExperimentalDesignImportCli extends ExpressionExperimentManipulatingCLI {

    @Autowired
    private ExperimentalDesignImporter edImp;
    @Autowired
    private ExpressionExperimentService ees;
    @Autowired
    private ExperimentalFactorOntologyService mos;

    private Path experimentalDesignFile;

    public ExperimentalDesignImportCli() {
        super();
        setSingleExperimentMode();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        OntologyUtils.ensureInitialized( mos );
    }

    @Override
    public String getCommandName() {
        return "importDesign";
    }

    @Override
    public String getShortDesc() {
        return "Import an experimental design";
    }

    @Override
    protected void buildExperimentOptions( Options options ) {
        Option designFileOption = Option.builder( "f" ).required().hasArg().type( Path.class ).argName( "Design file" )
                .desc( "Experimental design description file" ).longOpt( "designFile" ).build();
        options.addOption( designFileOption );
    }

    @Override
    protected void processExperimentOptions( CommandLine commandLine ) throws ParseException {
        experimentalDesignFile = commandLine.getParsedOptionValue( 'f' );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        try ( InputStream inputStream = Files.newInputStream( experimentalDesignFile ) ) {
            expressionExperiment = ees.thawLite( expressionExperiment );
            edImp.importDesign( expressionExperiment, inputStream );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }
}
