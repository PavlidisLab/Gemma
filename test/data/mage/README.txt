These are example MAGE-ML files harvested from the web.

They are not all valid! The DTD definitions could also be the wrong version.

# remove the dtd declaration.
perl -n -i -e "s/<\!DOCTYPE.*DTD\">//;print;" *.xml
perl -n -i -e "s/<\!DOCTYPE.*DTD\">//;print;" *.XML
perl -n -i -e "s/<\!DOCTYPE.*dtd\">//;print;" *.xml
perl -n -i -e "s/<\!DOCTYPE.*dtd\">//;print;" *.XML

head *.xml
head *.XML

gzip   *.xml
gzip  *.XML