# pay-publicauth
Payments Public API Authentication Service

## API Specification

The [API Specification](openapi/publicauth_spec.yaml) provides more detail on the paths and operations including examples.

[View the API specification for publicauth in Swagger Editor](https://editor.swagger.io/?url=https://raw.githubusercontent.com/alphagov/pay-publicauth/master/openapi/publicauth_spec.yaml).

## API Keys

Anatomy of an api key:

```
api_live_u3tl8gajo9paj0xki31jm1psr3v21m5urh50zoa7a262w4ntzoo6cqhu82
`-------`'-----------------------------------'
 PREFIX           RANDOM BASE32 STRING
`--------------------------------------------'`------------------------'
                   TOKEN                        CHECKSUM
```

| Item | Definition |
|------|------------|
| `TOKEN` | randomly generated base 32 string, 130 bits entropy, variable length, optionally includes a human readable prefix |
| `CHECKSUM` | `hmacSha1(TOKEN + TOKEN_API_HMAC_SECRET)`, base32 encoded. Always 32 characters long |
| `TOKEN_API_HMAC_SECRET` | secret provided via application environment |
| `TOKEN_DB_BCRYPT_SALT` | bcrypt salt provided via application environment |
| `TOKEN_HASH` | `bcrypt(TOKEN, TOKEN_DB_BCRYPT_SALT)` - the value we actually store in the database |

API KEY generation algorithm:

1. `TOKEN` := 130 bit random number and encode to base32, optionally prefixed with a human readable string based on the token account type
2. `CHECKSUM` := `hmacSha1(concat(TOKEN, TOKEN_API_HMAC_SECRET))`
3. `API_KEY` := `concat(TOKEN, CHECKSUM)`
4. `TOKEN_HASH` := `bcrypt(TOKEN, TOKEN_DB_BCRYPT_SALT)`
5. store `TOKEN_HASH` in database
6. return `API_KEY`

API KEY validation algorithm:

1. `API_KEY` is provided as `Authorization: Bearer someverylongstringandachecksum`
2. Extract `API_KEY` := `someverylongstringandachecksum`
3. Split the string at a known character index based on the length of the sha1 suffix ie. `TOKEN` := `someverylongstring` `ACTUAL_CHECKSUM` := `andachecksum`
4. verify that `hmacsha1(concat(TOKEN, TOKEN_API_HMAC_SECRET))` == `ACTUAL_CHECKSUM`
5. `TOKEN_HASH` := `bcrypt(TOKEN, TOKEN_DB_BCRYPT_SALT)`
6. lookup `TOKEN_HASH` in database; return `true` iff found

## Environment variables
| NAME                    | DESCRIPTION                                                                    |
| ----------------------- | ------------------------------------------------------------------------------ |
| `ADMIN_PORT`            | The port number to listen for Dropwizard admin requests on. Defaults to `8081`. |
| `DB_HOST`               | The hostname of the database server. |
| `DB_PASSWORD`           | The password for the `DB_USER` user. |
| `DB_SSL_OPTION`         | To turn TLS on this value must be set as `ssl=true`. Otherwise must be empty. |
| `DB_USER`               | The username to log into the database as. |
| `JAVA_HOME`             | The location of the JRE. Set to `/opt/java/openjdk` in the `Dockerfile`. |
| `JAVA_OPTS`             | Commandline arguments to pass to the java runtime. Optional. |
| `METRICS_HOST`          | The hostname to send graphite metrics to. Defaults to `localhost`. |
| `METRICS_PORT`          | The port number to send graphite metrics to. Defaults to `8092`. |
| `PORT`                  | The port number to listen for requests on. Defaults to `8080`. |
| `RUN_APP`               | Set to `true` to run the application. Defaults to `true`. |
| `RUN_MIGRATION`         | Set to `true` to run a database migration. Defaults to `false`. |
| `TOKEN_API_HMAC_SECRET` | HMAC secret to create the signature for the API Key. |
| `TOKEN_DB_BCRYPT_SALT`  | Salt used for the hashing algorithm (bcrypt) to hash tokens before being stored in DB. |

## Integration tests

To run the integration tests, the `DOCKER_HOST` and `DOCKER_CERT_PATH` environment variables must be set up correctly. On OS X the environment can be set up with:

```
    eval $(boot2docker shellinit)
    eval $(docker-machine env <virtual-machine-name>)

```

The command to run the integration tests is:

```
    mvn test
```

## Licence

[MIT License](LICENSE)

## Vulnerability Disclosure

GOV.UK Pay aims to stay secure for everyone. If you are a security researcher and have discovered a security vulnerability in this code, we appreciate your help in disclosing it to us in a responsible manner. Please refer to our [vulnerability disclosure policy](https://www.gov.uk/help/report-vulnerability) and our [security.txt](https://vdp.cabinetoffice.gov.uk/.well-known/security.txt) file for details.
