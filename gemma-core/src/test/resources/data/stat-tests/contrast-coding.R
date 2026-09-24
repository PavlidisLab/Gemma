# Reference values for ContrastCodingTest -- the R fits that ubic.gemma.core.util.math.linearmodels
# asserts against. Tests depend on these numbers not changing.
#
# Run with: Rscript contrast-coding.R
#
# The fixture is seven samples over three tissues, 2 / 4 / 1. Unequal on purpose: on a balanced design
# the mean of the level means and the mean of the samples coincide, so a test built on one cannot tell
# which reference the sum-to-zero coefficients are against. Here they are 21.333 and 20.571.

options(digits = 15)

y <- c(10, 12, 20, 22, 24, 26, 30)

# ---------------------------------------------------------------------------------------------------
# contr.sum -- each level against the mean of the level means (Gemma: ContrastCoding.SUM_TO_ZERO)
#
# NOTE the level order. R's contr.sum drops the LAST level; Gemma drops the FIRST, the one setBaseline
# names. To compare like with like, put Gemma's baseline last here.
# ---------------------------------------------------------------------------------------------------

tissue <- factor(c("cortex", "cortex", "liver", "liver", "liver", "liver", "kidney"),
                 levels = c("liver", "kidney", "cortex"))
contrasts(tissue) <- contr.sum(3)
fit <- lm(y ~ tissue)
s <- summary(fit)

print(s$coefficients)
#                      Estimate       Std. Error           t value             Pr(>|t|)
# (Intercept) 21.33333333333334 1.03413947049924 20.62906787904974 3.26181828716962e-05
# tissue1      1.66666666666667 1.23603308118261  1.34839972492648 2.48820913808664e-01   (liver)
# tissue2      8.66666666666666 1.70375402502174  5.08680627566299 7.04714642242505e-03   (kidney)

cat("sigma =", s$sigma, "\n")   # 2.34520787991171
cat("df    =", s$df[2], "\n")   # 4

# Gemma's summary table stores the UNSCALED standard error in that column, not the one R prints.
# They differ by sigma; the estimate, t and p are directly comparable and this one is not.
print(sqrt(diag(s$cov.unscaled)))
#       (Intercept)           tissue1           tissue2
# 0.440958551844098 0.527046276694730 0.726483157256779

# ---------------------------------------------------------------------------------------------------
# The derived level (cortex), which this parameterization gives no row for. Its deviation is minus the
# sum of the others; the variance of that sum is the whole covariance BLOCK, not the diagonal.
# ---------------------------------------------------------------------------------------------------

b   <- coef(fit)[2:3]
est <- -sum(b)
v   <- sum(s$cov.unscaled[2:3, 2:3])
cat("cortex estimate    =", est, "\n")                                   # -10.3333333333333
cat("cortex unscaled sd =", sqrt(v), "\n")                               #   0.600925212577331
cat("cortex se          =", sqrt(v) * s$sigma, "\n")                     #   1.40929454377398
cat("cortex t           =", est / (sqrt(v) * s$sigma), "\n")             #  -7.33227371026462
cat("cortex p           =", 2 * pt(-abs(est / (sqrt(v) * s$sigma)), s$df[2]), "\n")  # 0.001841510578397

# The diagonal alone would give 0.897527467855751, not 0.600925212577331 -- wrong by 49%
# and entirely plausible-looking. That is the error ContrastCodingTest exists to keep out.
cat("diagonal-only (WRONG) =", sqrt(sum(diag(s$cov.unscaled[2:3, 2:3]))), "\n")

# Same numbers reached independently, by refitting so that cortex IS estimated and liver is derived.
# This is the cross-check the Java test mirrors: which level is derived is an arbitrary choice, so the
# contrast must come out the same either way.
tissue2 <- factor(c("cortex", "cortex", "liver", "liver", "liver", "liver", "kidney"),
                  levels = c("cortex", "kidney", "liver"))
contrasts(tissue2) <- contr.sum(3)
print(summary(lm(y ~ tissue2))$coefficients)
#                       Estimate       Std. Error           t value             Pr(>|t|)
# (Intercept)  21.33333333333333 1.03413947049924 20.62906787904973 3.26181828716962e-05
# tissue21    -10.33333333333333 1.40929454377398 -7.33227371026462 1.84151057839700e-03   (cortex)
# tissue22      8.66666666666667 1.70375402502174  5.08680627566299 7.04714642242505e-03   (kidney)

# ---------------------------------------------------------------------------------------------------
# contr.treatment, cortex as baseline -- R's default, and Gemma's behaviour before ContrastCoding.
# Here so the sum-to-zero work can be shown not to have disturbed it.
# ---------------------------------------------------------------------------------------------------

tissue3 <- factor(c("cortex", "cortex", "liver", "liver", "liver", "liver", "kidney"),
                  levels = c("cortex", "liver", "kidney"))
print(summary(lm(y ~ tissue3))$coefficients)
#               Estimate       Std. Error         t value            Pr(>|t|)
# (Intercept)         11 1.65831239517770 6.63324958071080 0.00268009608714780
# tissue3liver        12 2.03100960115899 5.90839156700797 0.00410747546615996
# tissue3kidney       19 2.87228132326901 6.61495092631652 0.00270778321164432
