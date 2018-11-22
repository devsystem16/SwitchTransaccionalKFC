package kfc.com.modelo

import java.lang.reflect.InvocationTargetException

import com.kfc.conexion.ConexionSqlServer
import com.kfc.modelo.reflexion.JarLector

import groovyjarjarantlr4.v4.misc.EscapeSequenceParsing

import java.time.Duration;
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
class Tarjetas {

	int exito
	ConexionSqlServer ocnn
	ColaProcesos cola

	Tarjetas () {}
	private static Tarjetas instance = null;

	//	public static  Tarjetas getInstance(String idCanalMovimiento, String ipEstacion, ColaProcesos cola, ConexionSqlServer ocnn) {
	//		if (instance == null) {
	//			instance = new Tarjetas(cola.iDCanalMovimiento ,cola.imp_ip_estacion , cola ,  ocnn)
	//		}
	//		instance
	//	}



	public static  Tarjetas getInstance( ColaProcesos cola, ConexionSqlServer ocnn) {
		if (instance == null) {
			instance = new Tarjetas( cola ,  ocnn)
		}
		instance
	}


	public Tarjetas(  ColaProcesos cola, ConexionSqlServer ocnn) {
		super();
		//		this.idCanalMovimiento = idCanalMovimiento
		//		this.ipEstacion = ipEstacion
		this.exito = exito = 0
		this.ocnn = ocnn
		this.cola = cola
	}

	void limpiarTransaccionesPendientes (boolean limpiarReverso) {

		RequerimientoAutorizacion requerimiento = null
		cola.oCnn = ocnn

		requerimiento = new RequerimientoAutorizacion(ocnn)
		requerimiento.requerimientoPendiente(cola)

		if (requerimiento.getTipoTransaccion().equals("REVERSO")) {
			if (limpiarReverso) {
				cola.actualizarEstadoEjecutado()
				requerimiento.actualizarEstadoProcesado()
			}
		}else {
			cola.actualizarEstadoEjecutado()
			requerimiento.actualizarEstadoProcesado()
		}


	}

