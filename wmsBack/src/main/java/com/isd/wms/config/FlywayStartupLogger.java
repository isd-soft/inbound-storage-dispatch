package com.isd.wms.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;

@Configuration
public class FlywayStartupLogger {

    @Bean
    ApplicationRunner flywayInfoLogger(ObjectProvider<Flyway> flywayProvider) {
        return args -> {
            Flyway flyway = flywayProvider.getIfAvailable();
            if (flyway == null) {
                System.out.println("===== FLYWAY STATUS =====");
                System.out.println("Flyway bean not available");
                System.out.println("=========================");
                return;
            }

            MigrationInfo[] applied = flyway.info().applied();
            MigrationInfo[] pending = flyway.info().pending();

            System.out.println("===== FLYWAY STATUS =====");
            System.out.println("Applied migrations: " + applied.length);
            System.out.println("Pending migrations: " + pending.length);

            for (MigrationInfo migration : applied) {
                System.out.println("Applied: " + migration.getVersion() + " - " + migration.getDescription());
            }

            for (MigrationInfo migration : pending) {
                System.out.println("Pending: " + migration.getVersion() + " - " + migration.getDescription());
            }

            System.out.println("=========================");
        };
    }
}
