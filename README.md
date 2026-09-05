# Protonema ISO 8583 Server

High-performance, event-driven ISO 8583 financial transaction processing server powered by **jPOS** and modern **Quarkus**.

---

## Features

- **ISO 8583 Financial Message Engine**: Full support for ISO 8583:1987 and ISO 8583:1993 ASCII message formats using jPOS `GenericPackager`.
- **jPOS Q2 XML Deployment Engine**: Directly parses and executes standard jPOS Q2 XML deployment descriptors (`10_transaction_manager.xml`, `15_server_port_trxman.xml`, `11_serverport.xml`, `12_biller_channel.xml`, `20_mux.xml`).
- **Dynamic Transaction Manager & Group Routing**:
  - `0100` (Inquiry) $\rightarrow$ `InquiryParticipant`
  - `0200` (Transaction) $\rightarrow$ `TransactionResponseParticipant`
  - `0800` (Network) $\rightarrow$ `NetworkParticipant`
  - `SenderResponseParticipant` for automatic response delivery over TCP ISOSource channels.
- **Automated Echo Service (`QEcho`)**: Quarkus scheduled service executing periodic ISO 0800 echo requests over space queues (`tspace:biller`).
- **REST Diagnostics & Simulation API**: HTTP JAX-RS endpoints providing real-time server health metrics and transaction simulation.
- **Persistence Layer**: Integrated Hibernate ORM & H2 database models (`Products`, `Users`, `Inboxes`, `Outboxes`, `Groups`).

---

## Prerequisites

- **Java**: 17 or higher (tested up to Java 25)
- **Maven**: 3.8.x or higher

---

## Getting Started

### 1. Run in Development Mode (Live Coding)

Start the server in Quarkus live-reload dev mode:

```bash
mvn quarkus:dev
```

*(Or via Maven wrapper)*:
```bash
./mvnw quarkus:dev
```

### 2. Run Test Suite

Execute the unit and integration test suite:

```bash
mvn test
```

### 3. Build & Package for Production

Build a production-ready runner package:

```bash
mvn clean package
```

Run the packaged application:

```bash
java -jar target/quarkus-app/quarkus-run.jar
```

---

## Architecture & Configuration

### Ports Overview

| Protocol | Port | Description |
| :--- | :--- | :--- |
| **HTTP / REST** | `8080` | Quarkus Management & Simulation REST API |
| **ISO 8583 TCP** | `2333` | Transaction Manager ISO 8583 Server (`ServerTrxMan`) |
| **ISO 8583 TCP** | `9991` | Primary ISO 8583 Server (`srv`) |
| **ISO 8583 Biller** | `2300` | Biller Channel Adaptor Target |

### Directory Structure

```text
Protonema/
├── pom.xml                                   # Maven Quarkus build configuration
└── src/
    ├── main/
    │   ├── java/
    │   │   └── protonema/
    │   │       ├── Constants.java            # Shared Context keys
    │   │       ├── Protonema.java            # ISORequestListener for TrxMan
    │   │       ├── Protonema2.java           # ISORequestListener for Srv
    │   │       ├── QEcho.java                # Scheduled Echo Service
    │   │       ├── model/                    # JPA Entities (Products, Users, etc.)
    │   │       ├── participant/              # Transaction Participants
    │   │       ├── resource/                 # REST API Resource Controllers
    │   │       ├── selector/                 # Switch Group Selector
    │   │       └── service/                  # ISO8583ServerService Lifecycle
    │   └── resources/
    │       ├── application.properties        # Quarkus configuration
    │       ├── deploy/                       # jPOS Q2 XML Deployment files
    │       └── packager/                     # ISO 8583 Packager XML & DTD
    └── test/
        └── java/                             # Quarkus unit & integration tests
```

---

## REST API Documentation

### Check Server Status
**Endpoint**: `GET /api/iso/status`

**Response**:
```json
{
  "application": "Protonema Quarkus ISO8583 Server",
  "status": "UP",
  "trxmanPort": 2333,
  "srvPort": 9991
}
```

### Simulate ISO 8583 Transaction
**Endpoint**: `POST /api/iso/simulate`

**Request Payload**:
```json
{
  "mti": "0100",
  "stan": "000001",
  "pan": "6228480000000001",
  "cid": "4567898"
}
```

**Response**:
```json
{
  "status": "QUEUED",
  "mti": "0100",
  "stan": "000001",
  "rawPackedHex": "0100600000000000000010..."
}
```

---

## License

Standard repository configuration.
