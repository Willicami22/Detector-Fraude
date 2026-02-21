import domain.PerfilCliente
import domain.Transaccion
import domain.Coordenadas

object Dataset {

  val perfiles: List[PerfilCliente] = List(
    PerfilCliente("C001", "Ana Torres",     250.0,  "STANDARD"),
    PerfilCliente("C002", "Luis Gómez",     800.0,  "PREMIUM"),
    PerfilCliente("C003", "María Ruiz",     120.0,  "NUEVO"),
    PerfilCliente("C004", "Jorge Herrera",  400.0,  "STANDARD"),
    PerfilCliente("C005", "Sofía Morales", 1200.0,  "PREMIUM")
  )

  val transacciones: List[Transaccion] = List(

    // — C001: comportamiento normal + una anomalía de monto —
    Transaccion("T001", "C001",  230.0,  100, Coordenadas("Colombia", "Bogotá"),    "Supermercado"),
    Transaccion("T002", "C001",  180.0,  200, Coordenadas("Colombia", "Bogotá"),    "Restaurante"),
    Transaccion("T003", "C001", 4200.0,  300, Coordenadas("Colombia", "Medellín"),  "Joyería"),      //  monto atípico
    Transaccion("T004", "C001",  260.0,  400, Coordenadas("Colombia", "Bogotá"),    "Farmacia"),
    Transaccion("T005", "C001",  210.0,  500, Coordenadas("Colombia", "Bogotá"),    "Gasolinera"),

    // — C002: ráfaga de ops + salto de ubicación imposible —
    Transaccion("T006", "C002",  750.0, 1000, Coordenadas("Colombia", "Bogotá"),    "Hotel"),
    Transaccion("T007", "C002",  820.0, 1002, Coordenadas("España",   "Madrid"),    "Restaurante"),  //  2 min después, otro país
    Transaccion("T008", "C002",  900.0, 1003, Coordenadas("España",   "Madrid"),    "Tienda"),
    Transaccion("T009", "C002",  600.0, 1004, Coordenadas("España",   "Madrid"),    "Supermercado"),
    Transaccion("T010", "C002", 1100.0, 1005, Coordenadas("España",   "Madrid"),    "Electrónica"), //  ráfaga: 5 ops en 5 min

    // — C003: cliente nuevo, transacciones pequeñas + intento de monto alto —
    Transaccion("T011", "C003",  100.0, 2000, Coordenadas("Colombia", "Cali"),      "Cafetería"),
    Transaccion("T012", "C003",   90.0, 2100, Coordenadas("Colombia", "Cali"),      "Transporte"),
    Transaccion("T013", "C003", 3500.0, 2200, Coordenadas("México",   "CDMX"),      "Casino"),      //  monto atípico + país diferente
    Transaccion("T014", "C003",  110.0, 2300, Coordenadas("Colombia", "Cali"),      "Farmacia"),
    Transaccion("T015", "C003",   95.0, 2400, Coordenadas("Colombia", "Cali"),      "Supermercado"),

    // — C004: comportamiento completamente estable, sin anomalías —
    Transaccion("T016", "C004",  380.0, 3000, Coordenadas("Colombia", "Bogotá"),    "Tecnología"),
    Transaccion("T017", "C004",  420.0, 3100, Coordenadas("Colombia", "Bogotá"),    "Ropa"),
    Transaccion("T018", "C004",  395.0, 3200, Coordenadas("Colombia", "Bogotá"),    "Restaurante"),
    Transaccion("T019", "C004",  410.0, 3300, Coordenadas("Colombia", "Bogotá"),    "Supermercado"),
    Transaccion("T020", "C004",  405.0, 3400, Coordenadas("Colombia", "Bogotá"),    "Gasolinera")
  )
}
