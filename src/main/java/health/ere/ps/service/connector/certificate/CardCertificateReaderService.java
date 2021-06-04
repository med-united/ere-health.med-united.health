package health.ere.ps.service.connector.certificate;

import de.gematik.ws.conn.certificateservice.v6.ReadCardCertificateResponse;
import de.gematik.ws.conn.certificateservicecommon.v2.X509DataInfoListType;
import de.gematik.ws.conn.connectorcommon.v5.Status;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import health.ere.ps.exception.common.security.SecretsManagerException;
import health.ere.ps.exception.connector.ConnectorCardCertificateReadException;
import health.ere.ps.exception.idp.crypto.IdpCryptoException;
import health.ere.ps.model.idp.crypto.PkiIdentity;
import health.ere.ps.service.common.security.SecretsManagerService;
import health.ere.ps.service.idp.crypto.CryptoLoader;

@ApplicationScoped
public class CardCertificateReaderService {

    private static Logger log = Logger.getLogger(CardCertificateReaderService.class.getName());

    public byte[] mockCertificate;

    @Inject
    CardCertReadExecutionService cardCertReadExecutionService;

    @Inject
    SecretsManagerService secretsManagerService;

    @ConfigProperty(name = "connector.simulator.smcbIdentityCertificate", defaultValue = "!")
    String smcbIdentityCertificate;

    @ConfigProperty(name = "app.trust.store")
    String ereTrustStoreFilePath;

    @ConfigProperty(name = "app.trust.store.pwd")
    String ereTrustStorePassword;

    private static final String STATUS_OK = "OK";

    @PostConstruct
    public void init() {
        if (smcbIdentityCertificate != null && !("".equals(smcbIdentityCertificate))
                && !("!".equals(smcbIdentityCertificate))) {
            log.info(CardCertificateReaderService.class.getSimpleName() + " uses SMCB " + smcbIdentityCertificate);
            try (InputStream is = new FileInputStream(smcbIdentityCertificate)) {
                setMockCertificate(is.readAllBytes());
            } catch (IOException e) {
                log.log(Level.SEVERE, "Could find file", e);
            }
        }
    }

    @PostConstruct
    public void initSecretsManagement() throws SecretsManagerException {
        secretsManagerService.createTrustStore(ereTrustStoreFilePath,
                SecretsManagerService.KeyStoreType.PKCS12, ereTrustStorePassword.toCharArray());
    }

    public void setMockCertificate(byte[] mockCertificate) {
        this.mockCertificate = mockCertificate;
    }

    /**
     * Reads the AUT certificate of a card managed in the connector.
     *
     * @param invocationContext The context for the call to the connector.
     * @param cardHandle        The handle of the card.
     * @return The card's AUT certificate.
     */
    public byte[] readCardCertificate(InvocationContext invocationContext, String cardHandle)
            throws ConnectorCardCertificateReadException {
        byte[] x509Certificate = new byte[0];

        if (mockCertificate != null) {
            return mockCertificate;
        }

        ReadCardCertificateResponse readCardCertificateResponse =
                cardCertReadExecutionService.doReadCardCertificate(invocationContext, cardHandle);

        Status status = readCardCertificateResponse.getStatus();
        if (status != null && status.getResult().equals(STATUS_OK)) {
            X509DataInfoListType x509DataInfoList = readCardCertificateResponse.getX509DataInfoList();
            List<X509DataInfoListType.X509DataInfo> x509DataInfos = x509DataInfoList.getX509DataInfo();
            if (CollectionUtils.isNotEmpty(x509DataInfos)) {
                log.log(Level.INFO, "Certificate list size = " + x509DataInfos.size());

                x509Certificate = x509DataInfos.get(0).getX509Data().getX509Certificate();
            }
        }

        if (ArrayUtils.isEmpty(x509Certificate)) {
            throw new ConnectorCardCertificateReadException("Could not retrieve connector smart " +
                    "card certificate from the connector.");
        }

        return x509Certificate;
    }

    public byte[] readCardCertificate(String clientId, String clientSystem, String workplace,
                                      String cardHandle) throws ConnectorCardCertificateReadException {
        return readCardCertificate(new InvocationContext(clientId, clientSystem, workplace),
                cardHandle);
    }

    public byte[] readCardCertificate(String clientId, String clientSystem, String workplace,
                                      String userId, String cardHandle)
            throws ConnectorCardCertificateReadException {
        return readCardCertificate(new InvocationContext(clientId, clientSystem, workplace, userId),
                cardHandle);
    }

    public PkiIdentity retrieveCardCertIdentity(String clientId, String clientSystem,
                                                String workplace, String cardHandle)
            throws ConnectorCardCertificateReadException, IdpCryptoException, SecretsManagerException {
        byte[] connector_cert_auth = readCardCertificate(clientId, clientSystem, workplace,
                cardHandle);
        PkiIdentity identity;

        secretsManagerService.saveTrustedCertificate(ereTrustStoreFilePath,
                ereTrustStorePassword.toCharArray(), "connector_cert_alias",
                connector_cert_auth);

        try (InputStream is = new FileInputStream(ereTrustStoreFilePath)) {
            identity = CryptoLoader.getIdentityFromP12(is, ereTrustStorePassword);

        } catch (Throwable e) {

            throw new ConnectorCardCertificateReadException("Error getting C_AUTH PKI Identity", e);
        }

        return identity;
    }
}
