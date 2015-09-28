package uk.gov.pay.publicauth.app;

import io.dropwizard.Application;
import io.dropwizard.setup.Environment;

public class PublicAuthApp extends Application<PublicAuthConfiguration> {
    @Override
    public void run(PublicAuthConfiguration configuration, Environment environment) throws Exception {
        System.out.println("Hello world!");
    }


    public static void main(String[] args) throws Exception {
        new PublicAuthApp().run(args);
    }

}
