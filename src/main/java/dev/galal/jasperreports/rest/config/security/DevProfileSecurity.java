package dev.galal.jasperreports.rest.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
@Profile({"dev", "security-off"})
public class DevProfileSecurity {

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers(PathPatternRequestMatcher.pathPattern("/**"));
    }
}