package com.saludvital;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;

public class App {

    public static void main(String[] args) throws Exception {

        CamelContext context = new DefaultCamelContext();

        context.addRoutes(new FileRoute());

        context.start();

        System.out.println(">>> [SISTEMA]: Camel corriendo y esperando archivos...");

        Thread.sleep(1000000);

        context.stop();
    }
}
