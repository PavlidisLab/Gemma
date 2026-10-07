# Reference values for RealDesignsFitTest: limma fits on REAL Gemma designs (fixtures from
# fetch_real_designs.py). Tests depend on these numbers not changing.
#
# Run with: Rscript real-designs-golden-gen.R   (needs limma; writes <GSE>/golden.txt next to each design)
#
# Per dataset:
#   paired : lmFit + eBayes on ~ block + cond      (subject as fixed block; skipped when rank-deficient)
#   mixed  : duplicateCorrelation(design = ~ cond, block) -> gls.series -> eBayes
# golden.txt: '# key: value' header lines, then  model  probe  term  coef  sigma  t  p  (moderated t and p).

options(digits = 15)
suppressMessages(library(limma))
here <- tryCatch(dirname(sys.frame(1)$ofile), error = function(e) ".")
if (is.null(here) || !nzchar(here)) here <- "."
args <- commandArgs(trailingOnly = TRUE)
dirs <- if (length(args)) args else list.dirs(here, recursive = FALSE, full.names = FALSE)

for (g in dirs) {
    dpath <- file.path(here, g, "design.txt")
    if (!file.exists(dpath)) next
    dd <- read.table(dpath, comment.char = "#", header = TRUE, sep = "\t", stringsAsFactors = FALSE, na.strings = character(0))
    dat <- as.matrix(read.table(file.path(here, g, "expmat.txt"), header = TRUE, sep = "\t", check.names = FALSE, row.names = "probe"))
    dd$cond <- ifelse(is.na(dd$cond), "", as.character(dd$cond))
    stopifnot(all(colnames(dat) == dd$sample))
    block <- factor(dd$block)
    hasCond <- any(nzchar(dd$cond))
    if (hasCond) cond <- factor(dd$cond, levels = unique(dd$cond[order(match(dd$cond, c("pre", "ctrl", "wt", "d0")))]))
    out <- file(file.path(here, g, "golden.txt"), "w")
    w <- function(...) cat(..., "\n", file = out, sep = "")
    w("# limma ", as.character(packageVersion("limma")), " golden for ", g, "; regenerate with real-designs-golden-gen.R")
    w("# samples: ", ncol(dat), "  blocks: ", nlevels(block), "  max block size: ", max(table(block)))

    rows <- function(model, fit, eb, terms, tt, pp) {
        for (i in seq_len(nrow(dat))) for (k in terms)
            w(model, "\t", rownames(dat)[i], "\t", k, "\t", format(fit$coefficients[i, k], digits = 15), "\t",
              format(fit$sigma[i], digits = 15), "\t", format(tt[i, k], digits = 15), "\t", format(pp[i, k], digits = 15))
    }

    if (hasCond) {
        mm <- model.matrix(~ block + cond)
        if (qr(mm)$rank == ncol(mm) && nrow(mm) > ncol(mm)) {
            fit <- eBayes(lmFit(dat, mm))
            w("# paired.df.residual: ", fit$df.residual[1])
            w("# paired.df.prior: ", format(fit$df.prior, digits = 15))
            w("# paired.s2.prior: ", format(fit$s2.prior, digits = 15))
            terms <- grep("^cond", colnames(mm), value = TRUE)
            w("# paired.terms: ", paste(terms, collapse = ","))
            w("model\tprobe\tterm\tcoef\tsigma\tt\tp")
            rows("paired", fit, fit, terms, fit$t, fit$p.value)
        } else {
            w("# paired: rank-deficient (rank ", qr(mm)$rank, " of ", ncol(mm), " columns, ", nrow(mm), " samples)")
            w("model\tprobe\tterm\tcoef\tsigma\tt\tp")
        }
        mdes <- model.matrix(~ cond)
    } else {
        w("model\tprobe\tterm\tcoef\tsigma\tt\tp")
        mdes <- model.matrix(~ 1, data = data.frame(x = seq_len(ncol(dat))))
    }

    dc <- tryCatch(duplicateCorrelation(dat, design = mdes, block = block), error = function(e) NULL, warning = function(w) suppressWarnings(duplicateCorrelation(dat, design = mdes, block = block)))
    rho <- dc$consensus.correlation
    w("# mixed.consensus.correlation: ", format(rho, digits = 15))
    if (!is.null(rho) && is.finite(rho) && max(table(block)) > 1) {
        gfit <- gls.series(dat, design = mdes, block = block, correlation = rho)
        geb <- eBayes(gfit)
        w("# mixed.df.prior: ", format(geb$df.prior, digits = 15))
        w("# mixed.df.total: ", format(geb$df.total[1], digits = 15))
        terms <- if (hasCond) grep("^cond", colnames(mdes), value = TRUE) else "(Intercept)"
        w("# mixed.terms: ", paste(terms, collapse = ","))
        colnames(geb$t) <- colnames(geb$p.value) <- colnames(gfit$coefficients)
        rows("mixed", gfit, geb, terms, geb$t, geb$p.value)
    }
    close(out)
    cat(g, "rho =", format(rho, digits = 6), "\n")
}
