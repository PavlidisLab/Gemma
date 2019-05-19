/*
 * The gemma project
 * 
 * Copyright (c) 2014 University of British Columbia
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

package ubic.gemma.core.analysis.report;

import org.springframework.security.access.annotation.Secured;

/**
 * Interface for twitter feed functionality.
 *
 * @author paul
 */
@Deprecated
public interface TwitterOutbound {

    void disable();

    void enable();

    @Secured({ "GROUP_AGENT" })
    void sendDailyFeed();

    void sendManualTweet( String feed );

    String generateDailyFeed();

}