# Kafka Stream Word Count Application

A Spring Boot application demonstrating real-time word counting using Kafka Streams with time-windowed aggregation and interactive queries.

## Overview

This application processes words from a Kafka topic, counts occurrences within time windows (10-second intervals), and exposes the results through a RESTful API. It showcases:

- **Real-time stream processing** using Kafka Streams API
- **Windowed aggregation** for time-based analytics
- **Interactive queries** to retrieve historical word counts
- **Spring Cloud Stream** integration for simplified Kafka configuration
- **Reactive programming** with Spring WebFlux

## Architecture

### Data Flow

```
Producer (REST API) → [word] topic → countWord processor → [wordCount] topic → processWord consumer
                                                ↓
                                        WordCounts-1 (State Store)
                                                ↓
                                        Query API (REST)
```

### Components

1. **WordController**: REST controller for sending words and querying counts
2. **WordProcessor**: Stream processing logic with two functions:
   - `countWord`: Aggregates word counts in 10-second windows
   - `processWord`: Consumes and logs word count results
3. **Word DTO**: Data transfer object containing word, count, and time window information

## Prerequisites

- Java 21 or higher
- Maven 3.6+
- Docker and Docker Compose (for running Kafka)

## Technology Stack

- **Spring Boot**: 3.5.10
- **Spring Cloud Stream**: 2025.0.1
- **Spring WebFlux**: Reactive web framework
- **Apache Kafka Streams**: Stream processing
- **Apache Kafka**: 3.x (latest)
- **Lombok**: Code generation
- **Maven**: Build tool

## Getting Started

### 1. Start Kafka Broker

The project includes a Docker Compose configuration for running Kafka in KRaft mode (without Zookeeper):

```bash
docker-compose up -d
```

This starts a single Kafka broker on `localhost:9092` with:
- 3 partitions per topic (auto-created)
- KRaft mode (controller + broker combined)

### 2. Build the Application

```bash
./mvnw clean install
```

### 3. Run the Application

```bash
./mvnw spring-boot:run
```

Or run the JAR directly:

```bash
java -jar target/kafka-stream-0.0.1-SNAPSHOT.jar
```

The application starts on the default port (8080).

## API Endpoints

### Send a Word

Submit a word to be processed and counted:

```bash
POST /words
Content-Type: text/plain

hello
```

**cURL Example:**
```bash
curl -X POST http://localhost:8080/words \
  -H "Content-Type: text/plain" \
  -d "hello"
```

### Query Word Counts

Retrieve word counts for a specific time window:

```bash
GET /words/{word}?from={ISO_DATETIME}&to={ISO_DATETIME}
```

**cURL Example:**
```bash
curl "http://localhost:8080/words/hello?from=2025-01-01T00:00:00Z&to=2025-12-31T23:59:59Z"
```

**Response:**
```json
{
  "word": "hello",
  "from": "2025-01-01T00:00:00Z",
  "to": "2025-12-31T23:59:59Z",
  "countsByWindowStart": {
    "1735689600000": 5,
    "1735689610000": 3,
    "1735689620000": 7
  }
}
```

The response includes:
- `word`: The queried word
- `from`/`to`: The time range queried
- `countsByWindowStart`: Map of window start timestamps (epoch millis) to counts

## Configuration

### Application Configuration (`application.yaml`)

Key configurations:

#### Kafka Streams Bindings
```yaml
spring.cloud.stream:
  bindings:
    countWord-in-0:
      destination: word           # Input topic
    countWord-out-0:
      destination: wordCount      # Output topic
    processWord-in-0:
      destination: wordCount      # Consumer topic
```

#### Kafka Streams Settings
```yaml
spring.cloud.stream.kafka.streams:
  binder:
    application-id: hello-word-count-sample
    configuration:
      commit.interval.ms: 100     # Frequent commits for low-latency
```

#### Kafka Broker
```yaml
spring.kafka:
  bootstrap-servers: localhost:9092
```

### Stream Processing Details

The `countWord` function performs the following operations:

