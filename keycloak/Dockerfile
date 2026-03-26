FROM quay.io/keycloak/keycloak:26.5.5

COPY keycloak/realm/*.json /opt/keycloak/data/import/
COPY keycloakify/dist_keycloak/keycloak-theme-for-kc-all-other-versions.jar /opt/keycloak/providers/heuermannplus-theme.jar
