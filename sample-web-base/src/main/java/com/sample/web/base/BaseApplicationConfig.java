package com.sample.web.base;

import static com.sample.web.base.WebConst.*;

import java.util.Arrays;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.io.ClassPathResource;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.filter.ForwardedHeaderFilter;
import org.springframework.web.filter.HiddenHttpMethodFilter;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import com.sample.domain.DefaultModelMapperFactory;
import com.sample.domain.dto.common.DefaultPageFactoryImpl;
import com.sample.domain.dto.common.PageFactory;
import com.sample.web.base.aop.*;
import com.sample.web.base.controller.LocalDateConverter;
import com.sample.web.base.controller.LocalDateTimeConverter;
import com.sample.web.base.controller.api.resource.DefaultResourceFactoryImpl;
import com.sample.web.base.controller.api.resource.ResourceFactory;
import com.sample.web.base.filter.CheckOverflowFilter;
import com.sample.web.base.filter.ClearMDCFilter;
import com.sample.web.base.filter.LoginUserTrackingFilter;
import com.sample.web.base.security.authorization.DefaultPermissionKeyResolver;
import com.sample.web.base.security.authorization.PermissionKeyResolver;

import lombok.val;
import nz.net.ultraq.thymeleaf.LayoutDialect;

/**
 * ????????????????????????????????????
 */
