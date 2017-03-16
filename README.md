# pay-publicauth
Payments Public API Authentication Service

## API Keys

One of the responsibilities of this service is to issue API keys so integrators can request operations through the Public API. An API Key is composed by: Token + HMAC (Token, Signature).

_Tokens_ are randomly generated values and these values are stored in the database (hashed) identifying a single accountID.

pay-publicauth creates an _API key_ by concatinating the token value as plain text with the HMAC signature of the same token using a secret key. The HMAC signature is used to confirm the token was issued by the pay-publicauth service.

## Environment variables
| NAME                  | DESCRIPTION                                                                    |
| ----------------------| ------------------------------------------------------------------------------ |
| DB_SSL_OPTION         | To turn TLS on this value must be set as _“ssl=true”_. Otherwise must be empty. |
| TOKEN_DB_BCRYPT_SALT  | Salt used for the hashing algorithm (bcrypt) to hash tokens before being stored in DB. |
| TOKEN_API_HMAC_SECRET | HMAC secret to create the signature for the API Key. |

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
|[```/v1/api/auth```](docs/api_specification.md#get-v1apiauth)              | GET    |  Look up the account ID for a token.            |
|[```/v1/frontend/auth```](docs/api_specification.md#post-v1frontendauth)             | POST   |  Generates a new dev token for a given account. |
|[```/v1/frontend/auth```](docs/api_specification.md#put-v1frontendauth)             | PUT   |  Updates the description of an existing dev token. |
|[```/v1/frontend/auth/{account_id}```](docs/api_specification.md#get-v1frontendauthaccount_id)             | GET   |  Retrieves all generated tokens for this account that are not revoked. |
|[```/v1/frontend/auth/{account_id}```](docs/api_specification.md#delete-v1frontendauthaccount_id)             | DELETE   |  Revokes the supplied dev token for this account. |

## Licence

[MIT License](LICENSE)

## Responsible Disclosure

GOV.UK Pay aims to stay secure for everyone. If you are a security researcher and have discovered a security vulnerability in this code, we appreciate your help in disclosing it to us in a responsible manner. We will give appropriate credit to those reporting confirmed issues. Please e-mail gds-team-pay-security@digital.cabinet-office.gov.uk with details of any issue you find, we aim to reply quickly.

