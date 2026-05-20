/*
 * The baseCode project
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
package ubic.gemma.core.util.math.linearmodels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ubic.gemma.core.util.matrix.DenseDoubleMatrix;
import ubic.gemma.core.util.matrix.DoubleMatrix;
import ubic.gemma.core.util.matrix.ObjectMatrix;
import ubic.gemma.core.util.matrix.StringMatrix;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

/**
 * Represents the A matrix in regression problems posed as Ax=b. The starting point is a matrix of sample information,
 * where the rows are sample names and the columns are the names of factors or covariates. Intercept and interaction
 * terms can be added.
 * <p>
 * Baseline levels are initially determined by the order in which factor levels appear. You can re-level using the
 * setBaseline method.
 * <p>
 *
 * @author paul
 * 
 */
public class DesignMatrix {

    private static Logger log = LoggerFactory.getLogger( DesignMatrix.class );
    /**
     *
     */
    private List<Integer> assign = new ArrayList<>();

    /**
     * Names of factors for which at least some coefficients were dropped.
     */
    private final Set<String> droppedFactors = new HashSet<>();

    private boolean hasIntercept = false;

    private final Set<String[]> interactions = new LinkedHashSet<>();

    /**
     * 
     * @return a collection of String arrays. If empty, there are no interactions. Each array represents an interaction
     *         in the model. The elements of the array are the terms included in the interaction, as Strings provided when calling addInteraction.
     */
    public Collection<String[]> getInteractionTerms() {
        return interactions;
    }

    /**
     * Only applied for categorical factors.
     */
    private final Map<String, List<String>> levelsForFactors = new LinkedHashMap<>();

    private DoubleMatrix<String, String> matrix;

    /**
     * Store which terms show up in which columns of the design
     */
    private Map<String, List<Integer>> terms = new LinkedHashMap<>();

    /**
     * Saved version of the original factors provided.
     */
    private final Map<String, List<Object>> valuesForFactors = new LinkedHashMap<>();

    /**
     * Whether to try to remove unusable factors from the design matrix (currently applies to interactions). If false,
     * the design matrix might need modification by the caller.
     */
    private boolean strict = true;

    /**
     * @param factor in form of Doubles or Strings. Any other types will yield errors.
     * @param start
     */
    public DesignMatrix( Object[] factor, int start, String factorName ) {
        matrix = this.buildDesign( 1, Arrays.asList( factor ), null, start, factorName );
    }

    /**
     * @param sampleInfo in form of Doubles or Strings. Any other types will yield errors.
     */
    public DesignMatrix( ObjectMatrix<String, String, ? extends Object> sampleInfo ) {
        this( sampleInfo, true );
    }

    /**
     * @param sampleInfo in form of Doubles or Strings. Any other types will yield errors.
     * @param intercept
     */
    public DesignMatrix( ObjectMatrix<String, String, ?> sampleInfo, boolean intercept ) {
        matrix = this.designMatrix( sampleInfo, intercept );
        this.hasIntercept = intercept;
        if ( sampleInfo.getRowNames().size() == matrix.rows() ) matrix.setRowNames( sampleInfo.getRowNames() );

    }

    /**
     * In limma, the contrasts are built by referring to the coefficients by name, which isn't what we want.
     * 
     * 
     * @return
     */
    public DoubleMatrix2D makeContrasts() {
        /*
         * Limma: This function expresses contrasts between a set of parameters as a numeric matrix. The parameters are
         * usually
         * the coefficients from a linear model fit, so the matrix specifies which comparisons between the coefficients
         * are to be extracted from the fit. The output from this function is usually used as input to contrasts.fit.
         * The contrasts can be specified either as expressions using ... or as a character vector through contrasts.
         * (Trying to specify contrasts both ways will cause an error.)
         * 
         * 
         */
        throw new RuntimeException();
    }

    public DesignMatrix( StringMatrix<String, String> sampleInfo ) {
        this( sampleInfo, true );
    }

    /**
     * @param design
     * @param intercept whether to include an intercept in the model
     * @param strict    whether to remove columns from the design matrix that are unlikely to be useable. By default
     *                  this is "true" for other constructors.
     */
    public DesignMatrix( ObjectMatrix<String, String, Object> design, boolean intercept, boolean strict ) {
        this( design, intercept );
        this.strict = strict;
    }

    /**
     * Append additional factors/covariates to this
     *
     * @param sampleInfo
     */
    public void add( ObjectMatrix<String, String, Object> sampleInfo ) {
        this.matrix = this.designMatrix( sampleInfo, this.matrix );
    }

