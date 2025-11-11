# JAVA Webhook Assignment

This project is a **Spring Boot application** built for the PES University Java Webhook Assignment.  
It performs automated REST API calls to generate a webhook and submit a final SQL query as JSON.

---

## 🧠 Overview

The application does the following:

1. Calls the `generateWebhook/JAVA` API using the student's name, registration number, and email.
2. Receives a **webhook URL** and **access token** in the response.
3. Reads the final SQL query from a local file (`finalQuery.sql`).
4. Submits the query to the generated webhook endpoint using the provided access token.

---

## ⚙️ Project Structure

