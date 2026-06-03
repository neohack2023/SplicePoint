let wavesurfer;
let regionsPlugin;
let currentFile;
let selectedRegion;
let currentAnalysis;

const API_BASE = window.location.protocol === 'file:' ? 'http://localhost:8080' : '';

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
  drop.addEventListener('dragover', (event) => {
    event.preventDefault();
    drop.classList.add('dragover');
  });
  drop.addEventListener('dragleave', () => drop.classList.remove('dragover'));
  drop.addEventListener('drop', (event) => {
    event.preventDefault();
    drop.classList.remove('dragover');
    handleFiles(event.dataTransfer.files);
  });
  fileInput.addEventListener('change', (event) => handleFiles(event.target.files));

  regionsPlugin.on('region-click', (region, event) => {
    event.stopPropagation();
    selectRegion(region);
    region.playLoop();
  });

  setStatus('Ready. Drop an audio file to begin.');
}

async function handleFiles(files) {
  if (!files.length) return;

  const file = files[0];
  if (!file.type.startsWith('audio')) {
    setStatus('Unsupported file type. Upload an audio file.', true);
    return;
  }

  currentFile = file;
  selectedRegion = null;
  currentAnalysis = null;
  clearCandidateList();
  clearRegions();

  setStatus('Loading waveform and analyzing audio...');

  const readyPromise = new Promise((resolve) => wavesurfer.once('ready', resolve));
  wavesurfer.loadBlob(file);

  try {
    const formData = new FormData();
    formData.append('file', file);

    const response = await fetch(`${API_BASE}/api/extract`, {
      method: 'POST',
      body: formData
    });

    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.message || 'Analysis failed.');
    }

    currentAnalysis = data;
    await readyPromise;
    showSuggestedLoops(data.loops || []);
    renderAnalysis(data);
    setStatus(`Analysis ready: ${formatDuration(data.durationSeconds)} seconds, ${data.loops?.length || 0} loop candidates.`);
  } catch (error) {
    await readyPromise.catch(() => {});
    setStatus(error.message || 'Could not analyze this file.', true);
    showFallbackLoop();
  }
}

function showSuggestedLoops(loops) {
  clearRegions();
  loops.forEach((candidate, index) => {
    const region = regionsPlugin.addRegion({
      start: candidate.start,
      end: candidate.end,
      color: index === 0 ? 'rgba(0, 180, 255, 0.18)' : 'rgba(0, 255, 0, 0.12)',
      drag: true,
      resize: true
    });

    region.splicePointCandidate = candidate;
    if (index === 0) {
      selectRegion(region);
    }
  });
  renderCandidates(loops);
}

function showFallbackLoop() {
  clearRegions();
  const duration = wavesurfer.getDuration();
  if (!Number.isFinite(duration) || duration <= 0) return;

  const region = regionsPlugin.addRegion({
    start: 0,
    end: Math.min(2, duration),
    color: 'rgba(255, 180, 0, 0.18)',
    drag: true,
    resize: true
  });
  selectRegion(region);
}

async function exportSelection() {
  if (!currentFile) {
    setStatus('Upload an audio file before exporting.', true);
    return;
  }

  const region = selectedRegion || getRegionList()[0];
  if (!region) {
    setStatus('Choose or create a loop region before exporting.', true);
    return;
  }

  const start = Number(region.start);
  const end = Number(region.end);
  if (!Number.isFinite(start) || !Number.isFinite(end) || end <= start) {
    setStatus('Selected region is invalid. Adjust the start and end points.', true);
    return;
  }

  setStatus(`Exporting ${formatDuration(end - start)} second loop...`);

  try {
    const formData = new FormData();
    formData.append('file', currentFile);
    formData.append('start', start);
    formData.append('end', end);
    formData.append('fadeMs', 5);

    const response = await fetch(`${API_BASE}/api/export`, {
      method: 'POST',
      body: formData
    });

    if (!response.ok) {
      let message = 'Export failed.';
      try {
        const error = await response.json();
        message = error.message || message;
      } catch (_) {
        // Keep fallback message when server did not return JSON.
      }
      throw new Error(message);
    }

    const blob = await response.blob();
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = buildExportName(start, end);
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    URL.revokeObjectURL(url);
    setStatus('Export complete. WAV loop downloaded.');
  } catch (error) {
    setStatus(error.message || 'Could not export the selected loop.', true);
  }
}

function selectRegion(region) {
  selectedRegion = region;
  const duration = region.end - region.start;
  document.getElementById('selection').textContent =
    `Selected: ${formatDuration(region.start)}s → ${formatDuration(region.end)}s (${formatDuration(duration)}s)`;
}

function clearRegions() {
  if (regionsPlugin?.clearRegions) {
    regionsPlugin.clearRegions();
  }
  selectedRegion = null;
  document.getElementById('selection').textContent = 'Selected: none';
}

function getRegionList() {
  if (regionsPlugin?.getRegions) {
    return regionsPlugin.getRegions();
  }
  return Object.values(regionsPlugin?.regions || {});
}

function renderAnalysis(data) {
  const details = document.getElementById('analysis');
  details.innerHTML = `
    <strong>${escapeHtml(data.fileName || 'uploaded audio')}</strong><br />
    Duration: ${formatDuration(data.durationSeconds)}s<br />
    Sample rate: ${data.sampleRate || 'unknown'} Hz<br />
    Channels: ${data.channels || 'unknown'}<br />
    Engine: ${escapeHtml(data.engineStatus || 'unknown')}
  `;

  const warnings = data.warnings || [];
  document.getElementById('warnings').innerHTML = warnings
    .map((warning) => `<li>${escapeHtml(warning)}</li>`)
    .join('');
}

function renderCandidates(loops) {
  const list = document.getElementById('candidates');
  if (!loops.length) {
    list.innerHTML = '<li>No loop candidates returned.</li>';
    return;
  }

  list.innerHTML = loops.map((candidate, index) => {
    const reasons = (candidate.reasons || []).map(escapeHtml).join(', ');
    return `
      <li>
        <button type="button" data-candidate-index="${index}">
          ${escapeHtml(candidate.label || 'candidate')} · ${formatDuration(candidate.start)}s → ${formatDuration(candidate.end)}s · confidence ${Math.round((candidate.confidence || 0) * 100)}%
        </button>
        <small>${reasons}</small>
      </li>
    `;
  }).join('');

  list.querySelectorAll('button[data-candidate-index]').forEach((button) => {
    button.addEventListener('click', () => {
      const region = getRegionList()[Number(button.dataset.candidateIndex)];
      if (region) {
        selectRegion(region);
        region.playLoop();
      }
    });
  });
}

function clearCandidateList() {
  document.getElementById('analysis').textContent = 'No analysis yet.';
  document.getElementById('warnings').innerHTML = '';
  document.getElementById('candidates').innerHTML = '';
}

function setStatus(message, isError = false) {
  const status = document.getElementById('status');
  status.textContent = message;
  status.classList.toggle('error', isError);
}

function buildExportName(start, end) {
  const base = (currentFile?.name || 'splicepoint-loop')
    .replace(/\.[^.]+$/, '')
    .replace(/[^a-z0-9-_]+/gi, '_')
    .replace(/^_+|_+$/g, '') || 'splicepoint-loop';
  return `${base}_${formatDuration(start)}s-${formatDuration(end)}s.wav`;
}

function formatDuration(value) {
  if (!Number.isFinite(Number(value))) return '0.000';
  return Number(value).toFixed(3);
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

document.addEventListener('DOMContentLoaded', init);
