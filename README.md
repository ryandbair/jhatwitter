# Description

Take-home interview assignment for processing some aggregates from the Twitter sample stream.

This examples makes use of Akka HTTP server and client and Akka streams for the majority of the processing. 

Akka-streams has a good back-pressuring scheme which works via a demand signal which propogates back through 
preceeding stages. At the HTTP source, akka-http leverage TCP back-pressure mechanisms. According to the Twitter docs
this will eventually cause the server to hangup if we don't keep close enough to the stream. We also are not leveraging
back pressure in the `Tally` actor stage of the graph, however the design does allow for this to be implemented.  

There is an actor stage in use to aggregate results and allow fetching them while the application is running. 

Currently not much work is being done in parallel. There are a bunch of ways we could tackle this:
+ place `async` boundries around parts of the streams graph
+ partition the stream, running all parts for a given tweet within the same thread, then aggregating results
+ run parts of the graph on separate systems
+ all of the above

## Running

Create an `application.conf` file on the classpath (/conf is a good place for testing) with an authenication like such:

```hocon
authentication {
    consumerKey = "somekey"
    consumerSecret = "secret"
    requestToken = "token"
    requestTokenSecret = "tokensecret"
}
```

From sbt, just type `run`. 

## TODOs

This is a rough cut but:
+ Logging
+ Tests, of any variety
+ Cut snapshots of KOAuth and akka-http-circe 
+ Adopt KOAuth
+ Time window counters - first thought is dropwizard metrics
+ Gracefully handling drops, reconnects, authentication issues, etc
