package io.github.gabrielvictorvaldivia.solopreneurwebplatform.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppConfig {
    private System system;
    private Features features;

    @Data
    public static class System {
        private String projectName;
        private String projectDescription;
        private String version;
        private String environment;
        private LocalDateTime lastUpdated;
    }

    @Data
    public static class Features {
        private Notifications notifications;
        private Dashboard dashboard;
    }

    @Data
    public static class Notifications {
        private boolean email;
        private boolean push;
        private boolean sms;
    }

    @Data
    public static class Dashboard {
        private String defaultView;
        private boolean showMetrics;
        private int autoRefresh;
    }
}