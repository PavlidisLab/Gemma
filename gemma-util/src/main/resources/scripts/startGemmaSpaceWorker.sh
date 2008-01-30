#starts all the workers. Report generation, index worker, loading experiments
# $Id$

#-Dcom.gs.home = where the gigaspaces log files will go

java -jar /spacef/grp/bin/gemmaCli.jar -Dcom.gs.home=/spacef/gemmaData/space -Xmx5000M -ea ubic.gemma.grid.javaspaces.index.IndexGemmaSpaceWorkerCLI -u administrator -p gemmatoast -gigaspacesOn -compassOn &


