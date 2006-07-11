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
package ubic.gemma.loader.entrez.pubmed;

import java.io.IOException;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;

import org.springframework.beans.factory.BeanFactory;
import org.xml.sax.SAXException;

import ubic.gemma.loader.util.AbstractSpringAwareCLI;
import ubic.gemma.model.common.description.BibliographicReference;

/**
 * Simple application to perform pubmed searches from a list of terms, and persist the results in the database.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class PubMedSearcher extends AbstractSpringAwareCLI {
    protected static BeanFactory ctx = null;
    static PubMedSearch pms = new PubMedSearch();

    /**
     * @param args
     */
    @SuppressWarnings("unchecked")
    protected static Exception doWork( String[] args ) throws Exception {
        PubMedSearcher searcher = new PubMedSearcher();
        Exception err = searcher.processCommandLine( "pubmed [options] searchterm1 searchterm2 ... searchtermN", args );

        if ( err != null ) return err;

        try {
            Collection<BibliographicReference> refs = pms.searchAndRetriveByHTTP( searcher.getArgList() );

            System.out.println( refs.size() + " references found" );

            if ( searcher.hasOption( "d" ) ) {
                searcher.getPersisterHelper().persist( refs );
            }

        } catch ( IOException e ) {
            e.printStackTrace();
        } catch ( SAXException e ) {
            e.printStackTrace();
        } catch ( ParserConfigurationException e ) {
            e.printStackTrace();
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.util.AbstractSpringAwareCLI#buildOptions()
     */
    @Override
    protected void buildOptions() {
        addOption( "d", "persist", false, "Persist the results. Otherwise just search." );
    }

}
