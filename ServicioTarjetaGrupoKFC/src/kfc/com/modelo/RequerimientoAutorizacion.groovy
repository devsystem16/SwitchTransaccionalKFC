package kfc.com.modelo

import java.sql.ResultSet
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import jdk.nashorn.internal.parser.JSONParser
import org.json.JSONArray
import org.json.JSONObject
import com.kfc.conexion.ConexionSqlServer

class RequerimientoAutorizacion {

	def rqaut_id
	def rqaut_fecha
	def rqaut_ip
	def rqaut_puerto
	def rqaut_trama
	def rqaut_movimiento
	def tpenv_id
	def IDFormapagoFactura
	def IDEstacion
	def IDUsersPos
	def IDStatus
	def replica
	def nivel
	String caracterSeparador
	String medioAutorizador
	Configuracion_Canal_Movimiento  switchConfig //configuracion_Canal_Movimiento
	ConexionSqlServer ocnn
	ArrayList<Catalogo> catalogo

	RequerimientoAutorizacion ( ) {
	}
	RequerimientoAutorizacion (ConexionSqlServer ocnn) {
		this.ocnn = ocnn
	}

	void requerimientoPendiente (ColaProcesos cola) {

		Object [] params = [cola.imp_ip_estacion, Fecha.actual()]

		ResultSet odr  = ocnn.select( Propiedades.get(Constantes.ARCHIVO_APPLICATION_STATIC, "query.RequerimientoPendientes"),params)

		if (odr != null) {
			if (odr.next()) {

				this.rqaut_id = odr.getObject("rqaut_id")
				this.rqaut_fecha = odr.getObject("rqaut_fecha")
				this.rqaut_ip = odr.getObject("rqaut_ip")
				this.rqaut_puerto = odr.getObject("rqaut_puerto")
				this.rqaut_trama = odr.getObject("rqaut_trama")
				this.rqaut_movimiento = odr.getObject("rqaut_movimiento")
				this.IDEstacion = odr.getObject("est_id")
				this.IDUsersPos = odr.getObject("usr_id")
				this.tpenv_id = odr.getObject("tpenv_id")
				this.IDStatus = odr.getObject("std_id")

				this.medioAutorizador= Propiedades.get(Constantes.ARCHIVO_CONFIGURACION_DINAMIC,  "dispositivo.${this.tpenv_id}").replace(" ", ".")
				this.caracterSeparador  = Propiedades.get(Constantes.ARCHIVO_CONFIGURACION_DINAMIC, "JAR.${getTipoTransaccion().toUpperCase()}.${medioAutorizador.replace(" ", ".")}.CARACTER.SEPARADOR").toString().replace("ESPACIO", " ")
			}
		}

		if (odr != null) {
			if (!odr.isClosed()) {
				odr.close()
			}
		}
	}


	String getLineasDeSecuencia () {
		String lineasDeEjecucion =Propiedades.get(Constantes.ARCHIVO_CONFIGURACION_DINAMIC, "JAR.${this.medioAutorizador.toUpperCase().replace(" ", ".")}.${this.getTipoTransaccion().toUpperCase()}.SECUENCIA")
				.replace("SwitchIp",this.switchConfig.ipSwitchT)
				.replace("?",  this.getSoloTrama())
				.replace("Trama",  this.getSoloTrama())
				.replace("PuertoSwitchT", this.switchConfig.puertoSwitchT)
				.replace("TimeOutSwitchT", this.switchConfig.timeOutSwitchT)
				.replace("LocalIp", Constantes.LOCAL_IP)


	}
	String getTipoTransaccion () {
		String trama  =this.rqaut_trama.toString()
		trama.substring(trama.lastIndexOf("@")+1, trama.length()).toUpperCase().replace(" ", ".")
	}

	int getTimeOut () {
		try {
			Integer.parseInt( Propiedades.get(Constantes.ARCHIVO_CONFIGURACION_DINAMIC, "JAR.${this.medioAutorizador}.${this.getTipoTransaccion()}.TIME.OUT"))
		} catch (Exception e) {
			0
		}

	}
	String getSoloTrama () {
		String trama  =this.rqaut_trama.toString()
		trama.substring(0, trama.lastIndexOf("@") )
	}

