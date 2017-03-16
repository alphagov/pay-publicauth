# pay-publicauth
Payments Public API Authentication Service

## API Keys

One of the responsabilities of this service is to issue Api keys so integrators can request operations throught the Public API. An API Key is composed by: Token + Hmac (Token, Secret).
- _Tokens_ are randomly generated values and these values are stored in the database (encrypted) identifying a single accountId.
- When issuing tokens, public auth stores this value (encrypted) along with the accountId in the DB and creates the API key with this
token value as plain text plus an Hmac of the same token using a secret key (enabling public auth to do an extra validation
confirming that the token provided when authorizing requests is genuine.

## Environment variables
| NAME                  | DESCRIPTION                                                                    |
| ----------------------| ------------------------------------------------------------------------------ |
| DB_SSL_OPTION         | To turn TLS on this value must be set as _“ssl=true”_. Otherwise must be empty |
| TOKEN_DB_BCRYPT_SALT  | Salt used for the encryption algorithm (Bcrypt) to encrypt tokens stored in DB |
| TOKEN_API_HMAC_SECRET | Hmac secret to create Hash composing the API Keys. |

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

## API Specification

The [API Specification](docs/api_specification.md) provides more detail on the paths and operations including examples.

| Path                          | Supported Methods | Description                        |
| ----------------------------- | ----------------- | ---------------------------------- |
|[```/v1/api/auth```](docs/api_specification.md#get-v1apiauth)              | GET    |  Look up the account id for a token.            |
|[```/v1/frontend/auth```](docs/api_specification.md#post-v1frontendauth)             | POST   |  Generates a new dev token for a given account. |
|[```/v1/frontend/auth```](docs/api_specification.md#put-v1frontendauth)             | PUT   |  Updates the description of an existing dev token. |
|[```/v1/frontend/auth/{account_id}```](docs/api_specification.md#get-v1frontendauthaccount_id)             | GET   |  Retrieves all generated and not revoked tokens for this account. |
|[```/v1/frontend/auth/{account_id}```](docs/api_specification.md#delete-v1frontendauthaccount_id)             | DELETE   |  Revokes the supplied dev token for this account. |

## Licence

[MIT License](LICENCE)

## Responsible Disclosure

GOV.UK Pay aims to stay secure for everyone. If you are a security researcher and have discovered a security vulnerability in this code, we appreciate your help in disclosing it to us in a responsible manner. We will give appropriate credit to those reporting confirmed issues. Please e-mail gds-team-pay-security@digital.cabinet-office.gov.uk with details of any issue you find, we aim to reply quickly.

