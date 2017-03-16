# API Specification

## GET /v1/api/auth

This endpoint looks up the account ID for a token.

### Request example

```
GET /v1/auth
Accept: application/json
Authorization: Bearer BEARER_TOKEN

```

### Response example

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

#### Response field description

| Field                    | always present | Description                         |
| ------------------------ |:--------------:| ----------------------------------- |
| `account_id`             | X              | The account ID for the bearer token |

-----------------------------------------------------------------------------------------------------------

## POST /v1/frontend/auth

Generate and return a new token for the given gateway account ID.

### Request example

```
Content-Type: application/json
{
    "account_id": "GATEWAY_ACCOUNT_1",
    "description": "Token description"
}

```

#### Request body description

| Field                    | required | Description                                   |
| ------------------------ |:--------:| --------------------------------------------- |
| `account_id`             | X        | Gateway account to associate the new token to |
| `description`            | X        | Description of the new token                  |


### Successful response example

```
200 OK
Content-Type: application/json

{
    "token": "GENERATED_TOKEN"
}
```

#### Successful response field description

| Field                  | Description                               |
| ---------------------- | ----------------------------------------- |
| `token`                | The newly generated token                 |

### Unsuccessful response example

```
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
    "message": "Missing fields: [account_id]"
}
```
#### Unsuccessful response field description

| Field              | Description                     |
| ------------------ | ------------------------------- |
| `message`          | The error message               |

-----------------------------------------------------------------------------------------------------------

## PUT /v1/frontend/auth

Updates the description of an existing dev token.

### Request example

```
Content-Type: application/json
{
    "token_link": "550e8400-e29b-41d4-a716-446655440000",
    "description": "New token description"
}

```

#### Request body description

| Field                    | required | Description                                                |
| ------------------------ |:--------:| ---------------------------------------------------------- |
| `token_link`             | X        | Token link as return by [GET /v1/frontend/auth/{account_id}](#get-v1frontendauthaccount_id) |
| `description`            | X        | New description of the existing token                      |


### Successful response example

```
200 OK
Content-Type: application/json

{
    "token_link": "550e8400-e29b-41d4-a716-446655440000",
    "description": "New token description"
}
```

#### Successful response field description

| Field                  | Description                               |
| ---------------------- | ----------------------------------------- |
| `token_link`           | Token link of the updated resource        |
| `description`          | New description of the existing token     |

### Unsuccessful response example

```
HTTP/1.1 404 Not Found
Content-Type: application/json

{
    "message": "Could not update token description"
}
```
#### Unsuccessful response field description

| Field              | Description                     |
| ------------------ | ------------------------------- |
| `message`          | The error message               |


-----------------------------------------------------------------------------------------------------------

## GET /v1/frontend/auth/{account_id}

Retrieves generated tokens for this account.

### Query Parameters
| Field              | Possible Values     | Description           |
| ------------------ | -----------| --------------------- |
| `state`            | `ACTIVE`   | Retrieve active tokens only |
| `state`            | `REVOKED`  | Retrieve revoked tokens only |

If query parameter `state` is missing, the request will retrieve **active** tokens by default.

### Request example

- Retrieve active tokens

    ```
    GET /v1/frontend/auth/15
    Accept: application/json
    ```
    
    or
    
    ```
    GET /v1/frontend/auth/15?state=active
    Accept: application/json
    ```
    
- Retrieve revoked tokens

    ```
    GET /v1/frontend/auth/15?state=revoked
    Accept: application/json
    ```
    
### Response example

```
200 OK
Content-Type: application/json

{
    "tokens": [
                {"token_link": "550e8400-e29b-41d4-a716-446655440000", "description": "token 1 description"},
                {"token_link": "550e8400-e29b-41d4-a716-446655441234", "description": "token 2 description", "revoked": "10 Oct 2015"}
              ]
}
```

-----------------------------------------------------------------------------------------------------------

## DELETE /v1/frontend/auth/{account_id}

Revokes the supplied dev token for this account.

### Request example

```
Content-Type: application/json
{
    "token_link": "550e8400-e29b-41d4-a716-446655440000"
}

```

#### Request body description

| Field                    | required | Description                                                |
| ------------------------ |:--------:| ---------------------------------------------------------- |
| `token_link`             | X        | Token link as return by [GET /v1/frontend/auth/{account_id}](#get-v1frontendauthaccount_id) |


### Successful response example

```
200 OK
Content-Type: application/json

{
    "revoked": "10 Oct 2015 - 12:12"
}
```

### Unsuccessful response example

```
HTTP/1.1 404 Not Found
Content-Type: application/json

{
    "message": "Could not revoke token"
}
```
#### Unsuccessful response field description

| Field              | Description                     |
| ------------------ | ------------------------------- |
| `message`          | The error message               |

