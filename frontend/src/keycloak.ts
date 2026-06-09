import Keycloak from 'keycloak-js';

const keycloakConfig = {
    url: 'http://localhost:8080',
    realm: 'netconfig-realm',
    clientId: 'netconfig-frontend'
};

const keycloak = new Keycloak(keycloakConfig);

export const initKeycloak = (onAuthenticatedCallback: () => void) => {
    keycloak.init({
        onLoad: 'check-sso',
        silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
        pkceMethod: 'S256',
    })
    .then((authenticated) => {
        if (!authenticated) {
            console.log("User is not authenticated via Keycloak SSO.");
        }
        onAuthenticatedCallback();
    })
    .catch((error) => {
        console.error("Keycloak init failed", error);
        onAuthenticatedCallback();
    });
};

export const doLogin = keycloak.login;
export const doLogout = keycloak.logout;
export const getToken = () => keycloak.token;
export const getParsedToken = () => keycloak.tokenParsed;
export const updateToken = (successCallback: () => void) =>
    keycloak.updateToken(70)
        .then(successCallback)
        .catch(doLogin);

export default keycloak;
