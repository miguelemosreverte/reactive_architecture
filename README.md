![badge_icon](https://images.credly.com/size/680x680/images/044acea6-eb45-4347-bc66-e81257dbce12/LRA-DistributedMessagingPatterns-badge.png)
[badge](https://www.credly.com/badges/1ed86ff7-61f6-4845-bc2a-3d2f329d698d/public_url)

# workshop-akka-actors


HTTP POST server -> Kafka[1] -> Akka writeside -> Kafka[2] 
Kafka[2] -> Readside that populates DB for query -> HTTP GET sever that hits database  
Kafka[2] -> Websocket Server that shows in realtime the biddings

![](https://raw.githubusercontent.com/miguelemosreverte/reactive_architecture/main/Architecture%20Proposal.svg)



## Objective overview

Build an application that will handle bidding on auction items (called lots). Auctions and Lots will be represented as Actors. In the next steps you will add Akka Persistence to preserve actor's state and Akka HTTP to build an API.


### General rules
- There can be multiple auctions with multiple items.
- The auction item is called a “lot”.
- One lot can be in only one auction.
- The auction can be started and ended but not restarted.
- To be able to bid on lots, the auction has to be started.

### Bidding
- The new bid has to be greater than the current bid.
- User can set a max bid value. The system should then use the lowest possible value to set as the current winning bid, but also remember the max value, so that when next time another user makes a bid it will be automatically resolved against the max bids.
- For this assignment Users are just Ids.

## Part 1

1. Create LotActor representing a lot, that will implement the bidding logic from the Domain Overview.
2. Create AuctionActor representing an auction. Should be implemented as FSM. There are three possible states: Closed, InProgress and Finished. Should support adding and removing lots but only when Closed. Should allow bidding only when InProgress.

Use Akka Typed
### Additional tasks:

- Implement AuctionActor as a [router](https://doc.akka.io/docs/akka/current/routing.html#how-routing-is-designed-within-akka)

## Part 2

Implement following endpoints using Akka Http:

#### Auction
- Create auction
- Start auction
- End auction
- Get all auctions

#### Lot
- Create lot
- Bid
- Get lots by auction
- Get lot by id
