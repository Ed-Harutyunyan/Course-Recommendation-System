# 📘 AUA Academic Helper – Backend

This is the backend for **AUA Academic Helper**, a course recommendation and academic planning system for undergraduate students at the **American University of Armenia (AUA)**. The platform assists students in planning their academic path and provides administrative tools for staff to monitor degree progress.

---

## 🚀 Features

- 🔒 **Secure Authentication & Authorization**
  - JWT-based token management
  - Role-based access control (`ROLE_STUDENT`, `ROLE_ADMIN`)
  - OTP email verification
- 📚 **Course Recommendation**
  - Integrates with a Python microservice for intelligent course suggestions using OpenAI embeddings and Qdrant
- 🎓 **Degree Audit System**
  - Verifies student progress across all degree components (Foundation, Core, GenEd, Track, Electives, Capstone)
- 🗓️ **Schedule Generator**
  - Automatically creates a personalized course schedule each semester
- 🧠 **GenEd Clustering**
  - Validates clusters based on division, theme, and level
- 📨 **Email Communication**
  - Integrates with MailHog for development testing
- ⚡ **Performance**
  - Uses Redis for caching frequently accessed data

---

## 🛠 Tech Stack

| Layer             | Tech                     |
|------------------|--------------------------|
| Language          | Java 21                  |
| Framework         | Spring Boot 3.4.2        |
| Authentication    | Spring Security + JWT    |
| Database          | MySQL (Flyway migrations)|
| Caching           | Redis                    |
| Email             | MailHog (dev only)       |
| Deployment        | Docker / AWS             |
| Recommendation    | Python Microservice (Flask + OpenAI + Qdrant) |

---

## 📂 Project Structure

```
course-recommendation/
├── config/              # JWT, Security, CORS config
├── controller/          # REST API Controllers
├── entity/              # JPA Entities (User, Course, Enrollment, etc.)
├── repository/          # Spring Data JPA Repositories
├── service/             # Business Logic (Auth, Audit, Course, Python)
├── dto/                 # Data Transfer Objects
├── util/                # Utility classes
└── Application.java     # Main Spring Boot application
```
---

## 🔐 Authentication & Security

- JWT access and refresh tokens
- OTP email verification using Spring Mail
- Stateless session management
- Public endpoints: `/api/auth/**`, `/api/python/**`, `/api/course/**`, etc.
- Admin-only: `/api/admin/**`

---

## 📡 Python Microservice Integration

- Recommendation endpoints:
  - `POST /api/python/send/message`: Send user keyword query
  - `POST /api/python/send/passed`: Send passed courses for personalized results
  - `POST /api/python/send/courses`: Update new semester data
- Integrated via `RestTemplate` and `PythonService`

---

## 📘 API Modules

### 🧾 AuthController
- `/api/auth/login`
- `/api/auth/logout`
- `/api/auth/password-setup/*`
- `/api/auth/email/verify`

### 👤 UserController
- `/api/user/profile-picture/**`

### 🎓 DegreeAuditController
- `/api/degree-audit/{studentId}`

### 📅 ScheduleController
- `/api/schedule/generate`

### 📖 CourseController
- `/api/course/**` (CRUD, filter, offering logic)

### ✍️ CourseReviewController
- `/api/course-reviews/**` (students can leave reviews)

---

## ⚙️ Build and Run

### Prerequisites

- Java 21
- Maven 3.8+
- Docker

### Run Locally

```bash
# Clone the repository
git clone https://github.com/YourUsername/course-recommendation-system.git
cd course-recommendation-system

# Set environment variables or update application.yml

# Run with Maven
./mvnw spring-boot:run
````

### Docker

The project supports Docker Compose for full-stack local deployment (backend, MySQL, Redis, MailHog, Python):

```bash
docker-compose up --build
```

---

## 📄 Flyway Migrations

* Flyway is used for schema versioning
* All migrations are stored under `/resources/db/migration`

---

## 📬 Contact

For questions or contributions, contact:

* Tariel Hakobyan
* Edgar Harutyunyan
* Norayr Sukiasyan

---

## 📜 License

This project is for educational use at the **American University of Armenia**(AUA) not officially endorsed by AUA
