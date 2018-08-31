package com.kfc

import java.beans.DesignMode
import java.sql.Connection
import com.kfc.conexion.ConexionSqlServer
import com.kfc.modelo.reflexion.JarLector

import groovy.transform.Field
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
	static main(args) {
// 
//		String trama ="01,4000,0,PRB00000,1046,0,0,amaxpoint,0,0,3F@ENVIO"
//		
//		println trama.substring( 1 , 2);
//		return 
		println "Iniciando Servicio..."
		// Creo Conexion SQl SERVER
		ConexionSqlServer conexion = ConexionSqlServer.getInstance()
		conexion.obtenerConexion()

		// Constuye el archivo de configuración .properties que contiene las configuraciones para el envio de requerimientos de pagos con tarjeta.
		ArchivoProperties p = new ArchivoProperties()
		p.oCnn = conexion
	    p.construir()
		//  return 
    
		def speed=   Propiedades.get(Constantes.ARCHIVO_CONFIGURACION_DINAMIC,  Constantes.TIMER_LOOP_APP)
		int sTiempo = Integer.parseInt(speed)
		Main  principal	 = new Main ()
		Despachador.ocnn = conexion

		garbage.gc()
		System.out.println("Memoria Liberada:  "+ garbage.freeMemory() );
	     println "Servicio Iniciado.."
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
			println "Exception dentro del servicio (!controlado)" + e.getMessage()
			Despachador.ocnn = null ;
			garbage.gc()
		}
	}
}
