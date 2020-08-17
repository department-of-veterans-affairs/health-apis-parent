# service-auto-config

This Spring module provides automatic configuration for common tasks.

### JacksonConfig
This configures Jackson ObjectMapper if one is not provided by your application.
This mapper is configured as follows
- Support for property level access
- JDK 8 data type support, e.g. Optional
- Java time support, e.g. Instant
- Fails on unknown properties
- Lombok `@Value` `@Builder` with out needing to specify Jackson annotations
- Automatic whitespace trimming


### SecureRestTemplateConfig
This configures RestTemplates to support SSL based on application-level configuration
properties. Additionally, this adds logging support to requests. 

Should an HTTP request fail, encrypted error messages will be printed.
You can enable this by setting `ssl.logging-encryption-key` to a secret value. 
If not specified, no detailed information will be printed in the logs.

Properties
- `ssl.enable-client` (boolean) Whether SSL support should be enabled for clients.
- `ssl.key-store` (resource) Location of the JKS key store to use for SSL connections
- `ssl.key-store-password` (string) The key store password used with `ssl.key-store`
- `ssl.client-key-password` (string) The client key password used with `ssl.key-store`
- `ssl.use-trust-store` (boolean) Whether a trust store should be used to validate server connections, i.e. mutual TLS
- `ssl.trust-store` (resource) Location of the JKS key stores used to verify servers
- `ssl.trust-store-password` (string) The password for `ssl.trust-store`
- `ssl.verify` (boolean) Whether hostnames should be verified
- `ssl.logging-encryption-key` (string) Key to use when encrypting fail messages for requests made using the SecureRestTemplate.
  - When unset, details will be suppressed and a message indicating you should set this property will be printed instead.
  - To decrypt the message, use the [decrypt script](./decrypt) as follows: `./decrypt <key> <encoded-message>`


### AutoLoggableConfiguration
This enables automatic entry/exit logging of Spring components. 
Methods in `@RestController` classes annotated with `@GetMapping` and `@PostMapping`
are automatically logged. Additional controller can be logged by adding the
provided `@Loggable` annotation. If applied to a class, all methods will be logged.
Otherwise, specific methods can be annotated.
