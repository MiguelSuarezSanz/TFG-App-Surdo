window.addEventListener('load', iniciarPantalla);

function iniciarPantalla() {

    llamarJava('cargarMusica,WaitRoom');

    llamarJava('iniciarServidor');
}

let participantes = []
localStorage.setItem("puntuaciones", "");

function renderizar_participantes() {

    let participants =
        document.querySelector('.participants');

    let participantesStorage = "";

    participants.innerHTML = "";

    participantes.forEach(element => {

        let span = document.createElement('b');

        span.innerHTML = element;

        participants.append(span);

        participantesStorage += element + ",";

    });

    localStorage.setItem(
        "participantes",
        participantesStorage.slice(0, -1)
    );
}

function annadirParticipante(participante) {
    if (participantes.indexOf(participante) == -1) {
        participantes.push(participante);
        renderizar_participantes();
        return;
    }
    console.log(`Participante ${participante} ya ingresado.`);   
}

function removerParticipante(participante) {
    let rmIndex = participantes.indexOf(participante);
    if (rmIndex != -1) {
        participantes.splice(rmIndex, 1);
        renderizar_participantes();
        return;
    }
    console.log(`Participante ${participante} no encontrado.`);   
}

function renderizar_codigo(codigoSala) {
    let code = document.querySelector('.main_tittle');
    code.innerHTML = `Código de la Sala: <br>${codigoSala}`;
}

function mostrarCodigoConexion(codigo) {

    document.getElementById("codigoSala")
        .innerHTML =
            "Código de la Sala:<br>" + codigo;
}

document.getElementById('btnPrep').addEventListener('click', function () {
    llamarJava('prepMinijuego,MainGame');
    window.location.href = '../loanding/loanding.html';
});