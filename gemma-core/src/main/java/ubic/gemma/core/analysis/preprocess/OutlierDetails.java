/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.analysis.preprocess;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Container for details about a proposed outlier.
 *
 * @author paul
 */
@Data
@EqualsAndHashCode(of = { "bioAssayId" })
public class OutlierDetails implements Serializable {

    /**
     * Compare outliers by first quartile Note: this comparator imposes orderings that are inconsistent with equals
     */
    public static final Comparator<OutlierDetails> FIRST_QUARTILE_COMPARATOR = Comparator.comparingDouble( OutlierDetails::getFirstQuartile );

    /**
     * Compare outliers by median correlation Note: this comparator imposes orderings that are inconsistent with equals
     */
    public static final Comparator<OutlierDetails> MEDIAN_COMPARATOR = Comparator.comparingDouble( OutlierDetails::getMedianCorrelation );

    private final Long bioAssayId;

    private double firstQuartile = Double.MIN_VALUE;
    private double medianCorrelation = Double.MIN_VALUE;
    private double thirdQuartile = Double.MIN_VALUE;
}
