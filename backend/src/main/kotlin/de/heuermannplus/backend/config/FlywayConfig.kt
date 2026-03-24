package de.heuermannplus.backend.config

import jakarta.persistence.EntityManagerFactory
import javax.sql.DataSource
import org.flywaydb.core.Flyway
import org.springframework.boot.autoconfigure.AbstractDependsOnBeanFactoryPostProcessor
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FlywayConfig {

    @Bean(initMethod = "migrate")
    @ConditionalOnMissingBean(Flyway::class)
    fun flyway(dataSource: DataSource): Flyway =
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
}

@Configuration
class EntityManagerFactoryDependsOnFlywayPostProcessor :
    AbstractDependsOnBeanFactoryPostProcessor(EntityManagerFactory::class.java, "flyway")
