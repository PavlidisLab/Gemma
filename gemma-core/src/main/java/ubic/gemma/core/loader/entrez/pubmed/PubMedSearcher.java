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

import org.xml.sax.SAXException;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.util.AbstractCLIContextCLI;
import ubic.gemma.model.common.description.BibliographicReference;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Collection;

/**
 * Simple application to perform pubmed searches from a list of terms, and persist the results in the database.
 *
 * @author pavlidis
 */
public class PubMedSearcher extends AbstractCLIContextCLI {
    private static final PubMedSearch pms = new PubMedSearch();

    PubMedSearcher() {
        super();
    }

    public static void main( String[] args ) {
        PubMedSearcher p = new PubMedSearcher();
        executeCommand( p, args );
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
    protected void buildOptions() {
        this.addOption( "d", "persist", "Persist the results. Otherwise just search.", null );
    }

    @Override
    protected Exception doWork( String[] args ) {

        Exception err = this.processCommandLine( args );

        if ( err != null )
            return err;

        try {
            @SuppressWarnings("unchecked")
            Collection<BibliographicReference> refs = PubMedSearcher.pms
                    .searchAndRetrieveByHTTP( ( Collection<String> ) this.getArgList() );

            System.out.println( refs.size() + " references found" );

            if ( this.hasOption( "d" ) ) {
                this.getPersisterHelper().persist( refs );
            }

        } catch ( IOException | ParserConfigurationException | SAXException e ) {
            return e;
        }
        return null;
    }

    @Override
    public String getShortDesc() {
        return "perform pubmed searches from a list of terms, and persist the results in the database";
    }

}