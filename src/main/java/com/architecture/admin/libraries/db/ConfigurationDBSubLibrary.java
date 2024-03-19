package com.architecture.admin.libraries.db;

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
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/*****************************************************
 * SUB DB 접속 설정
 ****************************************************/
@Configuration
@MapperScan(basePackages={"com.architecture.admin.models.daosub"},sqlSessionFactoryRef="dbsubSqlSessionFactory")
@EnableTransactionManagement
public class ConfigurationDBSubLibrary {
    // application.properties 설정 prefix 값 확인
    @Bean(name="dbsubDataSource")
    @ConfigurationProperties(prefix="spring.datasource.sub")
    public DataSource dbsubDataSource() {
        return DataSourceBuilder.create().build();

    }

    @Bean(name="dbsubSqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory(@Qualifier("dbsubDataSource") DataSource dbmainDataSource, ApplicationContext applicationContext) throws Exception{
        final SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dbmainDataSource);
        sessionFactory.setMapperLocations(applicationContext.getResources("classpath:/mybatis/mappersub/*/*.xml"));

        sessionFactory.setConfigLocation(applicationContext.getResource("classpath:/mybatis/mybatis-config.xml"));
        return sessionFactory.getObject();
    }

    @Bean(name="dbsubSqlSessionTemplate")
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory dbmainsqlSessionFactory) {
        return new SqlSessionTemplate(dbmainsqlSessionFactory);
    }

    @Bean(name = "dbsubtransactionManager")
    public PlatformTransactionManager transactionManager(@Qualifier("dbsubDataSource") DataSource dbmainDataSource) {
        return new DataSourceTransactionManager(dbmainDataSource);
    }
}