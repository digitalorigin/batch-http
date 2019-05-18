# FlowGGeocode
A [Singer.io](https://github.com/singer-io/getting-started) tap for processing addresses on the [Google geocoding service](https://developers.google.com/maps/documentation/geocoding/start). 
The flow accepts two types of records or `stream`s. In the `raw_address` stream, the flow expects the address in the
format `address`, `city` and `zip_code`, whilst in the `normalized_address` stream one can pass the `street` and `nr` on
separate fields. The flow makes a request to the geocoding service per record and outputs the full response.

# Example
```bash
echo '{ "type": "RECORD", "stream": "raw_address", "record": {"address": "Avenida Gran Vía, 12", "city": "Barcelona", "zip_code": "08013"} }' \
 flow-ggeocode
# will make a request https://maps.googleapis.com/maps/api/geocode/json?address=1600+Amphitheatre+Parkway,+Mountain+View,+CA&key=YOUR_API_KEY
# and one record will be output (in one line)
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