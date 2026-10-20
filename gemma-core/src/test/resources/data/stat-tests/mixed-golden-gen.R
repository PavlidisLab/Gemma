# Reference values for MixedModelFitTest -- the R fits ubic.gemma.core.util.math.linearmodels asserts
# against. Tests depend on these numbers not changing.
#
# Run with: Rscript mixed-golden-gen.R   (needs limma and statmod; writes the fixture beside this script
# and prints the golden values; running it again would change the fixture, so don't -- the values below
# are baked into the test for the committed fixture).
#
# Fixture: 80 probes x 11 samples, 8 subjects -- subjects 1, 3, 5 have both arms (ctrl/treat), the other
# five have a single arm (unbalanced ON PURPOSE: on a balanced design the mixed and fixed-effect paired
# models coincide). Subject shifts sd 1.5, noise sd 0.5, treatment effect 1.0 on the last 30 probes.
#
# The limma recipe for repeated measures: estimate the intra-block correlation with
# duplicateCorrelation(design = ~treatment, block = subject) -- the design EXCLUDES the blocking factor,
# whose columns would otherwise soak up the correlation and degenerate the estimate -- then fit by
# gls.series at the consensus correlation, then eBayes on the gls fit.

options(digits = 15)
suppressMessages(library(limma))
suppressMessages(library(statmod))

set.seed(4242)
n.subj <- 8
n.probe <- 80
rows <- data.frame()
for (s in 1:n.subj) {
    both <- s %in% c(1, 3, 5)
    if (both) {
        rows <- rbind(rows, data.frame(subject = paste0("s", s), treatment = c("ctrl", "treat")))
    } else {
        arm <- if (s %% 2 == 0) "ctrl" else "treat"
        rows <- rbind(rows, data.frame(subject = paste0("s", s), treatment = arm))
    }
}
n.samples <- nrow(rows)

subj.eff <- matrix(rnorm(n.probe * n.subj, 0, 1.5), nrow = n.probe)
noise <- matrix(rnorm(n.probe * n.samples, 0, 0.5), nrow = n.probe)
trt.eff <- c(rep(0, 50), rep(1.0, 30))

subj.idx <- match(rows$subject, paste0("s", 1:n.subj))
y <- subj.eff[, subj.idx] + noise
treat.cols <- rows$treatment == "treat"
y[, treat.cols] <- y[, treat.cols] + trt.eff

colnames(y) <- paste0(rows$subject, "_", rows$treatment)

if (!interactive()) {
    out <- cbind(data.frame(probe = paste0("probe_", 0:(n.probe - 1))), as.data.frame(y))
    write.table(out, "mixed-test-data.txt", sep = "\t", row.names = FALSE, quote = FALSE)
    design <- data.frame(sample = colnames(y), subject = rows$subject, treatment = rows$treatment)
    write.table(design, "mixed-design.txt", sep = "\t", row.names = FALSE, quote = FALSE)
    cat("fixture written:", dim(out), "\n")
}

mdat <- read.table("mixed-test-data.txt", header = TRUE, row.names = 1, sep = "\t")
md <- read.table("mixed-design.txt", header = TRUE, sep = "\t")
msubj <- factor(md$subject)
mtrt <- factor(md$treatment, levels = c("ctrl", "treat"))
mdes <- model.matrix(~ mtrt)

dc <- duplicateCorrelation(mdat, design = mdes, block = msubj)
cat("consensus.correlation =", format(dc$consensus.correlation, digits = 15), "\n")

gfit <- gls.series(mdat, design = mdes, block = msubj, correlation = dc$consensus.correlation)
geb <- eBayes(gfit)

# Gemma asserts (MixedModelFitTest): the consensus correlation and the first ten per-probe atanh
# correlations (tolerance 2e-3 / 3e-4 -- unbalanced block designs give QtZ repeated singular values, so
# the within-group orthonormal basis is implementation-defined and R itself moves by that much when the
# SVD is replaced by the eigendecomposition of QtZ QtZ'); then, at rho FIXED to the value above, the gls
# coefficients, sigma, stdev.unscaled and the eBayes t/p to machine precision.
cat("atanh correlations (first 10):", format(dc$atanh.correlations[1:10], digits = 15), "\n")
cat("stdev.unscaled:", format(gfit$stdev.unscaled[1, ], digits = 15), "\n")
cat("df.total (eBayes):", geb$df.total[1], "\n")
for (p in c("probe_0", "probe_4", "probe_10", "probe_49", "probe_50", "probe_79")) {
    i <- match(p, rownames(mdat))
    cat("\n--", p, "--\n")
    cat("coefficients:", format(gfit$coefficients[i, ], digits = 15), "\n")
    cat("sigma:", format(gfit$sigma[i], digits = 15), "\n")
    cat("t_treat:", format(geb$t[i, "mtrttreat"], digits = 15), "\n")
    cat("p_treat:", format(geb$p.value[i, "mtrttreat"], digits = 15), "\n")
}
