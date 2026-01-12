package org.raflab.studsluzba;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(
        scanBasePackages = {
                "org.raflab.studsluzba.ui",
                "org.raflab.studsluzba.service",
                "org.raflab.studsluzba.config"
        },
        exclude = {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                JpaRepositoriesAutoConfiguration.class,
                SecurityAutoConfiguration.class
        }
)
public class ClientSpringApp {
    public static ConfigurableApplicationContext start(String[] args) {
        return SpringApplication.run(ClientSpringApp.class, args);
    }
}
