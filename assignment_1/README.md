# Architectural Design and Trade-Off Analysis of a Real-Time Ride-Sharing Platform Backend

## Assignment Instructions Notes

### Overview

In this assignment, you are required to architect the backend infrastructure of a real-time ride-sharing platform similar to Uber or Lyft, using principles from distributed systems. Your task is to analyze and design a distributed backend system that supports high availability, low latency, and data consistency under high concurrency

You must integrate and analyze the following core architectural components:
- Database Sharding (Hash-based, Range-based, or Consistent Hashing)
- Replication and Consistency Models (Eventual, Strong, Quorum-based)
- Load Balancing (Layer 4 vs. Layer 7)

### Requirements

System Functional Requirements:
- Drivers broadcast live location and availability.
- Riders send trip requests that are matched with nearby drivers.
- The system supports live matching, route calculation, and payment processing.
- Data includes user accounts, trip histories, real-time GPS data, and pricing rules.

Non-Functional Requirements:
- Global scale support (multi-region architecture assumed)
- <200ms latency for ride-matching response in 90% of requests
- High read/write throughput for hotspots (e.g., downtown at 5 PM)