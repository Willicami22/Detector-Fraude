import domain.{PerfilCliente, Transaccion, ResultadoAnalisis, ReporteCliente}
import traits.{EventoFraude, MontoAtipico, UbicacionImposible, RafagaTransacciones, VelocidadExcesiva}

object DetectorFraude {
  val mapaPerfiles: Map[String, PerfilCliente] = Dataset.perfiles.map(x => (x.clienteId, x)).toMap

  /**
   * Valida que una transacción tenga datos consistentes.
   * Verifica que el monto sea positivo y que el ID del cliente no esté vacío.
   * 
   * @param t la transacción a validar
   * @return Right(t) si la transacción es válida, Left(mensaje) si hay error
   */
  def validarTransaccion(t: Transaccion): Either[String, Transaccion] = {
    if (t.monto > 0 && t.clienteId.nonEmpty) Right(t)
    else if (t.monto <= 0) Left("Monto invalido")
    else Left("Cliente invalido")
  }

  /**
   * Detecta si el monto de una transacción es atípico comparado con el histórico del cliente.
   * Se considera atípico si supera 5 veces el promedio histórico del cliente.
   * 
   * Ejemplo: Si el promedio histórico es $200, y la transacción es de $1500,
   * se detecta como MontoAtipico (1500 > 200 * 5 = 1000).
   * 
   * @param t la transacción a analizar
   * @param perfil el perfil del cliente con su promedio histórico
   * @return Some(MontoAtipico) si el monto es anómalo, None en caso contrario
   */
  def detectarMontoAtipico(t: Transaccion, perfil: PerfilCliente) : Option[EventoFraude] = {
    if (t.monto > perfil.promedioHistorico * 5 ) Some(MontoAtipico(t.monto, perfil.promedioHistorico))
    else None
  }

  /**
   * Detecta si un cliente ha realizado transacciones en países diferentes en un tiempo imposible.
   * 
   * Identifica cambios de país en menos de 60 minutos, lo cual es geográficamente imposible
   * considerando tiempos de viaje reales.
   * 
   * Ejemplo: Una transacción en Colombia a las 10:00 y otra en España a las 10:50
   * es considerada una ubicación imposible.
   * 
   * @param t la transacción actual a analizar
   * @param historico lista de transacciones previas del cliente para comparar
   * @return Some(UbicacionImposible) si se detecta cambio de país imposible, None en caso contrario
   */
  def detectarUbicacionImposible(t:Transaccion, historico: List[Transaccion]): Option[EventoFraude] = {
    val clienteTrans = historico.filter(_.clienteId == t.clienteId).sortBy(_.timeStamp)
        if (clienteTrans.isEmpty) return None

    val todasTrans = (clienteTrans :+ t).sortBy(_.timeStamp)
    val ventanas = todasTrans.sliding(2).toList

    ventanas.collectFirst {
      case List(anterior, actual) if anterior.coordenadas.pais != actual.coordenadas.pais && (actual.timeStamp - anterior.timeStamp) < 60 =>
        UbicacionImposible(anterior.coordenadas.pais, actual.coordenadas.pais, actual.timeStamp - anterior.timeStamp)
    }
  }

  /**
   * Detecta ráfagas de transacciones: múltiples transacciones en un tiempo muy corto.
   * 
   * Se considera una ráfaga cuando hay 5 o más transacciones dentro de una ventana de 5 minutos.
   * Este patrón puede indicar automatización de fraude o uso de credenciales robadas.
   * 
   * Ejemplo: 5 transacciones realizadas a los minutos 0, 1, 2, 3, 4 constituyen una ráfaga.
   * 
   * @param t la transacción actual a analizar
   * @param historico lista de transacciones previas del cliente para analizar el patrón temporal
   * @return Some(RafagaTransacciones) si se detecta ráfaga, None en caso contrario
   */
  def detectarRafaga(t:Transaccion, historico: List[Transaccion]): Option[EventoFraude] = {
    val clienteTrans = historico.filter(_.clienteId == t.clienteId).sortBy(_.timeStamp)
      if (clienteTrans.isEmpty) return None

    val todasTrans = (clienteTrans :+ t).sortBy(_.timeStamp)
    val ventanas = todasTrans.sliding(5).toList


        ventanas.collectFirst {
          case ventana if ventana.size == 5 && (ventana.last.timeStamp - ventana.head.timeStamp) <= 5 =>
            RafagaTransacciones(5, ventana.last.timeStamp - ventana.head.timeStamp)
        }
  }

