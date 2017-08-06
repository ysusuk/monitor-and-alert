curl -i -H "Content-type: application/json" -X POST http://localhost:9000/push -d '
  { "_id": "event_blah", "_type": "login", "user_name": "me@me.com", "user_id": "user_blah", "source_ip": "172.0.0.1", "browser": "chrome", "creation_date": "1485344457000", "status": "failed" }
'