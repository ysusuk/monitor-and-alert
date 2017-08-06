## General
This service will acept events in json format
```json
  {
    "_id": "event_blah",
    "_type": "login",
    "user_name": "me@me.com",
    "user_id": "user_blah",
    "source_ip": "172.0.0.1",
    "browser": "chrome",
    "creation_date": "1485344457000",
    "status": "failed"
  }
```
Event storage is implemented as in memory map `(user name -> Seq[event])`. Storage should preferable be replaced with sql like storage, because there are several scenarious for data retrival, like - by id, by type, by user name. Ideal will probably be InfluxDB, since it's optimized to work with time sensitive data.

If there will be more then **10 login events that failed in speicied time period (default is 30 mins)**, alert will be printed to console.

## Run
```shell
sbt run
```

## Test
```shell
curl -i -H "Content-type: application/json" -X POST http://localhost:9000/push -d '
  { "_id": "event_blah", "_type": "login", "user_name": "me@me.com", "user_id": "user_blah", "source_ip": "172.0.0.1", "browser": "chrome", "creation_date": "1485344457000", "status": "failed" }
'
```