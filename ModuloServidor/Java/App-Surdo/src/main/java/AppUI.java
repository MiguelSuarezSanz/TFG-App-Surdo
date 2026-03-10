import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;

import javax.swing.*;
import java.awt.*;
import java.io.File;

import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.handler.CefLoadHandler;

public class AppUI {

    public static void iniciar(String[] args) throws Exception {

        // Configurar JCEF
        CefAppBuilder builder = new CefAppBuilder();
        builder.getCefSettings().windowless_rendering_enabled = false;

        builder.addJcefArgs("--disable-web-security");
     	builder.getCefSettings().command_line_args_disabled = false;

     	// Añadir flags de Chromium
     	builder.addJcefArgs("--allow-file-access-from-files");

        builder.setAppHandler(new MavenCefAppHandlerAdapter() {
            @Override
            public void stateHasChanged(CefApp.CefAppState state) {
                if (state == CefApp.CefAppState.TERMINATED) {
                    System.exit(0);
                }
            }
        });

        CefApp cefApp = builder.build();
        CefClient client = cefApp.createClient();

        // Puente JS → Java
        PuenteJava puente = new PuenteJava();

        CefMessageRouter router = CefMessageRouter.create(
            new CefMessageRouter.CefMessageRouterConfig("cefQuery", "cefQueryCancel")
        );

        router.addHandler(new CefMessageRouterHandlerAdapter() {
            @Override
            public boolean onQuery(CefBrowser browser, CefFrame frame,
                                   long queryId, String request,
                                   boolean persistent, CefQueryCallback callback) {
                // Llamamos al mismo método que tenías en PuenteJava
                puente.botonPulsado(request);
                callback.success("OK");
                return true;
            }
        }, true);

        client.addMessageRouter(router);

        // Cargar el HTML con ruta absoluta
        File archivoHtml = new File("../../Prototipo/views/index/index.html").getAbsoluteFile();
        CefBrowser browser = client.createBrowser(
            archivoHtml.toURI().toString(),
            false,
            false
        );

        // Ventana Swing maximizada
        JFrame ventana = new JFrame("App-Surdo");
        ventana.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        ventana.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                cefApp.dispose();
                ventana.dispose();
            }
        });

        ventana.getContentPane().add(browser.getUIComponent(), BorderLayout.CENTER);
        ventana.setExtendedState(JFrame.MAXIMIZED_BOTH);
        ventana.setSize(1024, 768);
        ventana.setVisible(true);

        System.out.println("Launched");
        
        
    }
}