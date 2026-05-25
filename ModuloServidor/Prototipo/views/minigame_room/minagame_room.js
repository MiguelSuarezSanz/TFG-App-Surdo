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

    puntuaciones[index] += puntos;

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

function establecerMinijuego(nombre, descripcion) {

    document.querySelector('.main_tittle')
        .innerHTML = nombre;

    document.querySelector('.subtittle')
        .innerHTML = descripcion;
}