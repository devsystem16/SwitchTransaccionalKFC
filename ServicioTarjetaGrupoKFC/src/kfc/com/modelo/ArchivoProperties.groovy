package kfc.com.modelo

import com.kfc.conexion.ConexionSqlServer

import java.sql.ResultSet

class ArchivoProperties {
	ConexionSqlServer oCnn


	int verificar () {

		int resultado =0
		ResultSet odr =	 oCnn.selectSQL(Propiedades.get("Application", Constantes.QUERY_ACTUALIZAR_APP) )
		if (odr != null) {

			if (odr.next()) {
				resultado = odr.getInt("actualizar")
				odr.close()
				oCnn.insert(Propiedades.get("Application", Constantes.QUERY_INACTIVAR_ACTUALIZACION) )
			 
			}

			if (!odr.isClosed()) {
				odr.close()
				odr = null
			}else {
				odr = null
			}
		}

		return resultado
	}

	// Construye el archivo de configuración de la aplicación el cual contiene los métodos a usar desde el jar para el envío de transacciones con pagos mediante tarjetas.
	void construir () {

		ResultSet odr =	 oCnn.selectSQL(Propiedades.get("Application", Constantes.QUERY_CONFIGURACION_INICIAL) )
		String ruta = Constantes.RUTA_ARCHIVOS +"\\"+ Constantes.ARCHIVO_CONFIGURACION_DINAMICO
		File archivo = new File(ruta);
		BufferedWriter fichero;

		if (odr.next()) {

			if(archivo.exists()) {
				archivo.delete()
			}
			fichero = new BufferedWriter(new FileWriter(archivo));

			fichero.write(
					"# Configuracion actualziacion y timer de busqueda de pagos con tarjeta"  +  Constantes.NEW_LINE +
					Constantes.LINE_SALTO_RELLENO + Constantes.NEW_LINE +
					Constantes.TIMER_LOOP_ACTUALIZACION +"="+   odr.getString("intervaloActualizacion")  +  Constantes.NEW_LINE +
					Constantes.TIMER_LOOP_APP +"="+ odr.getString("intervaloTarjeta")  +  Constantes.NEW_LINE +
					Constantes.LINE_SALTO_RELLENO + Constantes.NEW_LINE +
					Constantes.RURA_JAR +"="+ odr.getString("rutaJar")  + Constantes.NEW_LINE +
					Constantes.LINE_SALTO_RELLENO + Constantes.NEW_LINE
					)

			if (!odr.isClosed()) {
				odr.close()
			}
		}


		odr = oCnn.selectSQL("EXEC switch.configuracionInicialPropertiesDispositivos"  )
		while (odr.next()) {

			String newString = new String(odr.getString("recurso").toString().getBytes("UTF-8"), "UTF-8");
			fichero.write( newString)
			fichero.newLine()
		}

		fichero.newLine()
		fichero.close()
		println "Archivo creado correctamente. [${ruta}]"

	}
}







