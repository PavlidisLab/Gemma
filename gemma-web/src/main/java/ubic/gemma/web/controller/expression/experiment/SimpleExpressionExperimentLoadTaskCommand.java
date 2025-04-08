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
package ubic.gemma.web.controller.expression.experiment;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.model.common.quantitationtype.ScaleType;

import java.util.Collection;
import java.util.Locale;

/**
 * Extends a SimpleExpressionExperimentMetaData with information about the file and provide simplified taxon/platform
 * information.
 *
 * @author pavlidis
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SimpleExpressionExperimentLoadTaskCommand extends TaskCommand {

    private static final long serialVersionUID = 1L;

    private String shortName;
    private String name;
    private String description;
    private Long taxonId;
    private Collection<Long> arrayDesignIds;
    private String quantitationTypeName;
    private String quantitationTypeDescription;
    private Boolean isRatio = Boolean.FALSE;
    private ScaleType scale;
    private Integer pubMedId;

    private boolean validateOnly;
    private String serverFilePath;
    private String originalFileName;
}
