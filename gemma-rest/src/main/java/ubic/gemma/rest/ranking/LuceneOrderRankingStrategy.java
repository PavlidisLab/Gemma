/*
 * The gemma-rest project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.rest.ranking;

import org.springframework.stereotype.Component;
import ubic.gemma.model.common.description.CharacteristicValueObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * No-op default — returns the input list unchanged (as a defensive copy). This is the regression-safe
 * fallback that preserves today's Lucene-relevance ordering. Selected when the client passes
 * {@code ?rank=lucene} (the default) or omits the parameter.
 */
@Component("lucene")
public class LuceneOrderRankingStrategy implements AnnotationSearchRankingStrategy {

    public static final String NAME = "lucene";

    @Override
    public List<CharacteristicValueObject> rank( String originalQuery,
            List<CharacteristicValueObject> rawHits,
            Map<String, Integer> usageCountsByUri ) {
        return new ArrayList<>( rawHits );
    }

    @Override
    public String getName() {
        return NAME;
    }
}
