/*
 * The Gemma_sec1 project
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
package gemma.gsec.model;

/**
 * Interface that indicates an entity can be secured. By default, permissions are inherited by associated objects.
 *
 * @author paul
 * @version $Id: Securable.java,v 1.4 2013/03/16 00:39:24 paul Exp $
 */
public interface Securable {

    /**
     * Obtain the ID of this object.
     */
    Long getId();
}