    /**
     * Add interaction term; only works if there exactly two factors so this can figure out which interaction to add.
     * For more control use the other method.
     */
    public void addInteraction() {
        if ( this.terms.size() != 2 && !hasIntercept ) {
            throw new IllegalArgumentException( "You must specify which two terms" );
        }

        if ( this.terms.size() == 2 && hasIntercept || this.terms.size() < 2 ) {
            throw new IllegalArgumentException( "You need at least two terms" );
        }

        if ( this.terms.size() > 3 ) {
            throw new IllegalArgumentException( "You must specify which two terms, there are " + this.terms.size()
                    + " terms: " + StringUtils.join( this.terms.keySet(), "," ) );
        }

        List<String> iterms = new ArrayList<>();
        for ( String t : terms.keySet() ) {
            if ( t.equals( LinearModelSummary.INTERCEPT_COEFFICIENT_NAME ) ) {
                continue;
            }
            iterms.add( t );
        }

        this.addInteraction( iterms.toArray( new String[] {} ) );
    }

    /**
     * This will not add the interaction unless all of the terms are already part of the design, and interactions that
     * obviously can't be estimated will be dropped if possible, but otherwise this is fairly brain dead.
     *
     * @param interactionTerms
     */
    public void addInteraction( String... interactionTerms ) {

        /*
         * If any of these terms were already dropped, bail.
         */
        for ( String t1 : interactionTerms ) {
            if ( !this.getLevelsForFactors().containsKey( t1 ) ) {
                log.warn( "Can't add interaction involving a non-existent or unused terms: " + t1 );
                return;
            }
        }

        /*
         * Figure out which columns of the data we need to look at.
         *
         * If the factor is in >1 columns - we have to add two or more columns
         */
        Collection<String> doneTerms = new HashSet<>();

        // the column where the interaction "goes"
        int interactionIndex = terms.size();

        String termName = StringUtils.join( interactionTerms, ":" );
        Set<String> usedInteractionTerms = new HashSet<>();
        Arrays.sort( interactionTerms );
        for ( String t1 : interactionTerms ) {

            if ( doneTerms.contains( t1 ) ) continue;
            List<Integer> cols1 = terms.get( t1 );

            for ( int i = 0; i < cols1.size(); i++ ) {
                double[] col1i = this.matrix.getColumn( cols1.get( i ) );

                assert col1i.length > 0;

                for ( String t2 : interactionTerms ) {
                    if ( t1.equals( t2 ) ) continue;
                    doneTerms.add( t2 );
                    List<Integer> cols2 = terms.get( t2 );

                    assert cols2 != null;

                    for ( int j = 0; j < cols2.size(); j++ ) {
                        double[] col2i = this.matrix.getColumn( cols2.get( j ) );

                        Double[] prod = new Double[col1i.length];

                        this.matrix = this.copyWithSpace( this.matrix, this.matrix.columns() + 1 );
                        String columnName = null;
                        int numValid = 0;
                        for ( int k = 0; k < col1i.length; k++ ) {
                            prod[k] = col1i[k] * col2i[k]; // compute the value for the interaction, should be either 0 or 1
                            if ( prod[k] != 0 ) numValid++;
                            // We don't really need the columnName to be distinct from the term name, it just
                            // makes it clearer which factor values are considered non-zero combination.
                            if ( prod[k] != 0 && StringUtils.isBlank( columnName ) ) {
                                String if1 = this.valuesForFactors.get( t1 ).get( k ).toString();
                                String if2 = this.valuesForFactors.get( t2 ).get( k ).toString();
                                columnName = t1 + if1 + ":" + t2 + if2;
                            }

                            matrix.set( k, this.matrix.columns() - 1, prod[k] );
                        }

                        if ( numValid < 2 && strict ) {
                            /*
                             * remove the column, we won't be able to estimate it
                             */
                            log.info( "Interaction term " + termName + " won't be estimable, dropping" );
                            matrix = matrix.getColRange( 0, this.matrix.columns() - 2 );
                            continue;
                        }

                        boolean redundant = checkForRedundancy( this.matrix, this.matrix.columns() - 2 );
                        if ( redundant && strict ) {
                            /*
                             * remove the column.
                             */
                            log.info( "Interaction term " + termName + " is redundant with another column, dropping" );
                            matrix = matrix.getColRange( 0, this.matrix.columns() - 2 );
                            continue;
                        }

                        usedInteractionTerms.add( t1 );
                        usedInteractionTerms.add( t2 );

                        matrix.addColumnName( columnName );
                        if ( !this.terms.containsKey( termName ) ) {
                            this.terms.put( termName, new ArrayList<Integer>() );
                        }
                        terms.get( termName ).add( this.matrix.columns() - 1 );
                        assign.add( interactionIndex );
                    }
                }
            }
        }

        if ( !usedInteractionTerms.isEmpty() ) {
            this.interactions.add( usedInteractionTerms.toArray( new String[] {} ) );
        }

    }

