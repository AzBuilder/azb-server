package org.azbuilder.api.plugin.security.groups;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@PropertySources({
        @PropertySource(value = "classpath:application.properties", ignoreResourceNotFound = true),
        @PropertySource(value = "classpath:application-${spring.profiles.active}.properties", ignoreResourceNotFound = true)
})
@ConfigurationProperties(prefix = "org.azbuilder.api.groups")
public class GroupServiceProperties {
    private GroupType type;
}

enum GroupType {
    AzureAd,
    Local
}
