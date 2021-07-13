package health.ere.ps.validation.fhir.context.support;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.ValueSet;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import health.ere.ps.service.logging.EreLogger;

public class ErePrePopulatedValidationSupport extends PrePopulatedValidationSupport {
    private static final EreLogger ereLogger =
            EreLogger.getLogger(ErePrePopulatedValidationSupport.class);
    private static final List<EreLogger.SystemContext> systemContextList = List.of(
            EreLogger.SystemContext.KbvBundleValidator,
            EreLogger.SystemContext.KbvBundleValidatorConfiguration);
    private IParser xmlParser = FhirContext.forR4().newXmlParser();

    protected enum ConfigType {
        PROFILE, EXTENSION, VALUE_SET, CODE_SYSTEM, NAMING_SYSTEM, UNKNOWN
    }

    private static final List<List<String>> structureDefinitionsAndExtensions = Arrays.asList(
            // Add StructureDefinition profiles.
            List.of("/fhir/r4/profile/v1_0_0/address-de-basis.xml",
                    "http://fhir.de/StructureDefinition/address-de-basis|0.9.13", "1.0.0"),
            List.of("/fhir/r4/profile/v1_0_0/identifier-efn.xml",
                    "http://fhir.de/StructureDefinition/identifier-efn|0.9.13", "1.0.0"),
            List.of("/fhir/r4/profile/v1_0_0/identifier-lanr.xml",
                    "http://fhir.de/StructureDefinition/identifier-lanr|0.9.13", "1.0.0"),
            List.of("/fhir/r4/profile/v1_0_0/identifier-iknr.xml",
                    "http://fhir.de/StructureDefinition/identifier-iknr|0.9.13", "1.0.0"),
            List.of("/fhir/r4/profile/v1_0_0/identifier-zanr.xml",
                    "http://fhir.de/StructureDefinition/identifier-zanr|0.9.13", "1.0.0"),
            List.of("/fhir/r4/profile/v1_0_0/humanname-de-basis.xml",
                    "http://fhir.de/StructureDefinition/humanname-de-basis|0.9.13", "1.0.0"),
            List.of("/fhir/r4/profile/v1_0_0/identifier-bsnr.xml",
                    "http://fhir.de/StructureDefinition/identifier-bsnr|0.9.13", "1.0.0"),
            List.of("/fhir/r4/profile/v1_0_0/identifier-vknr.xml",
                    "http://fhir.de/StructureDefinition/identifier-vknr|0.9.13", "1.0.0"),
            List.of("/fhir/r4/profile/v1_0_0/identifier-kzva.xml",
                    "http://fhir.de/StructureDefinition/identifier-kzva|0.9.13", "1.0.0"),
            List.of("/fhir/r4/profile/v1_0_0/identifier-pid.xml",
                    "http://fhir.de/StructureDefinition/identifier-pid|0.9.13", "1.0.0"),
            List.of("/fhir/r4/profile/v1_0_0/identifier-kvid-10.xml",
                    "http://fhir.de/StructureDefinition/identifier-kvid-10|0.9.13", "1.0.0"),
            List.of("/fhir/r4/profile/v1_0_0/identifier-pkv.xml",
                    "http://fhir.de/StructureDefinition/identifier-pkv|0.9.13", "1.0.0"),
            List.of("/fhir/r4/profile/v1_0_0/identifier-iknr-2.xml",
                    "http://fhir.de/StructureDefinition/identifier-iknr", "1.0.0"),

            List.of("/fhir/r4/profile/v1_0_1/KBV_PR_FOR_Coverage.xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_PR_FOR_Coverage|1.0.1", "1.0.1"),
            List.of("/fhir/r4/profile/v1_0_3/KBV_PR_FOR_Patient.xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_PR_FOR_Patient|1.0.1", "1.0.1"),
            List.of("/fhir/r4/profile/v1_0_1/KBV_PR_ERP_Bundle.xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_PR_ERP_Bundle|1.0.1", "1.0.1"),
            List.of("/fhir/r4/profile/v1_0_1/KBV_PR_ERP_Composition.xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_PR_ERP_Composition|1.0.1", "1.0" +
                            ".1"),
            List.of("/fhir/r4/profile/v1_0_1/KBV_PR_ERP_Medication_Compounding.xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_PR_ERP_Medication_Compounding|1" +
                            ".0.1", "1.0.1"),
            List.of("/fhir/r4/profile/v1_0_1/KBV_PR_ERP_Medication_FreeText.xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_PR_ERP_Medication_FreeText|1.0.1",
                    "1.0.1"),
            List.of("/fhir/r4/profile/v1_0_1/KBV_PR_ERP_Medication_Ingredient.xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_PR_ERP_Medication_Ingredient|1.0" +
                            ".1", "1.0.1"),
            List.of("/fhir/r4/profile/v1_0_1/KBV_PR_ERP_Medication_PZN.xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_PR_ERP_Medication_PZN|1.0.1", "1" +
                            ".0.1"),
            List.of("/fhir/r4/profile/v1_0_1/KBV_PR_ERP_PracticeSupply.xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_PR_ERP_PracticeSupply|1.0.1", "1" +
                            ".0.1"),
            List.of("/fhir/r4/profile/v1_0_1/KBV_PR_ERP_Prescription.xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_PR_ERP_Prescription|1.0.1", "1.0" +
                            ".1"),

            List.of("/fhir/r4/profile/v1_0_3/KBV_PR_FOR_Coverage.xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_PR_FOR_Coverage|1.0.3", "1.0.3"),
            List.of("/fhir/r4/profile/v1_0_3/KBV_PR_FOR_Organization.xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_PR_FOR_Organization|1.0.3", "1.0" +
                            ".3"),
            List.of("/fhir/r4/profile/v1_0_3/KBV_PR_FOR_Patient.xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_PR_FOR_Patient|1.0.3", "1.0.3"),
            List.of("/fhir/r4/profile/v1_0_3/KBV_PR_FOR_Practitioner.xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_PR_FOR_Practitioner|1.0.3", "1.0" +
                            ".3"),
            List.of("/fhir/r4/profile/v1_0_3/KBV_PR_FOR_PractitionerRole.xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_PR_FOR_PractitionerRole|1.0.3",
                    "1.0.3"),

            List.of("/fhir/r4/profile/v1_1_3/KBV_PR_Base_Organization.xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_PR_Base_Organization|1.1.3", "1" +
                            ".1.3"),
            List.of("/fhir/r4/profile/v1_1_3/KBV_PR_Base_Practitioner.xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_PR_Base_Practitioner|1.1.3", "1" +
                            ".1.3"),
            List.of("/fhir/r4/profile/v1_1_3/KBV_PR_Base_Patient.xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_PR_Base_Patient|1.1.3", "1.1.3"),

            // Add extensions.
            List.of("/fhir/r4/extension/v1_0_0/humanname-namenszusatz.xml",
                    "http://fhir.de/StructureDefinition/humanname-namenszusatz", "1.0.0"),
            List.of("/fhir/r4/extension/v1_0_0/normgroesse.xml",
                    "http://fhir.de/StructureDefinition/normgroesse", "1.0.0"),
            List.of("/fhir/r4/extension/v1_0_0/besondere-personengruppe.xml",
                    "http://fhir.de/StructureDefinition/gkv/besondere-personengruppe", "1.0.0"),
            List.of("/fhir/r4/extension/v1_0_0/dmp-kennzeichen.xml",
                    "http://fhir.de/StructureDefinition/gkv/dmp-kennzeichen", "1.0.0"),
            List.of("/fhir/r4/extension/v1_0_0/versichertenart.xml",
                    "http://fhir.de/StructureDefinition/gkv/versichertenart", "1.0.0"),
            List.of("/fhir/r4/extension/v1_0_0/wop.xml",
                    "http://fhir.de/StructureDefinition/gkv/wop", "1.0.0"),

            List.of("/fhir/r4/extension/v1_0_1/KBV_EX_ERP_Accident.xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_EX_ERP_Accident", "1.0.1"),
            List.of("/fhir/r4/extension/v1_0_1/KBV_EX_ERP_BVG.xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_EX_ERP_BVG", "1.0.1"),
            List.of("/fhir/r4/extension/v1_0_1/KBV_EX_ERP_DosageFlag.xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_EX_ERP_DosageFlag", "1.0.1"),
            List.of("/fhir/r4/extension/v1_0_1/KBV_EX_ERP_EmergencyServicesFee.xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_EX_ERP_EmergencyServicesFee",
                    "1.0.1"),
            List.of("/fhir/r4/extension/v1_0_1/KBV_EX_ERP_Medication_Category.xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_EX_ERP_Medication_Category",
                    "1.0.1"),
            List.of("/fhir/r4/extension/v1_0_1/KBV_EX_ERP_Medication_CompoundingInstruction" +
                            ".xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_EX_ERP_Medication_CompoundingInstruction",
                    "1.0.1"),
            List.of("/fhir/r4/extension/v1_0_1/KBV_EX_ERP_Medication_Ingredient_Amount.xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_EX_ERP_Medication_Ingredient_Amount",
                    "1.0.1"),
            List.of("/fhir/r4/extension/v1_0_1/KBV_EX_ERP_Medication_Ingredient_Form.xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_EX_ERP_Medication_Ingredient_Form",
                    "1.0.1"),
            List.of("/fhir/r4/extension/v1_0_1/KBV_EX_ERP_Medication_Packaging.xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_EX_ERP_Medication_Packaging",
                    "1.0.1"),
            List.of("/fhir/r4/extension/v1_0_1/KBV_EX_ERP_Medication_Vaccine.xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_EX_ERP_Medication_Vaccine",
                    "1.0.1"),
            List.of("/fhir/r4/extension/v1_0_1/KBV_EX_ERP_Multiple_Prescription.xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_EX_ERP_Multiple_Prescription",
                    "1.0.1"),
            List.of("/fhir/r4/extension/v1_0_1/KBV_EX_ERP_PracticeSupply_Payor.xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_EX_ERP_PracticeSupply_Payor",
                    "1.0.1"),
            List.of("/fhir/r4/extension/v1_0_1/KBV_EX_ERP_StatusCoPayment.xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_EX_ERP_StatusCoPayment", "1.0.1"),

            List.of("/fhir/r4/extension/v1_0_3/KBV_EX_FOR_Alternative_IK.xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_EX_FOR_Alternative_IK", "1.0.3"),
            List.of("/fhir/r4/extension/v1_0_3/KBV_EX_FOR_Legal_basis.xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_EX_FOR_Legal_basis", "1.0.3"),
            List.of("/fhir/r4/extension/v1_0_3/KBV_EX_FOR_PKV_Tariff.xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_EX_FOR_PKV_Tariff", "1.0.3"),

            List.of("/fhir/r4/extension/v1_1_3/KBV_EX_Base_Terminology_German.xml",
                    "https://fhir.kbv.de/StructureDefinition/KBV_EX_Base_Terminology_German",
                    "1.1.3")
    );
    private static final List<List<String>> valueSets = Arrays.asList(
            List.of("/fhir/r4/valueset/v1_01/KBV_VS_SFHIR_KBV_STATUSKENNZEICHEN.xml",
                    "https://fhir.kbv.de/ValueSet/KBV_VS_SFHIR_KBV_STATUSKENNZEICHEN", "1.01"),

            List.of("/fhir/r4/valueset/v1_02/KBV_VS_SFHIR_KBV_VERSICHERTENSTATUS.xml",
                    "https://fhir.kbv.de/ValueSet/KBV_VS_SFHIR_KBV_VERSICHERTENSTATUS", "1.02"),

            List.of("/fhir/r4/valueset/v1_0_1/KBV_VS_ERP_Accident_Type.xml",
                    "https://fhir.kbv.de/ValueSet/KBV_VS_ERP_Accident_Type", "1.0.1"),
            List.of("/fhir/r4/valueset/v1_0_1/KBV_VS_ERP_Medication_Category.xml",
                    "https://fhir.kbv.de/ValueSet/KBV_VS_ERP_Medication_Category", "1.0.1"),
            List.of("/fhir/r4/valueset/v1_0_1/KBV_VS_ERP_StatusCoPayment.xml",
                    "https://fhir.kbv.de/ValueSet/KBV_VS_ERP_StatusCoPayment", "1.0.1"),

            List.of("/fhir/r4/valueset/v1_0_3/KBV_VS_FOR_Payor_type.xml",
                    "https://fhir.kbv.de/ValueSet/KBV_VS_FOR_Payor_type", "1.0.3"),
            List.of("/fhir/r4/valueset/v1_0_3/KBV_VS_FOR_Qualification_Type.xml",
                    "https://fhir.kbv.de/ValueSet/KBV_VS_FOR_Qualification_Type", "1.0.3")
    );
    private static final List<List<String>> namingSystems = Collections.singletonList(
            List.of("/fhir/r4/namingsystems/Pruefnummer.xml",
                    "https://fhir.kbv.de/NamingSystem/KBV_NS_FOR_Pruefnummer"));
    private static final List<List<String>> codeSystems = Arrays.asList(
            List.of("/fhir/r4/codesystem/v1_00/KBV_CS_SFHIR_ITA_WOP.xml",
                    "https://fhir.kbv.de/CodeSystem/KBV_CS_SFHIR_ITA_WOP", "1.00"),

            List.of("/fhir/r4/codesystem/v1_01/KBV_CS_SFHIR_KBV_STATUSKENNZEICHEN.xml",
                    "https://fhir.kbv.de/CodeSystem/KBV_CS_SFHIR_KBV_STATUSKENNZEICHEN", "1.01"),

            List.of("/fhir/r4/codesystem/v1_02/KBV_CS_SFHIR_KBV_VERSICHERTENSTATUS.xml",
                    "https://fhir.kbv.de/CodeSystem/KBV_CS_SFHIR_KBV_VERSICHERTENSTATUS", "1.02"),
            List.of("/fhir/r4/codesystem/v1_02/KBV_CS_SFHIR_KBV_PERSONENGRUPPE.xml",
                    "https://fhir.kbv.de/CodeSystem/KBV_CS_SFHIR_KBV_PERSONENGRUPPE", "1.02"),

            List.of("/fhir/r4/codesystem/v1_05/KBV_CS_SFHIR_KBV_DMP.xml",
                    "https://fhir.kbv.de/CodeSystem/KBV_CS_SFHIR_KBV_DMP", "1.05"),

            List.of("/fhir/r4/codesystem/v1_0_1/KBV_CS_ERP_Medication_Category.xml",
                    "https://fhir.kbv.de/CodeSystem/KBV_CS_ERP_Medication_Category", "1.0.1"),
            List.of("/fhir/r4/codesystem/v1_0_1/KBV_CS_ERP_Medication_Type.xml",
                    "https://fhir.kbv.de/CodeSystem/KBV_CS_ERP_Medication_Type", "1.0.1"),
            List.of("/fhir/r4/codesystem/v1_0_1/KBV_CS_ERP_Section_Type.xml",
                    "https://fhir.kbv.de/CodeSystem/KBV_CS_ERP_Section_Type", "1.0.1"),
            List.of("/fhir/r4/codesystem/v1_0_1/KBV_CS_ERP_StatusCoPayment.xml",
                    "https://fhir.kbv.de/CodeSystem/KBV_CS_ERP_StatusCoPayment", "1.0.1"),

            List.of("/fhir/r4/codesystem/v1_0_3/KBV_CS_FOR_Payor_Type_KBV.xml",
                    "https://fhir.kbv.de/CodeSystem/KBV_CS_ERP_StatusCoPayment", "1.0.3"),
            List.of("/fhir/r4/codesystem/v1_0_3/KBV_CS_FOR_Qualification_Type.xml",
                    "https://fhir.kbv.de/CodeSystem/KBV_CS_FOR_Qualification_Type", "1.0.3"),
            List.of("/fhir/r4/codesystem/v1_0_3/KBV_CS_FOR_Ursache_Type.xml",
                    "https://fhir.kbv.de/CodeSystem/KBV_CS_FOR_Ursache_Type", "1.0.3")
    );

