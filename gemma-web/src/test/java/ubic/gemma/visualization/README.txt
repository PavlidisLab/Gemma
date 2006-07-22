The following is an example of how to create dynamic images in servlets.  To run the webapp, copy DynamicImageApp.war in your web-container 
webapps directory.  In Tomcat, this is $TOMCAT_HOME/webapps.

If you plan on hacking the java files, you will need to do the following to deploy it:

1. Create the following directory structure:
DynamicImageApp/
	lib/
	WEB-INF/
		classes/
	web.xml

2. Copy your .class files into the classes directory.  You can get the web.xml file by dropping the DynamicImageApp.war into tomcat, start Tomcat,
then open the exploded DynamicImageApp in $TOMCAT_HOME/webapps and copy web.xml to your directory.

3. To package this, cd into your DynamicImageApp directory and run: jar tvf DynamicImageApp.war

4. To list the contents run: jar cvf DynamicImageApp.war

5. Copy this to your $TOMCAT_HOME/webapps directory and start Tomcat.  		