import domain.{PerfilCliente, Transaccion, ResultadoAnalisis, ReporteCliente}
import traits.{EventoFraude, MontoAtipico, UbicacionImposible, RafagaTransacciones, VelocidadExcesiva}

object DetectorFraude {
  val mapaPerfiles: Map[String, PerfilCliente] = Dataset.perfiles.map(x => (x.clienteId, x)).toMap

  def validarTransaccion(t: Transaccion): Either[String, Transaccion] = {
    if (t.monto > 0 || t.clienteId.nonEmpty) Right(t)
    else if (t.monto <= 0) Left("Monto invalido")
    else Left("Cliente invalido")
  }

  def detectarMontoAtipico(t: Transaccion, perfil: PerfilCliente) : Option[EventoFraude] = {
    if (t.monto > perfil.promedioHistorico * 5 ) Some(MontoAtipico(t.monto, perfil.promedioHistorico))
    else None
  }

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

  def calcularScore(eventos: List[EventoFraude]): Double = {
    eventos.foldLeft(0.0)((acc, e) => {
      e match {
        case _: MontoAtipico => acc + 40
        case _: UbicacionImposible => acc + 50
        case _: RafagaTransacciones => acc + 30
        case _: VelocidadExcesiva => acc + 25
        case _ => acc
      }
    })
  }

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

  def procesarTodas(transacciones: List[Transaccion], historico: List[Transaccion]): List[ResultadoAnalisis] = {
    @scala.annotation.tailrec
    def procesarRec(restantes: List[Transaccion], acumulador: List[ResultadoAnalisis]): List[ResultadoAnalisis] = {
      restantes match {
        case Nil => acumulador
        case t :: tail =>
          analizarTransaccion(t, historico) match {
            case Right(resultado) => procesarRec(tail, acumulador :+ resultado)
            case Left(error) =>
              println(s"Error procesando transacción ${t.id}: $error")
              procesarRec(tail, acumulador)
          }
      }
    }
    
    procesarRec(transacciones, List())
  }

  def generarReporte(clienteId: String, resultados: List[ResultadoAnalisis]): ReporteCliente = {
    val resultadosCliente = resultados.filter(_.clienteId == clienteId)
    val totalTransacciones = resultadosCliente.length
    val transaccionesSospechosas = resultadosCliente.count(r => r.nivelRiesgo == traits.Medio || r.nivelRiesgo == traits.Alto)
    
    val scorePromedio = if (totalTransacciones > 0) {
      resultadosCliente.foldLeft(0.0)((acc, r) => acc + r.scoreRiesgo) / totalTransacciones
    } else 0.0
    
    val tendencia = if (totalTransacciones > 1) {
      val mitad = totalTransacciones / 2
      val primeraHalf = resultadosCliente.slice(0, mitad)
      val segundaHalf = resultadosCliente.slice(mitad, totalTransacciones)
      
      val scorePromedioFirstHalf = primeraHalf.foldLeft(0.0)((acc, r) => acc + r.scoreRiesgo) / primeraHalf.length
      val scorePromedioSecondHalf = segundaHalf.foldLeft(0.0)((acc, r) => acc + r.scoreRiesgo) / segundaHalf.length
      
      if (scorePromedioSecondHalf > scorePromedioFirstHalf * 1.2) "Tendencia al alza"
      else if (scorePromedioSecondHalf < scorePromedioFirstHalf * 0.8) "Tendencia a la baja"
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
