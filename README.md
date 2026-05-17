
# 📝 Notes API - Core Backend Specification

This repository contains the backend REST API for a multi-user notes management service (conceptually similar to Google Keep or Apple Notes). The system handles secure user authentication, granular data isolation, and secure cross-user note sharing.

This project was engineered to satisfy the requirements of an intensive backend engineering internship assignment, with a strong emphasis on edge-case handling, security, and scalability.

**🌐 Live Environment Base URL:** `https://notes-app-5-arhb.onrender.com`

---

## 🏗️ System Architecture & Tech Stack

The application is built using a modern, scalable Java ecosystem.

* **Language:** Java 17+
* **Framework:** Spring Boot 3.2.x (Spring Web, Spring Security, Spring Data JPA)
* **Database:** PostgreSQL (Cloud-hosted via Render)
* **Authentication:** Stateless JSON Web Tokens (JWT) via `io.jsonwebtoken`
* **Password Security:** BCrypt Hashing Algorithm
* **Rate Limiting:** Bucket4j (Token-bucket algorithm)
* **Containerization:** Docker (Stretch goal achieved)
* **Deployment:** Render (PaaS)

---

## 🔐 Security & Design Decisions

### 1. Stateless Authentication (JWT)

Instead of relying on server-side session cookies (which limit scalability), this API uses **Stateless JWT Authentication**.

* Upon successful login, the server issues an encrypted JWT signed with a secret key.
* The client must attach this token as a `Bearer` token in the `Authorization` header for all protected requests.
* The server verifies the token's cryptographic signature on every request, ensuring the user is authenticated without hitting the database just to check session state.

### 2. Strict Data Isolation

Every API endpoint that interacts with a Note entity enforces strict ownership checks. A user can only fetch, modify, or delete a note if their User ID matches the Note's `owner_id`, or if the note has been explicitly shared with them via the sharing table. Unauthorized attempts yield a `403 Forbidden` or `404 Not Found` to prevent data leakage.

### 3. Edge Case Mitigation (Custom Features)

* **Brute-Force Protection:** The `/login` endpoint is protected by a Bucket4j rate limiter, restricting users to 5 attempts per minute. Excess attempts yield a `429 Too Many Requests`.
* **Duplicate Identity Protection:** The registration endpoint gracefully catches `DataIntegrityViolationException` and returns a `400 Bad Request` if an email is already in use, preventing raw SQL errors from leaking to the client.
* **Archive/Trash Feature (Soft Delete):** Instead of immediately destroying data on a `DELETE` request, the application supports a trash/archive state to prevent accidental data loss, fulfilling the custom product feature requirement.

---

## 🗄️ Database Schema (Conceptual)

The PostgreSQL database relies on three core entities:

1. **`users` Table:**
* `id` (UUID, Primary Key)
* `email` (String, Unique, Not Null)
* `password_hash` (String, BCrypt, Not Null)


2. **`notes` Table:**
* `id` (UUID, Primary Key)
* `title` (String, Not Null)
* `content` (Text)
* `owner_id` (UUID, Foreign Key -> users.id)
* `created_at` (Timestamp)
* `updated_at` (Timestamp)
* `is_archived` / `is_trashed` (Boolean for custom feature)


3. **`shared_notes` Table (Many-to-Many mapping):**
* `note_id` (UUID, Foreign Key -> notes.id)
* `shared_with_user_id` (UUID, Foreign Key -> users.id)



---

## 📡 API Endpoint Specifications

### 1. Public Endpoints (No Auth Required)

#### 1.1 Developer Info

* **Endpoint:** `GET /about`
* **Purpose:** Exposes developer details and metadata about custom features.
* **Response (200 OK):**
```json
{
  "name": "Harshith Banothu",
  "email": "your-email@example.com",
  "my features": {
    "Archive and Trash Management": "Soft-delete system preventing accidental data loss.",
    "Bucket4j Rate Limiting": "Protects /login from brute force attacks."
  }
}

```



