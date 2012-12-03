tdw<-read.delim("test-meta-analysis-input-down.txt", header=T, row.names=1)
tup<-read.delim("test-meta-analysis-input.up.txt", header=T, row.names=1)


length(which (p.adjust(apply(tup, 1, function(x) 1 - pchisq(-2*sum(log(x)), 2*length(x)) ), method="BH") <= 0.1))
length(which (p.adjust(apply(tdw, 1, function(x) 1 - pchisq(-2*sum(log(x)), 2*length(x)) ), method="BH") <= 0.1))

apply(tup, 1, function(x) 1 - pchisq(-2*sum(log(x)), 2*length(x)) )
apply(tdw, 1, function(x) 1 - pchisq(-2*sum(log(x)), 2*length(x)) )

up<-sort(names(which (p.adjust(apply(tup, 1, function(x) 1 - pchisq(-2*sum(log(x)), 2*length(x)) ), method="BH") < 0.1)))
dw<-sort(names(which (p.adjust(apply(tdw, 1, function(x) 1 - pchisq(-2*sum(log(x)), 2*length(x)) ), method="BH") < 0.1)))

apply(tdw, 1, function(x) 1 - pchisq(-2*sum(log(x)), 2*length(x)) )["BCAP31"]
apply(tup, 1, function(x) 1 - pchisq(-2*sum(log(x)), 2*length(x)) )["GUK1"]
apply(tdw, 1, function(x) 1 - pchisq(-2*sum(log(x)), 2*length(x)) )["ABCF1"]
apply(tup, 1, function(x) 1 - pchisq(-2*sum(log(x)), 2*length(x)) )["KXD1"]
apply(tdw, 1, function(x) 1 - pchisq(-2*sum(log(x)), 2*length(x)) )["THRA"]
apply(tdw, 1, function(x) 1 - pchisq(-2*sum(log(x)), 2*length(x)) )["PPM1G"]
apply(tup, 1, function(x) 1 - pchisq(-2*sum(log(x)), 2*length(x)) )["SEPW1"]
apply(tdw, 1, function(x) 1 - pchisq(-2*sum(log(x)), 2*length(x)) )["BCAP31"]
apply(tup, 1, function(x) 1 - pchisq(-2*sum(log(x)), 2*length(x)) )["BCAP31"]
apply(tup, 1, function(x) 1 - pchisq(-2*sum(log(x)), 2*length(x)) )["CAPRIN1"]

# trouble genes, that don't necessarily show up.
apply(tup, 1, function(x) 1 - pchisq(-2*sum(log(x)), 2*length(x)) )["ACLY"]
apply(tup, 1, function(x) 1 - pchisq(-2*sum(log(x)), 2*length(x)) )["ACTA2"]
apply(tdw, 1, function(x) 1 - pchisq(-2*sum(log(x)), 2*length(x)) )["ABCF1"]
apply(tdw, 1, function(x) 1 - pchisq(-2*sum(log(x)), 2*length(x)) )["ACO2" ]

1-pchisq(-2*sum(log(c(0.17976911531153372 , 0.6371425366325183	,0.0034514804690969347))), 6)
 
