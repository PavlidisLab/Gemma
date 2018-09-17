/*
 * The Gemma project
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
package ubic.gemma.core.image.aba;

/**
 * allen brain Atlas Image class. represents 1 image in the Allen brain atlas library.
 *
 * @author kelsey
 */
@SuppressWarnings("unused") // Frontend use
public class Image {

    private String displayName;
    private int id;
    private int height;
    private int width;
    private String affPath;
    private String addExpressionPath;
    private String url;
    private Integer sectionNumber;

    public Image( String displayName, int id, int height, int width, String affPath, String affExpressionPath,
            String url, int sectionNumber ) {

        this.displayName = displayName;
        this.id = id;
        this.height = height;
        this.width = width;
        this.affPath = affPath;
        this.addExpressionPath = affExpressionPath;
        this.url = url;
        this.sectionNumber = sectionNumber;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getId() {
        return id;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public String getPath() {
        return affPath;
    }

    public String getAffPath() {
        return affPath;
    }

    public String getAddExpressionPath() {
        return addExpressionPath;
    }

    public String getUrl() {
        return url;
    }

    public Integer getSectionNumber() {
        return sectionNumber;
    }

}
