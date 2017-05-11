setGeneric("maGreen", function(object) standardGeneric("maGreen"))
setMethod("maGreen", "marrayRaw",
          function(object)
          {
            A<-matrix(nr=0,nc=0)
            A<-maGf(object)-maGb(object)
            A
          }
          )

setGeneric("maRed", function(object) standardGeneric("maRed"))
setMethod("maRed", "marrayRaw",
          function(object)
          {
            M<-matrix(nr=0,nc=0)
            M<-maRf(object)-maRb(object)
            M
          }
          )


mymaNormMain<-function (mbatch, f.loc = list(maNormLoess()), f.scale = NULL, 
    a.loc = maCompNormEq(), a.scale = maCompNormEq(), Mloc = TRUE, 
    Mscale = TRUE, echo = FALSE) 
{
    if (is(mbatch, "marrayRaw")) {
        mnorm <- as(mbatch, "marrayNorm")
        M <- Ml <- Ms <- NULL
    }
    if (is(mbatch, "marrayNorm")) 
        mnorm <- mbatch
    slot(mnorm, "maNormCall") <- match.call()
    if (length(f.loc) > 0) 
        M <- Ml <- NULL
    if (length(f.scale) > 0) 
        M <- Ms <- NULL
    for (i in 1:ncol(maM(mbatch))) {
        if (echo) 
            cat(paste("Normalizing array ", i, ".\n", sep = ""))
        m <- mbatch[, i]
        cat(paste("raw m Rf:" , m@maRf[1], "\n"))
        M1 <- M2 <- NULL
#        Mnorm <- maM(m)
        Mnorm <- maRed(m)
        cat(paste("maRed(m): " , Mnorm[1], "\n"))
        if (length(f.loc) > 0) {
          for (func in f.loc) {
            cat("locationing\n")
            M1 <- cbind(M1, func(m))
          }
          if (length(f.loc) > 1) {
            cat("adjusting")
            M1 <- rowSums(M1 * a.loc(maGreen(m), length(f.loc)))
          }
          cat(paste("M1:", M1[1], "\n"))
          Ml <- cbind(Ml, M1)
          cat(paste("Mnorm:", Mnorm[1], "\n"))
         # Mnorm <- (Mnorm - M1)
          Mnorm<-M1
          cat(paste("Mnorm adj:", Mnorm[1], "\n"))
        }
        if (length(f.scale) > 0) {
          cat("scaling\n")
          m <- mnorm[, i]
          slot(m, "maM") <- Mnorm
          for (func in f.scale) {
            M2 <- cbind(M2, func(m))
            cat(paste("M2:", M2[1], "\n"))
          }
          if (length(f.scale) > 1) {
            M2 <- rowSums(M2 * a.scale(maGreen(m), length(f.scale)))
          }
          # m2 is the scaling factor.
          Ms <- cbind(Ms, M2)
          Mnorm <- Mnorm/M2
          cat(paste("Mnorm adj:", Mnorm[1], "\n"))
        }
        M <- cbind(M, Mnorm)
    }
    slot(mnorm, "maM") <- M
    if (length(f.loc) > 0 & Mloc) 
        slot(mnorm, "maMloc") <- Mnorm
    if (length(f.scale) > 0 & Mscale) 
        slot(mnorm, "maMscale") <- Ms
    return(mnorm)
}
