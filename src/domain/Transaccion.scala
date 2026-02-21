package domain

case class Transaccion(id: String, clienteId: String, monto: Double, timeStamp: Long, coordenadas: Coordenadas, comercio: String)
