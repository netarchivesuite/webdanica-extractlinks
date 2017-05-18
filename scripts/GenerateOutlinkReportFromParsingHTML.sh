#  all deps available in Netarchivesuite dist for release 5.2.2 in lib folder
# so make a symbolic link to this lib folder, or make a copy of the lib-folder to this directory

DEPS="common-core-5.2.2.jar dom4j-1.6.1.jar jaxen-1.1.jar commons-io-2.0.1.jar log4j-1.2.17.jar slf4j-api-1.7.7.jar slf4j-log4j12-1.7.5.jar commons-logging-1.2.jar webarchive-commons-1.1.5.jar commons-lang-2.3.jar httpclient-4.3.6.jar commons-httpclient-3.1.jar guava-17.0.jar fastutil-6.5.2.jar"
LIB="extractlinks-1.0.0.jar"
CP=$LIB
for jar in DEPS
do
    CP=${CP}:lib/${jar}
done


java -cp $CP dk.netarkivet.extractlinks.GenerateOutlinkReportFromParsingHTML $1
