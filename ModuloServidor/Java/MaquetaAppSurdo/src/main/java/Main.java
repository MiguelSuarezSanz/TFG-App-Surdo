public class Main {
	
	static String[] arrayMinijuegos = {"Beber la birra", "Pulsa el boton", "No Pulses el boton", "Showdown"};

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int numMinijuegos = 5;
		
		System.out.println("Inicio de juego\n");
		
		for (int i = 0; i < numMinijuegos; i++) {
			
			minijuego();
			
		}
		
	}
	
	private static void minijuego() {
		System.out.println(arrayMinijuegos[(int) Math.floor(Math.random()*arrayMinijuegos.length)]);
	}

}
