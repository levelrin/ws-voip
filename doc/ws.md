## About

WebSocket connection.

## Make WebSocket Connection

Endpoint:
```
ws://{domain}:{port}/connect?username={username}
```

`username` should be url encoded.

## Get online users

Endpoint:
```
http://{domain}:{port}/onlineUsers
```

HTTP method: `GET`

Response on success:
```
HTTP/{version} 200 OK
Content-Type: application/json

{
   "users":[
      "username1",
      "username2"
   ]
}
```

## Messages from the server

The message will be always in JSON.

There must be an attribute `about` for the client to apply the appropriate logic.

### When the user is connected to the server

The server will broadcast the following message:

```json
{
   "about":"user is connected to the websocket server",
   "username":"string value"
}
```

### When the user is disconnected from the server

The server will broadcast the following message:

```json
{
   "about":"user is disconnected from the websocket server",
   "username":"string value"
}
```

### When the user switched the device

The server will send the following message to the user when another device is used.

The client should inform the user and signs out the old device.

```json
{
   "about":"another device is used"
}
```
