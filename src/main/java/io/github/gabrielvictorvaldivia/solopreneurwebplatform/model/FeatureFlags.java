package io.github.gabrielvictorvaldivia.solopreneurwebplatform.model;

import lombok.Data;

@Data
public class FeatureFlags {
    private Features features;
    private Permissions permissions;

    @Data
    public static class Features {
        private Beta beta;
        private Experimental experimental;
        private Modules modules;
    }

    @Data
    public static class Beta {
        private boolean newDashboard;
        private boolean advancedReports;
        private boolean aiAssistant;
    }

    @Data
    public static class Experimental {
        private boolean darkModeV2;
        private boolean realTimeSync;
        private boolean voiceCommands;
    }

    @Data
    public static class Modules {
        private boolean invoicing;
        private boolean timeTracking;
        private boolean clientManagement;
        private boolean analytics;
        private boolean integrations;
    }

    @Data
    public static class Permissions {
        private boolean canExportData;
        private boolean canModifySettings;
        private boolean canAccessBetaFeatures;
    }
}
