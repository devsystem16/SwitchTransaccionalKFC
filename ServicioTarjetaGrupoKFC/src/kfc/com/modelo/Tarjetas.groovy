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

	String idCanalMovimiento
	String ipEstacion
	int exito
	ConexionSqlServer ocnn
	ColaProcesos cola

	Tarjetas () {}
	private static Tarjetas instance = null;


	public   Tarjetas getInstance() {
		if (instance == null) {
			instance = new Tarjetas()
		}
		instance
	}


	public Tarjetas(String idCanalMovimiento, String ipEstacion, ColaProcesos cola, ConexionSqlServer ocnn) {
		super();
		this.idCanalMovimiento = idCanalMovimiento
		this.ipEstacion = ipEstacion
		this.exito = exito = 0
		this.ocnn = ocnn
		this.cola = cola
	}



	void procesar() {

		boolean TIME_OUT = false

		Respuesta_Autorizacion respuestaSwitch =  Respuesta_Autorizacion.getInstancia()
		try {


			LogsApp.getInstance().Escribir("################################################################")
			// Cambiar a estado 61 canal movimiento (en estado pendiente.)
			LogsApp.getInstance().Escribir("Procesando ID Canal Movimineto :"+cola.iDCanalMovimiento)
			cola.oCnn = ocnn
			cola.actalizarEsatoEnProceso()
			LogsApp.getInstance().Escribir("Actualiza estado de Canal Moviminetos 61 :"+cola.iDCanalMovimiento)


			//  Obtiene los requerimientos pendientes.
			RequerimientoAutorizacion requerimiento = new RequerimientoAutorizacion(ocnn)
			requerimiento.requerimientoPendiente(cola)

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


			JarLector jl = new JarLector()
			String secuenciaValidacion = Propiedades.get(Constantes.ARCHIVO_CONFIGURACION_DINAMIC, Constantes.VALIDADOR_CONEXION_DISPOSITIVO)

			boolean estado = false
			if (secuenciaValidacion !="") {
				estado = jl.executeMetodoSecuencia(secuenciaValidacion, respuestaSwitch)
			}else {
				estado = true
			}
			jl = null

			if ( estado) {

				println "Esperando respuesta"
				String lineasDeEjecucion = requerimiento.getLineasDeSecuencia()
				LogsApp.getInstance().Escribir("Secuencia Transaccion: " +lineasDeEjecucion)

				// REFLEXION PARA ENVIAR TRAMA Y RETORNA UNA TRAMA.
				String jar =Propiedades.get(Constantes.ARCHIVO_CONFIGURACION_DINAMIC, "jar.ruta")
				JarLector j = JarLector.getInstancia(jar.split(Constantes.SEPARADOR_PROPERTIES))
				j.ocnn = ocnn

				String tramaRespuesta =""

				final Duration timeout = Duration.ofSeconds( requerimiento.getTimeOut() )
				ExecutorService executor = Executors.newSingleThreadExecutor()

				final Future<String> handler = executor.submit(new Callable() {
							@Override
							public String call() throws Exception {
							  return j.executeMetodoSecuencia(lineasDeEjecucion, respuestaSwitch)
								//return j.executeMetodoSecuencia("obtieneClaseNI(com.credibanco.entidades.EnvioProcesoPago[])->asignaAtributo([0]>TipoTransaccion,RedAdquirente,CodigoDiferido)->ejecutaMetodo(obtieneClase(com.credibanco.entidades.LAN)>[]>ProcesoPago>[[1]])->creaTrama([2])" ,respuestaSwitch )
								//						         return j.executeMetodoSecuencia("obtieneClaseNI(DF.LANConfig[120.17.6.1>3000>99999>1000000101>10100402>CID123456>1>1])->obtieneClaseNI(DF.EnvioProcesoPago[[0]])->ejecutaMetodo(obtieneClase(DF.LAN)>[[0]]>ProcesoPago>[[1]])" )//lineasDeEjecucion)
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
					LogsApp.getInstance().Escribir("Respuesta obtenida: ${tramaRespuesta}")
					respuestaSwitch.procesarRespuestaSwitch(tramaRespuesta)

				}else {
					respuestaSwitch.insetarBugTIME_OUT("TIME OUT");
					LogsApp.getInstance().Escribir("Se obtubo un TIME_OUT esperando respuesta")
				}

			}else {

				respuestaSwitch.insetarBug("Problemas por conexión con el dispositivo [${requerimiento.medioAutorizador}]");
				println "DIspositivo no conectado"
			}


			cola.actualizarEstadoEjecutado()
			LogsApp.getInstance().Escribir("Actualizar a estado 42 ID: ${cola.iDCanalMovimiento}")

			requerimiento.actualizarEstadoProcesado()

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
