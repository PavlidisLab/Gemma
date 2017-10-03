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

    public PubMedSearcher() {
        super();
    }

    public static void main( String[] args ) {
        PubMedSearcher p = new PubMedSearcher();
        try {
            p.doWork( args );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
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
    public String getShortDesc() {
        return "perform pubmed searches from a list of terms, and persist the results in the database";
    }

    @Override
    protected void buildOptions() {
        addOption( "d", "persist", false, "Persist the results. Otherwise just search." );
    }

    @Override
    protected Exception doWork( String[] args ) {

        Exception err = processCommandLine( args );

        if ( err != null )
            return err;

        try {
            @SuppressWarnings("unchecked") Collection<BibliographicReference> refs = pms
                    .searchAndRetrieveByHTTP( ( Collection<String> ) getArgList() );

            System.out.println( refs.size() + " references found" );

            if ( hasOption( "d" ) ) {
                getPersisterHelper().persist( refs );
            }

        } catch ( IOException | ParserConfigurationException | SAXException e ) {
            return e;
        }
        resetLogging();
        return null;
    }

}