public abstract class BaseApplicationConfig
        implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory>, WebMvcConfigurer {

    @Value("${application.cors.allowCredentials:true}")
    Boolean allowCredentials;

    @Value("#{'${application.cors.allowedHeaders:*}'.split(',')}")
    List<String> allowedHeaders;

    @Value("#{'${application.cors.allowedMethods:*}'.split(',')}")
    List<String> allowedMethods;

    @Value("#{'${application.cors.allowedOrigins:*}'.split(',')}")
    List<String> corsAllowedOrigins;

    @Value("${application.cors.maxAge:86400}")
    Long maxAge;

    @Override
    public void customize(ConfigurableServletWebServerFactory container) {
        container.addErrorPages(new ErrorPage(HttpStatus.NOT_FOUND, NOTFOUND_URL));
        container.addErrorPages(new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_URL));
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // ????????????????????????????????????
        registry.addViewController(NOTFOUND_URL).setViewName(NOTFOUND_VIEW);
        registry.addViewController(FORBIDDEN_URL).setViewName(FORBIDDEN_VIEW);
        registry.addViewController(ERROR_URL).setViewName(ERROR_VIEW);
        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new LocalDateConverter(LOCALDATE_FORMAT));
        registry.addConverter(new LocalDateTimeConverter(LOCALDATETIME_FORMAT));
    }

    @Bean
    public ForwardedHeaderFilter forwardedHeaderFilter() {
        // X-Forwarded-XXX?????????????????????????????????????????????????????????
        return new ForwardedHeaderFilter();
    }

    @Bean
    public HiddenHttpMethodFilter hiddenHttpMethodFilter() {
        // hidden?????????????????????????????????HTTP???????????????????????????
        return new HiddenHttpMethodFilter();
    }

    @Bean
    public ShallowEtagHeaderFilter shallowEtagHeaderFilter() {
        // ETag??????????????????
        return new ShallowEtagHeaderFilter();
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        val config = new CorsConfiguration();
        config.setAllowCredentials(allowCredentials);
        config.setAllowedHeaders(allowedHeaders);
        config.setAllowedOrigins(corsAllowedOrigins);
        config.setAllowedMethods(allowedMethods);
        config.setMaxAge(maxAge);

        val source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        val bean = new FilterRegistrationBean<CorsFilter>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }

    @Bean
    public FilterRegistrationBean<LoginUserTrackingFilter> loginUserTrackingFilterBean() {
        val filter = new LoginUserTrackingFilter();
        filter.setExcludeUrlPatterns(Arrays.asList(WEBJARS_URL, STATIC_RESOURCES_URL));

        val bean = new FilterRegistrationBean<LoginUserTrackingFilter>(filter);
        bean.setOrder(Ordered.LOWEST_PRECEDENCE);
        return bean;
    }

    @Bean
    public FilterRegistrationBean<ClearMDCFilter> clearMDCFilterBean() {
        val filter = new ClearMDCFilter();
        val bean = new FilterRegistrationBean<ClearMDCFilter>(filter);
        bean.setOrder(Ordered.LOWEST_PRECEDENCE);
        return bean;
    }

    @Bean
    public FilterRegistrationBean<CheckOverflowFilter> checkOverflowFilterBean() {
        val filter = new CheckOverflowFilter();
        val bean = new FilterRegistrationBean<CheckOverflowFilter>(filter);
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }

    @Bean
    public ModelMapper modelMapper() {
        // ObjectMapping????????????????????????
        return DefaultModelMapperFactory.create();
    }

    @Bean
    public LocaleResolver localeResolver() {
        // Cookie????????????????????????
        val resolver = new CookieLocaleResolver();
        resolver.setCookieName("lang");
        return resolver;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        // lang????????????????????????????????????????????????
        val interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    @Bean
    public LocalValidatorFactoryBean beanValidator(MessageSource messageSource) {
        val bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource);
        return bean;
    }

    @Bean
    public LayoutDialect layoutDialect() {
        return new LayoutDialect();
    }

    @Bean
    public CacheManager cacheManager() {
        val manager = new EhCacheCacheManager();
        manager.setCacheManager(ehcache().getObject());
        return manager;
    }

    @Bean
    public EhCacheManagerFactoryBean ehcache() {
        val ehcache = new EhCacheManagerFactoryBean();
        ehcache.setConfigLocation(new ClassPathResource("ehcache.xml"));
        return ehcache;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // webjars???ResourceHandler???????????????
        registry.addResourceHandler(WEBJARS_URL)
                // JAR???????????????????????????????????????????????????
                .addResourceLocations("classpath:/META-INF/resources/webjars/")
                // webjars-locator?????????????????????????????????????????????????????????????????????????????????
                .resourceChain(false);
    }

    @Bean
    public RequestTrackingInterceptor requestTrackingInterceptor() {
        // MDC???ID?????????????????????????????????????????????????????????
        return new RequestTrackingInterceptor();
    }

    @Bean
    public LoggingFunctionNameInterceptor loggingFunctionNameInterceptor() {
        // MDC????????????????????????????????????????????????
        return new LoggingFunctionNameInterceptor();
    }

    @Bean
    public SetAuditInfoInterceptor setAuditInfoInterceptor() {
        // ???????????????????????????????????????DB????????????????????????
        return new SetAuditInfoInterceptor();
    }

    @Bean
    public SetDoubleSubmitCheckTokenInterceptor setDoubleSubmitCheckTokenInterceptor() {
        // ?????????????????????????????????
        return new SetDoubleSubmitCheckTokenInterceptor();
    }

    @Bean
    public SetModelAndViewInterceptor setModelAndViewInterceptor() {
        // ????????????????????????????????????????????????????????????
        return new SetModelAndViewInterceptor();
    }

    @Bean
    public PermissionKeyResolver permissionKeyResolver() {
        // ????????????????????????????????????????????????????????????????????????
        return new DefaultPermissionKeyResolver();
    }

    @Bean
    public AuthorizationInterceptor authorizationInterceptor() {
        // ????????????????????????????????????????????????
        return new AuthorizationInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
        registry.addInterceptor(requestTrackingInterceptor());
        registry.addInterceptor(loggingFunctionNameInterceptor());
        registry.addInterceptor(setAuditInfoInterceptor());
        registry.addInterceptor(setDoubleSubmitCheckTokenInterceptor());
        registry.addInterceptor(setModelAndViewInterceptor());
        registry.addInterceptor(authorizationInterceptor());
    }

    @Bean
    public SnakeToLowerCamelCaseModelAttributeMethodProcessor attributeMethodProcessor() {
        // login_id ?????????????????? loginId ????????????????????????
        return new SnakeToLowerCamelCaseModelAttributeMethodProcessor(true);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(attributeMethodProcessor());
    }

    @Bean
    public PageFactory pageFactory() {
        return new DefaultPageFactoryImpl();
    }

    @Bean
    public ResourceFactory resourceFactory() {
        return new DefaultResourceFactoryImpl();
    }
}
