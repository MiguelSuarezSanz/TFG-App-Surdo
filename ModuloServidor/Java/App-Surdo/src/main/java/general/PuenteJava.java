package general;

import java.util.ArrayList;

import org.cef.browser.CefBrowser;

import gestiones_tunnel.ServidorTunnel;

public class PuenteJava {
	
	private static PuenteJava instancia;
	
	private final CefBrowser browser;
	
	static Reproductor musicHandeler = new Reproductor();
	
	public PuenteJava(CefBrowser browser) {
		this.browser = browser;
	    instancia = this;
	}

    public void botonPulsado(String mensaje) {
    	String[] splitted = mensaje.split(",");
    	String metodo = splitted[0];
    	ArrayList<String> args = new ArrayList<String>(); 
    	
    	if (splitted.length > 1) {
			
    		for (int i = 1; i < splitted.length; i++) {
    			args.add(splitted[i]);
			}
    		
		}
    	
    	switch (metodo) {
		case "cargarMusica":
			musicHandeler.detener();
			musicHandeler.cargar(args.get(0));
			break;
			
		case "iniciarServidor":

		    String codigo =
		            ServidorTunnel.iniciar();

		    lamarJavascript(
		            "mostrarCodigoConexion",
		            "'" + codigo + "'"
		    );

		    break;
		    
		case "exit":
            System.exit(0);
            
		default:
			System.err.println("Método no encontrado");
			break;
		}
    }

    public void enviarTexto(String texto) {
        System.out.println("Texto recibido desde HTML: " + texto);
    }
    
    public void lamarJavascript(String metodoJs) {
    	browser.executeJavaScript(metodoJs+"();", browser.getURL(), 0);
	}
    
    public void lamarJavascript(String metodoJs,String args) {
    	browser.executeJavaScript(metodoJs+"("+args+");", browser.getURL(), 0);
	}
    
    public static PuenteJava getInstancia() {

        return instancia;
    }

}
