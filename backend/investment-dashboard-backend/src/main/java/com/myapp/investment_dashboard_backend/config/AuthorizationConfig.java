package com.myapp.investment_dashboard_backend.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app.security")
@Getter
public class AuthorizationConfig {
    
    private boolean enable = true;
    private List<UrlToRoleMapping> urlToRoleList = new ArrayList<>();

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public void setUrlToRoleList(List<UrlToRoleMapping> urlToRoleList) {
        this.urlToRoleList = urlToRoleList;
    }

    @Getter
    public static class UrlToRoleMapping {
        private String url;
        private String method;
        private List<String> roles;

        public void setUrl(String url) {
            this.url = url;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }
    }
} 