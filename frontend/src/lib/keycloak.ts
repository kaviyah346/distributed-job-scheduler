import Keycloak from 'keycloak-js';

const keycloakConfig = {
  url: process.env.NEXT_PUBLIC_KEYCLOAK_URL || 'https://keycloak-production-fa76.up.railway.app',
  realm: process.env.NEXT_PUBLIC_KEYCLOAK_REALM || 'job-scheduler',
  clientId: process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_ID || 'job-scheduler-client',
};

// Keycloak instance should only be instantiated in client-side environment
const keycloak = typeof window !== 'undefined' ? new Keycloak(keycloakConfig) : null;

export default keycloak;
