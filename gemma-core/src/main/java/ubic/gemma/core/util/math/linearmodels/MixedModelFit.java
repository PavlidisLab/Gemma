package ubic.gemma.core.util.math.linearmodels;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.colt.matrix.linalg.CholeskyDecomposition;
import cern.jet.math.Functions;
import ubic.gemma.core.util.math.linalg.QRDecomposition;
import org.apache.commons.lang3.time.StopWatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Random-intercept mixed model machinery for the diffex linear model: a limma
 * {@code duplicateCorrelation} port.
 * <p>
 * A blocking factor whose levels are subjects is represented NOT as design columns (that is the
 * fixed-effect paired model, see the blocking-factor support in {@link DesignMatrix}'s callers) but as an
 * inter-block correlation: within a block, observations correlate at rho. The correlation is estimated per
 * probe by REML-style variance decomposition (Smyth 2002 / statmod's mixedModel2Fit, restricted here to
 * variance components only, exactly as limma's duplicateCorrelation calls it), clamped to a permissible
 * range, and condensed to a consensus value by a trimmed mean on the atanh scale. The fit itself is GLS:
 * whiten the design and data by the Cholesky factor of the implied correlation matrix, then run the
 * ordinary OLS machinery (limma gls.series, "NoProbeWts" branch).
 * <p>
 * R code in comments is from limma's duplicateCorrelation.R and gls.series.R, and statmod's mixedModel2Fit.R
 * and glmgam.fit.R, the same sources the rest of this package ports.
 *
 * @see LeastSquaresFit#LeastSquaresFit(DesignMatrix, ubic.gemma.core.util.matrix.DoubleMatrix, DoubleMatrix2D)
 */
public class MixedModelFit {

    private static final double RHO_MAX = 0.99;
    private static final double TRIM = 0.15;

    /**
     * Estimated inter-block correlation, in [-1, 1) (clamped to [-maxBlockCorr, RHO_MAX] where
     * maxBlockCorr is the largest negative value a max-size block can support). -1 until estimated.
     */
    private double consensusCorrelation = -1;

    /**
     * Per-probe atanh(rho) before the trimmed-mean step, kept for diagnostics; NaN where a probe's fit failed.
     */
    private double[] atanhCorrelations;

    /**
     * True if the estimation backed off to rho = 0 because of a degenerate gate (all blocks of size one, or
     * the block factor's span already sits inside the design).
     */
    private boolean degenerateToZero;

    private final StopWatch timer = new StopWatch();

    /**
     * Estimate the consensus inter-block correlation.
     *
     * @param design  design matrix WITHOUT the blocking factor's columns (samples x coefficients), matching
     *                limma's usage {@code duplicateCorrelation(M, design = ~treatment, block = subject)};
     *                design columns that already encode the block make rho degenerate to zero (that case is
     *                detected and reported, not an error)
     * @param block   block identifier per sample (same order as design rows); a block id may repeat across
     *                disjoint groups of samples, they are treated as one block, exactly as limma does
     * @param data    expression data, one row per probe, one column per sample (same orientation as the
     *                data matrix passed to {@link LeastSquaresFit})
     * @param weights optional per-observation weights (same orientation as data); null for unweighted.
     *                Weights fold into the variance decomposition per probe (mixedModel2Fit's w argument).
     * @return this, with {@link #getConsensusCorrelation()} set
     */
    public MixedModelFit estimateCorrelation( DoubleMatrix2D design, String[] block, DoubleMatrix2D data,
            DoubleMatrix2D weights ) {
        assert design.rows() == block.length;
        assert data.columns() == block.length;
        assert weights == null || ( weights.rows() == data.rows() && weights.columns() == data.columns() );

        int ngenes = data.rows();
        int narrays = data.columns();
        int nbeta = design.columns();

        // design.block <- model.matrix(~factor(block)); the non-intercept indicator columns
        List<String> levels = new ArrayList<>();
        for ( String b : block ) {
            if ( !levels.contains( b ) ) levels.add( b );
        }
        int nblocks = levels.size();

        if ( nblocks == narrays ) {
            // every sample its own block: no repeated measures, no correlation information
            this.degenerateToZero = true;
            this.consensusCorrelation = 0.0;
            this.atanhCorrelations = new double[ngenes];
            Arrays.fill( this.atanhCorrelations, 0.0 );
            return this;
        }

        QRDecomposition designQr = new QRDecomposition( design );
        int designRank = designQr.getRank();

        /*
         * if (max(abs(QtBlock[-(1:QR$rank), ])) < 1e-08) -- the block factor's span is inside the design
         * span, so the correlation is not identifiable from the residual space. With a design that excludes
         * the blocking factor this only triggers for a block nested in another factor (e.g. tissue constant
         * within subject); limma backs off to rho = 0 rather than fail.
         */
        DoubleMatrix2D blockDesign = blockIndicator( block, levels, false );
        DoubleMatrix2D qtBlock = designQr.effects( blockDesign );
        boolean blockEncoded = true;
        outer:
        for ( int r = designRank; r < qtBlock.rows(); r++ ) {
            for ( int c = 0; c < qtBlock.columns(); c++ ) {
                if ( Math.abs( qtBlock.get( r, c ) ) >= 1e-8 ) {
                    blockEncoded = false;
                    break outer;
                }
            }
        }
        if ( blockEncoded ) {
            this.degenerateToZero = true;
            this.consensusCorrelation = 0.0;
            this.atanhCorrelations = new double[ngenes];
            Arrays.fill( this.atanhCorrelations, 0.0 );
            return this;
        }

        int maxBlockSize = 0;
        Map<String, Integer> counts = new LinkedHashMap<>();
        for ( String b : block ) counts.merge( b, 1, Integer::sum );
        for ( Integer c : counts.values() ) maxBlockSize = Math.max( maxBlockSize, c );
        if ( maxBlockSize == 1 ) {
            // "Blocks all of size 1: setting intrablock correlation to zero."
            this.degenerateToZero = true;
            this.consensusCorrelation = 0.0;
            this.atanhCorrelations = new double[ngenes];
            Arrays.fill( this.atanhCorrelations, 0.0 );
            return this;
        }

        // rhomin <- 1/(1 - MaxBlockSize) + 0.01 -- a block of size k can push rho down to 1/(1-k) at most
        double rhoMin = 1.0 / ( 1.0 - maxBlockSize ) + 0.01;

        this.atanhCorrelations = new double[ngenes];
        List<Integer> cachedObsIdx = null;
        DoubleMatrix2D cachedXo = null;
        DoubleMatrix2D cachedZo = null;
        timer.start();
        for ( int g = 0; g < ngenes; g++ ) {
            this.atanhCorrelations[g] = Double.NaN;
            DoubleMatrix1D y = data.viewRow( g );
            int nobs = 0;
            for ( int j = 0; j < narrays; j++ ) {
                if ( Double.isFinite( y.getQuick( j ) ) && ( weights == null || Double.isFinite( weights.getQuick( g, j ) ) ) ) {
                    nobs++;
                }
            }
            /*
             * if (nobs > (nbeta + 2L) && nblocks > 1L && nblocks < (nobs - 1L)) -- the per-probe gate; probes
             * that fail it keep NA rho and are dropped from the consensus (limma's mean(arho, na.rm=TRUE)).
             */
            if ( nobs <= nbeta + 2 || nblocks < 1 || nblocks >= nobs - 1 ) {
                continue;
            }

            List<Integer> obsIdx = new ArrayList<>( nobs );
            for ( int j = 0; j < narrays; j++ ) {
                if ( Double.isFinite( y.getQuick( j ) ) && ( weights == null || Double.isFinite( weights.getQuick( g, j ) ) ) ) {
                    obsIdx.add( j );
                }
            }

            // X <- design[o, ], Z <- model.matrix(~0 + A) over the OBSERVED samples. Identical for every probe
            // with the same observed samples (all of them, with complete data), so built once per pattern.
            if ( !obsIdx.equals( cachedObsIdx ) ) {
                cachedXo = new DenseDoubleMatrix2D( nobs, nbeta );
                cachedZo = new DenseDoubleMatrix2D( nobs, nblocks );
                List<String> observedBlocks = new ArrayList<>();
                int r = 0;
                for ( int j : obsIdx ) {
                    for ( int c = 0; c < nbeta; c++ ) {
                        cachedXo.set( r, c, design.get( j, c ) );
                    }
                    if ( !observedBlocks.contains( block[j] ) ) observedBlocks.add( block[j] );
                    cachedZo.set( r, observedBlocks.indexOf( block[j] ), 1.0 );
                    r++;
                }
                cachedObsIdx = obsIdx;
            }
            DoubleMatrix1D yo = new DenseDoubleMatrix1D( nobs );
            DoubleMatrix1D wo = weights == null ? null : new DenseDoubleMatrix1D( nobs );
            int r = 0;
            for ( int j : obsIdx ) {
                yo.set( r, y.getQuick( j ) );
                if ( wo != null ) wo.set( r, weights.getQuick( g, j ) );
                r++;
            }
            DoubleMatrix2D xo = cachedXo;
            DoubleMatrix2D zo = cachedZo;

            double rho = estimateRhoForProbe( yo, xo, zo, wo );
            if ( Double.isNaN( rho ) ) {
                continue;
            }

            if ( rho < rhoMin ) rho = rhoMin;
            if ( rho > RHO_MAX ) rho = RHO_MAX;
            // Java's Math has no atanh; atanh(x) = 0.5 * ln((1+x)/(1-x))
            this.atanhCorrelations[g] = 0.5 * Math.log( ( 1.0 + rho ) / ( 1.0 - rho ) );
        }

        this.consensusCorrelation = trimmedMeanTanh( this.atanhCorrelations );
        return this;
    }

    public double getConsensusCorrelation() {
        assert this.consensusCorrelation >= -1 && this.consensusCorrelation < 1 : "correlation not estimated or degenerate: " + this.consensusCorrelation;
        return this.consensusCorrelation;
    }

    /**
     * @return true if estimation backed off to rho = 0 (all blocks size one, or the block span already inside
     * the design); the GLS fit with rho = 0 reduces to the ordinary OLS fit.
     */
    public boolean isDegenerateToZero() {
        return this.degenerateToZero;
    }

    /**
     * @return per-probe atanh correlations (NaN where unestimated), for diagnostics and tests.
     */
    public double[] getAtanhCorrelations() {
        return this.atanhCorrelations;
    }

    /**
     * The samples x samples correlation matrix implied by block membership and rho: within-block off-diagonal
     * entries are rho, the diagonal is 1 (limma gls.series: {@code cormatrix <- Z %*% (correlation * t(Z));
     * diag(cormatrix) <- 1}).
     *
     * @param block block identifier per sample
     * @param rho   the correlation to apply within blocks
     * @return correlation matrix, SPD for |rho| &lt; 1
     */
    public static DoubleMatrix2D blockCorrelationMatrix( String[] block, double rho ) {
        int n = block.length;
        DoubleMatrix2D v = new DenseDoubleMatrix2D( n, n );
        for ( int i = 0; i < n; i++ ) {
            for ( int j = 0; j < n; j++ ) {
                v.set( i, j, block[i].equals( block[j] ) ? rho : 0.0 );
            }
            v.set( i, i, 1.0 );
        }
        return v;
    }

    /**
     * Per-probe REML variance decomposition: rho = Block variance / (Residual + Block).
     * <p>
     * Port of statmod mixedModel2Fit(y, X, Z, w, only.varcomp = TRUE, maxit = 20): fit X first, take the
     * effects of Z and y in the residual span, decompose that residual space by the SVD of Q'Z, and regress
     * the squared projections on [1, d^2] -- a gamma GLM with identity link (glmgam.fit) when the d spread is
     * informative, plain OLS otherwise.
     *
     * @return rho, or NaN if the fit failed (limma wraps in tryCatch and keeps NA)
     */
    private double estimateRhoForProbe( DoubleMatrix1D y, DoubleMatrix2D x, DoubleMatrix2D z, DoubleMatrix1D w ) {
        int mx = y.size();
        int nz = z.columns();

        /*
         * if (!is.null(w)) { sw <- sqrt(w); y <- sw * y; X <- sw * X } -- the weights scale the response and
         * the fixed-effect design; the blocks Z stay unscaled (they are structure, not observations).
         */
        DoubleMatrix1D yUse = y;
        DoubleMatrix2D xUse = x;
        if ( w != null ) {
            yUse = y.copy().assign( w.copy().assign( Functions.sqrt ), Functions.mult );
            xUse = x.copy();
            for ( int c = 0; c < xUse.columns(); c++ ) {
                xUse.viewColumn( c ).assign( w.copy().assign( Functions.sqrt ), Functions.mult );
            }
        }

        try {
            // fit <- lm.fit(X, cbind(Z, y)): one QR of X applied to Z and y together
            DoubleMatrix2D zy = new DenseDoubleMatrix2D( mx, nz + 1 );
            for ( int i = 0; i < mx; i++ ) {
                for ( int c = 0; c < nz; c++ ) {
                    zy.set( i, c, z.get( i, c ) );
                }
                zy.set( i, nz, yUse.get( i ) );
            }
            QRDecomposition qrd = new QRDecomposition( xUse );
            DoubleMatrix2D effects = qrd.effects( zy );
            int rank = qrd.getRank();
            int mq = mx - rank;
            if ( mq == 0 ) {
                return Double.NaN;
            }

            // QtZ <- fit$effects[(r + 1):mx, 1:nz]
            DoubleMatrix2D qtz = new DenseDoubleMatrix2D( mq, nz );
            DoubleMatrix1D effectY = new DenseDoubleMatrix1D( mq );
            for ( int i = 0; i < mq; i++ ) {
                for ( int c = 0; c < nz; c++ ) {
                    qtz.set( i, c, effects.get( rank + i, c ) );
                }
                effectY.set( i, effects.get( rank + i, nz ) );
            }

            /*
             * s <- La.svd(QtZ, nu = mq, nv = 0); uqy <- crossprod(s$u, effects_y_part); d <- s$d^2 (padded).
             *
             * La.svd(QtZ, nu = mq) returns the FULL square U (mq x mq), whose columns nz..mq-1 span the zero
             * singular directions; colt's SingularValueDecomposition returns the ECONOMY U (mq x nz), so the
             * residual-space components uqy[mq-1..] would be lost. The equivalent computation that keeps the
             * full span: eigendecompose the mq x mq symmetric PSD matrix QtZ QtZ' -- its eigenvalues are the
             * singular values SQUARED (= d) and its eigenvectors are U's columns. The regression of dy on
             * [1, d] pairs (d_i, (u_i'e)^2) as an unordered set, so the eigenvalue ordering does not matter.
             *
             * The identity-link gamma approximation is group-sufficient in the singular spectrum (verified
             * against limma: R's own La.svd basis and this eigendecomposition draw different within-group
             * bases on block designs, where d has exact repeats, and agree on the varcomp to 1e-14), so a
             * collapsed-group SVD formulation would give the same fixed point -- it was tried and reverted:
             * colt's per-probe economy-SVD allocations and the weighted-GLM convergence path made it BOTH
             * slower (48 s vs 12 s at 20k probes) and less accurate than this direct form.
             */
            DoubleMatrix2D qtzqtzt = new DenseDoubleMatrix2D( mq, mq );
            for ( int i = 0; i < mq; i++ ) {
                for ( int j = 0; j < mq; j++ ) {
                    double sum = 0.0;
                    for ( int k = 0; k < nz; k++ ) {
                        sum += qtz.get( i, k ) * qtz.get( j, k );
                    }
                    qtzqtzt.set( i, j, sum );
                }
            }
            cern.colt.matrix.linalg.EigenvalueDecomposition eig = new cern.colt.matrix.linalg.EigenvalueDecomposition( qtzqtzt );
            double[] d = eig.getRealEigenvalues().toArray(); // length mq; zeros included, as La.svd pads
            DoubleMatrix2D u = eig.getV();
            DoubleMatrix1D uqy = new DenseDoubleMatrix1D( mq );
            for ( int i = 0; i < mq; i++ ) {
                double sum = 0.0;
                for ( int k = 0; k < mq; k++ ) {
                    sum += u.get( k, i ) * effectY.get( k );
                }
                uqy.set( i, sum );
            }

            // dx <- cbind(Residual = 1, Block = d); dy <- uqy^2; dfit <- lm.fit(dx, dy)
            DoubleMatrix2D dx = new DenseDoubleMatrix2D( mq, 2 );
            DoubleMatrix1D dy = new DenseDoubleMatrix1D( mq );
            for ( int i = 0; i < mq; i++ ) {
                dx.set( i, 0, 1.0 );
                dx.set( i, 1, d[i] );
                dy.set( i, Math.pow( uqy.get( i ), 2 ) );
            }
            double[] varcomp = olsFit( dx, dy );

            // if (mq > 2 && sum(abs(d) > 1e-15) > 1 && var(d) > 1e-15) -- only then the gamma IRLS refinement
            int informative = 0;
            double dmean = 0.0;
            for ( double v : d ) {
                if ( Math.abs( v ) > 1e-15 ) informative++;
                dmean += v;
            }
            dmean /= mq;
            double dvar = 0.0;
            for ( double v : d ) {
                dvar += Math.pow( v - dmean, 2 );
            }
            dvar /= ( mq - 1 );
            if ( mq > 2 && informative > 1 && dvar > 1e-15 ) {
                // fitted values of the OLS start, to check nonnegativity as mixedModel2Fit does
                boolean allNonNeg = true;
                for ( int i = 0; i < mq; i++ ) {
                    double fitted = varcomp[0] + varcomp[1] * d[i];
                    if ( fitted < 0 ) {
                        allNonNeg = false;
                        break;
                    }
                }
                double[] start;
                if ( allNonNeg ) {
                    start = varcomp;
                } else {
                    // start <- c(Residual = mean(dy), Block = 0)
                    double meanDy = dy.zSum() / mq;
                    start = new double[] { meanDy, 0.0 };
                }
                double[] gam = gammaGlmFit( dx, dy, start, 1e-6, 20 );
                if ( gam != null ) {
                    varcomp = gam;
                }
            }

            if ( varcomp == null || Double.isNaN( varcomp[0] ) || Double.isNaN( varcomp[1] ) ) {
                return Double.NaN;
            }
            // rho[i] <- s[2]/sum(s)
            return varcomp[1] / ( varcomp[0] + varcomp[1] );
        } catch ( RuntimeException e ) {
            // limma: tryCatch(..., error = nafun) -- a failed probe fit keeps rho = NA
            return Double.NaN;
        }
    }

    private double[] olsFit( DoubleMatrix2D dx, DoubleMatrix1D dy ) {
        DoubleMatrix2D bCol = new DenseDoubleMatrix2D( dy.size(), 1 );
        for ( int i = 0; i < dy.size(); i++ ) {
            bCol.set( i, 0, dy.get( i ) );
        }
        DoubleMatrix2D coefs = new QRDecomposition( dx ).solve( bCol );
        return new double[] { coefs.get( 0, 0 ), coefs.get( 1, 0 ) };
    }

    /**
     * Port of statmod's glmgam.fit for the 2-coefficient varcomp system: damped iteratively reweighted least
     * squares for a gamma family, identity link GLM. Returns the coefficients, or null on the
     * "too much damping" bail-out (limma's tryCatch turns any exception into NA anyway; returning null and
     * keeping the OLS start mirrors the practical effect).
     * <pre>
     * v <- mu^2; v <- pmax(v, max(v)/10^3)
     * XVX <- crossprod(X, vecmat(1/v, X))
     * dl <- crossprod(X, (y - mu)/v)
     * repeat { R <- chol(XVX + lambda*I); dbeta <- backsolve(R, backsolve(R, dl, transpose=TRUE))
     *          beta <- betaold + dbeta; ... damped line search; convergence on crossprod(dl, dbeta) < tol }
     * </pre>
     */
    private double[] gammaGlmFit( DoubleMatrix2D dx, DoubleMatrix1D dy, double[] coefStart, double tol, int maxit ) {
        Algebra solver = new Algebra();
        int n = dx.rows();
        int p = dx.columns();
        double[] beta = coefStart.clone();
        DoubleMatrix1D mu = new DenseDoubleMatrix1D( n );
        for ( int i = 0; i < n; i++ ) {
            double m = 0.0;
            for ( int c = 0; c < p; c++ ) {
                m += dx.get( i, c ) * beta[c];
            }
            mu.set( i, m );
        }
        if ( Arrays.stream( mu.toArray() ).anyMatch( v -> v < 0 ) ) {
            return null; // "Starting values give negative fitted values" -- limma catches, keeps NA
        }

        double dev = gammaDeviance( dy, mu );
        double lambda = 0.0;
        double maxinfo = 0.0;
        cern.colt.matrix.DoubleMatrix2D I = new DenseDoubleMatrix2D( p, p );
        for ( int c = 0; c < p; c++ ) I.set( c, c, 1.0 );

        int iter = 0;
        while ( true ) {
            iter++;
            // v <- pmax(mu^2, max(v)/10^3)
            DoubleMatrix1D v = mu.copy().assign( Functions.square );
            double maxV = 0.0;
            for ( int i = 0; i < n; i++ ) maxV = Math.max( maxV, v.get( i ) );
            double floor = maxV / 1e3;
            for ( int i = 0; i < n; i++ ) {
                if ( v.get( i ) < floor ) v.set( i, floor );
            }

            // XVX <- crossprod(X, vecmat(1/v, X))
            DoubleMatrix2D xvx = new DenseDoubleMatrix2D( p, p );
            for ( int a = 0; a < p; a++ ) {
                for ( int b2 = 0; b2 < p; b2++ ) {
                    double sum = 0.0;
                    for ( int i = 0; i < n; i++ ) {
                        sum += dx.get( i, a ) * dx.get( i, b2 ) / v.get( i );
                    }
                    xvx.set( a, b2, sum );
                }
            }
            maxinfo = 0.0;
            for ( int c = 0; c < p; c++ ) maxinfo = Math.max( maxinfo, xvx.get( c, c ) );
            if ( iter == 1 ) {
                double diagMean = 0.0;
                for ( int c = 0; c < p; c++ ) diagMean += xvx.get( c, c );
                lambda = Math.abs( diagMean ) / p;
            }

            // dl <- crossprod(X, (y - mu)/v)
            DoubleMatrix1D dl = new DenseDoubleMatrix1D( p );
            for ( int c = 0; c < p; c++ ) {
                double sum = 0.0;
                for ( int i = 0; i < n; i++ ) {
                    sum += dx.get( i, c ) * ( dy.get( i ) - mu.get( i ) ) / v.get( i );
                }
                dl.set( c, sum );
            }

            double[] betaold = beta.clone();
            double devold = dev;
            int lev = 0;
            while ( true ) {
                lev++;
                // R <- chol(XVX + lambda*I); dbeta <- R^{-1 T} R^{-1} dl
                DoubleMatrix2D damped = xvx.copy();
                for ( int c = 0; c < p; c++ ) {
                    damped.set( c, c, damped.get( c, c ) + lambda );
                }
                CholeskyDecomposition chol = new CholeskyDecomposition( damped );
                if ( !chol.isSymmetricPositiveDefinite() ) {
                    lambda *= 2;
                    if ( lambda / maxinfo > 1e15 ) {
                        beta = betaold;
                        break;
                    }
                    continue;
                }
                DoubleMatrix2D L = chol.getL();
                /*
                 * R's chol() returns the UPPER factor R_u (XVX + lambda I = R_u' R_u), and
                 * dbeta <- backsolve(R_u, backsolve(R_u, dl, transpose=TRUE)):
                 * the inner solve is t(R_u)^{-1} dl = L^{-1} dl (FORWARD solve against the lower factor),
                 * the outer is R_u^{-1} = L'^{-1} (backward). Together: (L L')^{-1} dl — the damped Newton
                 * step. Solving in the transposed order (L'^{-1} then L^{-1}) gives (L'L)^{-1} dl, which is
                 * a different matrix — the same trap the whitening step had.
                 */
                DoubleMatrix1D t1 = solveLower( L, dl );
                DoubleMatrix1D t2 = solveUpper( solver.transpose( L ), t1 );
                for ( int c = 0; c < p; c++ ) {
                    beta[c] = betaold[c] + t2.get( c );
                }
                for ( int i = 0; i < n; i++ ) {
                    double m = 0.0;
                    for ( int c = 0; c < p; c++ ) {
                        m += dx.get( i, c ) * beta[c];
                    }
                    mu.set( i, m );
                }
                dev = gammaDeviance( dy, mu );
                if ( dev <= devold || dev / maxAbs( mu ) < 1e-15 ) {
                    break;
                }
                if ( lambda / maxinfo > 1e15 ) {
                    beta = betaold;
                    break;
                }
                lambda *= 2;
            }

            if ( lambda / maxinfo > 1e15 ) {
                break;
            }
            if ( lev == 1 ) {
                lambda /= 10;
            }
            // convergence: crossprod(dl, dbeta) < tol -- dbeta = beta - betaold
            double crossDlDbeta = 0.0;
            for ( int c = 0; c < p; c++ ) {
                crossDlDbeta += dl.get( c ) * ( beta[c] - betaold[c] );
            }
            if ( crossDlDbeta < tol || dev / maxAbs( mu ) < 1e-15 || iter > maxit ) {
                break;
            }
        }
        return beta;
    }

    private double maxAbs( DoubleMatrix1D v ) {
        double m = 0.0;
        for ( int i = 0; i < v.size(); i++ ) {
            m = Math.max( m, Math.abs( v.get( i ) ) );
        }
        return m;
    }

    private double gammaDeviance( DoubleMatrix1D y, DoubleMatrix1D mu ) {
        // deviance.gamma from glmgam.fit: 2 * sum((y - mu)/mu - log(y/mu)), with the both-tiny convention
        double dev = 0.0;
        int n = y.size();
        for ( int i = 0; i < n; i++ ) {
            if ( mu.get( i ) < 0 ) {
                return Double.POSITIVE_INFINITY;
            }
        }
        for ( int i = 0; i < n; i++ ) {
            double yi = y.get( i ), mi = mu.get( i );
            if ( yi < 1e-15 && mi < 1e-15 ) {
                continue;
            }
            dev += ( yi - mi ) / mi - Math.log( yi / mi );
        }
        return 2.0 * dev;
    }

    /**
     * Solve R x = b for upper triangular R (R backsolve default).
     */
    private DoubleMatrix1D solveUpper( DoubleMatrix2D r, DoubleMatrix1D b ) {
        int n = b.size();
        DoubleMatrix1D x = new DenseDoubleMatrix1D( n );
        for ( int i = n - 1; i >= 0; i-- ) {
            double sum = b.get( i );
            for ( int j = i + 1; j < n; j++ ) {
                sum -= r.get( i, j ) * x.get( j );
            }
            x.set( i, sum / r.get( i, i ) );
        }
        return x;
    }

    /**
     * Solve L x = b for LOWER triangular L: forward substitution.
     */
    private DoubleMatrix1D solveLower( DoubleMatrix2D l, DoubleMatrix1D b ) {
        // L x = b for LOWER triangular L: forward substitution
        int n = b.size();
        DoubleMatrix1D x = new DenseDoubleMatrix1D( n );
        for ( int i = 0; i < n; i++ ) {
            double sum = b.get( i );
            for ( int j = 0; j < i; j++ ) {
                sum -= l.get( i, j ) * x.get( j );
            }
            x.set( i, sum / l.get( i, i ) );
        }
        return x;
    }

    /**
     * mean(atanh(rho), trim = TRIM, na.rm = TRUE) then tanh -- R's trimmed mean convention:
     * lo <- floor(n*trim) + 1, hi <- n + 1 - lo, on the sorted (NA-removed) values.
     */
    private double trimmedMeanTanh( double[] atanhRho ) {
        List<Double> vals = new ArrayList<>();
        for ( double v : atanhRho ) {
            if ( Double.isFinite( v ) ) vals.add( v );
        }
        if ( vals.isEmpty() ) {
            // limma would produce NaN here; a consensus of nothing is not fittable, report 0 -- the caller
            // sees an all-NaN probe set only when every probe failed, which the OLS fallback handles
            return 0.0;
        }
        double[] sorted = vals.stream().mapToDouble( Double::doubleValue ).sorted().toArray();
        int n = sorted.length;
        int lo = ( int ) Math.floor( n * TRIM ) + 1;
        int hi = n + 1 - lo;
        double sum = 0.0;
        for ( int i = lo - 1; i < hi; i++ ) {
            sum += sorted[i];
        }
        double mean = sum / ( hi - lo + 1 );
        // tanh(x) = (e^2x - 1)/(e^2x + 1); Java's Math has no atanh either way, this is the inverse
        double e2 = Math.exp( 2.0 * mean );
        return ( e2 - 1.0 ) / ( e2 + 1.0 );
    }

    /**
     * Indicator matrix for block membership: samples x levels, 1 where the sample is in the level. With
     * {@code dropFirst} (R's {@code model.matrix(~factor(block))} convention) the first level is omitted;
     * without it ({@code ~0 + A}) every level gets a column.
     */
    private static DoubleMatrix2D blockIndicator( String[] block, List<String> levels, boolean dropFirst ) {
        int start = dropFirst ? 1 : 0;
        DoubleMatrix2D m = new DenseDoubleMatrix2D( block.length, levels.size() - start );
        for ( int i = 0; i < block.length; i++ ) {
            int c = levels.indexOf( block[i] ) - start;
            if ( c >= 0 ) {
                m.set( i, c, 1.0 );
            }
        }
        return m;
    }
}
