# http-end2end-encryption

A demonstration to implement end to end encryption over http. This project is inpired from [RFC 8188](https://httpwg.org/specs/rfc8188.html) but it doesn't implement it.

## Demonstration application

This is a Spring Boot application exposing a DepartmentController handling requests like:

```http request
GET /departments/35
```

and returning responses like:

```http request
HTTP/1.1 200
Content-Type: application/json
Transfer-Encoding: chunked
Date: Thu, 04 Apr 2024 22:51:56 GMT
Keep-Alive: timeout=60
Connection: keep-alive

{
    "dep": "35",
    "reg": 53,
    "cheflieu": "35238",
    "tncc": 1,
    "ncc": "ILLE ET VILAINE",
    "nccend": "Ille-et-Vilaine",
    "libelle": "Ille-et-Vilaine"
}
```

## EncryptedContentEncodingFilter

The same controller is exposed behind path `/encrypted` and a [Servlet Filter is configured](https://github.com/jeremiehuchet/http-end2end-encryption/blob/main/src/main/kotlin/com/github/jeremiehuchet/httpend2endencryption/HttpEnd2endEncryptionApplication.kt#L19) to process every request matching glob pattern `/encrypted/**`.

Requests and responses are encrypted using Advanced Encryption Standard (AES) in Galois/Counter Mode (GCM).

Requests processed by the `EncryptedContentEncodingFilter` must have a `Content-Encoding` header set to `aes128gcm` with a `public-key` attribute.  
Request body must be encrypted with the server's public key.

The registered Servlet Filter will decrypt the request body before forwarding it to the Spring controllers. This way we achieve separation of concerns and our controllers implementations don't have to deal with encryption/decryption logic.

```http request
PUT http://localhost:42445/encrypted/departments/75
Content-Encoding: aes128gcm; public-key=MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEjhasrU1y0ze8kpZbTL6iytpBfuFsJ1Oir/MhnmvPkAWhwmHikuw4MppZ8inMEhwDxwFmhQWReNuz90DebA87og==
Content-Type: application/json

🔐 <content encrypted with server's public key>
🔐 {
🔐   "dep": "75",
🔐   "reg": 11,
🔐   "cheflieu": "75056",
🔐   "tncc": 0,
🔐   "ncc": "PARIS",
🔐   "nccend": "Paris",
🔐   "libelle": "Paris 🇫🇷"
🔐 }
```

The result returned by the spring controller will be encrypted by the Servlet Filter and according headers will be set:

```http request
HTTP/1.1 200
Content-Encoding: aes128gcm
Content-Type: application/json
Transfer-Encoding: chunked
Date: Thu, 04 Apr 2024 22:51:56 GMT
Keep-Alive: timeout=60
Connection: keep-alive

🔐 <content encoded with client's public key>
🔐 {
🔐   "dep": "75",
🔐   "reg": 11,
🔐   "cheflieu": "75056",
🔐   "tncc": 0,
🔐   "ncc": "PARIS",
🔐   "nccend": "Paris",
🔐   "libelle": "Paris 🇫🇷"
🔐 }
```
