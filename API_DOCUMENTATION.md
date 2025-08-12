# Xreal FAQ Management API Documentation

## Overview
This is a FAQ management system built with Spring Boot, MySQL, and Elasticsearch for Xreal intelligent customer service.

## Requirements
- Java 17+
- MySQL 8.0+
- Elasticsearch 8.0+
- Maven 3.6+

## Setup

### 1. Database Configuration
- MySQL will automatically create the database if it doesn't exist
- Update `src/main/resources/application.yml` with your MySQL credentials

### 2. Elasticsearch Configuration
- Ensure Elasticsearch is running on port 9200
- The index will be created automatically

### 3. OpenAI Configuration
- Set the environment variable `OPENAI_API_KEY` with your OpenAI API key
- Or update it in `application.yml`

### 4. Running the Application
```bash
mvn clean install
mvn spring-boot:run
```

## API Endpoints

### Swagger UI
Access the API documentation at: `http://localhost:8080/swagger-ui.html`

### FAQ Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| DELETE | `/faqs/all` | Delete all FAQs |
| POST | `/faqs` | Create a new FAQ |
| PUT | `/faqs/{id}` | Update an existing FAQ |
| DELETE | `/faqs/{id}` | Delete a specific FAQ |
| GET | `/faqs/{id}` | Get a specific FAQ |
| GET | `/faqs` | Get all FAQs (paginated) |
| GET | `/faqs/search` | Search FAQs by tags |

### Tag Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/tags` | Create a new tag |
| PUT | `/tags/{name}` | Update an existing tag |
| DELETE | `/tags/{name}` | Delete a tag |
| GET | `/tags` | Get all tags |
| GET | `/tags/active` | Get active tags only |

## Request/Response Examples

### Create FAQ
```json
POST /faqs
{
  "question": "How to connect Xreal Air to iPhone?",
  "answer": "You need an adapter...",
  "instruction": "Check compatibility first",
  "url": "https://xreal.com/support",
  "active": true,
  "comment": "Common question",
  "tags": ["Xreal Air", "Connectivity", "Setup"]
}
```

### Create Tag
```json
POST /tags
{
  "name": "New Product",
  "description": "Tag for new product FAQs",
  "active": true
}
```

### Search by Tags
```
GET /faqs/search?tags=Xreal Air&tags=Setup&active=true&page=0&size=10
```

## Query Parameters

All list endpoints support:
- `page`: Page number (default: 0)
- `size`: Page size (default: 10)
- `active`: Filter by active status
- `sort`: Sort field and direction (e.g., `timestamp,desc`)

## Data Consistency

The system implements dual-write to both MySQL and Elasticsearch:
1. All writes go to MySQL first (primary storage)
2. Then synchronized to Elasticsearch (for search and vector operations)
3. If Elasticsearch sync fails, the MySQL transaction is rolled back

## Default Tags

The following tags are created automatically on startup:
- Xreal Air, Xreal Air 2, Xreal Air 2 Pro, Xreal Air 2 Ultra
- Xreal Light, Xreal Beam, Xreal Beam Pro
- Hardware, Software, Troubleshooting
- Setup, Compatibility, Display, Audio, Connectivity

## Error Handling

All errors return a standardized response:
```json
{
  "status": 404,
  "message": "FAQ not found with id: 123",
  "timestamp": "2024-01-01T12:00:00"
}
```

## Testing

Run tests with:
```bash
mvn test
```