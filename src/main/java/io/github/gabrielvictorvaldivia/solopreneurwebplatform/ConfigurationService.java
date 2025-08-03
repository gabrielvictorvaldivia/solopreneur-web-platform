package io.github.gabrielvictorvaldivia.solopreneurwebplatform;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.gabrielvictorvaldivia.solopreneurwebplatform.model.AppConfig;
import io.github.gabrielvictorvaldivia.solopreneurwebplatform.model.BusinessProfile;
import io.github.gabrielvictorvaldivia.solopreneurwebplatform.model.FeatureFlags;
import io.github.gabrielvictorvaldivia.solopreneurwebplatform.model.UiConfig;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
@Getter
@RequiredArgsConstructor
public class ConfigurationService {

    private final ObjectMapper objectMapper;

    @Value("classpath:config/app-config.json")
    private Resource appConfigResource;

    @Value("classpath:config/business-config.json")
    private Resource businessProfileResource;

    @Value("classpath:config/ui-config.json")
    private Resource uiConfigResource;

    @Value("classpath:config/feature-flags.json")
    private Resource featureFlagsResource;

    private AppConfig appConfig;
    private BusinessProfile businessProfile;
    private UiConfig uiConfig;
    private FeatureFlags featureFlags;

    @EventListener(ApplicationReadyEvent.class)
    public void loadConfiguration() {
        try {
            log.info("Carregando configurações da aplicação...");

            // Configurar ObjectMapper para lidar com LocalDateTime
            objectMapper.registerModule(new JavaTimeModule());

            this.appConfig = loadConfig(appConfigResource, AppConfig.class);
            this.businessProfile = loadConfig(businessProfileResource, BusinessProfile.class);
            this.uiConfig = loadConfig(uiConfigResource, UiConfig.class);
            this.featureFlags = loadConfig(featureFlagsResource, FeatureFlags.class);

            log.info("Configurações carregadas com sucesso!");
            logConfigurationSummary();

        } catch (IOException e) {
            log.error("Erro ao carregar configurações: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao inicializar configurações", e);
        }
    }

    private <T> T loadConfig(Resource resource, Class<T> configClass) throws IOException {
        if (!resource.exists()) {
            throw new IOException("Arquivo de configuração não encontrado: " + resource.getFilename());
        }

        return objectMapper.readValue(resource.getInputStream(), configClass);
    }

    private void logConfigurationSummary() {
        log.info("=== Resumo das Configurações ===");
        log.info("Projeto: {} v{}",
                appConfig.getSystem().getProjectName(),
                appConfig.getSystem().getVersion());
        log.info("Ambiente: {}", appConfig.getSystem().getEnvironment());
        log.info("Empresa: {}", businessProfile.getContacts().getBusiness().getCompanyName());
        log.info("Tema: {}", uiConfig.getPreferences().getTheme());
        log.info("Módulos ativos: Invoicing={}, Analytics={}, TimeTracking={}",
                featureFlags.getFeatures().getModules().isInvoicing(),
                featureFlags.getFeatures().getModules().isAnalytics(),
                featureFlags.getFeatures().getModules().isTimeTracking());
    }

    public boolean isConfigurationLoaded() {
        return appConfig != null && businessProfile != null &&
                uiConfig != null && featureFlags != null;
    }

    public boolean isFeatureEnabled(String module) {
        if (featureFlags == null) return false;

        switch (module.toLowerCase()) {
            case "invoicing":
                return featureFlags.getFeatures().getModules().isInvoicing();
            case "timetracking":
                return featureFlags.getFeatures().getModules().isTimeTracking();
            case "clientmanagement":
                return featureFlags.getFeatures().getModules().isClientManagement();
            case "analytics":
                return featureFlags.getFeatures().getModules().isAnalytics();
            case "integrations":
                return featureFlags.getFeatures().getModules().isIntegrations();
            default:
                return false;
        }
    }

    public boolean isBetaFeatureEnabled(String feature) {
        if (featureFlags == null) return false;

        switch (feature.toLowerCase()) {
            case "newdashboard":
                return featureFlags.getFeatures().getBeta().isNewDashboard();
            case "advancedreports":
                return featureFlags.getFeatures().getBeta().isAdvancedReports();
            case "aiassistant":
                return featureFlags.getFeatures().getBeta().isAiAssistant();
            default:
                return false;
        }
    }

    public String getCompanyDisplayName() {
        return businessProfile != null && businessProfile.getContacts() != null
                ? businessProfile.getContacts().getBusiness().getCompanyName()
                : "Empresa";
    }

    public String getPrimaryColor() {
        return uiConfig != null && uiConfig.getBranding() != null
                ? uiConfig.getBranding().getPrimaryColor()
                : "#007bff";
    }

    public String getCurrentTheme() {
        return uiConfig != null && uiConfig.getPreferences() != null
                ? uiConfig.getPreferences().getTheme()
                : "light";
    }

    public boolean isDevelopmentEnvironment() {
        return appConfig != null && appConfig.getSystem() != null
                && "development".equalsIgnoreCase(appConfig.getSystem().getEnvironment());
    }
}
