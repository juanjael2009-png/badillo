import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;

public class MenuPrincipal {
    public static final String RESET = "\u001B[0m";
    public static final String VERDE = "\u001B[32m";
    public static final String AMARILLO = "\u001B[33m";
    public static final String AZUL = "\u001B[34m";
    public static final String CIAN = "\u001B[36m";

    public static void main(String[] args) {
        String direccionEnlace = "mongodb://localhost:27017";

        try (MongoClient clienteMongo = MongoClients.create(direccionEnlace);
             Scanner teclado = new Scanner(System.in)) {
            
            MongoDatabase baseDeDatos = clienteMongo.getDatabase("TallerMecanico_DB");
            int opcionSeleccionada;

            do {
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

                System.out.println(AZUL + "==========================================" + RESET);
                System.out.println(CIAN + "       SISTEMA DE GESTION DE TALLER       " + RESET);
                System.out.println(AZUL + "==========================================" + RESET);
                System.out.println("1. VENDEDOR: Crear Cita (Entrada)");
                System.out.println("2. MECANICO: Atender Citas Pendientes");
                System.out.println("3. ADMIN: Registro de Clientes Frecuentes");
                System.out.println("4. CATALOGO: Consultar Servicios y Refacciones");
                System.out.println("5. SALIR");
                System.out.print("\nSeleccione una opcion: ");
                
                if (!teclado.hasNextInt()) {
                    teclado.next(); 
                    teclado.nextLine();
                    opcionSeleccionada = 0; 
                    continue; 
                }

                opcionSeleccionada = teclado.nextInt();
                teclado.nextLine(); 

                switch (opcionSeleccionada) {
                    case 1 -> {
                        System.out.println(CIAN + "\n--- REGISTRO DE NUEVA CITA ---" + RESET);
                        System.out.print("Nombre del Dueño: ");
                        String nombreCliente = teclado.nextLine();
                        System.out.print("Diagnostico de falla: ");
                        String diagnosticoFalla = teclado.nextLine();
                        System.out.print("Fecha de la cita (AAAA/MM/DD): ");
                        String fechaCita = teclado.nextLine();

                        baseDeDatos.getCollection("citas").insertOne(new Document("cliente", nombreCliente)
                                            .append("falla", diagnosticoFalla)
                                            .append("fecha_cita", fechaCita)
                                            .append("estado", "Pendiente"));
                        
                        System.out.println(VERDE + "\n>> Cita guardada correctamente." + RESET);
                        System.out.println("Presione ENTER para continuar...");
                        teclado.nextLine();
                    }
                    case 2 -> {
                        System.out.println(CIAN + "\n--- CITAS PENDIENTES EN SISTEMA ---" + RESET);
                        FindIterable<Document> citasPendientes = baseDeDatos.getCollection("citas").find(new Document("estado", "Pendiente"));
                        
                        boolean hayCitas = false;
                        for (Document cita : citasPendientes) {
                            System.out.println("- [" + AMARILLO + cita.getString("cliente") + RESET + "] Falla: " + cita.getString("falla") + " (Cita: " + cita.getString("fecha_cita") + ")");
                            hayCitas = true;
                        }

                        if (!hayCitas) {
                            System.out.println("No hay citas pendientes.");
                        } else {
                            System.out.print("\nEscriba el NOMBRE del cliente a atender: ");
                            String nombreIngresado = teclado.nextLine();

                            Document encontrada = null;
                            for (Document cita : citasPendientes) {
                                if (cita.getString("cliente").equalsIgnoreCase(nombreIngresado)) {
                                    encontrada = cita;
                                    break;
                                }
                            }

                            if (encontrada != null) {
                                String nombreBaseDatos = encontrada.getString("cliente");
                                String fechaCitaOriginal = encontrada.getString("fecha_cita");
                                String fechaFin;
                                while (true) {
                                    System.out.println("Fecha registrada de cita: " + fechaCitaOriginal);
                                    System.out.print("Fecha de entrega (AAAA/MM/DD): ");
                                    fechaFin = teclado.nextLine();
                                    if (fechaFin.compareTo(fechaCitaOriginal) >= 0) break;
                                    System.out.println("   [!] La fecha no puede ser anterior a la cita."); 
                                }
                                System.out.print("¿Que reparacion se le hizo finalmente?: ");
                                String reparacion = teclado.nextLine();

                                baseDeDatos.getCollection("reportes").insertOne(new Document("cliente", nombreBaseDatos)
                                                            .append("trabajo", reparacion)
                                                            .append("fecha", fechaFin));
                                
                                baseDeDatos.getCollection("citas").updateOne(
                                    new Document("cliente", nombreBaseDatos).append("estado", "Pendiente"), 
                                    new Document("$set", new Document("estado", "Terminado"))
                                );
                                System.out.println(VERDE + "\n>> ¡Proceso completado!" + RESET);
                            } else {
                                System.out.println("\n   [!] El cliente '" + nombreIngresado + "' no tiene citas pendientes.");
                            }
                        }
                        System.out.println("Presione ENTER para continuar...");
                        teclado.nextLine();
                    }
                    case 3 -> {
                        System.out.println(AMARILLO + "\n--- CLIENTES FRECUENTES ---" + RESET);
                        var procesoAgregacion = Arrays.asList(
                            Aggregates.group("$cliente", Accumulators.sum("total", 1)), 
                            Aggregates.match(new Document("total", new Document("$gte", 2)))
                        );
                        var resultados = baseDeDatos.getCollection("reportes").aggregate(procesoAgregacion);
                        
                        boolean hayFrecuentes = false;
                        for (Document res : resultados) {
                            System.out.println("⭐ " + VERDE + res.getString("_id") + RESET + " | Visitas: " + res.getInteger("total"));
                            hayFrecuentes = true;
                        }
                        if (!hayFrecuentes) System.out.println("No hay clientes frecuentes registrados.");
                        
                        System.out.println("\nPresione ENTER para continuar...");
                        teclado.nextLine();
                    }
                    case 4 -> {
                        int opcionCatalogo;
                        do {
                            try {
                                if (System.getProperty("os.name").contains("Windows")) {
                                    new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                                } else {
                                    System.out.print("\033[H\033[2J");
                                    System.out.flush();
                                }
                            } catch (IOException | InterruptedException e) {}

                            System.out.println(AZUL + "==========================================" + RESET);
                            System.out.println(CIAN + "           CATALOGO DE SECCIONES          " + RESET);
                            System.out.println(AZUL + "==========================================" + RESET);
                            System.out.println("1. SECCION: Llantas (Marcas y Medidas)");
                            System.out.println("2. SECCION: Motores (Nuevos y Reconstruidos)");
                            System.out.println("3. SECCION: Pilas y Baterías");
                            System.out.println("4. REGRESAR AL MENÚ PRINCIPAL");
                            System.out.print("\nSeleccione una sección: ");

                            if (!teclado.hasNextInt()) {
                                teclado.next(); teclado.nextLine();
                                opcionCatalogo = 0; continue;
                            }
                            opcionCatalogo = teclado.nextInt();
                            teclado.nextLine();

                            switch (opcionCatalogo) {
                                case 1 -> {
                                    int paginaLlantas = 1;
                                    String navegacionLlantas;
                                    do {
                                        try {
                                            if (System.getProperty("os.name").contains("Windows")) {
                                                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                                            } else {
                                                System.out.print("\033[H\033[2J");
                                                System.out.flush();
                                            }
                                        } catch (IOException | InterruptedException e) {}

                                        System.out.println(AMARILLO + "--- SECCION LLANTAS (Pagina " + paginaLlantas + " de 2) ---" + RESET);
                                        System.out.printf("%-25s %-10s\n", "PRODUCTO / MARCA", "PRECIO");
                                        System.out.println("------------------------------------------");
                                        
                                        if (paginaLlantas == 1) {
                                            System.out.printf("%-25s " + VERDE + "%-10s\n" + RESET, "Michelin Defender R16", "$2,450");
                                            System.out.printf("%-25s " + VERDE + "%-10s\n" + RESET, "Bridgestone Ecopia R15", "$1,890");
                                            System.out.printf("%-25s " + VERDE + "%-10s\n" + RESET, "Goodyear Assurance R17", "$2,150");
                                            System.out.println("------------------------------------------");
                                            System.out.println("N. Siguiente Página | M. Volver al Menú Catálogo");
                                        } else {
                                            System.out.printf("%-25s " + VERDE + "%-10s\n" + RESET, "Continental TrueContact", "$2,300");
                                            System.out.printf("%-25s " + VERDE + "%-10s\n" + RESET, "Pirelli Cinturato P7", "$2,750");
                                            System.out.printf("%-25s " + VERDE + "%-10s\n" + RESET, "Hankook Ventus R14", "$1,450");
                                            System.out.println("------------------------------------------");
                                            System.out.println("A. Anterior Página | M. Volver al Menú Catálogo");
                                        }
                                        
                                        System.out.print("\nSeleccione acción: ");
                                        navegacionLlantas = teclado.nextLine().trim().toUpperCase();

                                        if (navegacionLlantas.equals("N") && paginaLlantas == 1) paginaLlantas = 2;
                                        else if (navegacionLlantas.equals("A") && paginaLlantas == 2) paginaLlantas = 1;

                                    } while (!navegacionLlantas.equals("M"));
                                }
                                case 2 -> {
                                    int paginaMotores = 1;
                                    String navegacionMotores;
                                    do {
                                        try {
                                            if (System.getProperty("os.name").contains("Windows")) {
                                                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                                            } else {
                                                System.out.print("\033[H\033[2J");
                                                System.out.flush();
                                            }
                                        } catch (IOException | InterruptedException e) {}

                                        System.out.println(AMARILLO + "--- SECCION MOTORES (Pagina " + paginaMotores + " de 2) ---" + RESET);
                                        System.out.printf("%-25s %-10s\n", "MOTOR INTERCAMBIO", "PRECIO");
                                        System.out.println("------------------------------------------");
                                        
                                        if (paginaMotores == 1) {
                                            System.out.printf("%-25s " + VERDE + "%-10s\n" + RESET, "Motor Chevrolet 5.3L V8", "$32,000");
                                            System.out.printf("%-25s " + VERDE + "%-10s\n" + RESET, "Motor Ford 4.6L V8", "$28,500");
                                            System.out.printf("%-25s " + VERDE + "%-10s\n" + RESET, "Motor Nissan 1.6L (Tsuru)", "$14,000");
                                            System.out.println("------------------------------------------");
                                            System.out.println("N. Siguiente Página | M. Volver al Menú Catálogo");
                                        } else {
                                            System.out.printf("%-25s " + VERDE + "%-10s\n" + RESET, "Motor Honda 1.8L (Civic)", "$22,500");
                                            System.out.printf("%-25s " + VERDE + "%-10s\n" + RESET, "Motor VW 2.0L (Jetta)", "$19,800");
                                            System.out.printf("%-25s " + VERDE + "%-10s\n" + RESET, "Motor Toyota 2.4L (Tacoma)", "$26,000");
                                            System.out.println("------------------------------------------");
                                            System.out.println("A. Anterior Página | M. Volver al Menú Catálogo");
                                        }
                                        
                                        System.out.print("\nSeleccione acción: ");
                                        navegacionMotores = teclado.nextLine().trim().toUpperCase();

                                        if (navegacionMotores.equals("N") && paginaMotores == 1) paginaMotores = 2;
                                        else if (navegacionMotores.equals("A") && paginaMotores == 2) paginaMotores = 1;

                                    } while (!navegacionMotores.equals("M"));
                                }
                                case 3 -> {
                                    int paginaPilas = 1;
                                    String navegacionPilas;
                                    do {
                                        try {
                                            if (System.getProperty("os.name").contains("Windows")) {
                                                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                                            } else {
                                                System.out.print("\033[H\033[2J");
                                                System.out.flush();
                                            }
                                        } catch (IOException | InterruptedException e) {}

                                        System.out.println(AMARILLO + "--- SECCION BATERIAS (Pagina " + paginaPilas + " de 2) ---" + RESET);
                                        System.out.printf("%-25s %-10s\n", "BATERIA / MARCA", "PRECIO");
                                        System.out.println("------------------------------------------");
                                        
                                        if (paginaPilas == 1) {
                                            System.out.printf("%-25s " + VERDE + "%-10s\n" + RESET, "LTH AC-42-400", "$1,980");
                                            System.out.printf("%-25s " + VERDE + "%-10s\n" + RESET, "Optima RedTop (Gel)", "$3,650");
                                            System.out.printf("%-25s " + VERDE + "%-10s\n" + RESET, "America AM-42-400", "$1,420");
                                            System.out.println("------------------------------------------");
                                            System.out.println("N. Siguiente Página | M. Volver al Menú Catálogo");
                                        } else {
                                            System.out.printf("%-25s " + VERDE + "%-10s\n" + RESET, "Gonher G-42", "$1,750");
                                            System.out.printf("%-25s " + VERDE + "%-10s\n" + RESET, "Bosch S4 High Output", "$2,290");
                                            System.out.printf("%-25s " + VERDE + "%-10s\n" + RESET, "DieHard Silver", "$2,100");
                                            System.out.println("------------------------------------------");
                                            System.out.println("A. Anterior Página | M. Volver al Menú Catálogo");
                                        }
                                        
                                        System.out.print("\nSeleccione acción: ");
                                        navegacionPilas = teclado.nextLine().trim().toUpperCase();

                                        if (navegacionPilas.equals("N") && paginaPilas == 1) paginaPilas = 2;
                                        else if (navegacionPilas.equals("A") && paginaPilas == 2) paginaPilas = 1;

                                    } while (!navegacionPilas.equals("M"));
                                }
                            }

                        } while (opcionCatalogo != 4);
                    }
                    case 5 -> {
                        System.out.println("\n" + AMARILLO + "Has salido del menú de gestión." + RESET);
                        System.out.println("Presione ENTER para finalizar la sesión...");
                        teclado.nextLine();
                    }
                }
            } while (opcionSeleccionada != 5);
        } catch (Exception error) {
        }
    }
}