# Auction System
Reactive programming in Scala using Akka.

## Objectives
Main goal of the project was to create simple auction system based on  Akka framework in Scala.

## How the system works
In our system we have several kind of actors:
* Auction
* AuctionSearch
* Buyer
* Seller

Seller is able to register an auction in system. It is suposed to provide basic details about auction such like init price, and timeouts (time intervals of auction's state changes). After being created, Seller actor register its auctions in AuctionSearch actor, which is responsible for maintaining opened auctions in system so that it can answer for Buyers queries. While Auction life-time the actor receives bids and sends update messages for all interested actors that follow its state (like buyer and sellers).

## Demo
Initializer class contains system demo.
 
