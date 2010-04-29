rowlm<-function(formula,data) {
    mf<-lm(formula,data,method="model.frame");
    mt <- attr(mf, "terms");
    x <- model.matrix(mt, mf);
    design<-model.matrix(formula);
    cl <- match.call();
    r<-nrow(data);
    res<-vector("list",r);
    lev<-.getXlevels(mt, mf);
    clz<-c("lm");
    D<-as.matrix(data);
    ids<-row.names(data);
    for(i in 1:r) {
        y<-as.vector(D[i,]);
        id<-ids[i];
        m<-is.finite(y); 
        if (sum(m) > 0) {
            X<-design[m,,drop=FALSE];
            attr(X,"assign")<-attr(design,"assign");
            y<-y[m];
            z<-lm.fit(X,y);
            class(z) <- clz;
            z$na.action <- na.exclude;
            z$contrasts <- attr(x, "contrasts");
            z$xlevels <- lev;
            z$call <- cl;
            z$terms <- mt;
            z$model <- mf;
            res[[i]]<-z;
        } 
    }
    names(res)<-row.names(data);
    return(res)
}

# Special case of a one-sample t-test
rowTtest<-function(data) {
    mf<-lm("~1",data,method="model.frame");
    mt <- attr(mf, "terms");
    x <- model.matrix(mt, mf);
    design<-matrix(1,ncol(data),1)
    attr(design, "assign")<-0
    cl <- match.call();
    r<-nrow(data);
    res<-vector("list",r);
    lev<-.getXlevels(mt, mf);
    clz<-c("lm");
    D<-as.matrix(data);
    ids<-row.names(data);
    for(i in 1:r) {
        y<-as.vector(D[i,]);
        id<-ids[i];
        m<-is.finite(y); 
        if (sum(m) > 0) {
            X<-design[m,,drop=FALSE];
            attr(X,"assign")<-attr(design,"assign");
            y<-y[m];
            z<-lm.fit(X,y);
            class(z) <- clz;
            z$na.action <- na.exclude;
            z$contrasts <- attr(x, "contrasts");
            z$xlevels <- lev;
            z$call <- cl;
            z$terms <- mt;
            z$model <- mf;
            res[[i]]<-z;
        } 
    }
    names(res)<-row.names(data);
    return(res)
}