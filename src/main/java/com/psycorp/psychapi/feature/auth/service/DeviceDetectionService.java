package com.psycorp.psychapi.feature.auth.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.time.Instant;

import org.jboss.logging.Logger;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.psycorp.psychapi.feature.auth.model.DeviceInfo;

import io.vertx.core.http.HttpServerRequest;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.HttpHeaders;

@ApplicationScoped
public class DeviceDetectionService {

    private static final Logger log = Logger.getLogger(DeviceDetectionService.class);
    private static final String UNKNOWN = "Unknown";
    private static final String UNKNOWN_LOCATION = UNKNOWN + " Location";
    
    private DatabaseReader dbReader;
    private volatile boolean geoIpInitialized = false;

    @PostConstruct
    public void init() {
        try {
            InputStream dbStream = getClass().getClassLoader().getResourceAsStream("geoip/GeoLite2-City.mmdb");
            if (dbStream != null) {
                dbReader = new DatabaseReader.Builder(dbStream).build();
                geoIpInitialized = true;
                log.info("✅ GeoIP2 Database loaded successfully");
            } else {
                log.warn("⚠️ GeoLite2-City.mmdb not found in resources. Location detection will be disabled.");
            }
        } catch (IOException e) {
            log.error("❌ Failed to read GeoIP2 database", e);
        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid GeoIP2 database format", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (dbReader != null) {
                dbReader.close();
            }
        } catch (IOException e) {
            log.error("Failed to close GeoIP2 reader", e);
        }
    }

    public DeviceInfo extractDeviceInfo(HttpHeaders headers, HttpServerRequest request) {
        String userAgent = headers.getHeaderString("User-Agent");
        String ipAddress = extractIpAddress(headers, request);
        
        String timezone = headers.getHeaderString("X-Timezone");
        String location = extractLocation(ipAddress);
        
        String[] parsedUa = parseUserAgent(userAgent);

        return new DeviceInfo.DeviceInfoBuilder()
            .userAgent(userAgent)
            .browser(parsedUa[0])
            .browserVersion(parsedUa[1])
            .os(parsedUa[2])
            .osVersion(parsedUa[3])
            .ip(ipAddress)
            .location(location)
            .timezone(timezone) 
            .lastActive(Instant.now())
            .build();
    }

    private String extractIpAddress(HttpHeaders headers, HttpServerRequest request) {
        String xForwardedFor = headers.getHeaderString("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = headers.getHeaderString("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp.trim();
        }
        
        if (request != null && request.remoteAddress() != null) {
            return request.remoteAddress().hostAddress();
        }
        
        return UNKNOWN;
    }

    private String extractLocation(String ip) {
        if (!geoIpInitialized || UNKNOWN.equals(ip) || "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return UNKNOWN_LOCATION;
        }
        try {
            InetAddress ipAddress = InetAddress.getByName(ip);
            CityResponse response = dbReader.city(ipAddress);
            
            String city = response.getCity().getName();
            String country = response.getCountry().getName();
            
            if (city != null && country != null) return city + ", " + country;
            if (country != null) return country;
            
        } catch (IOException | IllegalArgumentException | GeoIp2Exception e) {
            log.debugf("Could not resolve location for IP: %s - %s", ip, e.getMessage());
        }
        return UNKNOWN_LOCATION;
    }

    // ✅ Method-method ini HARUS ada (mungkin terhapus)
    private String[] parseUserAgent(String userAgent) {
        String browser = UNKNOWN;
        String browserVersion = UNKNOWN;
        String os = UNKNOWN;
        String osVersion = UNKNOWN;
        
        if (userAgent != null && !userAgent.isEmpty()) {
            String ua = userAgent.toLowerCase();
            
            // Browser detection
            if (ua.contains("firefox")) {
                browser = "Firefox";
                browserVersion = extractVersion(ua, "firefox/");
            } else if (ua.contains("chrome") && !ua.contains("edg")) {
                browser = "Chrome";
                browserVersion = extractVersion(ua, "chrome/");
            } else if (ua.contains("safari") && !ua.contains("chrome")) {
                browser = "Safari";
                browserVersion = extractVersion(ua, "version/");
            } else if (ua.contains("edg")) {
                browser = "Edge";
                browserVersion = extractVersion(ua, "edg/");
            } else if (ua.contains("msie") || ua.contains("trident")) {
                browser = "Internet Explorer";
                browserVersion = extractVersion(ua, "msie ");
            }
            
            // OS detection
            if (ua.contains("windows")) {
                os = "Windows";
                osVersion = extractWindowsVersion(ua);
            } else if (ua.contains("mac os")) {
                os = "macOS";
                osVersion = extractVersion(ua, "mac os x ");
            } else if (ua.contains("linux")) {
                os = "Linux";
                osVersion = UNKNOWN;
            } else if (ua.contains("android")) {
                os = "Android";
                osVersion = extractVersion(ua, "android ");
            } else if (ua.contains("ios") || ua.contains("iphone") || ua.contains("ipad")) {
                os = "iOS";
                osVersion = extractVersion(ua, "os ");
            }
        }
        
        return new String[]{browser, browserVersion, os, osVersion};
    }

    private String extractVersion(String userAgent, String prefix) {
        int start = userAgent.indexOf(prefix);
        if (start == -1) return UNKNOWN;
        start += prefix.length();
        int end = userAgent.indexOf(" ", start);
        if (end == -1) end = userAgent.length();
        return userAgent.substring(start, end).replace("_", ".");
    }

    private String extractWindowsVersion(String userAgent) {
        if (userAgent.contains("windows nt 10.0")) return "10";
        if (userAgent.contains("windows nt 6.3")) return "8.1";
        if (userAgent.contains("windows nt 6.2")) return "8";
        if (userAgent.contains("windows nt 6.1")) return "7";
        return UNKNOWN;
    }
}
