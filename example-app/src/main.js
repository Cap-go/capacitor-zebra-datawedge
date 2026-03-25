import './style.css';
import { ZebraDataWedge, ZebraQuery, ZebraRuntime } from '@capgo/capacitor-zebra-datawedge';

const output = document.getElementById('plugin-output');
const statusBadge = document.getElementById('status-badge');
const refreshButton = document.getElementById('refresh-status');
const profileButton = document.getElementById('get-active-profile');
const versionButton = document.getElementById('get-version');
const scanButton = document.getElementById('soft-scan');
const actionInput = document.getElementById('intent-action');

const setOutput = (value) => {
  output.textContent = typeof value === 'string' ? value : JSON.stringify(value, null, 2);
};

const setStatus = (enabled) => {
  statusBadge.textContent = enabled ? 'Enabled' : 'Disabled';
  statusBadge.dataset.enabled = String(enabled);
};

const readIntentAction = () => actionInput.value.trim();

ZebraDataWedge.addListener('scan', (event) => {
  setOutput({
    kind: 'scan',
    event,
  });
}).catch(() => {
  // Web and non-Zebra devices reject listener registration. The buttons will show the platform error.
});

const refreshStatus = async () => {
  try {
    const result = await ZebraQuery.getDatawedgeStatus();
    setStatus(result.isEnabled);
    setOutput(result);
  } catch (error) {
    setOutput(`Error: ${error?.message ?? error}`);
  }
};

refreshButton.addEventListener('click', refreshStatus);

profileButton.addEventListener('click', async () => {
  try {
    const profileName = await ZebraQuery.getActiveProfile();
    setOutput({ profileName });
  } catch (error) {
    setOutput(`Error: ${error?.message ?? error}`);
  }
});

versionButton.addEventListener('click', async () => {
  try {
    const result = await ZebraQuery.getVersionInfo();
    setOutput(result);
  } catch (error) {
    setOutput(`Error: ${error?.message ?? error}`);
  }
});

scanButton.addEventListener('click', async () => {
  try {
    const intentAction = readIntentAction();
    const result = await ZebraRuntime.softScanTrigger(intentAction);
    setOutput({
      kind: 'softScanTrigger',
      result,
    });
  } catch (error) {
    setOutput(`Error: ${error?.message ?? error}`);
  }
});

refreshStatus();
