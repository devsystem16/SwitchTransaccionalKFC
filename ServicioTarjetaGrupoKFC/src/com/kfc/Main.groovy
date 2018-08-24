package com.kfc

import java.beans.DesignMode
import java.sql.Connection
import com.kfc.conexion.ConexionSqlServer
import com.kfc.modelo.reflexion.JarLector
import kfc.com.modelo.ArchivoProperties
import kfc.com.modelo.ColaProcesos
import kfc.com.modelo.Constantes
import kfc.com.modelo.Despachador
import kfc.com.modelo.LogsApp
import kfc.com.modelo.Propiedades
import kfc.com.modelo.ValidadorDispositivos
import sun.security.util.Length

import java.lang.reflect.InvocationTargetException



import java.time.Duration;
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
public class Main {



	static Runtime garbage = Runtime.getRuntime();





	public static   String ejecutarFuncion () {
		println "inicio"

		//		int i =0
		//		for (;;) {
		//			//			 println i  + ", "
		//			i++
		//		}
		Thread.sleep(2000);

		//println "Se completo"
		return "Obtuvo respuesta"
	}

	static main(args) {
		//		ExecutorService executor = Executors.newFixedThreadPool(10);
		//		println "FIN 1"
		//		return

//		final Duration timeout = Duration.ofSeconds(3);
//		ExecutorService executor = Executors.newSingleThreadExecutor();
//
//		final Future<String> handler = executor.submit(new Callable() {
//					@Override
//					public String call() throws Exception {
//						return ejecutarFuncion ()
//					}
//				});
//
//		try {
//
//			  println 	handler.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
//
//		} catch (TimeoutException e) {
//			println "No se completo"
//			handler.cancel(true);
//		}
//		executor.shutdown();
//		println "fin"
//		System.exit(1)
//		return
		//        String trama ="013000000036542000621820=18055011505000467     022163 000000000010000000000009000000000000000000000001                                    00019115270620160430      JVZ00000000000JVZ000   300416B36542000621820^DANIEL LLERENA            ^180550115050000000000000467                                         "
		//		String jar =Propiedades.get(Constantes.ARCHIVO_CONFIGURACION_DINAMIC, "jar.ruta")
		//		JarLector j = JarLector.getInstancia(jar.split(Constantes.SEPARADOR_PROPERTIES))
		//		String tramaRespuesta =  j.executeMetodoSecuencia("obtieneClase(com.lectora.cnx.Envio)->ejecutaMetodo([0]>[]>Envio_requerimiento>[*192.168.100.18*&5000&99999&?&1&1])".replace("?", trama))
		//		println tramaRespuesta
		//
		//		return

		//System.out.println(" Memoria libre antes de limpieza:  "+ garbage.freeMemory() );
		// Creo Conexion SQl SERVER
		ConexionSqlServer conexion = ConexionSqlServer.getInstance()
		conexion.obtenerConexion()

		// Constuye el archivo de configuración .properties que contiene las configuraciones para el envio de requerimientos de pagos con tarjeta.
		ArchivoProperties p = new ArchivoProperties()
		p.oCnn = conexion
//  p.construir()
// 		return 
    
		def speed=   Propiedades.get(Constantes.ARCHIVO_CONFIGURACION_DINAMIC,  Constantes.TIMER_LOOP_APP)
		int sTiempo = Integer.parseInt(speed)
		Main  principal	 = new Main ()
		Despachador.ocnn = conexion

		garbage.gc()
		System.out.println("Memoria libre desp de limpieza:  "+ garbage.freeMemory() );
		while (true)
		{
			if (p.verificar() == 1)
				p.construir()

			principal.Ejecutardemonio()
			Thread.sleep(sTiempo)
		}
	}

	void Ejecutardemonio() {
		try {
			Despachador.despacharTarjeta(garbage)
		} catch (Exception e) {
			println "Error en el servicio (no controlado.!!)" + e.getMessage()
			Despachador.ocnn = null ;
			garbage.gc()
		}
	}
}
