import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates; // Necesario para la limpieza de pantalla pro

public class MenuPrincipal {
    public static void main(String[] args) {
        String direccionEnlace = "mongodb://localhost:27017";

        try (MongoClient clienteMongo = MongoClients.create(direccionEnlace);
             Scanner teclado = new Scanner(System.in)) {
            
            MongoDatabase baseDeDatos = clienteMongo.getDatabase("TallerMecanico_DB");
            int opcionSeleccionada;

            do {
                // LIMPIEZA DE PANTALLA MEJORADA (Sin advertencias de VS Code)
                try {
                    if (System.getProperty("os.name").contains("Windows")) {
                        new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                    } else {
                        System.out.print("\033[H\033[2J");
                        System.out.flush();
                    }
                } catch (IOException | InterruptedException errorLimpieza) {
                    for (int i = 0; i < 50; i++) System.out.println();
                }

                System.out.println("\n==========================================");
                System.out.println("       SISTEMA DE GESTION DE TALLER       ");
                System.out.println("==========================================");
                System.out.println("1. VENDEDOR: Crear Cita (Entrada)");
                System.out.println("2. MECANICO: Atender Citas Pendientes");
                System.out.println("3. ADMIN: Ver Clientes Frecuentes (Automatico)");
                System.out.println("4. CATALOGO: Lista de Precios y Servicios");
                System.out.println("5. SALIR");
                System.out.print("Seleccione una opcion: ");
                
                if (!teclado.hasNextInt()) {
                    System.out.println("\n  [!] ERROR: Por favor ingrese solo números.");
                    teclado.next(); teclado.nextLine();
                    opcionSeleccionada = 0; continue;
                }

                opcionSeleccionada = teclado.nextInt();
                teclado.nextLine(); 

                switch (opcionSeleccionada) {
                    case 1 -> {
                        System.out.println("\n--- REGISTRO DE NUEVA CITA ---");
                        System.out.print("Nombre del Dueño: ");
                        String nombreCliente = teclado.nextLine();
                        System.out.print("Diagnostico de falla: ");
                        String diagnosticoFalla = teclado.nextLine();
                        System.out.print("Fecha de la cita (AAAA/MM/DD): ");
                        String fechaCita = teclado.nextLine();

                        Document nuevaCita = new Document("cliente", nombreCliente)
                                            .append("falla", diagnosticoFalla)
                                            .append("fecha_cita", fechaCita)
                                            .append("estado", "Pendiente");

                        baseDeDatos.getCollection("citas").insertOne(nuevaCita);
                        
                        System.out.println("\n>> Cita guardada correctamente.");
                        System.out.println("Presione ENTER para volver al menú...");
                        teclado.nextLine();
                    }
                    case 2 -> {
                        System.out.println("\n--- CITAS PENDIENTES ---");
                        FindIterable<Document> citasPendientes = baseDeDatos.getCollection("citas")
                                .find(new Document("estado", "Pendiente"));
                        
                        boolean hayCitasDisponibles = false;
                        for (Document cita : citasPendientes) {
                            System.out.println("- [" + cita.getString("cliente") + "] Falla: " + cita.getString("falla") + " (Fecha: " + cita.getString("fecha_cita") + ")");
                            hayCitasDisponibles = true;
                        }

                        if (!hayCitasDisponibles) {
                            System.out.println("No hay citas pendientes por el momento.");
                        } else {
                            System.out.print("\nEscriba el nombre del cliente a atender: ");
                            String clienteAAtender = teclado.nextLine();

                            Document citaEncontrada = baseDeDatos.getCollection("citas").find(
                                new Document("cliente", clienteAAtender).append("estado", "Pendiente")
                            ).first();

                            if (citaEncontrada == null) {
                                System.out.println("\n  [!] ERROR: El cliente no existe en la lista de pendientes.");
                            } else {
                                String fechaCitaOriginal = citaEncontrada.getString("fecha_cita");
                                String fechaDeFinalizacion;

                                while (true) {
                                    System.out.println("Fecha registrada de la cita: " + fechaCitaOriginal);
                                    System.out.print("Ingrese fecha de entrega (AAAA/MM/DD): ");
                                    fechaDeFinalizacion = teclado.nextLine();
                                    
                                    if (fechaDeFinalizacion.compareTo(fechaCitaOriginal) >= 0) {
                                        break; 
                                    } else {
                                        System.out.println("\n  [!] ERROR: La fecha de entrega no puede ser anterior a la cita.");
                                    }
                                }

                                System.out.print("¿Que reparacion se le realizo finalmente?: ");
                                String reparacionRealizada = teclado.nextLine();

                                baseDeDatos.getCollection("reportes").insertOne(new Document("cliente", clienteAAtender)
                                                            .append("trabajo_realizado", reparacionRealizada)
                                                            .append("fecha_finalizacion", fechaDeFinalizacion));

                                baseDeDatos.getCollection("citas").updateOne(
                                    new Document("cliente", clienteAAtender).append("estado", "Pendiente"), 
                                    new Document("$set", new Document("estado", "Terminado"))
                                );
                                System.out.println("\n>> ¡Proceso completado! Cita cerrada y reporte generado.");
                            }
                        }
                        System.out.println("\nPresione ENTER para continuar...");
                        teclado.nextLine();
                    }
                    case 3 -> {
                        System.out.println("\n--- LISTA DE CLIENTES FRECUENTES ---");
                        var procesoAgregacion = Arrays.asList(
                            Aggregates.group("$cliente", Accumulators.sum("total_visitas", 1)),
                            Aggregates.match(new Document("total_visitas", new Document("$gte", 2)))
                        );

                        var resultadosFrecuentes = baseDeDatos.getCollection("reportes").aggregate(procesoAgregacion);
                        
                        boolean hayClientesFrecuentes = false;
                        for (Document resultado : resultadosFrecuentes) {
                            System.out.println("⭐ Cliente: " + resultado.getString("_id") + " | Total Visitas: " + resultado.getInteger("total_visitas"));
                            hayClientesFrecuentes = true;
                        }

                        if (!hayClientesFrecuentes) {
                            System.out.println("Aún no hay clientes registrados con visitas recurrentes.");
                        }

                        System.out.println("\nPresione ENTER para continuar...");
                        teclado.nextLine();
                    }
                    case 4 -> {
                        System.out.println("\n==========================================");
                        System.out.println("       CATALOGO DE PRECIOS REALES 2026     ");
                        System.out.println("==========================================");
                        System.out.printf("%-25s %-10s\n", "SERVICIO", "PRECIO (MXN)");
                        System.out.println("------------------------------------------");
                        System.out.printf("%-25s %-10s\n", "Cambio de Aceite Sintetico", "$1,250");
                        System.out.printf("%-25s %-10s\n", "Afinacion Mayor", "$2,800");
                        System.out.printf("%-25s %-10s\n", "Frenos Delanteros", "$1,450");
                        System.out.printf("%-25s %-10s\n", "Escaneo por Computadora", "$450");
                        System.out.printf("%-25s %-10s\n", "Carga de Aire Acondicionado", "$850");
                        System.out.printf("%-25s %-10s\n", "Alineacion y Balanceo", "$650");
                        System.out.printf("%-25s %-10s\n", "Lavado de Motor", "$350");
                        System.out.println("------------------------------------------");
                        teclado.nextLine();
                    }
                    case 5 -> System.out.println("Cerrando el sistema...");
                    
                    default -> {
                        System.out.println("\n  [!] ERROR: Opción no válida.");
                        teclado.nextLine();
                    }
                }
            } while (opcionSeleccionada != 5);

        } catch (Exception error) {
            System.err.println("Error en el sistema: " + error.getMessage());
        }
    }
}