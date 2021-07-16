package health.ere.ps.service.connector.provider;

import de.gematik.ws.conn.authsignatureservice.wsdl.v7.AuthSignatureService;
import de.gematik.ws.conn.authsignatureservice.wsdl.v7.AuthSignatureServicePortType;
import de.gematik.ws.conn.cardservice.wsdl.v8.CardService;
import de.gematik.ws.conn.cardservice.wsdl.v8.CardServicePortType;
import de.gematik.ws.conn.certificateservice.wsdl.v6.CertificateService;
import de.gematik.ws.conn.certificateservice.wsdl.v6.CertificateServicePortType;
import de.gematik.ws.conn.connectorcontext.v2.ContextType;
import de.gematik.ws.conn.eventservice.wsdl.v7.EventService;
import de.gematik.ws.conn.eventservice.wsdl.v7.EventServicePortType;
import de.gematik.ws.conn.signatureservice.wsdl.v7.SignatureService;
import de.gematik.ws.conn.signatureservice.wsdl.v7.SignatureServicePortType;
import de.gematik.ws.conn.signatureservice.wsdl.v7.SignatureServicePortTypeV755;
import de.gematik.ws.conn.signatureservice.wsdl.v7.SignatureServiceV755;
import health.ere.ps.config.UserConfig;
import health.ere.ps.config.interceptor.ProvidedConfig;
import health.ere.ps.service.common.security.SecretsManagerService;
import health.ere.ps.service.connector.endpoint.EndpointDiscoveryService;
import health.ere.ps.service.connector.endpoint.SSLUtilities;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.xml.ws.BindingProvider;
import java.util.logging.Logger;

@ApplicationScoped

public class ConnectorServicesProvider {
    private final Logger log = Logger.getLogger(getClass().getName());

    @Inject
    UserConfig userConfig;
    @Inject
    EndpointDiscoveryService endpointDiscoveryService;
    @Inject
    SecretsManagerService secretsManagerService;

    private CardServicePortType cardServicePortType;
    private CertificateServicePortType certificateService;
    private EventServicePortType eventServicePortType;
    private AuthSignatureServicePortType authSignatureServicePortType;
    private SignatureServicePortType signatureServicePortType;
    private SignatureServicePortTypeV755 signatureServicePortTypeV755;
    private ContextType contextType;

    @PostConstruct
    void init() {
        initializeServices();
    }

    public void initializeServices() {
        initializeCardServicePortType();
        initializeCertificateService();
        initializeEventServicePortType();
        initializeAuthSignatureServicePortType();
        initializeSignatureServicePortType();
        initializeSignatureServicePortTypeV755();
        initializeContextType();
    }

    private void initializeCardServicePortType() {
        CardServicePortType cardService = new CardService(getClass().getResource("/CardService.wsdl"))
                .getCardServicePort();

        BindingProvider bp = (BindingProvider) cardService;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                endpointDiscoveryService.getCardServiceEndpointAddress());
        configureBindingProvider(bp);

        cardServicePortType = cardService;
    }

    private void initializeCertificateService() {
        CertificateServicePortType service = new CertificateService(getClass()
                .getResource("/CertificateService_v6_0_1.wsdl")).getCertificateServicePort();

        BindingProvider bp = (BindingProvider) service;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                endpointDiscoveryService.getCertificateServiceEndpointAddress());
        configureBindingProvider(bp);

        this.certificateService = service;
    }

    private void initializeEventServicePortType() {
        EventServicePortType service = new EventService(getClass().getResource("/EventService.wsdl"))
                .getEventServicePort();

        BindingProvider bp = (BindingProvider) service;
        log.info(getClass().getName() + " ::eventServicePortType");
        log.info(String.valueOf(secretsManagerService == null));
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                endpointDiscoveryService.getEventServiceEndpointAddress());
        log.info(String.valueOf(secretsManagerService == null));
        configureBindingProvider(bp);

        eventServicePortType = service;
    }

    private void initializeAuthSignatureServicePortType() {
        AuthSignatureServicePortType service = new AuthSignatureService(getClass().getResource(
                "/AuthSignatureService_v7_4_1.wsdl")).getAuthSignatureServicePort();
        BindingProvider bp = (BindingProvider) service;

        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                endpointDiscoveryService.getAuthSignatureServiceEndpointAddress());
        configureBindingProvider(bp);

        authSignatureServicePortType = service;
    }

    private void initializeSignatureServicePortType() {
        SignatureServicePortType service = new SignatureService(getClass()
                .getResource("/SignatureService.wsdl")).getSignatureServicePort();

        BindingProvider bp = (BindingProvider) service;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                endpointDiscoveryService.getSignatureServiceEndpointAddress());
        configureBindingProvider(bp);

        signatureServicePortType = service;
    }

    private void initializeSignatureServicePortTypeV755() {
        SignatureServicePortTypeV755 service = new SignatureServiceV755(getClass()
                .getResource("/SignatureService_V7_5_5.wsdl")).getSignatureServicePortTypeV755();

        BindingProvider bp = (BindingProvider) service;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                endpointDiscoveryService.getSignatureServiceEndpointAddress());
        configureBindingProvider(bp);

        signatureServicePortTypeV755 = service;
    }

    private void initializeContextType() {
        ContextType contextType = new ContextType();
        contextType.setMandantId(userConfig.getMandantId());
        contextType.setClientSystemId(userConfig.getClientSystemId());
        contextType.setWorkplaceId(userConfig.getWorkplaceId());
        contextType.setUserId(userConfig.getUserId());

        this.contextType = contextType;
    }

    private void configureBindingProvider(BindingProvider bindingProvider) {
        bindingProvider.getRequestContext().put("com.sun.xml.ws.transport.https.client.SSLSocketFactory",
                secretsManagerService.getSslContext().getSocketFactory());
        bindingProvider.getRequestContext().put("com.sun.xml.ws.transport.https.client.hostname.verifier",
                new SSLUtilities.FakeHostnameVerifier());
    }

    @ProvidedConfig
    public CardServicePortType getCardServicePortType() {
        return cardServicePortType;
    }

    @ProvidedConfig
    public CertificateServicePortType getCertificateService() {
        return certificateService;
    }

    @ProvidedConfig
    public EventServicePortType getEventServicePortType() {
        return eventServicePortType;
    }

    @ProvidedConfig
    public AuthSignatureServicePortType getAuthSignatureServicePortType() {
        return authSignatureServicePortType;
    }

    @ProvidedConfig
    public SignatureServicePortType getSignatureServicePortType() {
        return signatureServicePortType;
    }

    @ProvidedConfig
    public SignatureServicePortTypeV755 getSignatureServicePortTypeV755() {
        return signatureServicePortTypeV755;
    }

    @ProvidedConfig
    public ContextType getContextType() {
        return contextType;
    }
}