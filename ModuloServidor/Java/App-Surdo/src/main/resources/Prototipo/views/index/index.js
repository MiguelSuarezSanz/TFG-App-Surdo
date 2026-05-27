window.addEventListener('load', llamarJava('cargarMusica,Intro'));

document.querySelectorAll('button')[1].addEventListener('click', () => {
    llamarJava("exit");
});