1. **Map**: Convert each word to a key-value pair `(word, word)`
2. **GroupByKey**: Group by word
3. **WindowedBy**: Create 10-second tumbling windows
4. **Count**: Count occurrences per window
5. **Materialize**: Store results in "WordCounts-1" state store
6. **Map**: Transform to Word DTO with timestamp information

Time window duration: **10 seconds** (configured in `WordProcessor.java:28`)

## Development

### Project Structure

```
kafka-stream/
├── src/
│   ├── main/
│   │   ├── java/com/example/kafkastream/
│   │   │   ├── KafkaStreamApplication.java      # Main application
│   │   │   ├── configuration/
│   │   │   │   └── WordProcessor.java           # Stream processing logic
│   │   │   ├── controller/
│   │   │   │   └── WordController.java          # REST API
│   │   │   └── dto/
│   │   │       └── Word.java                    # Data model
│   │   └── resources/
│   │       └── application.yaml                 # Configuration
│   └── test/
├── docker-compose.yml                           # Kafka setup
└── pom.xml                                      # Maven dependencies
```

### Key Classes

#### WordProcessor.java
- **countWord()**: Stream processing function that counts words in time windows
- **processWord()**: Consumer that logs word count results

#### WordController.java
- **POST /words**: Produces messages to the "word" topic
- **GET /words/{word}**: Queries the state store for historical counts

#### Word.java
- DTO containing: `word`, `count`, `start` (window start), `end` (window end)

## Testing

### Manual Testing Workflow

1. Start Kafka and the application
2. Send several words:
```bash
curl -X POST http://localhost:8080/words -H "Content-Type: text/plain" -d "hello"
curl -X POST http://localhost:8080/words -H "Content-Type: text/plain" -d "world"
curl -X POST http://localhost:8080/words -H "Content-Type: text/plain" -d "hello"
```

3. Check application logs for processed word counts

4. Query word counts (adjust time range):
```bash
curl "http://localhost:8080/words/hello?from=2025-01-01T00:00:00Z&to=2026-12-31T23:59:59Z"
```

### Run Unit Tests

```bash
./mvnw test
```

## Kafka Topics

The application uses the following topics (auto-created):

- **word**: Input topic for raw words
- **wordCount**: Output topic containing Word objects with counts

## State Store

- **Name**: `WordCounts-1`
- **Type**: WindowStore
- **Purpose**: Stores word counts per time window for interactive queries
- **Cleanup**: Enabled on startup (clears old state)

## Troubleshooting

### Application won't start
- Ensure Kafka is running: `docker ps`
- Check Kafka connectivity: `localhost:9092`
- Verify Java version: `java -version` (requires Java 21+)

### Topics not created
- Topics are auto-created when messages are first sent
- Check Kafka logs: `docker logs broker`

### State store query returns empty results
- Ensure words were sent within the queried time range
- Check that 10 seconds have passed for the window to complete
- Verify application logs show "Received message" entries

### Port 9092 already in use
- Stop existing Kafka: `docker-compose down`
- Or modify `docker-compose.yml` to use a different port

## Performance Considerations

- **Commit interval**: Set to 100ms for low-latency processing
- **Window size**: 10 seconds (configurable in `WordProcessor.java`)
- **Partitions**: 3 partitions for parallel processing
- **State store cleanup**: Enabled on startup to prevent unbounded growth

## Future Enhancements

Potential improvements for this application:

- [ ] Add WebSocket endpoint for real-time word count updates
- [ ] Implement sliding windows for overlapping time periods
- [ ] Add filtering for stop words or minimum word length
- [ ] Include metrics and monitoring (Micrometer/Prometheus)
- [ ] Add Docker image build and Kubernetes deployment
- [ ] Implement exactly-once semantics
- [ ] Add REST API documentation (OpenAPI/Swagger)
- [ ] Support for custom window sizes via configuration

## License

This project is a demonstration application for educational purposes.

## References

- [Spring Cloud Stream Documentation](https://spring.io/projects/spring-cloud-stream)
- [Kafka Streams Documentation](https://kafka.apache.org/documentation/streams/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
