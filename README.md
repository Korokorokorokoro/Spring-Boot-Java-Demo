-------------------------------
Spring-Boot-Java-Demo README.md (22-01-2026)
-------------------------------

Core technologies utilised in this app:

Core Java:
Java 21 (JDK 21)
Core concepts: Generics, Streams, Optional, Lambda expressions, Annotations

Spring Framework:
Spring Boot 4.0.1
Spring Boot Starter Parent
Auto-configuration
Embedded Tomcat server
@SpringBootApplication main class

Spring MVC (Web Layer):
@RestController - RESTful API endpoints
@RequestMapping, @GetMapping, @PostMapping, @PutMapping, @DeleteMapping
@PathVariable, @RequestParam, @RequestBody
ResponseEntity<T> for HTTP responses

Spring Data JPA (Persistence Layer):
Repository pattern with JpaRepository
Custom query methods (findByDetailsContainingIgnoreCase)
Entity management with @Entity
Transaction management

Spring Security:
Basic authentication
Security configuration with SecurityFilterChain
In-memory user details

Spring Retry:
@EnableRetry
@Retryable for automatic retry logic
@Recover for fallback methods
@Backoff for exponential backoff strategies

Spring AOP (Aspect-Oriented Programming):
Used by Spring Retry (spring-aspects)
Cross-cutting concerns (logging, retry logic)

Jakarta EE (formerly Java EE):
Jakarta Persistence API (JPA) - @Entity, @Id, @GeneratedValue, @Table, @Enumerated
Jakarta Servlet - HttpServletRequest
Hibernate - JPA implementation (ORM)

Database:
H2 Database (in-memory, embedded)
JDBC connection pooling (HikariCP)

Build & Dependency Management:
Maven (pom.xml)
Maven Wrapper (mvnw, mvnw.cmd)

Logging:
SLF4J (Simple Logging Facade for Java)
Logback (default with Spring Boot)
Structured logging with parameterized messages

Email Integration:
Spring Boot Mail Starter
JavaMail API for SMTP

JSON Processing:
Jackson (jackson-databind)
Automatic JSON serialization/deserialization

Testing Frameworks:
JUnit 5 (Jupiter)
Mockito - Mocking framework
Spring Test - @SpringBootTest, @WebMvcTest
MockMvc - Testing MVC controllers
Spring Security Test - Security testing utilities

Design Patterns & Architectural Concepts:
MVC Pattern - Model (Order), View (JSON responses), Controller (OrderController)
Repository Pattern - Data access abstraction
DTO Pattern - ErrorResponse for data transfer
Dependency Injection - Constructor injection throughout
Exception Handling Pattern - @ControllerAdvice for global error handling
Retry Pattern - Automatic retries with exponential backoff
Builder Pattern - Implicit in entity construction

REST Principles:
Resource-based URLs (/order, /order/{id})
HTTP verbs (GET, POST, PUT, DELETE)
HTTP status codes (200, 201, 404, 400, 500)
JSON content negotiation


-------------------------------
Technical design decision explanations:
-------------------------------

-------------------------------
@ControllerAdvice Pattern:

Decision: Centralized exception handling instead of try-catch blocks in every controller method.

Why:
DRY Principle: Avoid repeating error handling logic across multiple controllers
Consistency: Ensures all errors follow the same response format automatically
Separation of Concerns: Controllers focus on business logic, not error formatting
Maintainability: Changes to error handling only need to happen in one place
Testability: Exception handling logic can be tested independently
Alternative Rejected: @ExceptionHandler in each controller would lead to code duplication and inconsistent error responses.
-------------------------------


-------------------------------
Custom Exception Classes (OrderNotFoundException, etc.):

Decision: Domain-specific exceptions instead of throwing generic exceptions.

Why:
Semantic Clarity: OrderNotFoundException is more meaningful than RuntimeException("Order not found")
Type Safety: Compiler helps catch which exceptions need handling
Selective Handling: Can handle different business errors with different HTTP status codes
Self-Documenting: Exception name describes the problem
Easier Filtering: Logs and monitoring tools can categorize errors by type
Why RuntimeException (unchecked):
No forced try-catch clutter in calling code
Spring transaction management still rolls back on unchecked exceptions
Aligns with modern Java practice (checked exceptions often considered a mistake)
-------------------------------


