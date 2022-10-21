package uk.gov.pay.publicauth.utils;

import org.testcontainers.containers.PostgreSQLContainer;

import static java.lang.String.format;

public abstract class PostgresTestContainersBase {

    public final static String VERSION = "11.16";
    public static final PostgreSQLContainer POSTGRES_CONTAINER;
    
    static {
        POSTGRES_CONTAINER = new PostgreSQLContainer(format("postgres:%s", VERSION)).withUsername("postgres").withPassword("mysecretpassword");
        POSTGRES_CONTAINER.start();
    }
}