  /**
   * Calcula el score de riesgo acumulado basado en los eventos de fraude detectados.
   * 
   * Puntajes asignados:
   * - MontoAtipico: 40 puntos
   * - UbicacionImposible: 50 puntos
   * - RafagaTransacciones: 60 puntos
   * - VelocidadExcesiva: 25 puntos
   * 
   * @param eventos lista de eventos de fraude detectados en la transacción
   * @return el score total acumulado (suma de puntos de todos los eventos)
   */
  def calcularScore(eventos: List[EventoFraude]): Double = {
    eventos.foldLeft(0.0)((acc, e) => {
      e match {
        case _: MontoAtipico => acc + 40
        case _: UbicacionImposible => acc + 50
        case _: RafagaTransacciones => acc + 60
        case _: VelocidadExcesiva => acc + 25
        case _ => acc
      }
    })
  }

  /**
   * Realiza un análisis completo de una transacción para detectar fraude.
   * 
   * Proceso:
   * 1. Valida que la transacción tenga datos consistentes
   * 2. Obtiene el perfil del cliente
   * 3. Ejecuta todos los detectores de fraude (monto, ubicación, ráfaga)
   * 4. Calcula el score de riesgo combinado
   * 5. Clasifica en nivel de riesgo (Bajo, Medio, Alto)
   * 6. Genera una recomendación de acción
   * 
   * @param t la transacción a analizar
   * @param historico las transacciones previas del cliente para comparar patrones
   * @return Right(ResultadoAnalisis) con el análisis completo, o Left(error) si hay problema
   */
  def analizarTransaccion(t: Transaccion, historico: List[Transaccion]): Either[String, ResultadoAnalisis] = {
    for {
      transaccionValida <- validarTransaccion(t)
      perfil <- mapaPerfiles.get(transaccionValida.clienteId).toRight(s"Perfil no encontrado para cliente ${transaccionValida.clienteId}")
      eventoMonto = detectarMontoAtipico(transaccionValida, perfil)
      eventoUbicacion = detectarUbicacionImposible(transaccionValida, historico)
      eventoRafaga = detectarRafaga(transaccionValida, historico)
      eventos = List(eventoMonto, eventoUbicacion, eventoRafaga).flatten
      scoreRiesgo = calcularScore(eventos)
      nivelRiesgo = scoreRiesgo match {
        case s if s < 30 => traits.Bajo
        case s if s < 70 => traits.Medio
        case _ => traits.Alto
      }
      recomendacion = nivelRiesgo match {
        case traits.Bajo => "Transacción aprobada"
        case traits.Medio => "Requiere verificación adicional"
        case traits.Alto => "Transacción rechazada"
      }
      resultado = ResultadoAnalisis(
        transaccionId = transaccionValida.id,
        clienteId = transaccionValida.clienteId,
        nivelRiesgo = nivelRiesgo,
        eventos = eventos,
        scoreRiesgo = scoreRiesgo,
        recomendacion = recomendacion
      )
    } yield resultado
  }