-------------------------------
ErrorResponse DTO - Consistent Structure:

Decision: Standardized error format with timestamp, status, error, message, path, and optional details.

Why:
API Contract: Clients know exactly what structure to expect for all errors
Machine-Readable: Consistent JSON structure enables automated error parsing
Debugging Aid: Timestamp helps correlate with server logs; path shows exact endpoint
Frontend-Friendly: UI can display meaningful messages without parsing different formats
OpenAPI/Swagger: Can document a single error response schema

Structure Choice:
{
  "timestamp": "2026-01-22T...",   // Correlation with logs
  "status": 404,                   // HTTP code (redundant with header, but convenient)
  "error": "Not Found",            // Standard HTTP status text
  "message": "Order not found...", // Human-readable, actionable message
  "path": "/order/999",            // Which endpoint caused the error
  "details": [...]                 // Optional: validation errors, multiple issues
}
-------------------------------


-------------------------------
HTTP Status Code Mapping Strategy:

Decision: Different exceptions map to semantically correct HTTP status codes.

OrderNotFoundException        → 404 Not Found       // Resource doesn't exist
InvalidOrderStateException    → 400 Bad Request     // Client sent invalid data
InvalidOrderDataException     → 400 Bad Request     // Validation failure
NotificationException         → 500 Internal Error  // Server-side issue
DataIntegrityViolation        → 409 Conflict        // Database constraint

REST Best Practices: Status codes communicate what went wrong at HTTP level
Client Behavior: Browsers/tools handle different codes differently (retries, caching)
Monitoring: Production monitoring can alert on 5xx errors vs 4xx errors
Semantic Correctness: 404 means "not found," 400 means "your request is bad," 500 means "our fault"
Business Rule: 4xx = Client's fault (don't retry), 5xx = Server's fault (may be transient, retry)
-------------------------------


-------------------------------
Logging Strategy:

Decision: Log at ERROR level with context, include exception stack traces for server errors.

logger.error("Order not found: {}", ex.getMessage());  // Structured logging
logger.error("Unexpected error: {}", ex.getMessage(), ex);  // Include stack trace

Log Level Rationale:
OrderNotFoundException: ERROR (business-critical, may indicate data integrity issue)
ValidationException: ERROR (indicates client sending bad data repeatedly)
GenericException: ERROR + full stack trace (production bugs need debugging)

Why Include Context:
Request path in ErrorResponse (not logged to avoid duplication)
Exception message logged (searchable in log aggregation)
Stack trace only for unexpected errors (finding root cause)
Security Consideration: Never log sensitive data (passwords, tokens) or expose internal stack traces to clients.
-------------------------------


-------------------------------
Generic Exception Handler (Catch-All):

Decision: Final @ExceptionHandler(Exception.class) catches everything not explicitly handled.

Why:
Safety Net: Prevents unhandled exceptions from returning default Tomcat/Spring error pages
Production Safety: Ensures clients never see stack traces or internal details
Monitoring: All errors still get logged and counted
Graceful Degradation: Return 500 with helpful message instead of crashing

Without This: Unhandled exceptions might expose:
<!-- BAD: Default error page -->
<h1>Whitelabel Error Page</h1>
<pre>java.lang.NullPointerException at line 42...</pre>
-------------------------------


-------------------------------
Validation Error Handling with Details Array:

Decision: Return all validation errors at once in a details array.

{
  "status": 400,
  "message": "Invalid input data",
  "details": [
    "email: must be valid email format",
    "age: must be greater than 0"
  ]
}

Benefits:
Better UX: User sees all problems at once, not one-by-one
Reduced Round-Trips: Client doesn't need multiple submissions
Form Validation: Frontend can highlight all invalid fields simultaneously
API Efficiency: One request → complete feedback
-------------------------------


-------------------------------
Spring Retry Configuration:

Decision: @Retryable with exponential backoff (2s, 3s, 4.5s) and max 3 attempts.

