import domain.{PerfilCliente, Transaccion}

object DetectorFraude {
  val mapaPerfiles: Map[String, PerfilCliente] = Dataset.perfiles.map(x => (x.clienteId, x)).toMap

  def validarTransaccion(t: Transaccion): Either[String, Transaccion] = {
    if (t.monto > 0 || !t.clienteId.isEmpty) Right(t)
    else if (t.monto <= 0) Left("Monto invalido")
    else Left("Cliente invalido")
  }

}
