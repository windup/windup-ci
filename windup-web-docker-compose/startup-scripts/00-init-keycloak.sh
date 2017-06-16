#!/bin/bash
echo "=> Validate keycloak configuration"
echo "=> KEYCLOAK_REALM_PUBLIC_KEY: " $KEYCLOAK_REALM_PUBLIC_KEY
echo "=> KEYLOCAK_AUTH_URL: " $KEYLOAK_AUTH_URL

echo "=> Executing the commands"
$JBOSS_CLI -c << EOF
# Mark the commands below to be run as a batch
batch

# Properties
/system-property=keycloak.realm.public.key:add(value="$KEYCLOAK_REALM_PUBLIC_KEY")
/system-property=keycloak.server.url:add(value="$KEYCLOAK_AUTH_URL")

# keycloak
/subsystem=keycloak/secure-deployment=api.war:add(realm=rhamt, realm-public-key="\${keycloak.realm.public.key}", auth-server-url="\${keycloak.server.url}", public-client=true, ssl-required=EXTERNAL, resource=rhamt-web)
/subsystem=keycloak/secure-deployment=rhamt-web.war:add(realm=rhamt, realm-public-key="\${keycloak.realm.public.key}", auth-server-url="\${keycloak.server.url}", public-client=true, ssl-required=EXTERNAL, resource=rhamt-web)
# Execute the batch
run-batch
EOF