@Retryable(
    retryFor = {MailException.class},        // Only transient failures
    maxAttempts = 3,                         // Don't retry forever
    backoff = @Backoff(delay = 2000, multiplier = 1.5)  // Exponential backoff
)

Specific Choices:
2-second initial delay: Long enough for transient issues to resolve, short enough not to delay response
1.5x multiplier: Exponential backoff prevents overwhelming failing service
Max 3 attempts: Balance between reliability and response time (worst case: ~10 seconds)
Only MailException: Don't retry authentication failures or validation errors

Why Exponential Backoff:
Linear (2s, 2s, 2s): May hit service while still recovering
Exponential (2s, 3s, 4.5s): Gives service more time to recover
Prevents "thundering herd" if many requests fail simultaneously
-------------------------------


-------------------------------
@Recover Fallback Methods:

Decision: After all retries fail, call a recovery method instead of silently failing.

@Recover
public void recoverEmailNotification(MailException e, String subject, String body) {
    logger.error("Failed after all retries");
    throw new NotificationException("Email failed after 3 attempts", e);
}

Benefits:
Explicit Failure: Application knows notification failed (can queue for later, alert ops)
Consistent Error Handling: Still goes through GlobalExceptionHandler
Auditability: Failure is logged with context
Business Decision: Controller can decide how to handle (fail request? continue?)
Alternative Rejected: Silent failure would result in lost notifications without anyone knowing.
-------------------------------


-------------------------------
No Try-Catch in Controller Methods:

Decision: Controllers throw exceptions directly instead of wrapping in try-catch.

Before (BAD):
try {
    Order order = repo.findById(id).orElseThrow();
    return ResponseEntity.ok(order);
} catch (Exception e) {
    return ResponseEntity.status(404).body(new ErrorResponse(...));
}

After (GOOD):
return repo.findById(id)
    .map(order -> ResponseEntity.ok(order))
    .orElseThrow(() -> new OrderNotFoundException(id));
    
Why:
Cleaner Code: 3 lines instead of 6
Consistent Errors: GlobalExceptionHandler ensures uniform format
Easier Testing: Test exception throwing, not ResponseEntity building
Reduced Duplication: Error formatting logic not repeated
-------------------------------


-------------------------------
Including Request URI in Error Response:

Decision: Use HttpServletRequest.getRequestURI() to include the failing endpoint.

{
  "path": "/order/999",  // Shows which endpoint failed
  "message": "Order not found with ID: 999"
}

Benefits:
Debugging: Developers know exactly which API call failed
Log Correlation: Can search logs by path
Client-Side Routing: SPAs can show context-specific error pages
API Gateway: Helps trace failures across microservices
-------------------------------


-------------------------------
Logging Before Throwing in Services:

Decision: Log at service layer before throwing exception.

// In OrderController
logger.error("Order not found with ID: {}", id);
throw new OrderNotFoundException(id);

// GlobalExceptionHandler also logs
logger.error("Order not found: {}", ex.getMessage());

Why Duplicate Logging?
Service log: Has business context (controller name, operation being performed)
Handler log: Centralized, always happens even if exception source changes
Different audiences: Developers check controller logs; ops check error handler logs
Alternative: Could log only in handler, but lose contextual information.
-------------------------------


-------------------------------
Summary of Key Principles:

Fail Fast, Fail Clearly: Throw specific exceptions immediately
Centralize Cross-Cutting Concerns: Error handling, logging in one place
Design for Operations: Include monitoring, debugging info in logs/responses
Security First: Never expose stack traces or sensitive data to clients
User Experience: Consistent, helpful error messages with all validation failures
Resilience: Retry transient failures with exponential backoff
Maintainability: Code is self-documenting with domain-specific exceptions
These decisions create a production-ready error handling system that's secure, maintainable, and provides excellent developer/user experience!
-------------------------------


-------------------------------
Retry best practises implementation:

Log each attempt - Helps with debugging
Use exponential backoff - Prevents overwhelming failing services
Set max attempts - Don't retry forever
Be selective - Only retry transient failures
Add timeout - Don't wait forever per attempt
Consider circuit breakers - Stop retrying if service is consistently down
Make it configurable - Use application.properties for retry params
Monitor metrics - Track retry rates and failures
-------------------------------
