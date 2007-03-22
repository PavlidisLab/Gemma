These classes act as a starting point for using gigaspaces.  That is, they have been tested and serve the purpose of the
Master sending a task to a space, the Worker executing it, and the Master obtaining the results.

WINDOWS

To successfully run the Master and Worker apps, I did the following:

a) Downloaded the Community edition of Gigaspaces, version 5.2 and unzipped it.
b) Added the following to gemma-core/pom.xml (I am not checking in my updated pom just yet):
		<dependency>
			<groupId>spring</groupId>
			<artifactId>spring-modules-javaspaces</artifactId>
			<version>0.8</version>
			<type>jar</type>
		</dependency>
		<dependency>
			<groupId>jini</groupId>
			<artifactId>jini-core</artifactId>
			<version>2.1</version>
			<type>jar</type>
		</dependency>
		<dependency>
			<groupId>jini</groupId>
			<artifactId>jini-ext</artifactId>
			<version>2.1</version>
			<type>jar</type>
		</dependency>
		<dependency>
			<groupId>javaspaces</groupId>
			<artifactId>JSpaces-dl</artifactId>
			<version>0.8</version>
			<type>jar</type>
		</dependency>
		<dependency>
			<groupId>javaspaces</groupId>
			<artifactId>JSpaces</artifactId>
			<version>0.8</version>
			<type>jar</type>
		</dependency>
c) Took took ubic\gemma\javaspaces\gigaspaces\Result.class and placed it (not just the class, but the directory as well) in %GIGASPACES_HOME%\spring\examples\remote\classes.
d) cd into $GIGASPACES_HOME\spring\examples\remote\bin and run startAll.bat.  This starts the Java Space.
e) From eclipse, create a run configuration for Worker and start it with the VM argument -Dcom.gs.home=C:\java\.m2\respsitory\javaspaces\JSpaces\0.8 (replace .m2\repository with your repository).
This starts the Worker, which will be polling the space for "tasks".  Once a task exists in the space, the Worker will take it
and execute it, then return it to the space along with the Result object (this is why we copied the compiled Result into the
space in the step above ... because it needs to know about it).
f) The Master will obtain the results.

LINUX

same as above but replace \ with / and use .sh instead of .bat (ie. startAll.sh instead of startAll.bat).

OUTPUT

Master:
Submitted Job 1 with  in 18423 ms
Submitted Job 2 with  in 47 ms

Worker:
I am doing the task id = 1 with data : data0
I am doing the task id = 2 with data : data1
