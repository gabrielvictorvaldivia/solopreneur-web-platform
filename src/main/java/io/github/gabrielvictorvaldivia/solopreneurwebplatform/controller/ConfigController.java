package io.github.gabrielvictorvaldivia.solopreneurwebplatform.controller;

import io.github.gabrielvictorvaldivia.solopreneurwebplatform.service.AppConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {

    private final AppConfigurationService configService;

    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentConfigurations() {
        try {
            Map<String, Object> configs = Map.of(
                    "app", configService.getAppConfig(),
                    "business", configService.getBusinessProfile(),
                    "ui", configService.getUiConfig(),
                    "features", configService.getFeatureFlags()
            );

            return ResponseEntity.ok(configs);

        } catch (Exception e) {
            log.error("Erro ao buscar configurações: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/reload")
    public ResponseEntity<String> forceReload() {
        try {
            configService.forceReload();
            return ResponseEntity.ok("Configurações recarregadas com sucesso!");

        } catch (Exception e) {
            log.error("Erro ao recarregar configurações: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body("Erro ao recarregar configurações: " + e.getMessage());
        }
    }

    @GetMapping("/feature/{feature}")
    public ResponseEntity<Boolean> isFeatureEnabled(@PathVariable String feature) {
        boolean enabled = configService.isFeatureEnabled(feature);
        return ResponseEntity.ok(enabled);
    }
}
