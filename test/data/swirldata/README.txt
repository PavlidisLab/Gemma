The swirl data set is taken from the marray bioConductor package
(Dudoit et al.). According to the 'swirl' documentation:

"These data were provided by Katrin Wuennenberg-Stapleton from the
Ngai Lab at UC Berkeley. The swirl embryos for this experiment were
provided by David Kimelman and David Raible at the University of
Washington."

The data can be analyzed in R like this (after unpacking the archives)

library(marray)
maGf<-as.matrix(read.delim("maGf.sample.txt"))[,2:5]
maGb<-as.matrix(read.delim("maGb.sample.txt"))[,2:5]
maRb<-as.matrix(read.delim("maRb.sample.txt"))[,2:5]
maRf<-as.matrix(read.delim("maRf.sample.txt"))[,2:5]


layout<-new("marrayLayout", maNgr=1, maNgc=1, maNsr=1, maNsc=8448, maNspots=8448, maSub=TRUE)
info.genes<-new("marrayInfo", maLabels=dimnames(maGf)[[1]])
info.targets<-new("marrayInfo", maLabels=dimnames(maGf)[[2]][2:5])
k<-new("marrayRaw", maRf=maRf, maGf=maGf, maRb=maRb, maGb=maGb, maLayout=layout, maGnames=info.genes, maTargets=info.targets)
g<-maNorm(k, norm="loess")
maM(g)[100,3]


# normalizing "by hand". This is a location-only normalization, so the
  normalization factor only involves maMloc and is the same for all
  rows.

rawlr <- -log2((maGf-maGb)/(maRf-maRb))
rawlr[1,] + maMloc(maNorm(swirl, norm="median"))[1,]
# compare to
maM(maNorm(swirl, norm="median"))[1,]

# here's a direct call to maNormMain
maNormMain(mbatch = k, f.loc = list(maNormLoess(x = "maA", y = "maM", z = NULL, w = NULL)), Mloc = TRUE, Mscale = TRUE, echo = TRUE)