	void procesar() {
        
		boolean TIME_OUT = false

		Respuesta_Autorizacion respuestaSwitch =  Respuesta_Autorizacion.getInstancia()
		RequerimientoAutorizacion requerimiento = null
		try {


			LogsApp.getInstance().Escribir("################################################################")
			// Cambiar a estado 61 canal movimiento (en estado pendiente.)
			LogsApp.getInstance().Escribir("Procesando ID Canal Movimineto :"+cola.iDCanalMovimiento)
			cola.oCnn = ocnn
			cola.actalizarEsatoEnProceso()
			LogsApp.getInstance().Escribir("Actualiza estado de Canal Moviminetos 61 :"+cola.iDCanalMovimiento)

			//  Obtiene los requerimientos pendientes.
			requerimiento = new RequerimientoAutorizacion(ocnn)
			requerimiento.requerimientoPendiente(cola)


			// Verifico si existe registro.
			if (requerimiento.rqaut_id != null ) {

				LogsApp.getInstance().Escribir("Tipo de transaccion:${requerimiento.getTipoTransaccion()} CON ${requerimiento.getMedioAutorizador()}" )


				// Actualizar SWT_Requerimiento_Autorizacion  a pendiente (61)
				requerimiento.actualizarEstado()
				LogsApp.getInstance().Escribir("Actualiza estado de Requerimineto 61 :"+requerimiento.getRqaut_id());


				// requerimiento.descomponerTrama()

				// Obtiene configuracion del switch
				Configuracion_Canal_Movimiento switchConfig= new  Configuracion_Canal_Movimiento(ocnn)
				switchConfig.cargarConfiguracion()
				requerimiento.switchConfig = switchConfig

				// Preparo la respuesta.
				respuestaSwitch.ocnn = ocnn
				respuestaSwitch.requerimiento = requerimiento
				String secuenciaValidacion = null

				try {
					secuenciaValidacion =requerimiento.asignarValoresSecuencia( Propiedades.get(Constantes.ARCHIVO_CONFIGURACION_DINAMIC, Constantes.VALIDADOR_CONEXION_DISPOSITIVO) )
				} catch (Exception e) {
					secuenciaValidacion =""
				}

				boolean estadoValidacion = false
				if (secuenciaValidacion !=""&&secuenciaValidacion.toLowerCase() != "ninguna"&& secuenciaValidacion.toLowerCase() !="no") {

					JarLector jl = new JarLector()
					estadoValidacion = jl.executeMetodoSecuencia(secuenciaValidacion, respuestaSwitch)
					jl = null

				}else {
					estadoValidacion = true
				}


				if ( estadoValidacion) {

					println "Esperando respuesta metodo."
					String lineasDeEjecucion =  requerimiento.getLineasDeSecuencia()
					LogsApp.getInstance().Escribir("Secuencia Transaccion: " +Encriptador.Encriptar( lineasDeEjecucion))

					// REFLEXION PARA ENVIAR TRAMA Y RETORNA UNA TRAMA.
					JarLector j = JarLector.getInstancia(Propiedades.get(Constantes.ARCHIVO_CONFIGURACION_DINAMIC, "jar.ruta")
							.split(Constantes.SEPARADOR_PROPERTIES))
					j.ocnn = ocnn

					// Verificar si la secuencia es de modalidad objeto
					if (!lineasDeEjecucion.contains("creaTrama") || !lineasDeEjecucion.contains("asignaAtributo")  ) {

						// Validar trama de envio.
						if (!requerimiento.tramaEnvioValida()) {

							println "trama: "+  requerimiento.getSoloTrama()
							println "Trama de requerimineto longitud Invalida"
							respuestaSwitch.insetarBug("Trama de requerimineto longitud Invalida")
							LogsApp.getInstance().Escribir("Trama de requerimineto longitud Invalida");
							return
						}
					}


					String tramaRespuesta =""
					final Duration timeout = Duration.ofSeconds( requerimiento.getTimeOut() )
					ExecutorService executor = Executors.newSingleThreadExecutor()


					if (requerimiento.getTipoTransaccion().equals("REVERSO") && requerimiento.existeReverso()) { // Validar si ya se hizo un reverso.
						cola.actualizarEstadoEjecutado()
						LogsApp.getInstance().Escribir("Actualizar a estado 42 ID: ${cola.iDCanalMovimiento}")
						requerimiento.actualizarEstadoProcesado()
						LogsApp.getInstance().Escribir("Se encontro un reverso realizado de esta transacción.")
						return
					}


					final Future<String> handler = executor.submit(new Callable() {
								@Override
								public String call() throws Exception {
									return j.executeMetodoSecuencia(lineasDeEjecucion, respuestaSwitch)
									//  return j.executeMetodoSecuencia("obtieneClaseNI(com.credibanco.entidades.EnvioProcesoPago[])->asignaAtributo([0]>TipoTransaccion|SwitchIp,RedAdquirente,CodigoDiferido)->ejecutaMetodo(obtieneClase(com.credibanco.entidades.LAN)>[]>ProcesoPago>[[1]])->creaTrama([2])" ,respuestaSwitch )
								}
							})

					try {
						tramaRespuesta=handler.get(timeout.toMillis(), TimeUnit.MILLISECONDS)
					} catch (TimeoutException e) {
						println e.getMessage()
						TIME_OUT = true
						handler.cancel(true)
					}
					executor.shutdown()

					// Si no se obtubo un time out procesar respuesta.
					if (!TIME_OUT) {
						println "Response: ${tramaRespuesta}"
						LogsApp.getInstance().Escribir("Respuesta obtenida: ${Encriptador.Encriptar(tramaRespuesta)}")
						respuestaSwitch.procesarRespuestaSwitch(tramaRespuesta)

					}else {
						respuestaSwitch.insetarBugTIME_OUT("TIME OUT");
						LogsApp.getInstance().Escribir("Se obtubo un TIME_OUT esperando respuesta")
					}


				}else {

					respuestaSwitch.insetarBug("Problemas de conexión con el dispositivo [${requerimiento.medioAutorizador}]");
					println "Dispositivo no conectado"
					cola.actualizarEstadoEjecutado()
					LogsApp.getInstance().Escribir("Actualizar a estado 42 ID: ${cola.iDCanalMovimiento}")

					requerimiento.actualizarEstadoProcesado()
				}


				cola.actualizarEstadoEjecutado()
				LogsApp.getInstance().Escribir("Actualizar a estado 42 ID: ${cola.iDCanalMovimiento}")
				requerimiento.actualizarEstadoProcesado()

			} // fin verificacion si existe requerimientoPendiente.
			else {
				println "No hay registros en Requerimientos pendientes."
				cola.actualizarEstadoEjecutado()
				LogsApp.getInstance().Escribir("Actualizar a estado 42 ID: ${cola.iDCanalMovimiento}")
			}


		} catch (InvocationTargetException e) {
			respuestaSwitch.insetarBug( e.getTargetException().toString())
			LogsApp.getInstance().Escribir("------------Exception:  ${e.getTargetException()}")
			println e.getTargetException()
		}
		catch (Exception e) {
			respuestaSwitch.insetarBug( e.getMessage().toString())
			LogsApp.getInstance().Escribir("------------Exception:  ${e.getMessage().toString()}")
			println e.getMessage().toString()
		}



	}
}
