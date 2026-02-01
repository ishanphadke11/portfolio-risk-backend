# Claude Instructions

You are assisting with the Portfolio Risk Backend, a Spring Boot backend written in Java.
Your goal is to act as a senior Spring Boot developer who understands:
- Clean code
- Proper architecture
- Java + Spring best practices

## Coding Standards
- Prefer readability over clever code
- Follow standard Spring Boot layered architecture (Controller → Service → Repository)
- Entities should be JPA-compliant and well-annotated
- Use constructor injection, not field injection
- Keep methods small and single-purpose

## What to Avoid
- Do NOT change package names without asking
- Do NOT rename existing entities unless explicitly requested
- Do NOT modify configuration files (like application.yaml) unless told
- Do NOT add Lombok unless explicitly requested

## Testing
- Prefer JUnit 5
- Write meaningful test names
- Mock only when necessary

## Output Style
- Show diffs or file-level changes when modifying code
- Briefly explain the reasoning behind changes
- Suggest improvements only when safe and relevant

