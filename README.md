# Jasper Reports API
## A service for generating arbitrary reports using jasper reports library

Jasper reports is one of the oldest and most-mature report generation libraries in the java eco-system, having several great features such as :
- Good support for multiple output formats, from PDF, to even XLSX files.
- A large set of features, configurations that covers a lot of use cases.
- Can work with numerous data sources, from SQL database to JSON and XML files.
- Has a mature feature rich report designer.
- Opensource library, while being backed by a large company like Tibco, so, its development is still active and kicking.

Unfortunately, in the era of microservices, Jasper Reports is not there yet. Being a java library means it is accessible mainly by java applications only, and even for java applications, adding a heavyweight library like that to generate a report or two in a single service is usually an overkill.
In a system using microservices architecture, or when working with another technology than java, it is usually easier to have a central report engine that all services use, which is expected to be:
- Light weight service, easily scalable.
- Flexible, so it can generate arbitrary reports.
- Secure. As reports can have access to sensitive data.

Unfortunately, I couldn't find an opensource solution that meet such criteria. Tibco Jasper server is a legacy application and mainly targets enterprises. It is a standalone heavyweight solution that provides report design, management and generation features, mainly for non-technical users. This is not the use case for microservice systems requiring a report service.

This project is an attempt to provide an opensource report server that fits in a microservices system, providing a REST API for just running and generating reports.
The project is still in early stages, and currently has the following limitations:
- Reports JRXML files are saved in a local storage accessible to the service.

The service is currently built using Spring boot 3.x, so, it can use Spring boot various features for security, like JWT, OIDC and OAuth2 support, and the service can be built as: as Fat-jar, Docker image, etc ..

This is still in alpha stage. It will mostly work, but use it with caution 😶.

If you met any bugs please create a issue on github or gitlab.

# Usage

The simplest way is to use the app as a docker image:
- The reports directory on the host machine must be mapped to `/var/reports` in the container.
- The reports directory is also added as a classpath, so, fonts jar files, and other report resources can be placed there as well.
- Spring boot configuration env variables can be used to configure security, database connections and other features.
- Reports are cached in-memory by default. For more info check the caching section below.

#### Examples 

This will run the report server, connect it to postgres database, and uses html BASIC security. By default, Spring boot will redirect to a login screen if no credentials are provided.

```sh
docker run -it --tty --rm\
 -v ./src/test/resources/test-reports:/var/reports\
 -e SPRING_SECURITY_USER_NAME=user\
 -e SPRING_SECURITY_USER_PASSWORD=pass\
 -e SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/postgres\
 -e SPRING_DATASOURCE_USERNAME=postgres\
 -e SPRING_DATASOURCE_PASSWORD=postgres\
 -p 8080:8080\
 registry.gitlab.com/a.galal7/jasper-reports-api:0.1
```

If for example you have the report `emp-report.jrxml` in the report directory, you can generate it as PDF using

[http://localhost:8080/report/emp-report.pdf](http://localhost:8080/report/emp-report.pdf)

Or with curl command

```sh
curl 'http://localhost:8080/report/emp-report.pdf' \
  -H 'Authorization: Basic dXNlcjpwYXNz'\
  -o emp-report.pdf
```

Report parameters can be added as query parameters :

[http://localhost:8080/report/emp-report.pdf?dep_id=1&month=11](http://localhost:8080/report/emp-report.pdf?dep_id=1&month=11)



To generate it in another format, change the file extension, for example:

[http://localhost:8080/report/emp-report.docx](http://localhost:8080/report/emp-report.docx)

Current supported extensions are : `pdf`, `docx`, `xlsx`,`html`, `csv`


# Installation and development

run to package the application as a jar file
```shell
./mvnw install
```

to generate a docker image, we are using spring boot default image builder, then customize the generated image.
```shell
./mvnw clean spring-boot:build-image docker:build
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

# Caching

- The application will cache compiled JasperReports instances by default, this provides a huge performance boost for reports saved on disk.
- As reports are rarely updated - at least not daily-, the cache life-time can be set to higher values.
- We are using in-memory cache, which has better performance, but also means restarting the application will evict all entries.
- Cache can be configured using the following env variables :
  - `DEV_GALAL_JASPER_REST_SERVER_REPORTS_CACHE_TTL` will set the time-to-live in minutes, default value is 1440 (24 hours).
  - `DEV_GALAL_DEV_GALAL_JASPER_REST_SERVER_REPORTS_CACHE_SIZE` will set the maximum number of reports to cache, default is 512.
