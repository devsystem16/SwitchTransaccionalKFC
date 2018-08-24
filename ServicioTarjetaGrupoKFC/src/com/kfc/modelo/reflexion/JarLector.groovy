package com.kfc.modelo.reflexion

import java.io.File
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import java.net.URLClassLoader
import java.lang.reflect.InvocationTargetException

import java.lang.Class
import java.lang.reflect.ReflectPermission
import java.lang.reflect.Type
import com.sun.org.apache.bcel.internal.generic.LoadClass
import com.sun.org.apache.xml.internal.utils.URI

import kfc.com.modelo.Constantes
import kfc.com.modelo.LogsApp

import java.lang.reflect.Constructor
import java.lang.reflect.Field;
import javax.print.attribute.standard.PrinterLocation
import javax.sound.sampled.spi.FormatConversionProvider
import java.net.URL
import java.security.CodeSource
import java.sql.Connection
import java.sql.DriverManager
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class JarLector {


	String path    // Ruta de ubicacion del .jar
	File jar
	URLClassLoader cl
	Class beanClass
	Object objClase
	Method metodo


	private static JarLector Singelton

	// Constructor
	JarLector (String Path) {
		this.path = Path
		jar =  new File(Path)
		cl = new URLClassLoader (new URL("jar", "","file:" + jar.getAbsolutePath()+"!/"))

	}
	JarLector ( ) {

		cl = new URLClassLoader()
	}


	JarLector (String [] Paths) {

		int tamanio = Paths.length
		File url = null
		cl = new URLClassLoader()
		for(int i=0 ; i < tamanio; i++) {
			url = new File(Paths[i].trim());
			cl.addURL(new URL("jar", "","file:" + url.getAbsolutePath()+"!/"))

		}
	}

	// Singelton
	static  JarLector getInstancia(){

		if (Singelton == null)
			Singelton = new JarLector ()
		Singelton
	}

	// Singelton
	static  JarLector getInstancia(String [] paths){
		if (Singelton == null)
			Singelton = new JarLector (paths)
		Singelton
	}

	// Singelton
	static  JarLector getInstancia(String path){

		if (Singelton == null)
			Singelton = new JarLector (path)
		Singelton
	}

	public  String   buscarPaquete  (String claseBuscar) {




		String paquete =claseBuscar
		try {
			if (!claseBuscar.contains(".")) {

				boolean encontro = false
				URL  [] rutas = cl.getURLs()
				for(int i=0 ; i< rutas.length;i++) {

					String location =  rutas[i].path
					location = location.substring(0 ,location.length() -2 )

					URL jarUrl = new URL(location)
					ZipInputStream zip = new ZipInputStream(jarUrl.openStream());
					ZipEntry ze = null;
					while((ze = zip.getNextEntry()) != null){
						String RutaClase = ze.getName();
						if (RutaClase.contains(".class") || RutaClase.contains(".java")) {
							String NombreClase = RutaClase.substring(RutaClase.lastIndexOf("/") +1, RutaClase.lastIndexOf("."))
							if (claseBuscar.equals(NombreClase)) {
								paquete = RutaClase.replaceAll("/", ".").substring(0 , RutaClase.lastIndexOf("."))
								encontro = true
								break
							}
						}
					}

					if (encontro) {
						break
					}

				} // fin de for urls

			} // Fin pregunta si tiene .
		} catch (Exception e) {
			LogsApp.getInstance().Escribir(e.getMessage())
			println e.getMessage()
			e.printStackTrace()
		}
		paquete
	}

	public Object obtieneClase (  String paquete_clase){
		String  ass ="";
		Object beanClass = cl.loadClass (paquete_clase);
		return beanClass
	}


	public  Object  ejecutaMetodo (   Class beanClass , Object [] paramsConstruct ,  String metodoEjecutar, Object [] ParametrosMetodo ){
		Object respuesta
		Object objClase
		if  (paramsConstruct.length == 1) {
			if (paramsConstruct[0] == "") {
				objClase = beanClass.newInstance()
			}else {
				objClase = beanClass.newInstance(paramsConstruct[0])
			}
		}else {
			objClase = beanClass.newInstance(paramsConstruct)
		}
		int numeroParametros = (ParametrosMetodo[0].equals("")) ? 0: ParametrosMetodo.length

		Method metodo = null;
		//Method[] metodos = beanClass.getMethods()

		Method[] metodos = beanClass.getDeclaredMethods()

		// Ubicar el metodo.
		int length = metodos.length
		for (int i = 0; i <length; i++) {
			if (metodos[i].getName().equals(metodoEjecutar) && metodos[i].getParameterCount() == numeroParametros ) {
				metodo = metodos[i]
				break
			}
		}


		try {

			Object [] resultados = new Object [numeroParametros]
			Class [] parametrosMetodo = metodo.getParameterTypes()

			if (numeroParametros > 0 ) {

				int c = 0
				for (Class p : parametrosMetodo) {

					String tipo  =	p.getSimpleName()
					switch (tipo){
						case  "int":
							resultados [c] =  Integer.parseInt(ParametrosMetodo[c])
							break

						case  "String":
							resultados [c] = ( ParametrosMetodo[c].toString() =="null" ) ? null  : ParametrosMetodo[c].toString()
							break

						case  "CString":
							resultados [c] =  ( ParametrosMetodo[c].toString() =="null" ) ? null  : ParametrosMetodo[c].toString()
							break

						case  "Integer":
							resultados [c] =  ( ParametrosMetodo[c].toString() =="null" ) ? null  :Integer.parseInt(ParametrosMetodo[c])
							break

						case  "Date":
							resultados [c] =( ParametrosMetodo[c].toString() =="null" ) ? null  :  ParametrosMetodo[c].toString()
							break

						case  "Object[]":
							String a =   ParametrosMetodo[c]

							if (a != null) {
								if (a !="") {
									resultados[c] = a.split(">")
								}else {
									resultados[c] = null
								}
							}else {
								resultados[c] = null
							}

							break

						case  "[LObject;":
						// String a =   ParametrosMetodo[c]
						// resultados[c] = a.split(">")
							String a =   ParametrosMetodo[c]
							if (a != null) {
								if (a !="") {
									resultados[c] = a.split(">")
								}else {
									resultados[c] = null
								}
							}else {
								resultados[c] = null
							}

							break

						case  "Object":
							resultados [c] =  (  ParametrosMetodo[c].toString() ==  "null" ) ? null : ParametrosMetodo[c]
							break
						case  "byte[]":
							resultados [c] =  (  ParametrosMetodo[c].toString() ==  "null" )  ? null:  ParametrosMetodo[c].toString().getBytes()
							break

						default :
							resultados [c] = (  ParametrosMetodo[c].toString() ==  "null" )  ? null:  ParametrosMetodo[c].toString()
							break
					}
					c++
				}
			}

			if  (resultados.length == 1) {
				if (resultados[0] == "") {
					respuesta =	metodo.invoke( objClase, null)

				}else {

					respuesta =	 metodo.invoke( objClase, resultados)
				}
			}else {

				respuesta =	metodo.invoke( objClase, resultados)
			}

		}  catch (java.io.IOException e) {// InvocationTargetException e) {
			respuesta =respuesta  + "*****catch " + e.getMessage() // e.getTargetException().getMessage() + " Catch"
		}


		return respuesta

	}
	Object executeMetodoSecuencia (String lineas) {
		Object mensajeRespuesta ;

		Object  [] secuencia = lineas.split(Constantes.SEPARADOR_PROPERTIES)
		Object  [] colaSecuencia  = new Object [secuencia.length]
		for (int i =0 ; i< secuencia.length; i++) {
			String metodo = secuencia[i].toString().substring(0,secuencia[i].toString().indexOf("(")).trim()
			String contenido =secuencia[i].toString().substring(secuencia[i].toString().indexOf("(") +1, secuencia[i].toString().length() -1 )
			if (metodo.equals("obtieneClase")) {
				try {
					Object objClase = cl.loadClass (buscarPaquete(contenido.toString()))  // obtieneClase(cl, contenido)
					colaSecuencia[i] = objClase
				} catch (Exception e) {
					LogsApp.getInstance().Escribir("No se pudo encontrar la Clase ${contenido} " + e.getMessage())
					e.printStackTrace()
				}

			}
			else if (metodo.equals("ejecutaMetodo")) {

				Object [] ConfigEjecutaMetodo = contenido.split(">")

				int  posicionElemento = -1
				Object objClaseTemp = null
				if (ConfigEjecutaMetodo[0].toString().contains("obtieneClase")) {
					String cadena = ConfigEjecutaMetodo[0].toString()
					String contenidoTem =cadena.toString().substring(cadena.toString().indexOf("(") +1, cadena.toString().length() -1 ).trim()
					objClaseTemp =cl.loadClass (contenidoTem.toString()) // obtieneClase(cl, contenidoTem)
				}else {
					// 1) Posicion de referencia anterior.
					posicionElemento =  Integer.parseInt( ConfigEjecutaMetodo[0].replaceAll("[\\[\\]]", ""))
				}

				//2) Parametros del contructor.
				Object [] ParametrosConstruc ;
				if (ConfigEjecutaMetodo[1].toString().equals("[]")) {
					ParametrosConstruc =  [""]
				}else {
					String parametrosSinCorchetes = ConfigEjecutaMetodo[1].toString().trim()
					parametrosSinCorchetes = parametrosSinCorchetes.substring(1 , parametrosSinCorchetes.length()-1)
					Object [] vectorParame = parametrosSinCorchetes.split("&")

					ParametrosConstruc = new Object [vectorParame.length]
					for (int j =0 ; j<vectorParame.length; j++ ) {
						if (vectorParame[j].toString().contains("[")) {
							int posc =  Integer.parseInt( vectorParame[j].replaceAll("[\\[\\]]", ""))
							ParametrosConstruc [j] =colaSecuencia[posc]
						}else {
							// Validar tipo de dato.
							String dato = vectorParame[j]
							if (dato.toString().contains("*")) {
								ParametrosConstruc [j] =  ( vectorParame[j].toString().replace("*", "")).toString()
							}else {
								ParametrosConstruc [j] = Integer.parseInt(  ( vectorParame[j].toString().replace("*", "")))
							}
							// boolean?
						}
					}
				}
				//3) Metodo a invocar
				String  metodoInvocar = ConfigEjecutaMetodo[2].toString().trim()

				//4) Parametros del Metodo.
				Object [] ParametrosMethod ;
				if (ConfigEjecutaMetodo[3].toString().equals("[]")) {
					ParametrosMethod =  [""]
				}else {

					String parametrosSinCorchetes = ConfigEjecutaMetodo[3].toString().trim()
					parametrosSinCorchetes = parametrosSinCorchetes.substring(1 , parametrosSinCorchetes.length()-1)
					Object [] vectorParame = parametrosSinCorchetes.split("&")

					ParametrosMethod = new Object [vectorParame.length]
					for (int j =0 ; j<vectorParame.length; j++ ) {
						if (vectorParame[j].toString().contains("[")) {
							int posc =  Integer.parseInt( vectorParame[j].replaceAll("[\\[\\]]", ""))
							ParametrosMethod [j] =colaSecuencia[posc]
						}else {

							String dato = vectorParame[j]
							if (dato.toString().contains("*")) {
								ParametrosMethod [j] =  ( vectorParame[j].toString().replace("*", "")).toString()
							}else {
								ParametrosMethod [j] =   vectorParame[j]
							}

							//ParametrosMethod = vectorParame;
						}
					}
					//pendiente.
					//ParametrosMethod =params
				}

				Object elemento = null
				if (posicionElemento == -1) {
					elemento = objClaseTemp;
				}else {
					elemento = colaSecuencia[posicionElemento]
				}

				mensajeRespuesta= ejecutaMetodo(elemento , ParametrosConstruc, metodoInvocar,  ParametrosMethod)
				colaSecuencia[i] =mensajeRespuesta
			}else if (metodo.equals("esperar")) {
				try
				{
					int sTiempo = Integer.parseInt(contenido)
					Thread.sleep(sTiempo);
				}catch(Exception e){
				}
			}
			else {
				LogsApp.getInstance().Escribir("Secuencia [${metodo}] no existe.")
				System.exit(1)
			}
		}
		return mensajeRespuesta//.toString();
	}

	// Metodo invocador.
	String  invocarMetodo (String Clase , String Metodo, Object [] paramsConstruct, Object [] ParametrosMetodo , int estado) {

		String respuesta =""
		estado =0

		beanClass = cl.loadClass (buscarPaquete( Clase))

		if  (paramsConstruct.length == 1) {
			if (paramsConstruct[0] == "") {
				objClase = beanClass.newInstance()

			}else {
				objClase = beanClass.newInstance(paramsConstruct[0])
			}
		}else {
			objClase = beanClass.newInstance(paramsConstruct)
		}

		int numeroParametros = (ParametrosMetodo[0].equals("")) ? 0: ParametrosMetodo.length



		Method metodo = null;
		//Method[] metodos = beanClass.getMethods()

		Method[] metodos = beanClass.getDeclaredMethods()



		// Ubicar el metodo.
		int length = metodos.length
		for (int i = 0; i <length; i++) {
			if (metodos[i].getName().equals(Metodo) && metodos[i].getParameterCount() == numeroParametros ) {
				metodo = metodos[i]
				break
			}
		}


		try {

			Object [] resultados = new Object [numeroParametros]
			Class [] parametrosMetodo = metodo.getParameterTypes()

			if (numeroParametros > 0 ) {

				int c = 0
				for (Class p : parametrosMetodo) {

					String tipo  =	p.getSimpleName()
					switch (tipo){
						case  "int":
							resultados [c] =   Integer.parseInt(ParametrosMetodo[c])
							break

						case  "String":
							resultados [c] =  ParametrosMetodo[c].toString()
							break

						case  "Integer":
							resultados [c] =  Integer.parseInt(ParametrosMetodo[c])
							break


						case  "Date":
							resultados [c] =ParametrosMetodo[c].toString()
							break


						case  "Object[]":
							String a =   ParametrosMetodo[c]
							resultados[c] = a.split(">")
							break

						case  "[LObject;":
							String a =   ParametrosMetodo[c]
							resultados[c] = a.split(">")
							break

						case  "Object":
							resultados [c] =   ParametrosMetodo[c]
							break

						default :
							resultados [c] =  ParametrosMetodo[c].toString()
							break

					}
					c++
				}
			}


			if  (resultados.length == 1) {
				if (resultados[0] == "") {
					respuesta =	metodo.invoke( objClase, null)

				}else {
					respuesta =	 metodo.invoke( objClase, resultados)
				}
			}else {
				respuesta =	metodo.invoke( objClase, resultados)
			}
			estado =1

		} catch (InvocationTargetException e) {
			respuesta = "Error Response Switch: ${e.getTargetException().getMessage()}"
			estado =0
		}

		return respuesta
	}



}