#### 1.2 OpenAPI Specification

* **Endpoint:** `GET /openapi.json`
* **Purpose:** Returns the full Swagger/OpenAPI v3 schema for the application.

---

### 2. User Identity Management (No Auth Required)

#### 2.1 Register New User

* **Endpoint:** `POST /register`
* **Payload:**
```json
{
  "email": "user@example.com",
  "password": "securePassword123"
}

```


* **Success Response (201 CREATED):** Status code `201` with a success message.
* **Failure Responses:** `400 Bad Request` (Invalid email format, weak password, or email already exists).

#### 2.2 Authenticate User (Login)

* **Endpoint:** `POST /login`
* **Payload:** Same as registration.
* **Success Response (200 OK):**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}

```


* **Failure Responses:** * `401 Unauthorized` (Wrong credentials)
* `429 Too Many Requests` (Rate limit exceeded)



---

### 3. Core Note Operations (JWT Required)

*All requests below require the HTTP Header: `Authorization: Bearer <your_jwt_token>*`

#### 3.1 Create a Note

* **Endpoint:** `POST /notes`
* **Payload:**
```json
{
  "title": "Project Architecture",
  "content": "Designing the database schema."
}

```


* **Success Response (201 CREATED):**
```json
{
  "id": "uuid-string",
  "title": "Project Architecture",
  "content": "Designing the database schema.",
  "created_at": "2024-05-17T12:00:00Z",
  "updated_at": "2024-05-17T12:00:00Z"
}

```



#### 3.2 Read All Notes

* **Endpoint:** `GET /notes`
* **Success Response (200 OK):** Returns a JSON Array `[...]` containing all active notes owned by the authenticated user.

#### 3.3 Read a Specific Note

* **Endpoint:** `GET /notes/{id}`
* **Success Response (200 OK):** Returns the specific note JSON object.
* **Security Edge Case:** If the requested ID belongs to a different user (and hasn't been shared), returns `404 Not Found` or `403 Forbidden` to prevent object-level enumeration.

#### 3.4 Update a Note

* **Endpoint:** `PUT /notes/{id}`
* **Payload:**
```json
{
  "title": "Updated Title",
  "content": "Updated content."
}

```


* **Success Response (200 OK):** Returns the updated note JSON with a refreshed `updated_at` timestamp.

#### 3.5 Delete a Note

* **Endpoint:** `DELETE /notes/{id}`
* **Success Response (204 NO CONTENT):** Returns an empty body. Note is moved to trash/archived state (Custom feature).

#### 3.6 Share a Note

* **Endpoint:** `POST /notes/{id}/share`
* **Payload:**
```json
{
  "share_with_email": "colleague@example.com"
}

```


* **Success Response (200 OK):** Returns a success message.
* **Logic:** The system looks up `colleague@example.com`. If they exist, it adds an entry to the `shared_notes` table. The colleague can now query this note using `GET /notes/{id}`.

---

## 🐳 Docker Deployment Details

This application is fully containerized to ensure perfect environment parity between local development and production.

* The `Dockerfile` compiles the Spring Boot application using Maven and packages it into an executable `.jar` running on a lightweight Alpine Linux Java runtime.
* Render utilizes this Dockerfile to build and deploy the container natively.

---

## 🚀 How to Run Locally

1. **Clone the repo:** `git clone https://github.com/har8shith/notes-app.git`
2. **Set Environment Variables:**
* `SPRING_DATASOURCE_URL`: PostgreSQL JDBC URL
* `SPRING_DATASOURCE_USERNAME`: Database Username
* `SPRING_DATASOURCE_PASSWORD`: Database Password
* `JWT_SECRET`: A 256-bit secure random string for token signing


3. **Run via Maven:** `./mvnw spring-boot:run`
4. **Run via Docker:** `docker build -t notes-api . && docker run -p 8080:8080 notes-api`
