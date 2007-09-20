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
package ubic.gemma.analysis.diff;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * @author keshav
 * @version $Id$
 */
public class TTestAnalyzerTest extends AbstractAnalyzerTest {

    TTestAnalyzer analyzer = new TTestAnalyzer();

    private Log log = LogFactory.getLog( this.getClass() );

    /**
     * 
     *
     */
    public void testTTest() {
        Collection<FactorValue> factorValues = ef.getFactorValues();
        assertEquals( factorValues.size(), 2 );

        Iterator<FactorValue> iter = factorValues.iterator();

        FactorValue factorValueA = iter.next();

        FactorValue factorValueB = iter.next();

        Map pvaluesMap = analyzer.tTest( matrix, factorValueA, factorValueB, biomaterials );

        log.info( pvaluesMap );

        assertEquals( pvaluesMap.size(), 6 );
    }

}
