import java.io.File;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.concurrent.Worker;
import netscape.javascript.JSObject;

public class AppUI extends Application {
	
	private PuenteJava puente = new PuenteJava();

    @Override
    public void start(Stage escenarioPrincipal) {
    	
        // Creamos el componente web
        WebView navegador = new WebView();
        WebEngine motorWeb = navegador.getEngine();
        
        // Apuntamos a tu archivo HTML local
        File archivoHtml = new File("ui/index.html");
        motorWeb.load(archivoHtml.toURI().toString());
        
        // Escuchamos los eventos del motor web
        motorWeb.getLoadWorker().stateProperty().addListener((observable, viejoEstado, nuevoEstado) -> {
            if (nuevoEstado == Worker.State.SUCCEEDED) {
                
                // Obtenemos la ventana (window) de JavaScript
                JSObject windowJS = (JSObject) motorWeb.executeScript("window");
                
                // Inyectamos nuestro objeto Java y le ponemos el nombre "conexionJava"
                windowJS.setMember("conexionJava", puente);
            }
        });

        // Creamos la "Escena" (el contenido de la ventana)
        Scene escena = new Scene(navegador, 1024, 768);

        // Configuramos la ventana principal
        escenarioPrincipal.setTitle("Mi App Moderna con JavaFX");
        escenarioPrincipal.setScene(escena);
        
        // ¡Magia! Maximizar la ventana sin parpadeos ni problemas
        escenarioPrincipal.setMaximized(true); 
        
        // Mostramos la ventana
        escenarioPrincipal.show();
        
        System.out.println("Launched.");
    
    }

}
