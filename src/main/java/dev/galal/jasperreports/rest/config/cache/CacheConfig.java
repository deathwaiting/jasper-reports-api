package dev.galal.jasperreports.rest.config.cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String REPORTS_CACHE = "reports";

    @Value("${dev.galal.dev.galal.jasper-rest-server.reports.cache.ttl:1440}")
    private int reportCacheTTL;

    @Value("${dev.galal.dev.galal.jasper-rest-server.reports.cache.size:512}")
    private int reportCacheSize;

    private  static Caffeine caffeineConfig(int reportCacheTTL, int size) {
        return Caffeine.newBuilder()
                .maximumSize(size)
                .expireAfterWrite(reportCacheTTL, TimeUnit.MINUTES);
    }

    @Bean
    public CacheManager myCacheManager() {
        var manager = new CaffeineCacheManager();
        manager.setCaffeine(caffeineConfig(reportCacheTTL, reportCacheSize));
        return manager;
    }
}