    /**
     * @return
     */
    public List<Integer> getAssign() {
        return assign;
    }

    public String getBaseline( String factorName ) {
        return this.levelsForFactors.get( factorName ).toString();
    }

    public DoubleMatrix2D getDoubleMatrix() {
        return new DenseDoubleMatrix2D( matrix.asDoubles() );
    }

    public Map<String, List<String>> getLevelsForFactors() {
        return levelsForFactors;
    }

    public DoubleMatrix<String, String> getMatrix() {
        return matrix;
    }

    public List<String> getTerms() {
        List<String> result = new ArrayList<>();
        result.addAll( terms.keySet() );
        return result;
    }

    public Map<String, List<Object>> getValuesForFactors() {
        return valuesForFactors;
    }

    public boolean hasIntercept() {
        return this.hasIntercept;
    }

    public boolean isHasIntercept() {
        return hasIntercept;
    }

    /**
     * @param factorName
     * @param baselineFactorValue
     */
    public void setBaseline( String factorName, String baselineFactorValue ) {
        if ( !this.levelsForFactors.containsKey( factorName ) ) {
            throw new IllegalArgumentException( "No factor known by name " + factorName + ", choices are: "
                    + StringUtils.join( this.levelsForFactors.keySet(), "," ) );
        }

        if ( this.droppedFactors.contains( factorName ) ) {
            log.warn( "Can't set baseline for a dropped factor, skipping" );
            return;
        }

        List<String> oldValues = this.levelsForFactors.get( factorName );
        int index = oldValues.indexOf( baselineFactorValue );
        if ( index < 0 ) {
            throw new IllegalArgumentException( baselineFactorValue + " is not a level of the factor " + factorName );
        }

        if ( index == 0 ) return;

        /*
         * Put the given level in the desired location; move the others along.
         */
        List<String> releveled = new ArrayList<>();
        releveled.add( oldValues.get( index ) );
        for ( int i = 0; i < oldValues.size(); i++ ) {
            if ( i == index ) continue;
            releveled.add( oldValues.get( i ) );
        }
        this.levelsForFactors.put( factorName, releveled );

        /*
         * Recompute the design.
         */
        this.rebuild();
    }

    @Override
    public String toString() {
        return this.matrix.toString();
    }

    /**
     * Refresh the design matrix, for example after releveling.
     */
    protected void rebuild() {
        this.matrix = null;
        this.assign.clear();
        this.terms.clear();

        if ( this.hasIntercept ) {
            int nrows = valuesForFactors.values().iterator().next().size();
            matrix = addIntercept( nrows );
        }

        int i = 0;
        for ( String factorName : valuesForFactors.keySet() ) {
            List<Object> factorValues = valuesForFactors.get( factorName );
            this.valuesForFactors.put( factorName, factorValues );

            if ( factorValues.get( 0 ) instanceof String && !this.levelsForFactors.containsKey( factorName ) ) {
                this.levels( factorName, factorValues.toArray( new String[] {} ) );
            }

            matrix = buildDesign( i + 1, factorValues, matrix, 2, factorName );

            i++;
        }

        if ( !this.interactions.isEmpty() ) {
            List<String[]> redoInteractionTerms = new ArrayList<>();
            for ( String[] interactionTerms : interactions ) {
                redoInteractionTerms.add( interactionTerms );
            }
            this.interactions.clear();
            for ( String[] t : redoInteractionTerms ) {
                this.addInteraction( t );
            }
        }
    }

    /**
     * @param  vec
     * @param  inputDesign
     * @return
     */
    private DoubleMatrix<String, String> addContinuousCovariate( List<?> vec,
            DoubleMatrix<String, String> inputDesign ) {
        DoubleMatrix<String, String> tmp;
        /*
         * CONTINUOUS COVARIATE
         */
        log.debug( "Treating factor as continuous covariate" );
        if ( inputDesign != null ) {
            /*
             * copy it into a new one.
             */
            assert vec.size() == inputDesign.rows();
            int numberofColumns = inputDesign.columns() + 1;
            tmp = copyWithSpace( inputDesign, numberofColumns );
        } else {
            tmp = new DenseDoubleMatrix<>( vec.size(), 1 );
            tmp.assign( 0.0 );
        }
        int startcol = 0;
        if ( inputDesign != null ) {
            startcol = inputDesign.columns();
        }
        for ( int i = startcol; i < tmp.columns(); i++ ) {
            for ( int j = 0; j < tmp.rows(); j++ ) {
                tmp.set( j, i, ( Double ) vec.get( j ) );
            }
        }
        return tmp;
    }

