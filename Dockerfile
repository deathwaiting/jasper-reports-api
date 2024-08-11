# Use the tag to fetch the base image
FROM  registry.gitlab.com/a.galal7/jasper-reports-api:temp AS base

USER root

#Fetch default font packages, else jasper will fail to generate the report even if they are not used
RUN apt-get update && \
    apt-get install -y libfreetype6-dev fontconfig fonts-dejavu-core && \
    fc-cache -fv && \
    rm -rf /var/lib/apt/lists/*

#Set report location in the image, reports directory on the host machine should be mapped to this location
ENV DEV_GALAL_JASPER_REST_SERVER_REPORTS_DIR=/var/reports

# Set the entrypoint. It also adds the reports directory as a classpath, so users can add additional jar files for fonts or other resources.
ENTRYPOINT ["/layers/paketo-buildpacks_bellsoft-liberica/jre/bin/java", "-cp" , ".:/var/reports/*", "-Dloader.path=./,/var/reports", "org.springframework.boot.loader.launch.PropertiesLauncher"]