    public ErePrePopulatedValidationSupport(FhirContext theContext) {
        super(theContext);

        ereLogger.setLoggingContext(systemContextList)
                .info("Loading KBV Validator configuration");
        initValidationConfigs();
//        initKbvValidatorConfiguration();
    }

    protected void addKbvProfile(InputStream configDefinitionInputStream) {
        addKbvProfile(null, null, configDefinitionInputStream);
    }

    protected void addKbvProfile(String configUrl,
                                 String configVersion,
                                 InputStream configDefinitionInputStream) {
        StructureDefinition structureDefinition;

        try (configDefinitionInputStream) {
            structureDefinition = xmlParser.parseResource(StructureDefinition.class,
                    configDefinitionInputStream);
            if (configUrl != null) {
                structureDefinition.setUrl(configUrl);
            } else {
                String canonicalUrl = structureDefinition.getUrl() + "|" +
                        structureDefinition.getVersion();

                ereLogger.setLoggingContext(systemContextList).infof("Configuring canonical " +
                        "url: %s", canonicalUrl);
                structureDefinition.setUrl(canonicalUrl);
            }

            if (configVersion != null) {
                structureDefinition.setVersion(configVersion);
            }

            addStructureDefinition(structureDefinition);
        } catch (IOException e) {
            ereLogger.setLoggingContext(systemContextList)
                    .errorf(e, "Error loading StructureDefinition " +
                            "profile %s", configUrl);
        }
    }

