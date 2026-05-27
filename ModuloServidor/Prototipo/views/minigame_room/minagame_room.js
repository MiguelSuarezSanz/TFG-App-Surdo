let participantes =
    localStorage.getItem("participantes").split(",");

let puntuacionesStorage =
    localStorage.getItem("puntuaciones");

let puntuaciones =
    puntuacionesStorage
        ? puntuacionesStorage.split(",").map(Number)
        : [];

// Si no coinciden tamaños, reiniciamos
if (puntuaciones.length !== participantes.length) {

    puntuaciones = [];

    participantes.forEach(() => {
        puntuaciones.push(0);
    });
}

renderizarPuntuaciones();

function renderizarPuntuaciones() {

    let participants =
        document.querySelector('.participants');

    participants.innerHTML = "";

    let ranking =
        ordenarPuntuaciones();

    ranking.forEach(element => {

        let span =
            document.createElement('span');

        span.innerHTML =
            `${element.nombre}: ${element.puntos}pts`;

        participants.append(span);
    });

    localStorage.setItem(
        "puntuaciones",
        puntuaciones.join(",")
    );
}

function annadirPuntuacion(participante, puntos) {
    
    let index =
        participantes.indexOf(participante);

    if (index === -1) {
        return;
    }

    puntuaciones[index] = puntos;

    renderizarPuntuaciones();
}

function ordenarPuntuaciones() {

    let ranking = [];

    for (let i = 0; i < participantes.length; i++) {

        ranking.push({
            nombre: participantes[i],
            puntos: Number(puntuaciones[i])
        });
    }

    // Mayor puntuación primero
    ranking.sort((a, b) => b.puntos - a.puntos);

    return ranking;
}

function establecerMinijuego(texto) {
    texto=texto.split("@")
    
    document.querySelector('.main_tittle')
        .innerHTML = texto[0];

    document.querySelector('.subtittle')
        .innerHTML = texto[1];
}

document.getElementById('btnIniciar').addEventListener('click', function () {
    llamarJava('iniciarMinijuego');
    this.style.display = "none"
});

document.getElementById('btnSiguiente').addEventListener('click', function () {
    llamarJava('prepMinijuego');
    this.style.display = "none"
    document.getElementById("btnSalir").style.display = "none"
    document.getElementById("btnIniciar").style.display = "block"
});

function habilitarJugar(){
    document.getElementById("btnSiguiente").style.display = "block"
    document.getElementById("btnSalir").style.display = "block"
}