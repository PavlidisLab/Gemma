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

import java.io.Serializable;

/**
 * Title: Spring based Master Worker example Description: This class is the task interface which all tasks should
 * implement.
 * <p>
 * The example demonstrates the Master Worker pattern using the GigaSpaces Spring based remote invocation.
 * 
 * @author keshav
 * @version $Id$
 * @since 5.1
 */
public interface Task extends Serializable {
    public Result execute( String data );
}
