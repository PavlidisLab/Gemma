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
import org.springmodules.javaspaces.DelegatingWorker;

import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

/**
 * Title: Spring based Master Worker example Description: This class is the worker which is called remotely by the
 * master
 * <p>
 * The example demonstrates the Master Worker pattern using the GigaSpaces Spring based remote invocation.
 * 
 * @author keshav
 * @version $Id$
 * @since 5.1
 */
public class Worker {
    // member for gigaspaces template
    private GigaSpacesTemplate template;
    // The delegator worker
    private DelegatingWorker iTestBeanWorker;
    private ApplicationContext applicationContext;

    private Thread itbThread;

    protected void init() throws Exception {
        applicationContext = new ClassPathXmlApplicationContext( "ubic/gemma/gigaspaces.xml" );
        template = ( GigaSpacesTemplate ) applicationContext.getBean( "gigaspacesTemplate" );
        iTestBeanWorker = ( DelegatingWorker ) applicationContext.getBean( "testBeanWorker" );
    }

    protected void start() {
        itbThread = new Thread( iTestBeanWorker );
        itbThread.start();
    }

    public static void main( String[] args ) {
        try {
            System.out.println( "\nWelcome to Spring GigaSpaces Worker remote Example!\n" );
            Worker worker = new Worker();
            worker.init();
            worker.start();
        } catch ( Exception ux ) {
            ux.printStackTrace();
            System.err.println( "transError problem..." + ux.getMessage() );
        }
    }
}
