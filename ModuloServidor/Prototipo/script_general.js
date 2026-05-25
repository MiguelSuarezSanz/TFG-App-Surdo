function llamarJava(metodod) {
    cefQuery({
        request: metodod,
        onSuccess: function(response) {
            console.log("Java respondió:", response);
        },
        onFailure: function(error_code, error_message) {
            console.error("Error:", error_code, error_message);
        }
    });
}

function cambiarPagina(ruta) {
    window.location.href = ruta;
}