package traits

sealed trait NivelRiesgo
case object Bajo  extends NivelRiesgo
case object Medio extends NivelRiesgo
case object Alto  extends NivelRiesgo
