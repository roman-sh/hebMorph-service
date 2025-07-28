# HebMorph Service

A standalone, containerized REST API for high-performance Hebrew lemmatization, powered by a resurrected version of the [HebMorph](https://github.com/synhershko/HebMorph) library.

## Overview

This project provides a simple, reliable, and always-on RESTful service for lemmatizing Hebrew text. It was created to fill a gap in the open-source ecosystem, where existing solutions for Hebrew NLP were found to be either unstable, inaccurate (especially with construct-state cases, or "smichut"), or no longer maintained.

The service is packaged in a lightweight Docker container, making it easy to deploy and scale anywhere. It exposes a single endpoint that accepts a list of sentences and returns their lemmatized forms.

## Features

-   **High-Quality Lemmatization**: Leverages the original, battle-tested linguistic rules of HebMorph.
-   **Standalone & Portable**: Packaged as a minimal Docker image with no external dependencies.
-   **High Performance**: Built on Javalin, a lightweight and fast web framework for Java.
-   **Simple REST API**: Easy to integrate into any application stack.
-   **Batch Processing**: Lemmatize multiple sentences in a single API call for efficiency.

## API Usage

The service runs on port `5001`.

### Endpoint: `POST /lemmatize`

Accepts a JSON object with a single key, `sentences`, which is an array of strings.

**Request:**

```bash
curl -X POST http://localhost:5001/lemmatize \
-H "Content-Type: application/json" \
-d '{
  "sentences": [
    "מיץ ענבים ותפוחים",
    "גבינת קוטג׳ טעימה"
  ]
}'
```

**Response:**

The service returns a JSON object with a `results` key. The value is an array of arrays, where each inner array contains the lemmas for the corresponding input sentence.

```json
{
  "results": [
    [
      "מיץ",
      "ענב",
      "תפוח"
    ],
    [
      "גבינה",
      "קוטג׳",
      "טעימה"
    ]
  ]
}
```
### Post-processing Logic

To provide a cleaner and more predictable output, the service applies the following logic to the raw results from the HebMorph library:

1.  **Tokenization**: Input sentences are split into words (tokens) by whitespace.
2.  **Lemmatization**: Each token is passed to the HebMorph engine.
3.  **Filtering & Selection**: For each token, the service processes the list of possible lemmas returned by the engine:
    *   It takes the **first valid lemma** that is longer than one character.
    *   If no valid lemma is found (e.g., the engine returns `null` or only single-character lemmas), it **falls back to the original word**.

This behavior is implemented in the `Api.java` file and can be easily adjusted to suit different use cases, such as returning all possible lemmas or implementing a different filtering strategy.

## Getting Started

### Using Docker (Recommended)

A pre-built image is available on [Docker Hub](https://hub.docker.com/repository/docker/shmigi/hebmorph-service/general).

1.  **Pull the image:**
    ```bash
    docker pull shmigi/hebmorph-service:latest
    ```

2.  **Run the container:**
    ```bash
    docker run -d -p 5001:5001 --name hebmorph-service shmigi/hebmorph-service:latest
    ```
    The service will now be available at `http://localhost:5001`.

### Building from Source

You need Java 11 (or higher) and Gradle installed.

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/roman-sh/hebMorph-service.git
    cd hebMorph-service
    ```

2.  **Build the fat JAR using the Gradle shadow plugin:**
    ```bash
    ./gradlew shadowJar
    ```
    This will create a self-contained JAR at `build/libs/hebmorph-service.jar`.

3.  **Run the service:**
    ```bash
    java -jar build/libs/hebmorph-service.jar
    ```

## Acknowledgements

This project would not be possible without the original work done by Shay Synhershko and the other contributors to the [HebMorph](https://github.com/synhershko/HebMorph) project. We have gratefully resurrected their powerful library to make it accessible as a modern microservice. 
