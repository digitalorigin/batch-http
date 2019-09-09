# BatchHttp
A tool for processing data batches through a REST API. It reads the `stdin` for JSON lines representing HTTP calls,
it makes the call and outputs input and output also in JSON format. For example, when passed a JSON string `{"query": {"address": "1600 Amphitheatre Parkway, Mountain View, 9090", "region":"es", "language":"es"}`,
it will make a request to `https://maps.googleapis.com/maps/api/geocode/json?region=es&language=es&address=1600+Amphitheatre+Parkway,+Mountain+View,+CA`
and return the output as a JSON line with both the query and the API response body, provided the `endpoint` and `path` configuration values are set to
`https://maps.googleapis.com` and `maps/api/geocode/json` respectively.

# Example
```bash
echo '{"query": {"address": "1600 Amphitheatre Parkway, Mountain View, 9090", "region":"es", "language":"es"}}' \
 | flow-ggeocode
# will make a request to <endpoint><path>?address=1600+Amphitheatre+Parkway,+Mountain+View,+CA
# where <endpoint> and <path> are configurable in the application.conf file, and output another JSON
# kjn

```

```json
    "type": "RECORD",
    "stream": "ggeocoding",
    "time_extracted": "2019-04-23T12:00:00Z",
    "record" :
        {
           "results" : [
              {
                 "address_components" : [
                    {
                       "long_name" : "1600",
                       "short_name" : "1600",
                       "types" : [ "street_number" ]
                    },
                    {
                       "long_name" : "Amphitheatre Pkwy",
                       "short_name" : "Amphitheatre Pkwy",
                       "types" : [ "route" ]
                    },
                    {
                       "long_name" : "Mountain View",
                       "short_name" : "Mountain View",
                       "types" : [ "locality", "political" ]
                    },
                    {
                       "long_name" : "Santa Clara County",
                       "short_name" : "Santa Clara County",
                       "types" : [ "administrative_area_level_2", "political" ]
                    },
                    {
                       "long_name" : "California",
                       "short_name" : "CA",
                       "types" : [ "administrative_area_level_1", "political" ]
                    },
                    {
                       "long_name" : "United States",
                       "short_name" : "US",
                       "types" : [ "country", "political" ]
                    },
                    {
                       "long_name" : "94043",
                       "short_name" : "94043",
                       "types" : [ "postal_code" ]
                    }
                 ],
                 "formatted_address" : "1600 Amphitheatre Parkway, Mountain View, CA 94043, USA",
                 "geometry" : {
                    "location" : {
                       "lat" : 37.4224764,
                       "lng" : -122.0842499
                    },
                    "location_type" : "ROOFTOP",
                    "viewport" : {
                       "northeast" : {
                          "lat" : 37.4238253802915,
                          "lng" : -122.0829009197085
                       },
                       "southwest" : {
                          "lat" : 37.4211274197085,
                          "lng" : -122.0855988802915
                       }
                    }
                 },
                 "place_id" : "ChIJ2eUgeAK6j4ARbn5u_wAGqWA",
                 "types" : [ "street_address" ]
              }
           ],
           "status" : "OK"
        }
```

# Mappings
A `mappings` key can be defined in the configuration to apply a set of mapping to the input record keys. This can be
helpful for adapting the output of a singer tap or flow so it can be understood for geo-coding. For example, the following
`mappings` can be defined in the `application.conf`
```hocon
flow {
  mappings {
        "calle": "street"
        "direccion": "address"
        "numero": "nr"
        "ciudad": "city"
        "codigo_postal": "zip_code"
  }
}
````
so we use the keys in Spanish in the input records
```bash
echo '{ "type": "RECORD", "stream": "normalized_address", "record": {"calle": "Avenida Gran Vía", "numero": "12", "ciudad": "Barcelona", "codigo_postal": "08013"} }' \
 flow-ggeocode
```
A `stream` configuration key can be provided as well to override the input stream name
```hocon
flow {
  stream = "normalized_address"
}
```
so the flow understands a record if the stream name is not `raw_address` or `normalized_address`
```bash
echo '{ "type": "RECORD", "stream": "addresses_export", "record": {"calle": "Avenida Gran Vía", "numero": "12", "ciudad": "Barcelona", "codigo_postal": "08013"} }' \
 flow-ggeocode
```

# Build and Run
This is an [SBT](https://www.scala-sbt.org/) project. If you don't have sbt installed, do so by running `brew install sbt`
on Mac. Then you can compile and package the project with
```bash
sbt package && sbt assembly
```
And next run the tap like
```bash
java -jar target/scala-2.12/flow-ggeocode-assembly-0.1-SNAPSHOT.jar -Dconfig.file=application.conf
```
