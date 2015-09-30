# pay-publicauth
Payments Public API Authentication Service

## Integration tests

To run the integration tests, the `DOCKER_HOST` and `DOCKER_CERT_PATH` environment variables must be set up correctly. On OS X the environment can be set up with:
Also `$GDS_CONNECTOR_WORLDPAY_PASSWORD` and`$GDS_CONNECTOR_WORLDPAY_PASSWORD` environment variable must be set for Worlpay integration tests.

```
    eval $(boot2docker shellinit)
    eval $(docker-machine env <virtual-machine-name>)

```

The command to run the integration tests is:

```
    mvn test
```

## API

| Path                          | Supported Methods | Description                        |
| ----------------------------- | ----------------- | ---------------------------------- |
|[```/v1/auth```](#get-v1auth)              | GET    |  Look up the account id for a token.            |
|[```/v1/auth```](#post-v1auth)             | POST   |  Generates a new dev token for a given account. |
|[```/v1/auth/{account_id}/revoke```](#post-v1authaccount_idrevoke) | POST  |  Disables all dev tokens currently enabled for this account.  |


### GET /v1/auth

This endpoint looks up the account id for a token.

#### Request example

```
GET /v1/auth
Content-Type: application/json
Authorization: Bearer BEARER_TOKEN

```

#### Response example

```
200 OK
Content-Type: application/json

{
    "account_id": "ACCOUNT_ID"
}
```

Or if the token does not exist or has been revoked:

```
401 UNAUTHORIZED
```

##### Response field description

| Field                    | always present | Description                               |
| ------------------------ |:--------:| ----------------------------------------- |
| `account_id`                 | X | The account Id for the bearer token|

-----------------------------------------------------------------------------------------------------------

### POST /v1/auth


#### Request example

```
Content-Type: application/json

```

#### Response example

```
200 OK
Content-Type: application/json

{
    "token": "GENERATED_TOKEN"
}
```


### POST /v1/auth/{account_id}/revoke

#### Request example

```
POST WITH EMPTY BODY
{
}
```


#### Response example

```
200 OK

```