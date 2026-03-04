const AUDIO_PATH = "../../resources/ost/WarioWareOst.wav";

const logo = document.getElementById('logo');

// --- Estado de oscilación ---
let angle = 0;
let angleDir = 1;
const MAX_ANGLE = 25;
let angleSpeed  = 1.2;

let audioCtx, analyser, source, dataArray, animId;

async function init() {
  try {
    const response = await fetch(AUDIO_PATH);
    if (!response.ok) throw new Error(`No se pudo cargar: ${AUDIO_PATH}`);
    const arrayBuffer = await response.arrayBuffer();

    audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    analyser = audioCtx.createAnalyser();
    analyser.fftSize = 512;
    dataArray = new Uint8Array(analyser.frequencyBinCount);
    analyser.connect(audioCtx.destination);

    const audioBuffer = await audioCtx.decodeAudioData(arrayBuffer);
    playAudio(audioBuffer);

  } catch (err) {
    console.error(err);
  }
}

function playAudio(audioBuffer) {
  source = audioCtx.createBufferSource();
  source.buffer = audioBuffer;
  source.connect(analyser);
  source.start(0);
  animate();
}

function animate() {
  animId = requestAnimationFrame(animate);
  analyser.getByteFrequencyData(dataArray);

  // Bajo → escala
  const bassEnd = Math.floor(dataArray.length * 0.12);
  let bassSum = 0;
  for (let i = 0; i < bassEnd; i++) bassSum += dataArray[i];
  const bassAvg = bassSum / bassEnd / 255;

  // Energía total → velocidad de oscilación
  let totalSum = 0;
  for (let i = 0; i < dataArray.length; i++) totalSum += dataArray[i];
  const energy = totalSum / dataArray.length / 255;

  const scale = 1 + bassAvg * 0.2;
  angleSpeed = 0.8 + energy * 4;

  angle += angleDir * angleSpeed;
  if (angle >=  MAX_ANGLE) { angle =  MAX_ANGLE; angleDir = -1; }
  if (angle <= -MAX_ANGLE) { angle = -MAX_ANGLE; angleDir =  1; }

  logo.style.transform = `rotate(${angle}deg) scale(${scale})`;

  const glowSize = Math.round(bassAvg * 40);
  const hue = Math.round(50 + energy * 220);
  logo.style.filter = `drop-shadow(0 0 ${glowSize}px hsl(${hue},100%,65%))`;
}

// Los navegadores bloquean AudioContext sin gesto del usuario.
// Se intenta arrancar solo; si falla, se espera el primer clic.
async function tryAutoStart() {
    document.addEventListener('click', () => init(), { once: true });
}

window.addEventListener('load', tryAutoStart);