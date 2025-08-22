let wavesurfer;
let regionsPlugin;
let currentFile;

function init() {
  wavesurfer = WaveSurfer.create({
    container: '#waveform',
    waveColor: '#a0a0a0',
    progressColor: '#555',
    plugins: [
      WaveSurfer.regions.create(),
      WaveSurfer.timeline.create({
        container: '#timeline'
      })
    ]
  });

  regionsPlugin = wavesurfer.getActivePlugins().regions;

  document.getElementById('play').onclick = () => wavesurfer.play();
  document.getElementById('pause').onclick = () => wavesurfer.pause();
  document.getElementById('stop').onclick = () => wavesurfer.stop();
  document.getElementById('export').onclick = exportSelection;

  const drop = document.getElementById('drop');
  const fileInput = document.getElementById('fileInput');

  drop.addEventListener('click', () => fileInput.click());
  drop.addEventListener('dragover', (e) => {
    e.preventDefault();
    drop.classList.add('dragover');
  });
  drop.addEventListener('dragleave', () => drop.classList.remove('dragover'));
  drop.addEventListener('drop', (e) => {
    e.preventDefault();
    drop.classList.remove('dragover');
    handleFiles(e.dataTransfer.files);
  });
  fileInput.addEventListener('change', (e) => handleFiles(e.target.files));

  regionsPlugin.on('region-click', (region, e) => {
    e.stopPropagation();
    region.playLoop();
  });
}

async function handleFiles(files) {
  if (!files.length) return;
  const file = files[0];
  if (!file.type.startsWith('audio')) return;
  currentFile = file;
  wavesurfer.loadBlob(file);

  const formData = new FormData();
  formData.append('file', file);
  try {
    const res = await fetch('/api/extract', {
      method: 'POST',
      body: formData
    });
    if (res.ok) {
      const data = await res.json();
      wavesurfer.once('ready', () => showSuggestedLoops(data.loops));
    }
  } catch (e) {
    console.error(e);
  }
}

function showSuggestedLoops(loops) {
  regionsPlugin.clearRegions();
  loops.forEach(({ start, end }) =>
    regionsPlugin.addRegion({
      start,
      end,
      color: 'rgba(0, 255, 0, 0.1)',
      drag: false,
      resize: false
    })
  );
}

async function exportSelection() {
  if (!currentFile) return;
  const regions = Object.values(regionsPlugin.regions);
  if (!regions.length) return;
  const { start, end } = regions[0];
  const formData = new FormData();
  formData.append('file', currentFile);
  formData.append('start', start);
  formData.append('end', end);

  const res = await fetch('/api/export', {
    method: 'POST',
    body: formData
  });

  if (res.ok) {
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'export.wav';
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  }
}

document.addEventListener('DOMContentLoaded', init);
