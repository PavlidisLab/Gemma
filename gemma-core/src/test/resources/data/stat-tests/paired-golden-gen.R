# Reference values for PairedDesignFitTest -- the R fits ubic.gemma.core.util.math.linearmodels asserts
# against. Tests depend on these numbers not changing.
#
# Run with: Rscript paired-golden-gen.R   (needs limma; writes the fixture beside this script and prints
# the golden values; running it again would change the fixture, so don't -- the values below are baked
# into the test for the committed fixture).
#
# Fixture: 100 probes x 12 samples, 6 subjects x (ctrl, treat), per-probe subject shifts (sd 1.5),
# per-observation noise (sd 0.4), treatment effect 1.2 on the last 40 probes. The subject spread is what
# makes the paired analysis differ from an unpaired one on the same data.

options(digits = 15)
suppressMessages(library(limma))

set.seed(42)
n.subj <- 6
n.probe <- 100
subj <- factor(rep(1:n.subj, each = 2), labels = paste0("s", 1:n.subj))
trt <- factor(rep(c("ctrl", "treat"), n.subj), levels = c("ctrl", "treat"))

subj.eff <- matrix(rnorm(n.probe * n.subj, 0, 1.5), nrow = n.probe)
noise <- matrix(rnorm(n.probe * n.subj * 2, 0, 0.4), nrow = n.probe)
trt.eff <- c(rep(0, 60), rep(1.2, 40))

y <- subj.eff[, rep(1:n.subj, each = 2)] + noise
treat.cols <- as.vector(trt) == "treat"
y[, treat.cols] <- y[, treat.cols] + trt.eff

colnames(y) <- paste0("s", rep(1:n.subj, each = 2), "_", as.vector(trt))

if (!interactive()) {
    out <- cbind(data.frame(probe = paste0("probe_", 0:(n.probe - 1))), as.data.frame(y))
    write.table(out, "paired-test-data.txt", sep = "\t", row.names = FALSE, quote = FALSE)
    design <- data.frame(sample = colnames(y), subject = as.character(subj), treatment = as.vector(trt))
    write.table(design, "paired-design.txt", sep = "\t", row.names = FALSE, quote = FALSE)
    cat("fixture written:", dim(out), "\n")
}

# ---- the golden fit: lmFit + eBayes on ~ subject + treatment (subject as a fixed block) ----
dat <- read.table("paired-test-data.txt", header = TRUE, row.names = 1, sep = "\t")
pd <- read.table("paired-design.txt", header = TRUE, sep = "\t")
subject <- factor(pd$subject)
treatment <- factor(pd$treatment, levels = c("ctrl", "treat"))
mm <- model.matrix(~ subject + treatment)
fit <- lmFit(dat, mm)
fit <- eBayes(fit)

# Gemma asserts: residual dof = 12 - 7 coefficients = 5; sigma; the UNSCALED standard error (Gemma's
# summary table stores sqrt(diag(cov.unscaled)), not the scaled SE); the eBayes-moderated t and p of the
# treatment contrast (treatmenttreat; R drops s1, Gemma drops the level setBaseline names -- same level).
cat("df.residual =", fit$df.residual[1], "\n")
cat("df.prior =", fit$df.prior, " s2.prior =", format(fit$s2.prior, digits = 15), "\n")
cat("stdev.unscaled (treatment) =", format(fit$stdev.unscaled[1, "treatmenttreat"], digits = 15), "\n")
for (p in c("probe_0", "probe_4", "probe_10", "probe_60", "probe_98", "probe_99")) {
    cat("\n--", p, "--\n")
    cat("coefficients:", format(fit$coefficients[p, ], digits = 15), "\n")
    cat("sigma:", format(fit$sigma[p], digits = 15), "\n")
    cat("t_treat:", format(fit$t[p, "treatmenttreat"], digits = 15), "\n")
    cat("p_treat:", format(fit$p.value[p, "treatmenttreat"], digits = 15), "\n")
}
