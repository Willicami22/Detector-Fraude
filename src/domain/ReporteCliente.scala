package domain

case class ReporteCliente(clienteId: String, totalTransacciones: Int, transaccionesSospechosas: Int, scorePromedio: Double, tendencia: String)
