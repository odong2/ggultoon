package com.architecture.admin.libraries.db;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
/*****************************************************
 * MAIN DB 접속 설정
 ****************************************************/
@Configuration
@MapperScan(basePackages={"com.architecture.admin.models.dao"},sqlSessionFactoryRef="dbmainSqlSessionFactory")
@EnableTransactionManagement
public class ConfigurationDBMainLibrary {
    // application.properties 설정 prefix 값 확인
    @Bean(name="dbmainDataSource")
    @Primary
    @ConfigurationProperties(prefix="spring.datasource.main")
    public DataSource dbmainDataSource() {
        return DataSourceBuilder.create().build();

    }

    @Bean(name="dbmainSqlSessionFactory")
    @Primary
    public SqlSessionFactory sqlSessionFactory(@Qualifier("dbmainDataSource") DataSource dbmainDataSource, ApplicationContext applicationContext) throws Exception{
        final SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dbmainDataSource);
        sessionFactory.setMapperLocations(applicationContext.getResources("classpath:/mybatis/mapper/*/*.xml"));

        sessionFactory.setConfigLocation(applicationContext.getResource("classpath:/mybatis/mybatis-config.xml"));
        return sessionFactory.getObject();
    }

    @Bean(name="dbmainSqlSessionTemplate")
    @Primary
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory dbmainsqlSessionFactory) {
        return new SqlSessionTemplate(dbmainsqlSessionFactory);
    }

    @Bean(name = "dbmaintransactionManager")
    @Primary
    public PlatformTransactionManager transactionManager(@Qualifier("dbmainDataSource") DataSource dbmainDataSource) {
        return new DataSourceTransactionManager(dbmainDataSource);
    }
}