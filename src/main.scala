import domain.Transaccion


@main
def main(): Unit = {
  println("DETECTOR DE FRAUDE - ANÁLISIS DE TRANSACCIONES")

  // Obtener datos del Dataset
  val transacciones: List[Transaccion] = Dataset.transacciones
  val perfiles = Dataset.perfiles

  // Procesar todas las transacciones
  println("\n[1] Procesando transacciones...")
  val resultados = DetectorFraude.procesarTodas(transacciones, transacciones)
  println(s" ${resultados.length} transacciones procesadas exitosamente")

  // Extraer IDs únicos de clientes
  val clienteIds = perfiles.map(_.clienteId).distinct

  // Generar reportes por cliente
  println("\n[2] Generando reportes por cliente...")
  val reportes = clienteIds.map(clienteId => DetectorFraude.generarReporte(clienteId, resultados))

  // Imprimir reportes
  println("REPORTES POR CLIENTE")

  reportes.foreach(reporte => {
    println(s"\nCliente: ${reporte.clienteId}")
    println(s"  Total transacciones: ${reporte.totalTransacciones}")
    println(s"  Transacciones sospechosas: ${reporte.transaccionesSospechosas}")
    println(f"  Score promedio: ${reporte.scorePromedio}%.2f")
    println(s"  Tendencia: ${reporte.tendencia}")
  })

  // Verificaciones finales
  println("VERIFICACIONES FINALES")

  // Verificación 1: C002 debe tener tendencia "Tendencia al alza"
  val reporteC002 = reportes.find(_.clienteId == "C002")
  println("\n[Verificación 1] C002 debe tener tendencia al alza:")
  reporteC002 match {
    case Some(reporte) =>
      val tieneAlza = reporte.tendencia == "Tendencia al alza"
      val estado = if (tieneAlza) "PASÓ" else "FALLÓ"
      println(s"  $estado - Tendencia actual: ${reporte.tendencia}")
    case None => println("  FALLÓ - Reporte de C002 no encontrado")
  }

  // Verificación 2: C004 debe tener nivel Bajo en todas sus transacciones
  val resultadosC004 = resultados.filter(_.clienteId == "C004")
  val todosNivelBajo = resultadosC004.forall(r => r.nivelRiesgo == traits.Bajo)
  println("\n[Verificación 2] C004 debe tener nivel Bajo en TODAS sus transacciones:")
  if (todosNivelBajo) {
    println(s"  PASÓ - Todas las ${resultadosC004.length} transacciones tienen nivel Bajo")
  } else {
    val resumenC004 = resultadosC004.groupBy(_.nivelRiesgo).map { case (nivel, items) =>
      s"${nivel.getClass.getSimpleName.dropRight(1)}: ${items.length}"
    }.mkString(", ")
    println(s"  FALLÓ - Distribución: $resumenC004")
  }

}