    protected void addKbvValueSet(InputStream configDefinitionInputStream) {
        addKbvValueSet(null, null, configDefinitionInputStream);
    }

    protected void addKbvValueSet(String configUrl, String configVersion,
                                  InputStream configDefinitionInputStream) {

        ValueSet valueSet;

        try (configDefinitionInputStream) {
            valueSet = xmlParser.parseResource(ValueSet.class,
                    configDefinitionInputStream);
            if (configUrl != null) {
                valueSet.setUrl(configUrl);
            }

            if (configVersion != null) {
                valueSet.setVersion(configVersion);
            }

            addValueSet(valueSet);
        } catch (IOException e) {
            ereLogger.setLoggingContext(systemContextList)
                    .errorf(e, "Error loading ValueSet profile %s",
                            configUrl);
        }
    }

    protected void addKbvCodeSystem(InputStream configDefinitionInputStream) {
        addKbvCodeSystem(null, null, configDefinitionInputStream);
    }

    protected void addKbvCodeSystem(String configUrl, String configVersion,
                                    InputStream configDefinitionInputStream) {

        CodeSystem codeSystem;

        try (configDefinitionInputStream) {
            codeSystem = xmlParser.parseResource(CodeSystem.class,
                    configDefinitionInputStream);

            if (configUrl != null) {
                codeSystem.setUrl(configUrl);
            }

            if (configVersion != null) {
                codeSystem.setVersion(configVersion);
            }

            addCodeSystem(codeSystem);
        } catch (IOException e) {
            ereLogger.setLoggingContext(systemContextList)
                    .errorf(e, "Error loading CodeSystem profile %s", configUrl);
        }
    }

