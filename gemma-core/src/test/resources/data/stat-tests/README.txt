# $Id$
# Files here were generated b reading in other files and making them 
# more interesting for statistical analysis.

# Tests depend on them not changing.

factor1<-factor(c("a","a","a","a","b_base","b_base","b_base","b_base"));
factor2<-factor(c("c","c","d_base","d_base","c","c","d_base","d_base"));
factor3<-factor(c("u","v","w_base", "u","v","w_base","u","v"))

contrasts(factor1)<-contr.treatment(levels(factor1), base=2)
contrasts(factor2)<-contr.treatment(levels(factor2), base=2)
contrasts(factor3)<-contr.treatment(levels(factor3), base=3)

dat<-read.table("anova-test-data.txt", header=T,row.names=1, sep='\t')
 dm<-data.frame(factor1,factor2)

ancova<-apply(dat, 1, function(x){lm(x ~ factor1+factor2 )})
summary(ancova$probe_4)
summary(ancova$probe_10)
summary(ancova$probe_98)
anova(ancova$probe_4)
anova(ancova$probe_10)
anova(ancova$probe_98)
#etc

# ancova with continuous covariate
v<-c(1,2,3,4,5,6,7,8)
ancova2<-apply(dat, 1, function(x){ lm(x ~ factor1+factor2+v)})
summary(ancova2$probe_4)
summary(ancova2$probe_10)
summary(ancova2$probe_98)
anova(ancova2$probe_4)
anova(ancova2$probe_10)
anova(ancova2$probe_98)


# one way anoava

owanova<-apply(dat, 1,  function(x){anova(lm(x ~ factor3))});
owanova$probe_4
owanova$probe_10
owanova$probe_98

osttdat<-read.table("onesample-ttest-data.txt", header=T, row.names=1, sep='\t')
osttest<-apply(osttdat, 1, function(x){lm(x ~ 1)})
summary(osttest$probe_4)
summary(osttest$probe_10)
summary(osttest$probe_16)
summary(osttest$probe_17)
summary(osttest$probe_98)
anova(osttest$probe_4)
anova(osttest$probe_10)
anova(osttest$probe_16)
anova(osttest$probe_17)
anova(osttest$probe_98)

#etc

# anova without interactions
anovaA<-apply(dat, 1, function(x){lm(x ~ factor1+factor2)})
summary(anovaA$probe_4)
summary(anovaA$probe_10)
summary(anovaA$probe_98)
anova(anovaA$probe_4)
anova(anovaA$probe_10)
anova(anovaA$probe_98)
# etc

# anova with interactions
anovaB<-apply(dat, 1, function(x){lm(x ~ factor1*factor2)})
summary(anovaB$probe_4)
summary(anovaB$probe_10)
summary(anovaB$probe_98)
anova(anovaB$probe_4)
anova(anovaB$probe_10)
anova(anovaB$probe_98)

# anova with more than 2 levels in one factor
contrasts(factor3)<-contr.treatment(levels(factor3), base=3)
anovaD<-apply(dat, 1, function(x){lm(x ~ factor1+factor3)})
summary(anovaD$probe_4)
summary(anovaD$probe_10)
summary(anovaD$probe_98) 


# two-sample ttest
ttestd<-apply(dat, 1, function(x){try (lm(x ~ factor1), silent=T)})
summary(ttestd$probe_0)
summary(ttestd$probe_4)
summary(ttestd$probe_10)
summary(ttestd$probe_98)
anova(ttestd$probe_0)
anova(ttestd$probe_4)
anova(ttestd$probe_10)
anova(ttestd$probe_98)