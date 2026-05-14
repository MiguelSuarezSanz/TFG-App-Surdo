import java.util.ArrayList;

public class PuenteJava {
	
	static Reproductor musicHandeler = new Reproductor();

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

		default:
			System.err.println("Método no encontrado");
			break;
		}
    }

    public void enviarTexto(String texto) {
        System.out.println("Texto recibido desde HTML: " + texto);
    }

}
