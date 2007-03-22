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
package ubic.gemma.javaspaces.gigaspaces;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

/**
 * Title: Spring based Master Worker example Description: This class is the master which calls a remote task's execute
 * method.
 * <p>
 * The example demonstrates the Master Worker pattern using the GigaSpaces Spring based remote invocation.
 * 
 * @author keshav
 * @version $Id$
 * @since 5.1
 */
public class Master {
    // member for gigaspaces template
    private GigaSpacesTemplate template;
    private ApplicationContext applicationContext;

    public Master() {
    }

    public static void main( String[] args ) {
        try {

            System.out.println( "\nWelcome to the Spring GigaSpaces based Master Worker remote example!\n" );
            Master master = new Master();
            master.init();
            master.start();
        } catch ( Exception e ) {
            System.err.println( "Transformation error..." + e.getMessage() );
            e.printStackTrace();
        }

    }

    protected void init() throws Exception {
        applicationContext = new ClassPathXmlApplicationContext( "ubic/gemma/applicationContext-gigaspaces.xml" );
        template = ( GigaSpacesTemplate ) applicationContext.getBean( "gigaspacesTemplate" );
    }

    protected void start() {
        Task proxy = ( Task ) applicationContext.getBean( "proxy" );
        for ( int i = 0; i < 2; i++ ) {
            Stopwatch stopwatch = new Stopwatch();
            String name = "data" + i;
            stopwatch.start();
            Result res = proxy.execute( name );
            long wt = stopwatch.stop().getElapsedTime();
            System.out.println( "Submitted Job " + res.getTaskID() + " with " + " in " + wt + " ms" );

        }
    }

}
