# HTTP
This module is in charge of exposing creation of commands to Kafka via HTTP POST requests.

### How to build
`sbt 'http/docker:publishLocal'`

### How to run
##### Let's try out the --help flag

`docker run -it http:1.0.0 --help`

```
Kafka topics published to websocket 1.0.0
Usage: Kafka Topic as Websocket [options]

  --kafka <value>        URL of Kafka: By default: 0.0.0.0:29092
  --host <value>         Host of the websocket
  --port <value>         Port of the websocket
  --whitelist whitelist  Explicitely define what Kafka topics are available for exposure to write from HTTP.
  --blacklist blacklist  Explicitely define what Kafka topics are dissallowed from exposure to write from HTTP
  --group <value>        Kafka consumer group
  --help                 
                         
                          You can use tool to expose the stream of messages from 
                          Kafka topics to a single Websocket server.
                          
                          By default it will expose the following topics to be populated via HTTP POST:
                          
                          	-bid
                         	-lot
                         	-user
```

##### Let's try out an example:

`docker run -p 8081:8081 -it http:1.0.0 --kafka 0.0.0.0:29092 --port 8081`

```
Exposing the following topics for HTTP POST:

 	-bid
	-lot
	-user

To hit the API you can try the following commands:
  
[bid]
curl -d '{"user":{"id":{"id":"example user"}},"lot":{"id":{"id":"example lot"}},"bid":10}' -H "Content-Type: application/json" -X POST http://0.0.0.0:8081/bid 

 
[lot]
curl -d '{"id":{"id":"example lot"}}' -H "Content-Type: application/json" -X POST http://0.0.0.0:8081/lot 

 
[user]
curl -d '{"id":{"id":"example user"}}' -H "Content-Type: application/json" -X POST http://0.0.0.0:8081/user 

 



Server online at http://0.0.0.0:8081
Press RETURN to stop...

```