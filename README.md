# Detector de Fraude - Análisis de Transacciones

## Descripción General

Sistema inteligente de detección de fraude en transacciones desarrollado en **Scala**. Implementa múltiples estrategias de análisis para identificar patrones anómalos en el comportamiento de clientes y generar reportes de riesgo detallados.

## Objetivo

Procesar y analizar transacciones de múltiples clientes para detectar posibles actividades fraudulentas basándose en:
- Montos anómalos
- Ubicaciones geográficas imposibles
- Ráfagas de transacciones
- Patrones de velocidad de compra

## Arquitectura del Proyecto

### Estructura de Carpetas

```
Detector Fraude/
├── src/
│   ├── Dataset.scala              # Carga de datos de transacciones y perfiles
│   ├── DetectorFraude.scala       # Lógica principal de detección
│   ├── main.scala                 # Punto de entrada de la aplicación
│   ├── domain/                    # Modelos de datos
│   │   ├── Coordenadas.scala      # Ubicación geográfica
│   │   ├── PerfilCliente.scala    # Perfil e histórico del cliente
│   │   ├── ReporteCliente.scala   # Reporte de análisis por cliente
│   │   ├── ResultadoAnalisis.scala# Resultado del análisis de transacción
│   │   └── Transaccion.scala      # Estructura de una transacción
│   └── traits/                    # Tipos de eventos y niveles de riesgo
│       ├── EventoFraude.scala     # Interfaz base de eventos
│       └── NivelRiesgo.scala      # Niveles de riesgo (Bajo, Medio, Alto)
└── README.md                      # Este archivo
```

## Componentes Principales

### 1. **DetectorFraude.scala**
Objeto central que orquesta todo el análisis de fraude.

#### Métodos Principales:

| Método | Descripción | Parámetros | Retorno |
|--------|-------------|-----------|---------|
| `validarTransaccion` | Valida datos consistentes | Transacción | `Either[String, Transaccion]` |
| `detectarMontoAtipico` | Detecta montos > 5x promedio | Transacción, Perfil | `Option[EventoFraude]` |
| `detectarUbicacionImposible` | Detecta cambios de país < 60 min | Transacción, Histórico | `Option[EventoFraude]` |
| `detectarRafaga` | Detecta 5+ transacciones en 5 min | Transacción, Histórico | `Option[EventoFraude]` |
| `calcularScore` | Suma puntajes de eventos detectados | Lista de eventos | `Double` |
| `analizarTransaccion` | Análisis completo de una transacción | Transacción, Histórico | `Either[String, ResultadoAnalisis]` |
| `procesarTodas` | Procesa lote de transacciones | Lista de transacciones | `List[ResultadoAnalisis]` |
| `generarReporte` | Genera reporte por cliente | ID Cliente, Resultados | `ReporteCliente` |

### 2. **Detectores de Fraude**

#### Monto Atípico
- **Criterio**: Transacción > 5 × promedio histórico del cliente
- **Puntaje**: 40 puntos
- **Ejemplo**: Cliente con promedio $200 realiza transacción de $1500

#### Ubicación Imposible
- **Criterio**: Cambio de país en < 60 minutos
- **Puntaje**: 50 puntos
- **Ejemplo**: Colombia (10:00) → España (10:50)

#### Ráfaga de Transacciones
- **Criterio**: 5+ transacciones dentro de 5 minutos
- **Puntaje**: 60 puntos
- **Indicador**: Posible automatización de fraude o credenciales robadas

#### Velocidad Excesiva
- **Criterio**: Transacciones muy frecuentes en corto tiempo
- **Puntaje**: 25 puntos

### 3. **Niveles de Riesgo**

```
Score < 30  → BAJO       (Transacción aprobada)
Score < 70  → MEDIO      (Requiere verificación adicional)
Score ≥ 70  → ALTO       (Transacción rechazada)
```

### 4. **Análisis de Tendencias**

El sistema compara la primera mitad vs segunda mitad de transacciones de cada cliente:

- **Tendencia al alza**: Riesgo aumenta en transacciones recientes
- **Tendencia a la baja**: Riesgo disminuye en transacciones recientes
- **Tendencia estable**: Riesgo se mantiene consistente

## Características Técnicas

### Procesamiento Secuencial y Acumulativo

El sistema procesa transacciones **en orden temporal**, acumulando histórico:

1. **Transacción 1**: Analizada sin histórico previo
2. **Transacción 2**: Analizada con histórico de [T1]
3. **Transacción 3**: Analizada con histórico de [T1, T2]
4. ...

Esto simula **procesamiento en tiempo real** donde las detecciones se basan únicamente en eventos pasados.

### Manejo de Errores Robusto

- Validación completa de datos de entrada
- Errores registrados sin detener el procesamiento
- Uso de `Either` para manejo funcional de errores

### Programación Funcional