  /**
   * Procesa un lote completo de transacciones de forma secuencial.
   * 
   * Cada transacción se analiza considerando solo el histórico de transacciones previas
   * (simulando procesamiento en tiempo real). El histórico se acumula conforme se procesan
   * las transacciones, permitiendo la detección de patrones que se desarrollan en el tiempo.
   * 
   * @param transacciones lista de transacciones a procesar
   * @param historico parámetro no utilizado (conservado por compatibilidad), el histórico real
   *                  se construye acumulativamente durante el procesamiento
   * @return lista de ResultadoAnalisis con el análisis de cada transacción procesada exitosamente.
   *         Los errores se registran pero no detienen el procesamiento
   */
  def procesarTodas(transacciones: List[Transaccion], historico: List[Transaccion]): List[ResultadoAnalisis] = {
    @scala.annotation.tailrec
    def procesarRec(restantes: List[Transaccion], procesadas: List[Transaccion], acumulador: List[ResultadoAnalisis]): List[ResultadoAnalisis] = {
      restantes match {
        case Nil => acumulador
        case t :: tail =>
          analizarTransaccion(t, procesadas) match {
            case Right(resultado) => procesarRec(tail, procesadas :+ t, acumulador :+ resultado)
            case Left(error) =>
              println(s"Error procesando transacción ${t.id}: $error")
              procesarRec(tail, procesadas :+ t, acumulador)
          }
      }
    }

    procesarRec(transacciones, List(), List())
  }

  /**
   * Genera un reporte agregado de fraude para un cliente específico.
   * 
   * Calcula estadísticas y tendencias basadas en todos los análisis de transacciones del cliente:
   * - Total de transacciones
   * - Cantidad de transacciones sospechosas (nivel Medio o Alto)
   * - Score de riesgo promedio
   * - Tendencia de riesgo: comparando la primera mitad vs segunda mitad de transacciones
   * 
   * Clasificación de tendencias:
   * - "Tendencia al alza": el riesgo aumenta en las transacciones más recientes
   * - "Tendencia a la baja": el riesgo disminuye en las transacciones más recientes
   * - "Tendencia estable": el riesgo se mantiene consistente
   * - "Bajo riesgo" / "Riesgo moderado" / "Alto riesgo": para clientes con una única transacción
   * 
   * @param clienteId el ID del cliente para el cual generar el reporte
   * @param resultados lista completa de resultados de análisis de todas las transacciones
   * @return ReporteCliente con estadísticas y análisis de tendencia del cliente
   */
  def generarReporte(clienteId: String, resultados: List[ResultadoAnalisis]): ReporteCliente = {
    val resultadosCliente = resultados.filter(_.clienteId == clienteId)
    val totalTransacciones = resultadosCliente.length
    val transaccionesSospechosas = resultadosCliente.count(r => r.nivelRiesgo == traits.Medio || r.nivelRiesgo == traits.Alto)

    val scorePromedio = if (totalTransacciones > 0) {
      resultadosCliente.foldLeft(0.0)((acc, r) => acc + r.scoreRiesgo) / totalTransacciones
    } else 0.0

    val tendencia = if (totalTransacciones > 1) {
      val ordenados = resultadosCliente.sortBy(_.transaccionId)
      val mitad = totalTransacciones / 2
      val primeraHalf = ordenados.slice(0, mitad)
      val segundaHalf = ordenados.slice(mitad, totalTransacciones)

      val scoreAcumuladoFirstHalf = primeraHalf.map(_.scoreRiesgo).sum
      val scoreAcumuladoSecondHalf = segundaHalf.map(_.scoreRiesgo).sum
      if (scoreAcumuladoSecondHalf > scoreAcumuladoFirstHalf) "Tendencia al alza"
      else if (scoreAcumuladoSecondHalf < scoreAcumuladoFirstHalf && scoreAcumuladoFirstHalf > 0) "Tendencia a la baja"
      else "Tendencia estable"
    } else if (scorePromedio < 30) "Bajo riesgo"
    else if (scorePromedio < 70) "Riesgo moderado"
    else "Alto riesgo"

    ReporteCliente(
      clienteId = clienteId,
      totalTransacciones = totalTransacciones,
      transaccionesSospechosas = transaccionesSospechosas,
      scorePromedio = scorePromedio,
      tendencia = tendencia
    )
  }

}
