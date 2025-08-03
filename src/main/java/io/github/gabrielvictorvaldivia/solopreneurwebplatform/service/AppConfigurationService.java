package io.github.gabrielvictorvaldivia.solopreneurwebplatform.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.gabrielvictorvaldivia.solopreneurwebplatform.model.AppConfig;
import io.github.gabrielvictorvaldivia.solopreneurwebplatform.model.BusinessProfile;
import io.github.gabrielvictorvaldivia.solopreneurwebplatform.model.FeatureFlags;
import io.github.gabrielvictorvaldivia.solopreneurwebplatform.model.UiConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("SpellCheckingInspection")
@Slf4j
@Service
@Getter
@RequiredArgsConstructor
public class AppConfigurationService {

    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("classpath:config/app-config.json")
    private Resource appConfigResource;

    @Value("classpath:config/business-config.json")
    private Resource businessProfileResource;

    @Value("classpath:config/ui-config.json")
    private Resource uiConfigResource;

    @Value("classpath:config/feature-flags.json")
    private Resource featureFlagsResource;

    // Configurações em memória
    private AppConfig appConfig;
    private BusinessProfile businessProfile;
    private UiConfig uiConfig;
    private FeatureFlags featureFlags;

    // Observador de configurações
    private WatchService watchService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);;
    private final ConcurrentHashMap<String, Long> lastModified = new ConcurrentHashMap<>();

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        try {
            // Configurar ObjectMapper
            objectMapper.registerModule(new JavaTimeModule());

            // Carregar configurações iniciais
            loadAllConfigurations();

            // Iniciar monitoramento de arquivos
            startFileWatcher();

            log.info("✅ Configurações carregadas e monitoramento iniciado!");

        } catch (Exception e) {
            log.error("❌ Erro ao inicializar ConfigurationService: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao inicializar configurações", e);
        }
    }

    private void loadAllConfigurations() throws IOException {
        log.info("🔄 Carregando todas as configurações...");

        this.appConfig = loadConfig(appConfigResource, AppConfig.class);
        this.businessProfile = loadConfig(businessProfileResource, BusinessProfile.class);
        this.uiConfig = loadConfig(uiConfigResource, UiConfig.class);
        this.featureFlags = loadConfig(featureFlagsResource, FeatureFlags.class);

        // Atualizar timestamps
        updateLastModified();

        log.info("✅ Configurações carregadas com sucesso!");
    }

    private <T> T loadConfig(Resource resource, Class<T> configClass) throws IOException {
        if (!resource.exists()) {
            throw new IOException("Arquivo de configuração não encontrado: " + resource.getFilename());
        }

        return objectMapper.readValue(resource.getInputStream(), configClass);
    }

    @Async
    public void startFileWatcher() {
        try {
            watchService = FileSystems.getDefault().newWatchService();

            // Registrar diretório config para monitoramento
            Path configDir = Paths.get("src/main/resources/config");
            if (!Files.exists(configDir)) {
                configDir = Paths.get("config"); // Fallback para produção
            }

            if (Files.exists(configDir)) {
                configDir.register(watchService,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_CREATE);

                log.info("👁️ Monitorando alterações em: {}", configDir.toAbsolutePath());

                // Executar em thread separada
                scheduler.execute(this::watchForChanges);
            } else {
                log.warn("⚠️ Diretório de configurações não encontrado para monitoramento");
                // Fallback: verificação periódica
                scheduler.scheduleAtFixedRate(this::checkForChangesPolling, 5, 5, TimeUnit.SECONDS);
            }

        } catch (IOException e) {
            log.error("❌ Erro ao iniciar file watcher: {}", e.getMessage(), e);
            // Fallback: verificação periódica
            scheduler.scheduleAtFixedRate(this::checkForChangesPolling, 5, 5, TimeUnit.SECONDS);
        }
    }

    private void watchForChanges() {
        try {
            while (true) {
                WatchKey key = watchService.take(); // Bloqueia até haver mudança

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filename = ev.context();

                    String fileName = filename.toString();
                    log.info("🔔 Arquivo modificado: {}", fileName);

                    // Processar apenas arquivos JSON de configuração
                    if (isConfigFile(fileName)) {
                        // Aguardar um pouco para garantir que o arquivo foi completamente escrito
                        Thread.sleep(500);
                        reloadConfiguration(fileName);
                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    log.warn("⚠️ WatchKey não é mais válido, reiniciando monitoramento...");
                    break;
                }
            }
        } catch (InterruptedException e) {
            log.info("📴 File watcher interrompido");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("❌ Erro no file watcher: {}", e.getMessage(), e);
        }
    }

    private void checkForChangesPolling() {
        try {
            boolean hasChanges = false;

            // Verificar cada arquivo
            if (hasFileChanged(appConfigResource, "app-config.json")) {
                reloadConfiguration("app-config.json");
                hasChanges = true;
            }

            if (hasFileChanged(businessProfileResource, "business-config.json")) {
                reloadConfiguration("business-config.json");
                hasChanges = true;
            }

            if (hasFileChanged(uiConfigResource, "ui-config.json")) {
                reloadConfiguration("ui-config.json");
                hasChanges = true;
            }

            if (hasFileChanged(featureFlagsResource, "feature-flags.json")) {
                reloadConfiguration("feature-flags.json");
                hasChanges = true;
            }

            if (hasChanges) {
                log.debug("✅ Verificação periódica concluída - alterações detectadas");
            }

        } catch (Exception e) {
            log.error("❌ Erro na verificação periódica: {}", e.getMessage(), e);
        }
    }

    private boolean hasFileChanged(Resource resource, String fileName) {
        try {
            long currentModified = resource.getFile().lastModified();
            Long previousModified = lastModified.get(fileName);

            if (previousModified == null || currentModified > previousModified) {
                lastModified.put(fileName, currentModified);
                return previousModified != null; // Não considerar mudança na primeira verificação
            }

        } catch (IOException e) {
            log.debug("Erro ao verificar modificação do arquivo {}: {}", fileName, e.getMessage());
        }

        return false;
    }

    private void updateLastModified() {
        try {
            lastModified.put("app-config.json", appConfigResource.getFile().lastModified());
            lastModified.put("business-config.json", businessProfileResource.getFile().lastModified());
            lastModified.put("ui-config.json", uiConfigResource.getFile().lastModified());
            lastModified.put("feature-flags.json", featureFlagsResource.getFile().lastModified());
        } catch (IOException e) {
            log.debug("Erro ao atualizar timestamps: {}", e.getMessage());
        }
    }

    private boolean isConfigFile(String fileName) {
        return fileName.equals("app-config.json") ||
                fileName.equals("business-config.json") ||
                fileName.equals("ui-config.json") ||
                fileName.equals("feature-flags.json");
    }

    @Async
    public void reloadConfiguration(String fileName) {
        try {
            log.info("🔄 Recarregando configuração: {}", fileName);

            Object oldConfig = null;
            Object newConfig = null;
            String configType = null;

            switch (fileName) {
                case "app-config.json":
                    oldConfig = this.appConfig;
                    this.appConfig = loadConfig(appConfigResource, AppConfig.class);
                    newConfig = this.appConfig;
                    configType = "app";
                    break;

                case "business-config.json":
                    oldConfig = this.businessProfile;
                    this.businessProfile = loadConfig(businessProfileResource, BusinessProfile.class);
                    newConfig = this.businessProfile;
                    configType = "business";
                    break;

                case "ui-config.json":
                    oldConfig = this.uiConfig;
                    this.uiConfig = loadConfig(uiConfigResource, UiConfig.class);
                    newConfig = this.uiConfig;
                    configType = "ui";
                    break;

                case "feature-flags.json":
                    oldConfig = this.featureFlags;
                    this.featureFlags = loadConfig(featureFlagsResource, FeatureFlags.class);
                    newConfig = this.featureFlags;
                    configType = "features";
                    break;

                default:
                    log.warn("⚠️ Arquivo desconhecido: {}", fileName);
                    return;
            }

            // Publicar evento interno para outros serviços
            eventPublisher.publishEvent(new ConfigurationChangedEvent(configType, oldConfig, newConfig));

            // Notificar clientes via WebSocket
            notifyClients(configType, newConfig);

            log.info("✅ Configuração {} recarregada com sucesso!", fileName);

        } catch (Exception e) {
            log.error("❌ Erro ao recarregar {}: {}", fileName, e.getMessage(), e);
        }
    }

    private void notifyClients(String configType, Object newConfig) {
        try {
            // Enviar para todos os clientes conectados
            messagingTemplate.convertAndSend("/topic/config-updates",
                    new ConfigUpdateMessage(configType, newConfig, Instant.now().toEpochMilli()));

            log.info("📢 Clientes notificados sobre mudança em: {}", configType);

        } catch (Exception e) {
            log.error("❌ Erro ao notificar clientes: {}", e.getMessage(), e);
        }
    }

    public boolean isConfigurationLoaded() {
        return appConfig != null && businessProfile != null &&
                uiConfig != null && featureFlags != null;
    }

    // ===== MÉTODOS UTILITÁRIOS (mesmos de antes) =====

    public boolean isFeatureEnabled(String module) {
        FeatureFlags flags = this.featureFlags; // Snapshot local
        if (flags == null) return false;

        switch (module.toLowerCase()) {
            case "invoicing":
                return flags.getFeatures().getModules().isInvoicing();
            case "timetracking":
                return flags.getFeatures().getModules().isTimeTracking();
            case "clientmanagement":
                return flags.getFeatures().getModules().isClientManagement();
            case "analytics":
                return flags.getFeatures().getModules().isAnalytics();
            case "integrations":
                return flags.getFeatures().getModules().isIntegrations();
            default:
                return false;
        }
    }

    public String getCompanyDisplayName() {
        BusinessProfile profile = this.businessProfile; // Snapshot local
        return profile != null && profile.getContacts() != null
                ? profile.getContacts().getBusiness().getCompanyName()
                : "Empresa";
    }

    public String getPrimaryColor() {
        UiConfig ui = this.uiConfig; // Snapshot local
        return ui != null && ui.getBranding() != null
                ? ui.getBranding().getPrimaryColor()
                : "#007bff";
    }

    // Método para forçar recarga manual (útil para admin)
    public void forceReload() {
        try {
            loadAllConfigurations();
            notifyClients("all", getAllConfigurations());
            log.info("✅ Recarga manual executada com sucesso!");
        } catch (Exception e) {
            log.error("❌ Erro na recarga manual: {}", e.getMessage(), e);
            throw new RuntimeException("Erro na recarga manual", e);
        }
    }

    private ConfigurationSnapshot getAllConfigurations() {
        return new ConfigurationSnapshot(appConfig, businessProfile, uiConfig, featureFlags);
    }

    // Cleanup no shutdown
    public void destroy() {
        try {
            if (watchService != null) {
                watchService.close();
            }
            scheduler.shutdown();
            log.info("📴 ConfigurationService finalizado");
        } catch (IOException e) {
            log.error("Erro ao finalizar ConfigurationService: {}", e.getMessage());
        }
    }

    // Snapshot of all configurations
    @Getter
    @AllArgsConstructor
    static class ConfigurationSnapshot {
        private final AppConfig appConfig;
        private final BusinessProfile businessProfile;
        private final UiConfig uiConfig;
        private final FeatureFlags featureFlags;
    }

    // Event to notify other services about changes
    @Getter
    @AllArgsConstructor
    static class ConfigurationChangedEvent {
        private final String configType;
        private final Object oldConfig;
        private final Object newConfig;
    }

    // Message to WebSocket
    @Getter
    @AllArgsConstructor
    static class ConfigUpdateMessage {
        private final String type;
        private final Object config;
        private final long timestamp;
    }
}
