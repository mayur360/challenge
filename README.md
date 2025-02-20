# README

# Overview
This project is a Java-based Order Management System using Gradle as a build tool. 
The system manages order placements, pickups, and movements between different storage areas (cooler, heater, shelf) based on temperature and freshness constraints.

# Project Structure
challenge/
│-- src/
│   ├── main/
│   │   ├── java/com/css/challenge/
│   │   │   ├── client/      # Order & Action classes
│   │   │   ├── management/  # OrderManagementSystem
│   │   │   ├── storage/     # Storage & Shelf classes
│   ├── test/
│   │   ├── java/com/css/challenge/management/  # Test classes
│-- build.gradle
│-- settings.gradle
│-- README.md


# Operations 

DISCARD logic - We are using priorityQueue for Shelf storage. 
The reason for using priorityQueue is that we can sort every order in it based on order's freshness.
As any hot or cold order is moved to shelf, Its freshness is reduced to half of current freshness value, 
therefore we choose discard process based on freshness. As per business, perspective, customer values freshness of order.
If we serve less fresh order and if we are charging them like regular order that can affect the business.

**Handling Hot orders**
1 - Received Order with Temp = hot

2 - Try placing in Ideal option, in this case is - Heater

3 - if Heater is full, then place order on shelf

4 - if shelf is full, as it is hot order then check if Cooler has empty space

5 - if Cooler has empty space then search least fresh cold order from shelf and move it to Cooler and add new order to shelf and reduce its freshness by half and update Action Log

6 - in Action log, if order id is not present then it will be with Action = Place else it will be Action = Move

7 - but if neither Heater has space nor shelf and Cooler then simply discard least fresh order from Shelf and then add new order to shelf along with freshness reduced to half and update action log same as point 6

**Handling Cold orders**
1 - Received Order with Temp = cold

2 - Try placing in Ideal option, in this case is - Cooler

3 - if Cooler is full, then place order on shelf

4 - if shelf is full, as it is cold order then check if Heater has empty space

5 - if Heater has empty space then search least fresh hot order from shelf and move it to Heater and add new order to shelf and reduce its freshness by half and update Action Log

6 - in Action log, if order id is not present then it will be with Action = Place else it will be Action = Move

7 - but if neither Cooler has space nor shelf and Heater then simply discard least fresh order from Shelf and then add new order to shelf along with freshness reduced to half and update action log same as point 6

**Handling room temperature orders**
1 - Received order with temp = room

2 - Try placing it in ideal option i.e. Shelf

3 - If Shelf is full, check place in Heater

4 - If heater has space, move least fresh hot order from shelf to heater and then place new order in shelf

5 - if heater doesn’t have space then check in Cooler

6 - If Cooler has space,  move least fresh cold order from shelf to cooler and then place new order in shelf

7 - if cooler doesn’t have space that means Shelf , Heater & Cooler all are full so we will discard least fresh order from shelf and then move new order to shelf

# Installation & Setup
Prerequisites
Java 17+
Gradle (Recommended: Use the Gradle Wrapper)
IDE (IntelliJ)

# Git
git clone https://github.com/mayur360/challenge.git

# Build Project
./gradlew build

## How to run

```
$ ./gradlew run --args="--auth=<token>"
```

