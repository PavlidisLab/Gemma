# Reference values for GSE17183RealDesignFitTest -- the R fits ubic.gemma.core.util.math.linearmodels
# asserts against on a REAL Gemma experiment design. Tests depend on these numbers not changing.
#
# Run with: Rscript gse17183-golden-gen.R   (needs limma; reads the committed fixture and design;
# prints the golden values; nothing is rewritten).
#
# GSE17183 (Gemma eid 2877): "Hepatic gene expression before and during interferon and ribavirin
# combination therapy". 30 patients biopsied BEFORE therapy and DURING interferon+ribavirin, each in a
# subset of three cell types (liver bulk, laser-capture hepatocytes, laser-capture portal tracts):
# 108 samples, genuinely unbalanced paired/repeated-measures design. The subject (patient) factor is not
# curated in Gemma's design -- it is recoverable from the BioMaterial names (No<patient>), which is
# exactly what gse17183-derived-design.txt captures. Gemma's own batch factor (30 levels) is deliberately
# NOT in the model here; it is available to the analyzer as a second blocking factor.
#
# Reference fits:
#   paired  : limma lmFit + eBayes on ~ individual + organismPart + treatment   (subject as fixed block)
#   mixed   : duplicateCorrelation(design = ~ organismPart + treatment, block = individual) -> gls.series -> eBayes

options(digits = 15)
suppressMessages(library(limma))

dat <- read.table("gse17183-expmat-50probes.txt", header = TRUE, sep = "\t", check.names = FALSE, row.names = "probe")
dd <- read.table("gse17183-derived-design.txt", comment.char = "#", header = TRUE, sep = "\t")
stopifnot(all(colnames(dat) == dd$sample))

individual <- factor(dd$individual)
organismPart <- factor(dd$organismPart)
treatment <- factor(dd$treatment, levels = c("pre", "during_interferon_ribavirin"))
cat("subjects:", nlevels(individual), " samples:", nrow(dd), "\n")
cat("cells per subject (min/max):", min(table(individual)), max(table(individual)), "\n")

# ---- PAIRED (fixed-effect blocking) ----
mm <- model.matrix(~ individual + organismPart + treatment)
fit <- lmFit(dat, mm)
fit <- eBayes(fit)
cat("\n===== PAIRED golden (lmFit + eBayes, ~ individual + organismPart + treatment) =====\n")
cat("df.residual =", fit$df.residual[1], "\n")
cat("df.prior =", format(fit$df.prior, digits = 15), " s2.prior =", format(fit$s2.prior, digits = 15), "\n")
trCoef <- grep("^treatment", colnames(fit$coefficients), value = TRUE)
cat("treatment coefficient column:", trCoef, "\n")
cat("stdev.unscaled (treatment):", format(fit$stdev.unscaled[1, trCoef], digits = 15), "\n")
for (p in c("1255_g_at", "201601_x_at", "202086_at", "203153_at", "205483_s_at", "213797_at", "214059_at")) {
    if (!p %in% rownames(dat)) next
    i <- match(p, rownames(dat))
    cat("\n--", p, "--\n")
    cat("sigma:", format(fit$sigma[i], digits = 15), "\n")
    cat("t_treat:", format(fit$t[i, trCoef], digits = 15), "\n")
    cat("p_treat:", format(fit$p.value[i, trCoef], digits = 15), "\n")
}

# ---- MIXED (random intercept via inter-block correlation) ----
mdes <- model.matrix(~ organismPart + treatment)
dc <- duplicateCorrelation(dat, design = mdes, block = individual)
cat("\n===== MIXED golden (duplicateCorrelation + gls.series + eBayes) =====\n")
cat("consensus.correlation =", format(dc$consensus.correlation, digits = 15), "\n")
gfit <- gls.series(dat, design = mdes, block = individual, correlation = dc$consensus.correlation)
geb <- eBayes(gfit)
cat("df.total (eBayes):", geb$df.total[1], "\n")
for (p in c("1255_g_at", "202086_at", "203153_at", "205483_s_at", "213797_at", "214059_at")) {
    if (!p %in% rownames(dat)) next
    i <- match(p, rownames(dat))
    cat("\n--", p, "--\n")
    cat("coefficients:", format(gfit$coefficients[i, ], digits = 15), "\n")
    cat("sigma:", format(gfit$sigma[i], digits = 15), "\n")
    cat("t_treat:", format(geb$t[i, trCoef], digits = 15), "\n")
    cat("p_treat:", format(geb$p.value[i, trCoef], digits = 15), "\n")
}
