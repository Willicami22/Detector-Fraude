package traits

sealed trait EventoFraude
case class VelocidadExcesiva(cantidadOps: Int, ventanaMinutos: Int) extends EventoFraude
case class MontoAtipico(monto: Double, promedioHistorico: Double) extends EventoFraude
case class UbicacionImposible(pais1: String, pais2: String, minutos: Int) extends EventoFraude
case class RafagaTransacciones(cantidadOps: Int, ventanaMinutos: Int) extends EventoFraude