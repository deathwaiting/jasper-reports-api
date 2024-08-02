# Jasper Report REST
## A service for generating arbitrary reports using jasper reports library

Jasper reports is one of the oldest and most-mature report generation libraries in the java eco-system, having several greate features such as :
- Good support for multiple output formats, from PDF, to even XLSX files.
- A large set of features, configurations that covers a lot of use cases.
- Can work with numerous data sources, from SQL database to JSON and XML files.
- Has a mature feature rich report designer.
- Opensource library, while being backed by a large company like Tibco, so, its development is still active and kicking.

Unfortunately, in the era of microservices, Jasper Reports is not there yet. Being a java library means it is accessible mainly by java applications only, and even for java applications, adding a heavyweight library like that to generate a report or two in a single service is usually an overkill.
In a system using microservices architecture, or when working with another technology than java, it is usually easier to have a central report engine that all services use, which is expected to be:
- Light weight service, easily scalable.
- Flexible, so it can generate arbitrary reports.
- Secure. As reports can have acccess to sensitive data.

Unfortunately, I couldn't find an opensource solution that meet such criteria. Tibco Jasper server is a legacy application and mainly targets enterprises. It is a standalone heavyweight solution that provides report management and generation solutions, mainly for non-technical users. This is not the use case for mircroservice systems requiring a report service.

This project is an attempt to provide an opensource report service that fits in a mircroservice systems, providing a REST API for just running and generating reports.
The project is still in early stages, and currently has the following limitations:
- Reports JRXML files are saved in a local storage accessible to the service.
- Reports currently don't have access to the classpath, so, no custom jars or fonts can be used.

The service is currently built using Spring boot 3.x, so, it can use Spring boot various features for security, like JWT, OIDC and OAuth2 support, and the service can be built as: as Fat-jar, Docker image, etc ..

This is still in alpha stage. It will mostly work, but use it with caution 😶.

If you met any bugs please create a issue on github or gitlab.

# Installation and development

```shell
./mvnw install
```

To Build and run the service in dev mode. 
This will :
- Start the application with h2 database that can be accessed from [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
  - Note an in-memory database will be generated and populated with the data in `src/main/resources/data.sql`
  - The data base url will provided by spring-boot in the logs
  - The credentials is User: sa, and an empty password
- Use the sample report in directory `src/test/resources/test-reports`.
- Disable security
- 
```shell
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Now you can can generate reports by calling the GET REST API. If for example you have the report `myreport.jraxml`, and you need to run it against your mssql database, you can generate it as PDF using

`http://localhost:8080/report/emp-report.pdf`

To generate it in another format, change the file extension, for example

`http://localhost:8080/report/emp-report.docx`

Report paramters can be added as query parameters :
`http://localhost:8080/report/emp-report.pdf?dep_id=1&month=11`

Current supported extensions are : `pdf`, `docx`, `xlsx`,`html`, `csv`