    /**
     * @param  rows
     * @return
     */
    private DoubleMatrix<String, String> addIntercept( int rows ) {
        DoubleMatrix<String, String> tmp;
        tmp = new DenseDoubleMatrix<>( rows, 1 );
        tmp.addColumnName( LinearModelSummary.INTERCEPT_COEFFICIENT_NAME );
        tmp.assign( 1.0 );
        this.assign.add( 0 );
        this.terms.put( LinearModelSummary.INTERCEPT_COEFFICIENT_NAME, new ArrayList<Integer>() );
        this.terms.get( LinearModelSummary.INTERCEPT_COEFFICIENT_NAME ).add( 0 );
        return tmp;
    }

    /**
     * The primary method for actually setting up the design matrix. Redundant or constant columns are dropped (except
     * the intercept, if included)
     *
     * @param  columnNum        column of the input matrix are we working on.
     * @param  factorValues of doubles or strings.
     * @param  inputDesign
     * @param  start        1 or 2. Set to 1 to get a column for each level (must not have an intercept in the model);
     *                      Set to 2
     *                      to get a column for all but the last (Redundant) level (or another redundant column)
     * @param  factorName   String to associate with the factor
     * @return
     */
    @SuppressWarnings("unchecked")
    private DoubleMatrix<String, String> buildDesign( int columnNum, List<?> factorValues,
            DoubleMatrix<String, String> inputDesign, final int start, String factorName ) {

        int startUsed = start;

        if ( !terms.containsKey( factorName ) ) {
            terms.put( factorName, new ArrayList<Integer>() );
        }
        DoubleMatrix<String, String> tmp = null;
        if ( factorValues.get( 0 ) instanceof Double ) {
            tmp = addContinuousCovariate( factorValues, inputDesign );
            this.assign.add( columnNum );
            terms.get( factorName ).add( columnNum );
            tmp.addColumnName( factorName );
        } else {
            /*
             * CATEGORICAL COVARIATE
             */
            List<String> levels;
            if ( this.levelsForFactors.containsKey( factorName ) ) {
                levels = this.levelsForFactors.get( factorName );
            } else {
                levels = levels( factorName, ( List<String> ) factorValues );
            }

            // if ( levels.size() == 1 ) {
            // /*
            // * This is okay if it is the intercept (as in a one-sample t-test). So this isn't the place to
            // * do this. We assume the user knows what they are doing.
            // */
            // log.warn( "Factor " + factorName + " was constant; not adding to the design" );
            // this.droppedFactors.add( factorName );
            // return inputDesign;
            // }

            tmp = inputDesign;

            List<String> levelList = new ArrayList<>();
            levelList.addAll( levels );

            int startcol = 0;
            if ( tmp != null ) {
                startcol = inputDesign.columns();
            }

            int currentColumn = startcol;
            int maxColumn = levels.size() + startcol - ( startUsed - 1 );
            Collection<String> usedLevels = new HashSet<>();
            for ( int i = startcol; i < maxColumn; i++ ) {

                // log.info( tmp );

                int currentLevelIndex = i - startcol + ( startUsed - 1 );

                if ( currentLevelIndex >= levelList.size() ) {
                    if ( startUsed > 1 ) {
                        // go back and use it; we must have removed a redundant level.
                        currentLevelIndex = 0;
                    }
                }

                String level = levelList.get( currentLevelIndex );
                log.debug( "Adding column for Level=" + level + " at index " + currentLevelIndex );

                // make space for the new values
                if ( tmp != null ) {
                    tmp = copyWithSpace( tmp, tmp.columns() + 1 );
                } else {
                    tmp = new DenseDoubleMatrix<>( factorValues.size(), 1 );
                    tmp.assign( 0.0 );
                }

                String contrastingValue = "";
                assert tmp != null;
                for ( int j = 0; j < tmp.rows(); j++ ) {
                    Object fv = factorValues.get(j);

                    if (fv == null) {
                        // This happens if a factorvalue is not assigned somehwere, as in a DEExclude situation.
                        // In Gemma we can reach this during batch correction.
                        throw new IllegalArgumentException("Null value for factor " + factorName + " at row " + j);
                    }

                    boolean isBaseline = !fv.equals( level );
                    if ( !isBaseline ) {
                        contrastingValue = ( String ) fv;
                    }
                    tmp.set( j, currentColumn, isBaseline ? 0.0 : 1.0 );
                }

                // boolean redundant = checkForRedundancy( tmp, currentColumn );
                // if ( redundant ) {
                // log.warn( "Column for factor " + factorName + " level=" + level + " is redundant, dropping" );
                // droppedFactors.add( factorName );
                //
                // tmp = tmp.getColRange( 0, currentColumn - 1 );
                // maxColumn++; // add one more column
                // continue;
                // }

                currentColumn++;

                if ( StringUtils.isBlank( contrastingValue ) ) {
                    contrastingValue = "_" + i;
                }
                tmp.setColumnName( factorName + contrastingValue, currentColumn );
                this.assign.add( columnNum ); // all of these columns use this factor.
                terms.get( factorName ).add( i );
                usedLevels.add( level );
            }
            /*
             * if ( usedLevels.size() < levels.size() ) { log.info( "Used levels: " + StringUtils.join( usedLevels, " "
             * ) ); log.info( "All levels: " + StringUtils.join( levelList, " " ) ); }
             */

        }

        return tmp;
    }

