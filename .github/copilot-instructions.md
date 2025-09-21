# Copilot Instructions for rld-build-tools

## Project Overview
- **Purpose:** Provides Java support classes and configuration files for build-time use, especially for testing and build tooling. Used as a shared library for the RealLifeDeveloper blog and other projects.
- **Main Language:** Java (Maven project)
- **Key Directories:**
  - `src/main/java/` — Core Java source code
  - `src/test/java/` — Test code
  - `src/main/resources/` — Build-time resources
  - `target/` — Build output, reports, and generated files

## Developer Workflows
- **Build with all quality checks:**
  ```sh
  mvn -DcheckAll clean install
  ```
- **Generate Maven site (docs, Javadoc, coverage):**
  ```sh
  mvn -P coverage clean integration-test site
  # Output: target/site/index.html
  ```
- **Run tests:**
  ```sh
  mvn test
  ```
- **Check for issues:**
  - Checkstyle, PMD, SpotBugs, and coverage are integrated via Maven profiles and config files in `target/` and `src/`.

## Project Conventions & Patterns
- **No runtime dependencies:** All code is for build-time only (e.g., test helpers, build tools).
- **Configuration files:**
  - Checkstyle: `target/checkstyle-*.xml`
  - PMD: `target/pmd.xml`
  - SpotBugs: `target/spotbugs-exclude.xml`
- **Testing:**
  - Tests are in `src/test/java/` and use standard JUnit patterns.
  - Integration tests may be present (see `MoveMessagesIT` etc.).
- **Documentation:**
  - Main docs in `README.md` and Maven site.
  - Contribution guidelines in `CONTRIBUTING.md`.

## Integration & External Dependencies
- **Maven Central:** Published as `com.reallifedeveloper:rld-build-tools`.
- **CI/CD:** GitHub Actions workflows in `.github/workflows/` (see badges in `README.md`).
- **OpenSSF/Best Practices:** Project follows OpenSSF Scorecard and CII Best Practices (see badges).

## Examples
- **Add as a Maven dependency:**
  ```xml
  <dependency>
      <groupId>com.reallifedeveloper</groupId>
      <artifactId>rld-build-tools</artifactId>
      <version>${rld-build-tools.version}</version>
  </dependency>
  ```
- **Run all checks and build:**
  ```sh
  mvn -DcheckAll clean install
  ```

## Key Files
- `README.md` — Project overview, build/test instructions
- `pom.xml` — Maven configuration, dependencies, plugins
- `CONTRIBUTING.md` — Contribution process
- `.github/workflows/` — CI/CD definitions

## AI Agent Guidance
- Prefer build-time/test utilities, not runtime code
- Follow Maven/Java idioms unless project files specify otherwise
- Reference `README.md` and Maven site for up-to-date usage patterns
- Use provided config files for static analysis and quality checks
