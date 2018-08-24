package kfc.com.modelo
 import gnu.io.CommPortIdentifier;
import java.util.ArrayList
import java.util.List
 

   
import com.kfc.conexion.ConexionSqlServer

class Despachador {
	static ConexionSqlServer ocnn

	static void despacharTarjeta(Runtime garbage) {
		// Obtener los registros relacionados a pagos con tarjeta desde canal_movimiento
		if (ocnn == null) {
			ocnn =  ConexionSqlServer.getInstance()
			ocnn.abrirConexion()
			println "Se reestablecio la conexion."
		}


		ColaProcesos oColaP =  ColaProcesos.getInstance()
		oColaP.oCnn = ocnn


		List<ColaProcesos> listaColaProceso =	oColaP.getListado()
		int cantidadColas = listaColaProceso.size()
		println "${cantidadColas} Colas encontradas"

		if (cantidadColas >0) {
			Tarjetas tarjeta
			for (ColaProcesos cola : listaColaProceso) {
				tarjeta = new Tarjetas(cola.iDCanalMovimiento, cola.imp_ip_estacion, cola, ocnn)
					tarjeta.procesar ()
			}
			// Limpia Memoria
			garbage.gc()
			System.out.println(" Memoria libre despues de limpieza:  "+ garbage.freeMemory() );
		}
		println "Fin proceso colas."
	}
	 
	
}
