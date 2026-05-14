let participantes = localStorage.getItem("participantes").split(",");
let puntuaciones = localStorage.getItem("puntuaciones").split(",");

if (puntuaciones.length != participantes.length) {
    puntuaciones = [];
    participantes.forEach(element => {
        puntuaciones.push(0);
    });
}

function renderizar_puntuacioness() {
    let participants = document.querySelector('.participants');
    let puntuacionesOrdenadas = ordenarPuntuaciones();
    let localStorage = ""
    
    participants.innerHTML = "";

    for (const key in puntuacionesOrdenadas) {
        const element = object[key];

        let span = document.createElement('b');
        span.innerHTML = `${key}: ${element}`;
        participants.append(span);
    }

    puntuaciones.forEach(element => {
        localStorage += element+",";
    });
    
    localStorage.setItem("puntuaciones", localStorage.slice(0, -1));
}

function annadir_puntuaciones(participante, puntuacion) {
    let index = participante.indexOf(participante);
    if (index == -1) {
        return;
    }
    puntuacion[index] += puntuacion;
    renderizar_puntuacioness();
}

function ordenarPuntuaciones() {
    let ranking = [];

    for (let i = 0; i < participantes.length; i++) {
        ranking.push({
            key: Number(puntuaciones[i]),
            value: participantes[i]
        });
    }

    ranking.sort((a, b) => a.key - b.key);

    return ranking;
}
