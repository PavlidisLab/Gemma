# $Id$
# Files here were generated b reading in other files and making them 
# more interesting for statistical analysis.

# Tests depend on them not changing.

factor1<-factor(c("a","a","a","a","b","b","b","b"));
factor2<-factor(c("c","c","d","d","c","c","d","d"));


dat<-read.table("anova-test-data.txt", header=T,row.names=1, sep='\t')
ancova<-apply(dat, 1, function(x){summary(lm(x ~ factor1+factor2))})
ancova$probe_4
ancova$probe_10
ancova$probe_98
#etc


v<-c(1,2,3,4,5,6,7,8)
ancova2<-apply(dat, 1, function(x){summary(lm(x ~ factor1+factor2+v))})
ancova2$probe_4
ancova2$probe_10
ancova2$probe_98


osttdat<-read.table("onesample-ttest-data.txt", header=T, row.names=1, sep='\t')
osttest<-apply(osttdat, 1, function(x){t.test(x)})
osttest$probe_4
osttest$probe_10
osttest$probe_16
osttest$probe_17
osttest$probe_98

#etc

# anova without interactions
anovaA<-apply(dat, 1, function(x){anova(aov(x ~ factor1+factor2))})
anovaA$probe_4
anovaA$probe_10
anovaA$probe_98
# etc

# anova with interactions
anovaB<-apply(dat, 1, function(x){anova(aov(x ~ factor1*factor2))})

anovaB$probe_4
anovaB$probe_10
anovaB$probe_98

# two-sample ttest
ttestd<-apply(dat, 1, function(x){try (t.test(x ~ factor1), silent=T)})

ttestd$probe_0
ttestd$probe_4
ttestd$probe_10
ttestd$probe_98