	void actualizarEstado () {
		Object [] prm =  [this.rqaut_id , this.rqaut_ip]
		int registros = ocnn.update( Propiedades.get(Constantes.ARCHIVO_APPLICATION_STATIC,  "query.update_req_aut"), prm)
		println "Actualiza estado de Requerimineto 61 cod: ${rqaut_id}"
	}
	void actualizarEstadoProcesado () {
		Object [] prm =  [this.rqaut_id , this.rqaut_ip]
		int registros = ocnn.update( Propiedades.get(Constantes.ARCHIVO_APPLICATION_STATIC,  "query.update_req_aut_process"), prm)
		println "Actualiza estado de Requerimineto 42 cod: ${rqaut_id}"
	}
	void cargarCatalogo () {

		String trama =  this.rqaut_trama
		// Obtengo el medio autorizador usado en la transaccion desde la tabla SWT_Requerimiento_Autorizacion DB.
		//String medioAutorizador = Propiedades.get(Constantes.ARCHIVO_CONFIGURACION_DINAMIC ,"dispositivo.${this.tpenv_id}"  )

		// Determino el tipo de trama (compra, anulacion, recuperaTransaccion)
		String tipoTrama = trama.substring(trama.lastIndexOf("@")+1, trama.length())

		String jsonString = Propiedades.get(Constantes.ARCHIVO_CONFIGURACION_DINAMIC, "JAR.${tipoTrama.toUpperCase()}.${medioAutorizador.toUpperCase()}.CATALOGO.RESPUESTA")
		JSONObject jsonObject = new JSONObject(jsonString);
		JSONArray jsonArray = jsonObject.getJSONArray("catalogo");
		catalogo = new ArrayList<Catalogo>();
		int length = jsonArray.length()
		for(int i=0;i<length;i++){
			try {
				JSONObject json = jsonArray.getJSONObject(i)
				catalogo.add( new Catalogo(
						json.getString("nombre_campo")
						,json.getInt("posicion")
						,json.getInt("longitud")
						,json.getString("tipo_dato")
						,json.getString("caracter_relleno")
						,json.getString("orientacion_relleno")
						,json.getString("tabla")
						,json.getString("campo")
						))
			} catch (Exception e) {
				e.printStackTrace()
			}
		}
	}

	// Obtiene la longitud de la trama tomando en cuenta el caracter separador.!!
	int  getLongitudTrama () {
		int longitud =0 ;
		for (Catalogo c  : catalogo) {
			longitud += c.longitud  + caracterSeparador.length()
		}
		if(caracterSeparador.length() >0) {
			longitud = longitud - caracterSeparador.length()
		}
		return longitud
	}



	public void  InsertarTramaRespuestaAutorizacion (String tramaRespuesta) {

		// Asignar valores al catalogo de trama.
		int conteo =1
		int limite = catalogo.size()
		for (Catalogo c  : catalogo) {
			if (conteo == limite) {
				caracterSeparador=""
			}
			c.values = tramaRespuesta.substring(0,c.longitud)
			tramaRespuesta   = tramaRespuesta.substring(  c.longitud + caracterSeparador.length())
			conteo ++
		}

		// Generar el Insert segun la configuracion del catalogo.
		String SqlQuery  = "INSERT INTO SWT_Respuesta_Autorizacion"
		String param ="( rsaut_movimiento,raut_observacion,rsaut_fecha,"
		String values ="VALUES ( '${this.rqaut_movimiento}', '${tramaRespuesta}',  GETDATE(),"
		for (Catalogo c  : catalogo) {
			if (!c.tabla.equals("indefinido")) {
				param = param +  " ${c.campo},"
				values =values+  "'${c.values}',"
			}
		}
		param = param.substring(0 , param.length()-1) + ")"
		values = values.substring(0 , values.length()-1) + ")"

		ocnn.insert("${SqlQuery} ${param} ${values}")


	}
}
