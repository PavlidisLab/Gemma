# See DiffExTest
# setwd("~/Dev/eclipseworkspace/Gemma/gemma-core/src/test/resources/data/loader/expression/flatfileload")
library(edgeR)
library(limma)

d<-read.delim("GSE29006_expression_count.test.txt", row.names=1, header=T, sep='\t')
# We don't normalize this in our Gemma test because it's too small.
e<-read.delim("GSE29006_design.txt", row.names=1, header=T, sep='\t', comment.char = "#")

d["ENSG00000000938",]

y=voom(d, model.matrix(~ e$disease),plot=TRUE,lib.size=colSums(d))
y$E["ENSG00000000938",] # this is correct; we have  8.857276  9.183446 11.344523 11.156209
y$weights[3,] # we get  15.196517 15.011429  9.029577  5.309208 (different order)

# without weights
fit=lmFit(y$E,model.matrix(~ e$disease) )
summary(lm(y$E["ENSG00000000938",] ~ e$disease)) # 11.84, p=0.007056
coefficients(fit)["ENSG00000000938",]
fit<-noBayes(fit) # optional; see below.
topTable(fit, coef=2, sort.by="p")

# with voom weights
y=voom(d, model.matrix(~ e$disease),plot=TRUE,lib.size=colSums(d))
fit=lmFit(y,model.matrix(~ e$disease), weights=y$weights )
coefficients(fit)["ENSG00000000938",]
fit<-noBayes(fit)
topTable(fit, coef=2, sort.by="p")

# with our weights
# our weights are slightly different (lowess on small data set)
w<-read.delim("GSE29006.diffex.test.weights.txt", sep='', header=F)
# the ordering of this file is incorrect (sorry)
w<-w[,c(3,4,1,2)]
y$weights = w
w[3,]
fit=lmFit(y,model.matrix(~ e$disease), weights=w )
#stdevunscaled for this gene: 0.2727245  0.3273872
summary(lm(y$E["ENSG00000000938",] ~ e$disease, weights=as.vector(t(w[3,])))) # p=0.006680444, coef=2.232816
coefficients(fit)["ENSG00000000938",]
anova(lm(y$E["ENSG00000000938",] ~ e$disease, weights=as.vector(t(w[3,]))))
residuals(lm(y$E["ENSG00000000938",] ~ e$disease, weights=as.vector(t(w[3,])))) # matches ours: -0.160383 0.165787 0.094048 -0.094266
fit<-noBayes(fit)
topTable(fit, coef=2, sort.by="p") # p=0.009858270
qr.Q(lm(y$E["ENSG00000000938",] ~ e$disease, weights=as.vector(t(w[3,])))$qr)
qr.R(lm(y$E["ENSG00000000938",] ~ e$disease, weights=as.vector(t(w[3,])))$qr)

# low level call
fit<-lm.wfit(y=y$E["ENSG00000000938",], x=model.matrix(~ e$disease), w=as.vector(t(w[3,])))
coefficients(fit)
residuals(fit)
qr.R(fit$qr)

##########################################
noBayes <- function(fit)
{
  eb <- nobayes(fit=fit)
  
  fit$t <- eb$t
  fit$df.total <- eb$df.total
  fit$df.prior <- 0 # needed
  fit$p.value <- eb$p.value
  fit$s2.post <- eb$s2.post
  
  if(!is.null(fit$design) && is.fullrank(fit$design)) {
    F.stat <- classifyTestsF(fit,fstat.only=TRUE)
    fit$F <- as.vector(F.stat)
    df1 <- attr(F.stat,"df1")
    df2 <- attr(F.stat,"df2")
    if(df2[1] > 1e6) # Work around bug in R 2.1
      fit$F.p.value <- pchisq(df1*fit$F,df1,lower.tail=FALSE)
    else
      fit$F.p.value <- pf(fit$F,df1,df2,lower.tail=FALSE)
  }
  fit
}

nobayes <- function(fit)
{
  coefficients <- fit$coefficients
  stdev.unscaled <- fit$stdev.unscaled
  sigma <- fit$sigma
  df.residual <- fit$df.residual
  if(is.null(coefficients) || is.null(stdev.unscaled) || is.null(sigma) || is.null(df.residual)) stop("No data, or argument is not a valid lmFit object")
  if(all(df.residual==0)) stop("No residual degrees of freedom in linear model fits")
  if(all(!is.finite(sigma))) stop("No finite residual standard deviations")
  
  out <-  list(df.prior=0,var.prior=fit$scale)
  out$s2.prior <- 0
  out$s2.post <- sigma^2
  out$var.prior <- out$var.post <- NULL
  out$t <- coefficients / stdev.unscaled / fit$sigma
  df.total <- df.residual
  df.pooled <- sum(df.residual,na.rm=TRUE)
  df.total <- pmin(df.total,df.pooled)
  out$df.total <- df.total
  out$p.value <- 2*pt(-abs(out$t),df=df.total)
  
  out
}