    protected void addKbvNamingSystem(InputStream configDefinitionInputStream) {

    }

    protected void initValidationConfigs() {
        // Init Structure Definitions.
        structureDefinitionsAndExtensions.forEach(configList -> {
            String url = configList.get(1);

            addKbvProfile(url, configList.get(2),
                    ErePrePopulatedValidationSupport.class.getResourceAsStream(configList.get(0)));
        });

        // Init value sets.
        valueSets.forEach(configList -> addKbvValueSet(configList.get(1), configList.get(2),
                ErePrePopulatedValidationSupport.class.getResourceAsStream(configList.get(0))));

        // Init code systems
        codeSystems.forEach(configList -> {
            ereLogger.setLoggingContext(systemContextList)
                    .infof("Loading CodeSystem %s", configList.get(0));
            addKbvCodeSystem(configList.get(1), configList.get(2),
                    ErePrePopulatedValidationSupport.class.getResourceAsStream(configList.get(0)));
            ereLogger.setLoggingContext(systemContextList)
                    .infof("Loaded CodeSystem %s", configList.get(0));
        });
    }

    public void initKbvValidatorConfiguration() {
        String kbvValidatorConfigDirectory =
                ConfigProvider.getConfig().getValue("kbv.validator.config.dir", String.class);
        Path start = Path.of(kbvValidatorConfigDirectory).toAbsolutePath();

        try {
            Files.walkFileTree(start, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    applyConfiguration(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            ereLogger.setLoggingContext(systemContextList).fatal(
                    "Error occured while configuring validator", e);
        }
    }

    protected ConfigType getConfigType(Path kbvConfigFile) {
        String configFileName = kbvConfigFile.getFileName().toString();

        if (StringUtils.isNotBlank(configFileName) && configFileName.endsWith(".xml") &&
                (configFileName.contains("ERP") || configFileName.contains("FOR") ||
                configFileName.contains("Base") || configFileName.contains("Profile-") ||
                configFileName.contains("Extension-") || configFileName.contains("ValueSet-") ||
                configFileName.contains("CodeSystem-") || configFileName.contains("SFHIR"))) {
            if (configFileName.startsWith("KBV_PR") || configFileName.startsWith("KBVPR") ||
                    configFileName.startsWith("Profile-")) {
                return ConfigType.PROFILE;
            } else if (configFileName.startsWith("KBV_CS") || configFileName.startsWith("KBVCS") ||
                    configFileName.startsWith("CodeSystem-")) {
                return ConfigType.CODE_SYSTEM;
            } else if (configFileName.startsWith("KBV_EX") || configFileName.startsWith("KBVEX") ||
                    configFileName.startsWith("Extension-")) {
                return ConfigType.EXTENSION;
            } else if (configFileName.startsWith("KBV_VS") || configFileName.startsWith("KBVVS") ||
                    configFileName.startsWith("ValueSet-")) {
                return ConfigType.VALUE_SET;
            } else if (configFileName.startsWith("KBV_NS") || configFileName.startsWith("KBVNS")) {
                return ConfigType.NAMING_SYSTEM;
            }
        }
        return ConfigType.UNKNOWN;
    }

    protected void applyConfiguration(Path kbvConfigFile) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(kbvConfigFile))) {
            switch (getConfigType(kbvConfigFile)) {
                case PROFILE:
                    addKbvProfile(bis);
//                    ereLogger.setLoggingContext(systemContextList)
//                            .infof("Applied profile configuration file: %s",
//                                    kbvConfigFile.getFileName().toString());
                    break;

                case EXTENSION:
                    addKbvProfile(bis);
//                    ereLogger.setLoggingContext(systemContextList)
//                            .infof("Applied extension configuration file: %s",
//                                    kbvConfigFile.getFileName().toString());
                    break;

                case CODE_SYSTEM:
                    addKbvCodeSystem(bis);
//                    ereLogger.setLoggingContext(systemContextList)
//                            .infof("Applied code system configuration file: %s",
//                                    kbvConfigFile.getFileName().toString());
                    break;

                case VALUE_SET:
                    ereLogger.setLoggingContext(systemContextList)
                            .infof("Applying value set configuration file: %s",
                                    kbvConfigFile.getFileName().toString());
                    addKbvValueSet(bis);
                    break;

                case NAMING_SYSTEM: //TODO: Not sure what to do with naming system config file.
//                    ereLogger.setLoggingContext(systemContextList)
//                            .warnf("Ignoring naming system configuration file: %s",
//                                    kbvConfigFile.getFileName().toString());
                    break;

                case UNKNOWN:
//                    ereLogger.setLoggingContext(systemContextList)
//                            .warnf("Will not apply unknown configuration file: %s",
//                                    kbvConfigFile.getFileName().toString());
                    break;
            }
        }
    }
}
