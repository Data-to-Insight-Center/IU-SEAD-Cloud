IU-SEAD-Cloud : repository deprecated Fall 2016. See component services
============

Steps to build:
---------------
1) Checkout the SEAD-Monitoring tool from Github and build it using Maven
~~~
https://github.com/Data-to-Insight-Center/Curbee/tree/master/services/sead-monitoring
mvn clean install -DskipTests
~~~
2) Checkout the IU-SEAD-Cloud repository from Github
~~~
git clone https://github.com/Data-to-Insight-Center/IU-SEAD-Cloud.git
~~~
3) Move the to root directory and execute following command.
~~~
mvn clean install -DskipTests
~~~
This should build all module needed for the Matchmaker service.

Steps to deploy on Tomcat:
--------------------------

1) Copy the following .war files from relevant target directory into TOMCAT_HOME/webapps.
~~~
landing-page.war
iu-sead-cloud-search.war
~~~

2) Fix the endpoint URL in following configuration file under webapp.
~~~
landing-page/WEB-INF/classes/org/sead/sda/config.properties
iu-sead-cloud-search/WEB-INF/classes/org/iu/sead/cloud/util/default.properties
~~~

3) Start the server.

Now the Landing Page and IU-SEAD-Cloud Search web interfaces should be accessible through the following URLs.
~~~
IU SEAD Cloud Home Page : http://host:port/landing-page/home.html
IU SEAD Cloud Landing Page : http://host:port/landing-page/sda?tag=<ro_id>
IU SEAD Cloud Search Page: http://host:port/iu-sead-cloud-search/search.html
~~~

IU SEAD Cloud Agent setup:
--------------------------

Please follow the https://github.com/Data-to-Insight-Center/IU-SEAD-Cloud/tree/master/sda-agent/agent README file for IU SEAD Cloud Agent setup.
