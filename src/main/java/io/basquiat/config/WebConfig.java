package io.basquiat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.ViewResolverRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.view.freemarker.FreeMarkerConfigurer;

/**
 * created by basquiat
 */
@Configuration
@EnableWebFlux
public class WebConfig implements WebFluxConfigurer {

    /**
     * cors setup
     * @param registry
     */
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedMethods("*");
    }

    /**
     * freemarker view resolver
     * @param registry
     */
    public void configureViewResolvers(ViewResolverRegistry registry) {
        registry.freeMarker();
    }

    /**
     * freemarker config
     * @return FreeMarkerConfigurer
     */
    @Bean
    public FreeMarkerConfigurer freeMarkerConfigurer() {
        FreeMarkerConfigurer freeMakerConfig = new FreeMarkerConfigurer();
        freeMakerConfig.setTemplateLoaderPath("classpath:/templates");
        return freeMakerConfig;
    }

}
