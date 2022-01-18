## Cypher-Web

This is a cypher web server implementation for the core cypher library. It mimics a similar API specification to the Hashicorp Vault project to ease compatibility usage from vault lookup libraries.
Running the web server from the source code is as easy as running

```bash
./gradlew cypher-web:run
```

### Compiling

```bash
./gradlew cypher-web:shadowJar
```

This will create an all-in-one jar in the `build/` directory of `cypher-web`. It leverages micronaut and therefore cant take
advantage of some Micronaut configuration elements for ease of use.

### Configuration

The application can be configured by multiple ways. One is by using environment variables or providiong an application.yml
configuration file.

Sample YAML: 

```yaml
cypher:
  storageLocation: /tmp/cypherTest
  masterKey: zNmL01Nz6kDE6a5VKRwQr8hm4W2DVqgWY2fWutJyNhQ=
```
The `storageLocation` is the location the data should be stored. Please select a valid data directory.
The `masterKey` is an AES-256Bit key used for initial encryption. Do NOT Lose this key. if you do, there is no recovering of the database. It is **STRONGLY** Recommended you generate your own unique key and NOT to use the key in the sample above.

To specify a configuration file for the web server to use. One must specify it in the java command line arguments

```bash
java -jar cypher-web.jar -Dmicronaut.config.files=/path/to/application.yml
```

### Running

Running the server is as simple as running `java -jar cypher-web.jar`. The application will, by default, 
start on `http://localhost:8080`. This can be configured with the `-Dmicronaut.server.port=8080` option.

**NOTE:** Please pay attention to the first time startup log. The initial `root` access token is provided for consuming
the api.

