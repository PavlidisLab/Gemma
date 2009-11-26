/*
 * The Gemma project
 * 
 * Copyright (c) 2009 University of British Columbia
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
package ubic.gemma.apps;

import ubic.gemma.analysis.report.DatabaseViewGenerator;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Simple driver of DatabaseViewGenerator.
 * 
 * @author paul
 * @version $Id$
 * @see DatabaseViewGenerator.
 */
public class DatabaseViewGeneratorCLI extends AbstractSpringAwareCLI {

    /**
     * @param args
     */
    public static void main( String[] args ) {
        DatabaseViewGeneratorCLI o = new DatabaseViewGeneratorCLI();
        o.doWork( args );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.util.AbstractSpringAwareCLI#getShortDesc()
     */
    @Override
    public String getShortDesc() {
        return "Generate views of the database in flat files";
    }

    @Override
    protected void buildOptions() {
        super.buildStandardOptions();
    }

    @Override
    protected Exception doWork( String[] args ) {
        super.processCommandLine( "DatabaseViewGeneratorCLI", args );

        DatabaseViewGenerator v = ( DatabaseViewGenerator ) getBean( "databaseViewGenerator" );
        v.runAll();

        return null;
    }

}
