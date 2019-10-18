# health-apis-parent

This project provides starter POMs and shared libraries for Health APIs
[Spring Boot](https://spring.io/projects/spring-boot) microservices.
- api-starter - Parent POM for service APIs
- service-starter - Parent POM for service implementations
- [service-auto-config](service-auto-config/README.md) - Service configuration utilities
- [sentinel](sentinel/README.md) - Integration test harness

----

## Building
- [Java Development Kit](https://openjdk.java.net/) 12
- [Maven](http://maven.apache.org/) 3.6
- Recommended [IntelliJ](https://www.jetbrains.com/idea/)
  or [Eclipse](https://www.eclipse.org/downloads/packages/installer)
  with the following plugins
  - [Lombok](https://projectlombok.org/)
  - [Google Java Format](https://github.com/google/google-java-format)
- [git-secrets](https://github.com/awslabs/git-secrets)    

#### Maven
- Formats Java, XML, and JSON files
  (See the [Style Guide](https://google.github.io/styleguide/javaguide.html))
- Enforces unit test code coverage
- Performs [Checkstyle](http://checkstyle.sourceforge.net/) analysis using Google rules
- Performs [SpotBugs](https://spotbugs.github.io/) analysis
  with [Find Security Bugs](http://find-sec-bugs.github.io/) extensions
- Enforces Git branch naming conventions to support Jira integration

The above build steps can be skipped for use with IDE launch support by disabling the
_standard_ profile, e.g. `mvn -P'!standard' package`

#### git-secrets
git-secrets must be installed and configured to scan for AWS entries and the patterns in
[.git-secrets-patterns](.git-secrets-patterns). Exclusions are managed in 
[.gitallowed](.gitallowed).
git-secrets should be enabled with the following commands:

```
git secrets --register-aws
git secrets --add-provider -- cat .git-secrets-patterns
```

> ###### !!  Mac users
> If using [Homebrew](https://brew.sh/), use `brew install --HEAD git-secrets` as decribed
> by [this post](https://github.com/awslabs/git-secrets/issues/65#issuecomment-416382565) to
> avoid issues committing multiple files.
