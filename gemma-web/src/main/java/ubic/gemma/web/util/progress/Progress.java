/*
 * The Gemma project
 * 
 * Copyright (c) 2006 Columbia University
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

package ubic.gemma.web.util.progress;

/**
 * 
 *
 * <hr>
 * <p>Copyright (c) 2006 UBC Pavlab
 * @author klc
 * @version $Id$
 * 
 * Any process that would like to have a long process monitored and displayed to the user can implement this interface.
 * The implementer of this interface must simply
 */
public interface Progress {

    void updatePercent(ProgressData progress);
    
}
