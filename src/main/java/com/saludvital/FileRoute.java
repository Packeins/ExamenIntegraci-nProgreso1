package com.saludvital;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

public class FileRoute extends RouteBuilder {

    @Override
    public void configure() {

        onException(Exception.class)
            .process(exchange -> {
                System.out.println(">>> [ERROR]: " + exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class).getMessage());
            })
            .log(">>> [ERROR]: ${exception.message}")
            .handled(true)
            .to("file:data/error");

        from("file:data/input?delete=true")
            .routeId("procesar-csv")
            .process(exchange -> {
                System.out.println(">>> [ARCHIVO RECIBIDO]: " + exchange.getIn().getHeader("CamelFileName"));
            })
            .log(">>> [ARCHIVO RECIBIDO]: ${file:name}")

            .process(exchange -> {

                String body = exchange.getIn().getBody(String.class);
                if (body == null || body.trim().isEmpty()) {
                    throw new Exception("El archivo esta vacio");
                }

                String[] lines = body.split("\\r?\\n");

                // Validar encabezado
                String header = lines[0].trim();
                
                if (lines.length == 0 || !header.equals("patient_id,full_name,appointment_date,insurance_code")) {
                    throw new Exception("Encabezado invalido o faltante");
                }

                for (int i = 1; i < lines.length; i++) {
                    String line = lines[i].trim();
                    if (line.isEmpty()) continue;

                    String[] fields = line.split(",");

                    if (fields.length != 4) {
                        throw new Exception("Fila " + (i + 1) + ": Columnas incorrectas (se esperaban 4, se encontraron " + fields.length + ")");
                    }

                    // Limpiar campos
                    for (int j = 0; j < fields.length; j++) {
                        fields[j] = fields[j].trim();
                        if (fields[j].isEmpty()) {
                            throw new Exception("Fila " + (i + 1) + ": Campo vacio detectado");
                        }
                    }

                    // Validar fecha
                    if (!fields[2].matches("\\d{4}-\\d{2}-\\d{2}")) {
                        throw new Exception("Fila " + (i + 1) + ": Fecha invalida (" + fields[2] + ")");
                    }

                    // Validar seguro
                    String seguro = fields[3];
                    if (!(seguro.equals("IESS") ||
                          seguro.equals("PRIVADO") ||
                          seguro.equals("NINGUNO"))) {
                        throw new Exception("Fila " + (i + 1) + ": Seguro invalido (" + seguro + ")");
                    }
                }
            })

            .log(">>> [ESTADO]: Archivo valido")

            // Output
            .to("file:data/output")

            // Archive con timestamp
            .toD("file:data/archive?fileName=${file:name.noext}_${date:now:yyyy-MM-dd_HHmmss}.csv")

            .process(exchange -> {
                System.out.println(">>> [OK]: Archivo procesado correctamente");
            })
            .log(">>> [OK]: Archivo procesado correctamente");
    }
}
