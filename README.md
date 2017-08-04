curl -i -H "Content-type: application/json" -X POST http://localhost:9000/push -d '
  { "_id": "event_blah", "user_name": "me@me.com", "creation_date": "1485344457000", "status": "failed" }
'