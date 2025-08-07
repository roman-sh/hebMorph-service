# HebMorph Service

A standalone, containerized REST API for high-performance Hebrew lemmatization, powered by a resurrected version of the [HebMorph](https://github.com/synhershko/HebMorph) library.

## Overview

This project provides a simple, reliable, and always-on RESTful service for lemmatizing Hebrew text. It was created to fill a gap in the open-source ecosystem, where existing solutions for Hebrew NLP were found to be either unstable, inaccurate (especially with construct-state cases, or "smichut"), or no longer maintained.

The service is packaged in a lightweight Docker container, making it easy to deploy and scale anywhere. It exposes two endpoints:
- `POST /lemmatize` – canonical one-lemma-per-piece for production.
- `POST /lemmatize-raw` – raw HebMorph candidates per token for debugging.

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

#### Mixed examples

Request (smichut, plural/singular, adjective gender, punctuation/parentheses, numerals/Latin, units with quotes/gershaim):

```bash
curl -X POST http://localhost:5001/lemmatize \
  -H "Content-Type: application/json" \
  -d '{
    "sentences": [
      "אבוקדו האס, אורגני",
      "חוות הבופאלו (לא שקיל)",
      "מיץ תפוחים",
      "מיץ תפוח",
      "חמאת בוטנים טבעית",
      "חמאת בוטן טבעית",
      "גרעיני דלעת קלויים אורגניים",
      "גרעין דלעת קלוי אורגני",
      "עוגיות שוקולד אורגניות",
      "עוגיית שוקולד אורגנית",
      "1 ק\"ג תמרים",
      "1 ק״ג תמרים",
      "200 ג\"רם",
      "500 גרם Nutrazen אורגני"
    ]
  }'
```

Response (real output):

```json
{
  "results": [
    ["אבוקדו","האס","אורגני"],
    ["חווה","הבופאלו","לא","שקיל"],
    ["מיץ","תפוח"],
    ["מיץ","תפוח"],
    ["חמאה","בוטן","טבעי"],
    ["חמאה","בוטן","טבעי"],
    ["גרעיני","דלעת","קלוי","אורגני"],
    ["גרעין","דלעת","קלוי","אורגני"],
    ["עוגייה","שוקולד","אורגני"],
    ["עוגייה","שוקולד","אורגני"],
    ["1","קג","תמר"],
    ["1","קג","תמר"],
    ["200","גרם"],
    ["500","גרם","Nutrazen","אורגני"]
  ]
}
```

### Endpoint: `POST /lemmatize-raw`

Accepts a JSON object with a single key, `sentence`, which is one string. Returns the raw HebMorph candidates per token (no filtering/sorting by the service), useful for debugging and analysis.

Request:

```bash
curl -X POST http://localhost:5001/lemmatize-raw \
  -H "Content-Type: application/json" \
  -d '{
    "sentence": "אבוקדו האס, אורגני"
  }'
```

Response (real output excerpt):

```json
{
  "results": [
    [
      { "lemma": "אבוקדו", "score": 1.0, "mask": "D_NOUN", "prefixLength": 0 },
      { "lemma": "אבוקדו", "score": 1.0, "mask": "D_NOUN", "prefixLength": 0 }
    ],
    [],
    [
      { "lemma": "אורגן",  "score": 1.0, "mask": "D_NOUN", "prefixLength": 0 },
      { "lemma": "אורגן",  "score": 1.0, "mask": "D_NOUN", "prefixLength": 0 },
      { "lemma": "אורגני", "score": 1.0, "mask": "D_ADJ",  "prefixLength": 0 },
      { "lemma": "אורגני", "score": 1.0, "mask": "D_ADJ",  "prefixLength": 0 }
    ]
  ]
}
```

## Canonical Post-processing Logic (used by `/lemmatize`)

- Split input sentences by whitespace.
- Strip leading/trailing non-letter/digit characters from each piece (removes punctuation and parentheses at edges).
- For each cleaned piece, select exactly one canonical lemma from HebMorph candidates using the following policy:
  1. Sort by score descending.
  2. POS-based tie-breaks:
     - If the piece ends with "י" or "ית": prefer ADJ > NOUN > VERB.
     - If the piece ends with "ה": prefer NOUN > ADJ > VERB.
     - Otherwise: ADJ > NOUN > VERB.
  3. Next, prefer lemma identical to the cleaned surface piece.
  4. Finally, prefer the shortest lemma.
- Drop lemmas with length == 1.
- Numerals and Latin tokens pass through unchanged.
- Do not emit punctuation tokens.

## Getting Started

### Using Docker (Recommended)

Publish your image under your own namespace, then:

```bash
docker pull shmigi/hebmorph-service:latest
docker run -d -p 5001:5001 --name hebmorph-service shmigi/hebmorph-service:latest
```
The service will be available at `http://localhost:5001`.

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
