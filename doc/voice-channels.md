## About

Voice Channels

## Create a new channel

Endpoint:
```
http://{domain}:{port}/createVoiceChannel
```

HTTP method: `POST`

Required headers:
```
Content-Type: text/plain; charset=utf-8
```

Request body:
```
channel name
```

Response on success:
```
HTTP/{version} 201 Created
```

Response on failure because the channel exists already:
```
HTTP/{version} 409 Conflict
Content-Type: application/json

{
   "reason":"The channel exists already."
}
```

## Remove a channel

Endpoint:
```
http://{domain}:{port}/removeVoiceChannel
```

HTTP method: `DELETE`

Required headers:
```
Content-Type: text/plain; charset=utf-8
```

Request body:
```
channel name
```

Response on success:
```
HTTP/{version} 204 No Content
```

Response on failure because the channel does not exist:
```
HTTP/{version} 404 Not Found
Content-Type: application/json

{
   "reason":"The channel does not exist."
}
```

## Join the channel

Endpoint:
```
http://{domain}:{port}/joinVoiceChannel
```

HTTP method: `POST`

Required headers:
```
Content-Type: application/json; charset=utf-8
```

Request body:
```json
{
   "username":"string value",
   "channelName":"string value"
}
```

Response on success:
```
HTTP/{version} 204 No Content
```

Response on failure because the channel does not exist:
```
HTTP/{version} 404 Not Found
Content-Type: application/json

{
   "reason":"The channel does not exist."
}
```

Response on failure because the user does not exist:
```
HTTP/{version} 404 Not Found
Content-Type: application/json

{
   "reason":"The user does not exist."
}
```

## Leave the channel

Endpoint:
```
http://{domain}:{port}/leaveVoiceChannel
```

HTTP method: `POST`

Required headers:
```
Content-Type: application/json; charset=utf-8
```

Request body:
```json
{
   "username":"string value",
   "channelName":"string value"
}
```

Response on success:
```
HTTP/{version} 204 No Content
```

Response on failure because the channel does not exist:
```
HTTP/{version} 404 Not Found
Content-Type: application/json

{
   "reason":"The channel does not exist."
}
```

Response on failure because the user does not exist:
```
HTTP/{version} 404 Not Found
Content-Type: application/json

{
   "reason":"The user does not exist."
}
```

## Switch the channel

Endpoint:
```
http://{domain}:{port}/switchVoiceChannel
```

HTTP method: `POST`

Required headers:
```
Content-Type: application/json; charset=utf-8
```

Request body:
```json
{
   "username":"string value",
   "oldChannelName":"string value",
   "newChannelName":"string value"
}
```

Response on success:
```
HTTP/{version} 204 No Content
```

Response on failure because the new channel does not exist:
```
HTTP/{version} 404 Not Found
Content-Type: application/json

{
   "reason":"The new channel does not exist."
}
```

Response on failure because the old channel does not exist:
```
HTTP/{version} 404 Not Found
Content-Type: application/json

{
   "reason":"The old channel does not exist."
}
```

Response on failure because the user does not exist:
```
HTTP/{version} 404 Not Found
Content-Type: application/json

{
   "reason":"The user does not exist."
}
```

Response on failure because the user was not in the old channel:
```
HTTP/{version} 400 Bad Request
Content-Type: application/json

{
   "reason":"The user was not in the old channel. The user was in {channel name}."
}
```

Response on failure because the user was not in any channel:
```
HTTP/{version} 400 Bad Request
Content-Type: application/json

{
   "reason":"The user was not in any channel."
}
```

## Get the list of channels

Endpoint:
```
http://{domain}:{port}/voiceChannels
```

HTTP method: `GET`

Response on success:
```
HTTP/{version} 200 OK
Content-Type: application/json

{
   "channels":[
      {
         "name":"channel name1",
         "users":[
            "username1",
            "username2"
         ]
      },
      {
         "name":"channel name2",
         "users":[
            "username3",
            "username4"
         ]
      }
   ]
}
```

## Messages from the server

The message will be always in JSON.

There must be an attribute `about` for the client to apply the appropriate logic.

### When a new channel is created

The server will broadcast the following message:

```json
{
  "about":"voice channel is created",
  "name":"string value"
}
```

### When a channel is removed

The server will broadcast the following message:

```json
{
  "about":"voice channel is removed",
  "name":"string value"
}
```

### When a user joined the channel

The server will broadcast the following message:

```json
{
  "about":"user joined the voice channel",
  "username":"string value",
  "channelName":"string value"
}
```

### When a user left the channel

The server will broadcast the following message:

```json
{
  "about":"user left the voice channel",
  "username":"string value",
  "channelName":"string value"
}
```

### When a user switched the channel

The server will broadcast the following message:
```json
{
   "about":"user switched the voice channel",
   "username":"string value",
   "oldChannelName":"string value",
   "newChannelName":"string value"
}
```
