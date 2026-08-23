package com.psycorp.psychapi.feature.auth.model;

import java.time.Instant;

public class DeviceInfo {
    
    // Browser Info
    private String userAgent;
    private String browser;
    private String browserVersion;
    
    // OS Info
    private String os;
    private String osVersion;
    
    // Network Info (masked untuk privacy)
    private String ip;
    private String location;
    private String timezone;
    
    // Last Active
    private Instant lastActive;
    
    // Constructors
    public DeviceInfo() {}
    
    public DeviceInfo(String userAgent, String browser, String browserVersion,
                      String os, String osVersion, String ip, String location,
                      String timezone, Instant lastActive) {
        this.userAgent = userAgent;
        this.browser = browser;
        this.browserVersion = browserVersion;
        this.os = os;
        this.osVersion = osVersion;
        this.ip = ip;
        this.location = location;
        this.timezone = timezone;
        this.lastActive = lastActive;
    }
    
    // Getters
    public String getUserAgent() { return userAgent; }
    public String getBrowser() { return browser; }
    public String getBrowserVersion() { return browserVersion; }
    public String getOs() { return os; }
    public String getOsVersion() { return osVersion; }
    public String getIp() { return ip; }
    public String getLocation() { return location; }
    public String getTimezone() { return timezone; }
    public Instant getLastActive() { return lastActive; }
    
    // Setters
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public void setBrowser(String browser) { this.browser = browser; }
    public void setBrowserVersion(String browserVersion) { this.browserVersion = browserVersion; }
    public void setOs(String os) { this.os = os; }
    public void setOsVersion(String osVersion) { this.osVersion = osVersion; }
    public void setIp(String ip) { this.ip = ip; }
    public void setLocation(String location) { this.location = location; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public void setLastActive(Instant lastActive) { this.lastActive = lastActive; }
    
    // Builder pattern (optional, untuk convenience)
    public static DeviceInfoBuilder builder() {
        return new DeviceInfoBuilder();
    }
    
    public static class DeviceInfoBuilder {
        private String userAgent;
        private String browser;
        private String browserVersion;
        private String os;
        private String osVersion;
        private String ip;
        private String location;
        private String timezone;
        private Instant lastActive;
        
        public DeviceInfoBuilder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }
        
        public DeviceInfoBuilder browser(String browser) {
            this.browser = browser;
            return this;
        }
        
        public DeviceInfoBuilder browserVersion(String browserVersion) {
            this.browserVersion = browserVersion;
            return this;
        }
        
        public DeviceInfoBuilder os(String os) {
            this.os = os;
            return this;
        }
        
        public DeviceInfoBuilder osVersion(String osVersion) {
            this.osVersion = osVersion;
            return this;
        }
        
        public DeviceInfoBuilder ip(String ip) {
            this.ip = ip;
            return this;
        }
        
        public DeviceInfoBuilder location(String location) {
            this.location = location;
            return this;
        }
        
        public DeviceInfoBuilder timezone(String timezone) {
            this.timezone = timezone;
            return this;
        }
        
        public DeviceInfoBuilder lastActive(Instant lastActive) {
            this.lastActive = lastActive;
            return this;
        }
        
        public DeviceInfo build() {
            return new DeviceInfo(userAgent, browser, browserVersion, os, osVersion, ip, location, timezone, lastActive);
        }
    }
}
