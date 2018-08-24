package kfc.com.modelo

import java.sql.ResultSet
import java.util.ArrayList
import java.util.List

import com.kfc.conexion.ConexionSqlServer

class ColaProcesos {
	def  imp_ip_estacion
	def  tca_codigo
	def  imp_float1
	def  iDCanalMovimiento
	ConexionSqlServer oCnn

	private static ColaProcesos instance = null;

	private ColaProcesos() {
	}
	public static ColaProcesos getInstance() {
		if (instance == null) {
			instance = new ColaProcesos()
		}
		instance
	}

	void actalizarEsatoEnProceso () {

		Object [] prm = [61 , this.iDCanalMovimiento.toString()]
		 	 oCnn.update(Propiedades.get(Constantes.ARCHIVO_APPLICATION_STATIC,  "query.updateCanal"), prm)
		println "Actualizo a 61 el registro ${this.iDCanalMovimiento}"
	}

	void actualizarEstadoEjecutado() {
		Object [] prm = [42 , this.iDCanalMovimiento.toString()]
		 	 oCnn.update(Propiedades.get(Constantes.ARCHIVO_APPLICATION_STATIC,  "query.updateCanal"), prm)
		println "Actualizo a 42 el registro ${this.iDCanalMovimiento}"
	}

	ArrayList getListado ( ) {

		// Obtengo ip
		InetAddress addr = InetAddress.getLocalHost();
		String lsIpPOS =   addr.getHostAddress();


		List<ColaProcesos>	listaColaProceso =new ArrayList<ColaProcesos>()
		Object [] parametros =  [lsIpPOS]
		ResultSet odr = null
		try {

			odr =  oCnn.selectSQL(Propiedades.get( Constantes.ARCHIVO_APPLICATION_STATIC, Constantes.QUERY_PROCESO_COLA) , parametros)

			ColaProcesos cola
			if (odr !=null) {
				while (odr.next()) {
					cola = new  ColaProcesos ()
					cola.iDCanalMovimiento = odr.getObject("IDCanalMovimiento")
					cola.imp_ip_estacion =  odr.getObject("imp_ip_estacion")
					cola.tca_codigo =  odr.getObject("tca_codigo")
					cola.imp_float1 =  odr.getObject("imp_float1")

					listaColaProceso.add(cola)
				}
				if (!odr.isClosed()) {
					//System.out.println("Entro registros encolados CM")
					odr.close()
				}
			}
			return listaColaProceso
		} catch (Exception e) {
			println "\nCerro conexion por Error"
			odr.close()
			return listaColaProceso
		}
	}
}