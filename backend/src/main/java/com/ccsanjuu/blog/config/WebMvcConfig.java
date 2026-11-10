package com.ccsanjuu.blog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(
                "/api/v1",
                HandlerTypePredicate.forBasePackage("com.ccsanjuu.blog.modules")
                        .and(HandlerTypePredicate.forAnnotation(RestController.class))
        );
    }
}
