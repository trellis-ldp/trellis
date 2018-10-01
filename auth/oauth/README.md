# OAuth Module

This authentication module processes JWT tokens that are typically generated as part of OAuth and OIDC flows.
The module can be configured in the following ways:

  1. Use a remote JWKS key file. For this, set the `trellis.oauth.jwk.location` property with the appropriate value.
  2. Use a local keystore. For this, set the `trellis.oauth.keystore.path` and `trellis.oauth.keystore.password` properties.
     In addition, please also set the `trellis.oauth.keyids` property as a list of comma-separated key identifiers.
  3. Use a shared secret (this is the least secure!). For this, set the `trellis.oauth.sharedsecret` property with a
     base64-encoded string.