    /**
     * Check if the column is redundant with a previous column
     *
     * @param  tmp
     * @param  column index of column to check.
     * @return        true if a column with index < column is the same as the column at the given index.
     */
    private boolean checkForRedundancy( DoubleMatrix<String, String> tmp, int column ) {
        for ( int p = 0; p < column; p++ ) {

            boolean foundRedundant = true;
            for ( int v = 0; v < tmp.rows(); v++ ) {
                if ( tmp.get( v, column ) != tmp.get( v, p ) ) {
                    foundRedundant = false;
                    break;
                }
            }

            if ( foundRedundant ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add extra empty columns to a matrix, implemented by copying.
     *
     * @param  inputDesign
     * @param  numberofColumns how many to add.
     * @return
     */
    private DoubleMatrix<String, String> copyWithSpace( DoubleMatrix<String, String> inputDesign,
            int numberofColumns ) {
        DoubleMatrix<String, String> tmp;
        tmp = new DenseDoubleMatrix<>( inputDesign.rows(), numberofColumns );
        tmp.assign( 0.0 );

        for ( int i = 0; i < inputDesign.rows(); i++ ) {
            for ( int j = 0; j < inputDesign.columns(); j++ ) {
                String colName = inputDesign.getColName( j );
                if ( i == 0 && colName != null ) {
                    tmp.setColumnName( colName, j );
                }
                tmp.set( i, j, inputDesign.get( i, j ) );
            }
        }

        if ( !inputDesign.getRowNames().isEmpty() ) tmp.setRowNames( inputDesign.getRowNames() );
        return tmp;
    }

    /**
     * Build a "standard" design matrix from a matrix of sample information.
     *
     * @param  sampleInfo
     * @param  intercept  if true, an intercept term is included.
     * @return
     */
    private DoubleMatrix<String, String> designMatrix( ObjectMatrix<String, String, ?> sampleInfo, boolean intercept ) {
        DoubleMatrix<String, String> tmp = null;
        if ( intercept ) {
            int rows = sampleInfo.rows();
            tmp = addIntercept( rows );
        }
        return designMatrix( sampleInfo, tmp );
    }

    /**
     * @param  sampleInfo
     * @param  design
     * @return
     */
    private DoubleMatrix<String, String> designMatrix( ObjectMatrix<String, String, ?> sampleInfo,
            DoubleMatrix<String, String> design ) {
        for ( int i = 0; i < sampleInfo.columns(); i++ ) {
            Object[] factorValuesAr = sampleInfo.getColumn( i );
            List<Object> factorValues = Arrays.asList( factorValuesAr );
            design = buildDesign( i + 1, factorValues, design, 2, sampleInfo.getColName( i ) );
            this.valuesForFactors.put( sampleInfo.getColName( i ), factorValues );
        }
        return design;
    }

    /**
     * @param  vec
     * @return
     */
    private List<String> levels( String factorName, List<String> vec ) {
        return this.levels( factorName, vec.toArray( new String[] {} ) );
    }

    /**
     * @param  factorName
     * @param  vec
     * @return
     */
    private List<String> levels( String factorName, String[] vec ) {
        Set<String> flevs = new LinkedHashSet<>();
        for ( String v : vec ) {
            flevs.add( v );
        }
        List<String> result = new ArrayList<>();
        for ( String fl : flevs ) {
            result.add( fl );
        }
        this.levelsForFactors.put( factorName, result );
        return result;
    }

}
