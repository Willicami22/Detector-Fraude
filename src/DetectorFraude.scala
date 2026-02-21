import domain.{PerfilCliente, Transaccion}
import traits.{EventoFraude, MontoAtipico, UbicacionImposible, RafagaTransacciones}

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

}
