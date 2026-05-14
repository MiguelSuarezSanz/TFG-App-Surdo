import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class Reproductor {

    private Clip clip;
    static int volumenGeneral = 60;

    public void cargar(String nArchivo) {
    	String rutaArchivo = "../../Prototipo/resources/ost/" + nArchivo + ".wav";
        try {
            File archivo = new File(rutaArchivo);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(archivo);

            clip = AudioSystem.getClip();
            clip.open(audioStream);
            this.ajustarVolumen(volumenGeneral);
            this.repetir();
            this.reproducir();

        } catch (UnsupportedAudioFileException e) {
            System.out.println("Formato de audio no soportado.");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Error al leer el archivo.");
            e.printStackTrace();
        } catch (LineUnavailableException e) {
            System.out.println("No se pudo abrir el reproductor de audio.");
            e.printStackTrace();
        }
    }

    public void reproducir() {
        if (clip != null) {
            clip.setFramePosition(0); // Empieza desde el inicio
            clip.start();
        }
    }

    public void pausar() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
        }
    }

    public void continuar() {
        if (clip != null) {
            clip.start();
        }
    }

    public void detener() {
        if (clip != null) {
            clip.stop();
            clip.setFramePosition(0);
        }
    }

    public void repetir() {
        if (clip != null) {
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    public void ajustarVolumen(int volumen) {
        if (clip != null && clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {

            // Limitar el volumen entre 0 y 100
            if (volumen < 0) volumen = 0;
            if (volumen > 100) volumen = 100;

            FloatControl controlVolumen = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);

            float min = controlVolumen.getMinimum();
            float max = controlVolumen.getMaximum();

            float porcentaje = volumen / 100.0f;

            float valor = min + (max - min) * porcentaje;

            controlVolumen.setValue(valor);
        }
    }

}
