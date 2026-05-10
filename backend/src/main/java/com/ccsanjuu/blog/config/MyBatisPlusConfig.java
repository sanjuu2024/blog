package com.ccsanjuu.blog.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        // 1. 创建MyBatis-Plus拦截器
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 2. 创建分页插件
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor(DbType.POSTGRE_SQL);
        paginationInnerInterceptor.setMaxLimit(100L);  // 设置分页上限

        // 3. 添加分页插件
        interceptor.addInnerInterceptor(paginationInnerInterceptor);

        return interceptor;   // 配置类返回MybatisPlusInterceptor对象，@Bean注解会将其注册到Spring容器中，MyBatisPlus就会使用这个配置了分页功能的拦截器。
    }
}
