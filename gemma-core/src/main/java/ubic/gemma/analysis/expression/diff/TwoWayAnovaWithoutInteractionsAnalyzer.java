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
package ubic.gemma.analysis.expression.diff;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * A two way anova implementation without interactions as described by P. Pavlidis, Methods 31 (2003) 282-289.
 * 
 * @author keshav
 * @version $Id$
 * @see AbstractTwoWayAnovaAnalyzer
 */
@Component
@Scope(value = "prototype")
public class TwoWayAnovaWithoutInteractionsAnalyzer extends LinearModelAnalyzer {

    /*
     * TODO check constraints
     */
}