- Uso de `fold` y `map` para transformaciones
- Recursión de cola (`@tailrec`) para eficiencia
- Pattern matching para análisis de eventos

## Flujo de Ejecución

```
main.scala
    ↓
DetectorFraude.procesarTodas()
    ├── Para cada transacción:
    │   ├── validarTransaccion()
    │   ├── detectarMontoAtipico()
    │   ├── detectarUbicacionImposible()
    │   ├── detectarRafaga()
    │   ├── calcularScore()
    │   └── clasificar nivelRiesgo
    ↓
Generar reportes por cliente
    ├── Contar transacciones sospechosas
    ├── Calcular score promedio
    └── Analizar tendencia
    ↓
Imprimir resultados y verificaciones
```

## Ejemplo de Salida

```
DETECTOR DE FRAUDE - ANÁLISIS DE TRANSACCIONES

[1] Procesando transacciones...
 20 transacciones procesadas exitosamente

[2] Generando reportes por cliente...
REPORTES POR CLIENTE

Cliente: C001
  Total transacciones: 5
  Transacciones sospechosas: 1
  Score promedio: 8,00
  Tendencia: Tendencia al alza

Cliente: C002
  Total transacciones: 5
  Transacciones sospechosas: 2
  Score promedio: 22,00
  Tendencia: Tendencia al alza

VERIFICACIONES FINALES

[Verificación 1] C002 debe tener tendencia al alza:
  PASÓ - Tendencia actual: Tendencia al alza

[Verificación 2] C004 debe tener nivel Bajo en TODAS sus transacciones:
  PASÓ - Todas las 5 transacciones tienen nivel Bajo
```

## Lógica de Cálculo de Tendencias

### Comparación de Mitades

Para clientes con múltiples transacciones:

```scala
mitad = totalTransacciones / 2
primeraHalf = transacciones[0..mitad)
segundaHalf = transacciones[mitad..total)

scoreAcumulado1 = sum(primeraHalf.scores)
scoreAcumulado2 = sum(segundaHalf.scores)

if scoreAcumulado2 > scoreAcumulado1 
    → "Tendencia al alza"
else if scoreAcumulado2 < scoreAcumulado1 AND scoreAcumulado1 > 0
    → "Tendencia a la baja"
else
    → "Tendencia estable"
```

## Instalación y Ejecución

### Requisitos
- Scala 3.x
- Java Runtime Environment (JRE) 11+

### Compilación
```bash
scalac -d out src/domain/*.scala src/traits/*.scala src/*.scala
```

### Ejecución
```bash
scala -cp out main
```

## 📚 Estructura de Datos

### Transaccion
```scala
case class Transaccion(
  id: String,              // ID único de transacción
  clienteId: String,       // ID del cliente
  monto: Double,          // Monto en moneda local
  timeStamp: Int,         // Marca de tiempo en minutos
  coordenadas: Coordenadas // Ubicación geográfica
)
```

### PerfilCliente
```scala
case class PerfilCliente(
  clienteId: String,
  promedioHistorico: Double  // Promedio de transacciones previas
)
```

### ResultadoAnalisis
```scala
case class ResultadoAnalisis(
  transaccionId: String,
  clienteId: String,
  nivelRiesgo: NivelRiesgo,    // Bajo, Medio, Alto
  eventos: List[EventoFraude],
  scoreRiesgo: Double,
  recomendacion: String        // Acción sugerida
)
```

### ReporteCliente
```scala
case class ReporteCliente(
  clienteId: String,
  totalTransacciones: Int,
  transaccionesSospechosas: Int,
  scorePromedio: Double,
  tendencia: String            // Descripción de tendencia
)
```

## Conceptos de Programación Utilizados

- **Programación Funcional**: Funciones puras, composición, inmutabilidad
- **Pattern Matching**: Análisis de eventos y casos de error
- **Monadas**: `Either` para manejo de errores, `Option` para valores opcionales
- **Recursión de Cola**: Optimización de memoria en `procesarRec`
- **Case Classes**: Modelos de datos estructurados
- **Traits**: Interfaces para tipos de eventos

## Consideraciones Importantes

1. **Histórico Acumulativo**: Las detecciones de ráfaga y ubicación imposible dependen del orden de procesamiento
2. **Umbral de 60 Minutos**: Para ubicación imposible, configurable según políticas del negocio
3. **Ventana de 5 Minutos**: Para ráfaga, ajustable según patrones históricos
4. **Factor de 5x**: Para monto atípico, configurable por perfil de cliente

## Notas de Desarrollo

- Código totalmente documentado con comentarios JavaDoc
- Advertencias de estilo: recomendaciones de acceso privado (sin impacto en funcionalidad)
- Estructura modular facilita extensión con nuevos detectores
- Uso de `List` en lugar de `Array` para garantizar inmutabilidad



