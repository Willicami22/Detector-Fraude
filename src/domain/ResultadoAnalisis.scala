package domain

import traits.EventoFraude
import traits.NivelRiesgo

case class ResultadoAnalisis(transaccionId: String, clienteId: String, nivelRiesgo: NivelRiesgo, eventos: List[EventoFraude], scoreRiesgo: Double, recomendacion: String)
