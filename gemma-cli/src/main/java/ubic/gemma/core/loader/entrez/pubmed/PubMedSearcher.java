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
package ubic.gemma.core.loader.entrez.pubmed;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.util.AbstractAuthenticatedCLI;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.persistence.persister.PersisterHelper;

import java.util.Collection;

/**
 * Simple application to perform pubmed searches from a list of terms, and persist the results in the database.
 *
 * @author pavlidis
 */
public class PubMedSearcher extends AbstractAuthenticatedCLI {
    private static final PubMedSearch pms = new PubMedSearch();
    @Autowired
    private PersisterHelper persisterHelper;
    private Collection<String> args;
    private boolean persist = false;

    public PubMedSearcher() {
        setAllowPositionalArguments( true );
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.MISC;
    }

    @Override
    public String getCommandName() {
        return "pubmedSearchAndSave";
    }

    @Override
    protected void buildOptions( Options options ) {
        options.addOption( "d", "persist", false, "Persist the results. Otherwise just search." );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {
        this.args = commandLine.getArgList();
        this.persist = commandLine.hasOption( "persist" );
    }

    @Override
    protected void doWork() throws Exception {
        @SuppressWarnings("unchecked")
        Collection<BibliographicReference> refs = PubMedSearcher.pms
                .searchAndRetrieveByHTTP( this.args );

        System.out.println( refs.size() + " references found" );

        if ( this.persist ) {
            persisterHelper.persist( refs );
        }
    }

    @Override
    public String getShortDesc() {
        return "perform pubmed searches from a list of terms, and persist the results in the database";
    }

}