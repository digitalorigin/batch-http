# BatchHttp
A tool for processing data batches through a REST API. It reads the `stdin` for JSON lines representing HTTP calls,
it makes the call and outputs input and output also in JSON format. For example, when passed a JSON string `{"query": {"address": "1600 Amphitheatre Parkway, Mountain View, 9090", "region":"es", "language":"es"}`,
it will make a request to `https://maps.googleapis.com/maps/api/geocode/json?region=es&language=es&address=1600+Amphitheatre+Parkway,+Mountain+View,+CA`
and return the output as a JSON line with both the query and the API response body, provided the `endpoint` and `path` configuration values are set to
`https://maps.googleapis.com` and `/maps/api/geocode/json` respectively.

- If a `query` object is passed in the JSON, a request will be created with all parameters inside
the object as URL parameters. `GET` method will be used.

- If a `body` object is passed in the JSON, a request will be created with the contents of `body`
in the request body. `POST` method will be used.

## Configuration
You can find examples and descriptions for all configurations supported by `batch-http` in the [sample configuration file](src/main/resources/application.conf).
All this properties can be overridden on invocation by providing appropriate [JVM arguments](https://github.com/lightbend/config).

The util uses `logback` for logging. The default logging configuration can be found in the [`logback.xml` file](src/main/resources/logback.xml).

## Build and Run
This is an [SBT](https://www.scala-sbt.org/) project. If you don't have sbt installed, do so by running `brew install sbt`
on Mac. Then you can compile and package the project with
```bash
sbt ";compile;universal:packageBin"
```
And run it with
```bash
sbt run
```

## Integration Tests
Integration tests are based on [minio](https://github.com/minio/minio) public object store service. They are skipped by
default during build. If you want to run them, so can do so with `sbt it:test`.
