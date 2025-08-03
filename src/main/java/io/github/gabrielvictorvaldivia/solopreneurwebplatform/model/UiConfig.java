package io.github.gabrielvictorvaldivia.solopreneurwebplatform.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class UiConfig {
    private Preferences preferences;
    private Branding branding;
    private Layout layout;
    private Themes themes;

    @Data
    public static class Preferences {
        private String theme;
        private String language;
        private String timezone;
        private String dateFormat;
        private String currency;
    }

    @Data
    public static class Branding {
        private String primaryColor;
        private String secondaryColor;
        private String logo;
        private String favicon;
    }

    @Data
    public static class Layout {
        private boolean sidebarCollapsed;
        private boolean headerFixed;
        private boolean footerVisible;
        private boolean breadcrumbsEnabled;
    }

    @Data
    public static class Themes {
        private List<String> available;
        @JsonProperty("default")
        private String defaultTheme;
        private String customCss;
    }
}
