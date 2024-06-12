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
package ubic.gemma.core.analysis.report;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;

import java.util.Collection;

/**
 * @author paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public interface ArrayDesignReportService {

    /**
     * Report summarizing _all_ array designs.
     */
    void generateAllArrayDesignReport();

    /**
     * Generate reports for all array designs, as well as the "global" report.
     */
    @Secured({ "GROUP_AGENT" })
    void generateArrayDesignReport();

    @Secured({ "GROUP_AGENT" })
    void generateArrayDesignReport( ArrayDesignValueObject adVo );

    @Secured({ "GROUP_AGENT" })
    ArrayDesignValueObject generateArrayDesignReport( Long id );

    ArrayDesignValueObject getSummaryObject( Long id );

    ArrayDesignValueObject getSummaryObject();

    Collection<ArrayDesignValueObject> getSummaryObject( Collection<Long> ids );

    void fillEventInformation( Collection<ArrayDesignValueObject> adVos );

    void fillInSubsumptionInfo( Collection<ArrayDesignValueObject> valueObjects );

    void fillInValueObjects( Collection<ArrayDesignValueObject> adVos );

    String getLastSequenceUpdateEvent( Long id );

    String getLastSequenceAnalysisEvent( Long id );

    String getLastRepeatMaskEvent( Long id );

    String getLastGeneMappingEvent( Long id );

    String getCreateDate( Long id );

}