import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
    public static final String ROJO = "\u001B[31m";

    public static void main(String[] args) {
        String direccionEnlace = "mongodb://localhost:27017";
        DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("yyyy/MM/dd");

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

                System.out.println(AZUL + "==========================================================================" + RESET);
                System.out.println(CIAN + "                       SISTEMA DE GESTION DE TALLER                       " + RESET);
                System.out.println(AZUL + "==========================================================================" + RESET);
                
                // === CARROS EN MENÚ PRINCIPAL ===
                System.out.println(AMARILLO + "        ____________               ______                ______" + RESET);
                System.out.println(AMARILLO + "   ____//__][__\\\\___\\         ____//__][_\\        ______//__][_\\__" + RESET);
                System.out.println(AMARILLO + "  (o _ |  -|   _   o|        [o _ |  -| _ \\      /o _   |  -| _   \\" + RESET);
                System.out.println(AMARILLO + "   `(_)-------(_)---'         `(_)-----(_)-'     `-(_)-------(_)---'" + RESET);
                System.out.println(AZUL + "--------------------------------------------------------------------------" + RESET);

                System.out.println("1. VENDEDOR: Crear Cita ");
                System.out.println("2. MECANICO: Atender Citas Pendientes");
                System.out.println("3. ADMIN: Registro de Clientes Frecuentes");
                System.out.println("4. CATALOGO: Consultar Servicios y Refacciones");
                System.out.println("5. MÓDULO ADM: Control de Cambios, Borrado y Limpieza");
                System.out.println("6. SALIR");
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
                        System.out.println(CIAN + "\n--- REGISTRO DE NUEVA CITA   ---" + RESET);
                        
                        String nombreCliente;
                        while (true) {
                            System.out.print("Nombre del Dueño: ");
                            nombreCliente = teclado.nextLine().trim();
                            if (!nombreCliente.isEmpty()) break;
                            System.out.println("   [!] El nombre no puede quedar vacio.");
                        }

                        System.out.print("Diagnostico de falla: ");
                        String diagnosticoFalla = teclado.nextLine();
                        
                        String fechaCitaStr;
                        LocalDate fechaCitaObjeto;
                        LocalDate hoy = LocalDate.now();

                        while (true) {
                            System.out.print("Fecha de la cita (AAAA/MM/DD): ");
                            fechaCitaStr = teclado.nextLine().trim();
                            try {
                                fechaCitaObjeto = LocalDate.parse(fechaCitaStr, formatoFecha);
                                if (fechaCitaObjeto.isBefore(hoy)) {
                                    System.out.println("   [!] No puedes agendar una cita en una fecha que ya paso.");
                                    continue;
                                }
                                break;
                            } catch (DateTimeParseException e) {
                                System.out.println("   [!] Fecha invalida o formato incorrecto. Use AAAA/MM/DD (ej: " + hoy.format(formatoFecha) + ")");
                            }
                        }

                        Document existeCita = baseDeDatos.getCollection("citas").find(
                            new Document("cliente", nombreCliente).append("estado", "Pendiente")
                        ).first();

                        if (existeCita != null) {
                            System.out.println("\n   [!] Este cliente ya cuenta con una cita activa en el taller.");
                        } else {
                            Document nuevaCita = new Document("cliente", nombreCliente)
                                                .append("falla", diagnosticoFalla)
                                                .append("fecha_cita", fechaCitaStr)
                                                .append("estado", "Pendiente");
                                                
                            baseDeDatos.getCollection("citas").insertOne(nuevaCita);
                            
                            System.out.println(VERDE + "\n>> ¡datos guardados!" + RESET);
                            System.out.println(AMARILLO + "------------------------------------------");
                            System.out.println(" ID Generado: " + nuevaCita.getObjectId("_id"));
                            System.out.println(" Cliente:     " + nuevaCita.getString("cliente"));
                            System.out.println(" Falla:       " + nuevaCita.getString("falla"));
                            System.out.println(" Fecha Cita:  " + nuevaCita.getString("fecha_cita"));
                            System.out.println(" Estado:      " + nuevaCita.getString("estado"));
                            System.out.println("------------------------------------------" + RESET);
                        }
                        
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
                            String nombreIngresado = teclado.nextLine().trim();

                            Document encontrada = null;
                            for (Document cita : citasPendientes) {
                                if (cita.getString("cliente").equalsIgnoreCase(nombreIngresado)) {
                                    encontrada = cita;
                                    break;
                                }
                            }

                            if (encontrada != null) {
                                String nombreBaseDatos = encontrada.getString("cliente");
                                String fechaCitaOriginalStr = encontrada.getString("fecha_cita");
                                LocalDate fechaCitaOriginal = LocalDate.parse(fechaCitaOriginalStr, formatoFecha);
                                
                                String fechaFinStr;
                                LocalDate fechaFinObjeto;

                                while (true) {
                                    System.out.println("Fecha registrada de cita: " + fechaCitaOriginalStr);
                                    System.out.print("Fecha de entrega (AAAA/MM/DD): ");
                                    fechaFinStr = teclado.nextLine().trim();
                                    
                                    try {
                                        fechaFinObjeto = LocalDate.parse(fechaFinStr, formatoFecha);
                                        
                                        if (fechaFinObjeto.isBefore(fechaCitaOriginal)) {
                                            System.out.println("   [!] La fecha de entrega no puede ser anterior a la fecha de la cita.");
                                            continue;
                                        }
                                        break;
                                    } catch (DateTimeParseException e) {
                                        System.out.println("   [!] Fecha invalida o formato incorrecto. Use AAAA/MM/DD");
                                    }
                                }
                                System.out.print("¿Que reparacion se le hizo finalmente?: ");
                                String reparacion = teclado.nextLine();

                                baseDeDatos.getCollection("reportes").insertOne(new Document("cliente", nombreBaseDatos)
                                                            .append("trabajo", reparacion)
                                                            .append("fecha", fechaFinStr));
                                
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
                            System.out.println("! " + VERDE + res.getString("_id") + RESET + " | Visitas: " + res.getInteger("total"));
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
                            System.out.println("1. SECCION: Llantas ");
                            System.out.println("2. SECCION: Motores ");
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
                                case 4 -> {
                                }
                                default -> {
                                    System.out.println("\n   [!] Opcion invalida. Presione ENTER para volver...");
                                    teclado.nextLine();
                                }
                            }

                        } while (opcionCatalogo != 4);
                    }
                    case 5 -> {
                        int opcionBorrado;
                        do {
                            try {
                                if (System.getProperty("os.name").contains("Windows")) {
                                    new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                                } else {
                                    System.out.print("\033[H\033[2J");
                                    System.out.flush();
                                }
                            } catch (IOException | InterruptedException e) {}

                            // === EL BOTE DE BASURA AHORA ADORNA EL MÓDULO ADM (COMO LOS CARRITOS) ===
                            System.out.println(ROJO + "==========================================" + RESET);
                            System.out.println(ROJO + "        MÓDULO ADM: CAMBIOS Y LIMPIEZA    " + RESET);
                            System.out.println(ROJO + "==========================================" + RESET);
                            System.out.println(ROJO + "              __________               " + RESET);
                            System.out.println(ROJO + "            /____________\\           " + RESET);
                            System.out.println(ROJO + "          (________________)           " + RESET);
                            System.out.println(ROJO + "           |  ||  ||  ||  |           " + RESET);
                            System.out.println(ROJO + "           |  ||  ||  ||  |           " + RESET);
                            System.out.println(ROJO + "           |  ||  ||  ||  |           " + RESET);
                            System.out.println(ROJO + "           |  ||  ||  ||  |           " + RESET);
                            System.out.println(ROJO + "           |  ||  ||  ||  |           " + RESET);
                            System.out.println(ROJO + "           \\______________/           " + RESET);
                            System.out.println(ROJO + "------------------------------------------" + RESET);
                            System.out.println("1. MODIFICAR: Editar datos de una cita");
                            System.out.println("2. ELIMINAR: Borrar Cita Específica");
                            System.out.println("3. ELIMINAR: Borrar Reporte Específico");
                            System.out.println("4. VACIAR TODO (Borrar base de datos completa)");
                            System.out.println("5. REGRESAR AL MENÚ PRINCIPAL");
                            System.out.print("\nSeleccione una opción: ");

                            if (!teclado.hasNextInt()) {
                                teclado.next(); teclado.nextLine();
                                opcionBorrado = 0; continue;
                            }
                            opcionBorrado = teclado.nextInt();
                            teclado.nextLine();

                            switch (opcionBorrado) {
                                case 1 -> {
                                    System.out.println(CIAN + "\n--- CITAS DISPONIBLES PARA MODIFICAR ---" + RESET);
                                    FindIterable<Document> todasLasCitas = baseDeDatos.getCollection("citas").find(new Document("estado", "Pendiente"));
                                    
                                    boolean hayCitasActivas = false;
                                    for (Document cita : todasLasCitas) {
                                        System.out.println(" • " + AMARILLO + cita.getString("cliente") + RESET + " (Falla: " + cita.getString("falla") + ")");
                                        hayCitasActivas = true;
                                    }

                                    if (!hayCitasActivas) {
                                        System.out.println("No hay ninguna cita activa registrada en este momento.");
                                        System.out.println("\nPresione ENTER para continuar...");
                                        teclado.nextLine();
                                        continue;
                                    }

                                    System.out.println("----------------------------------------------------");
                                    System.out.print("Ingrese el NOMBRE del cliente cuya cita quiere editar: ");
                                    String clienteEditar = teclado.nextLine().trim();

                                    Document citaEncontrada = baseDeDatos.getCollection("citas").find(
                                        new Document("cliente", clienteEditar).append("estado", "Pendiente")
                                    ).first();

                                    if (citaEncontrada != null) {
                                        System.out.println("\nCita encontrada:");
                                        System.out.println("1. Nombre del Dueño: [" + citaEncontrada.getString("cliente") + "]");
                                        System.out.println("2. Diagnóstico Falla: [" + citaEncontrada.getString("falla") + "]");
                                        System.out.println("3. Fecha de la Cita: [" + citaEncontrada.getString("fecha_cita") + "]");
                                        System.out.print("\n¿Qué dato desea modificar? (1-3): ");
                                        String campoSeleccionado = teclado.nextLine().trim();

                                        switch (campoSeleccionado) {
                                            case "1" -> {
                                                System.out.print("Ingrese el NUEVO nombre del dueño: ");
                                                String nuevoNombre = teclado.nextLine().trim();
                                                if (!nuevoNombre.isEmpty()) {
                                                    baseDeDatos.getCollection("citas").updateOne(
                                                        new Document("_id", citaEncontrada.getObjectId("_id")),
                                                        new Document("$set", new Document("cliente", nuevoNombre))
                                                    );
                                                    System.out.println(VERDE + ">> Nombre actualizado con éxito." + RESET);
                                                } else {
                                                    System.out.println(ROJO + ">> Operación cancelada. El nombre no puede ser vacío." + RESET);
                                                }
                                            }
                                            case "2" -> {
                                                System.out.print("Ingrese el NUEVO diagnóstico de falla: ");
                                                String nuevaFalla = teclado.nextLine().trim();
                                                baseDeDatos.getCollection("citas").updateOne(
                                                    new Document("_id", citaEncontrada.getObjectId("_id")),
                                                    new Document("$set", new Document("falla", nuevaFalla))
                                                );
                                                System.out.println(VERDE + ">> Diagnóstico modificado con éxito." + RESET);
                                            }
                                            case "3" -> {
                                                String nuevaFechaStr;
                                                LocalDate hoy = LocalDate.now();
                                                while (true) {
                                                    System.out.print("Ingrese la NUEVA fecha (AAAA/MM/DD): ");
                                                    nuevaFechaStr = teclado.nextLine().trim();
                                                    try {
                                                        LocalDate nuevaFechaObj = LocalDate.parse(nuevaFechaStr, formatoFecha);
                                                        if (nuevaFechaObj.isBefore(hoy)) {
                                                            System.out.println("   [!] No puedes cambiar la cita a una fecha pasada.");
                                                            continue;
                                                        }
                                                        break;
                                                    } catch (DateTimeParseException e) {
                                                        System.out.println("   [!] Formato incorrecto. Intente de nuevo.");
                                                    }
                                                }
                                                baseDeDatos.getCollection("citas").updateOne(
                                                    new Document("_id", citaEncontrada.getObjectId("_id")),
                                                    new Document("$set", new Document("fecha_cita", nuevaFechaStr))
                                                );
                                                System.out.println(VERDE + ">> Fecha de cita re-programada con éxito." + RESET);
                                            }
                                            default -> System.out.println(ROJO + ">> Opción de campo inválida." + RESET);
                                        }
                                    } else {
                                        System.out.println(ROJO + "\n>> No se encontró ninguna cita PENDIENTE activa para el cliente '" + clienteEditar + "'." + RESET);
                                    }
                                    System.out.println("Presione ENTER para continuar...");
                                    teclado.nextLine();
                                }
                                case 2 -> {
                                    System.out.println(CIAN + "\n--- CITAS DISPONIBLES EN EL SISTEMA ---" + RESET);
                                    FindIterable<Document> todasLasCitas = baseDeDatos.getCollection("citas").find();
                                    
                                    boolean hayCitasParaBorrar = false;
                                    for (Document cita : todasLasCitas) {
                                        System.out.println(" • " + AMARILLO + cita.getString("cliente") + RESET + " (Falla: " + cita.getString("falla") + " | Estado: " + cita.getString("estado") + ")");
                                        hayCitasParaBorrar = true;
                                    }

                                    if (!hayCitasParaBorrar) {
                                        System.out.println("No hay ninguna cita registrada en la base de datos.");
                                        System.out.println("\nPresione ENTER para continuar...");
                                        teclado.nextLine();
                                        continue;
                                    }

                                    System.out.println("----------------------------------------------------");
                                    System.out.print("Ingrese el NOMBRE exacto del cliente a eliminar: ");
                                    String clienteBorrar = teclado.nextLine().trim();
                                    var result = baseDeDatos.getCollection("citas").deleteMany(new Document("cliente", clienteBorrar));
                                    if (result.getDeletedCount() > 0) {
                                        System.out.println(VERDE + "\n>> Cita(s) de '" + clienteBorrar + "' eliminada(s) con éxito." + RESET);
                                    } else {
                                        System.out.println(ROJO + "\n>> No se encontró ningún registro con ese nombre." + RESET);
                                    }
                                    System.out.println("Presione ENTER para continuar...");
                                    teclado.nextLine();
                                }
                                case 3 -> {
                                    System.out.println(CIAN + "\n--- REPORTES DISPONIBLES EN EL SISTEMA ---" + RESET);
                                    FindIterable<Document> todosLosReportes = baseDeDatos.getCollection("reportes").find();
                                    
                                    boolean hayReportesParaBorrar = false;
                                    for (Document reporte : todosLosReportes) {
                                        System.out.println(" • " + AMARILLO + reporte.getString("cliente") + RESET + " (Trabajo: " + reporte.getString("trabajo") + " | Fecha: " + reporte.getString("fecha") + ")");
                                        hayReportesParaBorrar = true;
                                    }

                                    if (!hayReportesParaBorrar) {
                                        System.out.println("No hay ningún reporte histórico registrado en la base de datos.");
                                        System.out.println("\nPresione ENTER para continuar...");
                                        teclado.nextLine();
                                        continue;
                                    }

                                    System.out.println("----------------------------------------------------");
                                    System.out.print("Ingrese el NOMBRE exacto del cliente a eliminar de reportes: ");
                                    String clienteBorrar = teclado.nextLine().trim();
                                    var result = baseDeDatos.getCollection("reportes").deleteMany(new Document("cliente", clienteBorrar));
                                    if (result.getDeletedCount() > 0) {
                                        System.out.println(VERDE + "\n>> Reporte(s) de '" + clienteBorrar + "' eliminado(s) con éxito." + RESET);
                                    } else {
                                        System.out.println(ROJO + "\n>> No se encontró ningún reporte con ese nombre." + RESET);
                                    }
                                    System.out.println("Presione ENTER para continuar...");
                                    teclado.nextLine();
                                }
                                case 4 -> {
                                    // === AQUÍ SE QUEDÓ SOLO LA PREGUNTA LIMPIA SIN EL DUPLICADO DEL BOTE ===
                                    System.out.println(ROJO + "==========================================" + RESET);
                                    System.out.println(ROJO + "     ALERTA: VACIADO DE BASE DE DATOS    " + RESET);
                                    System.out.println(ROJO + "==========================================" + RESET);

                                    System.out.print(ROJO + "\n¿Está completamente seguro de borrar la BASE DE DATOS COMPLETA? (S/N): " + RESET);
                                    String confirmar = teclado.nextLine().trim().toUpperCase();
                                    if (confirmar.equals("S")) {
                                        baseDeDatos.getCollection("citas").deleteMany(new Document());
                                        baseDeDatos.getCollection("reportes").deleteMany(new Document());
                                        System.out.println(VERDE + "\n>> ¡Base de datos completamente limpia!" + RESET);
                                    } else {
                                        System.out.println("\n>> Operación cancelada.");
                                    }
                                    System.out.println("Presione ENTER para continuar...");
                                    teclado.nextLine();
                                }
                                case 5 -> {
                                }
                                default -> {
                                    System.out.println("\n   [!] Opción inválida dentro del menú administrativo.");
                                    System.out.println("Presione ENTER para continuar...");
                                    teclado.nextLine();
                                }
                            }
                        } while (opcionBorrado != 5);
                    }
                    case 6 -> {
                        System.out.println("\n" + AMARILLO + "Has salido del menú de gestión." + RESET);
                        System.out.println("Presione ENTER para finalizar la sesión...");
                        teclado.nextLine();
                    }
                    default -> {
                        System.out.println("\n   [!] Esa opcion no existe en el menu principal.");
                        System.out.println("Presione ENTER para continuar...");
                        teclado.nextLine();
                    }
                }
            } while (opcionSeleccionada != 6);
        } catch (Exception error) {
            System.out.println("Error general: " + error.getMessage());
        }
    }
}