import { WorkerEnv } from './types';

const textEncoder = new TextEncoder();

let cachedCryptoKey: CryptoKey | null = null;
let cachedKeySource = '';

function isSegmentEncryptionEnabled(env: WorkerEnv): boolean {
  return (env.SEGMENT_ENCRYPTION_ENABLED || '').toLowerCase() === 'true';
}

function decodeBase64Key(base64Key: string): ArrayBuffer {
  const cleaned = base64Key.replace(/\s+/g, '');
  const binaryString = atob(cleaned);
  const len = binaryString.length;
  const bytes = new Uint8Array(len);
  for (let i = 0; i < len; i++) {
    bytes[i] = binaryString.charCodeAt(i);
  }
  return bytes.buffer;
}

async function importSegmentKey(env: WorkerEnv): Promise<CryptoKey> {
  const keySource = env.SEGMENT_ENCRYPTION_KEY || '';

  if (!keySource) {
    throw new Error('SEGMENT_ENCRYPTION_KEY 未配置');
  }

  if (cachedCryptoKey && cachedKeySource === keySource) {
    return cachedCryptoKey;
  }

  const keyBuffer = decodeBase64Key(keySource);
  cachedCryptoKey = await crypto.subtle.importKey(
    'raw',
    keyBuffer,
    { name: 'AES-CTR' },
    false,
    ['encrypt', 'decrypt']
  );
  cachedKeySource = keySource;
  return cachedCryptoKey;
}

async function deriveCounter(segmentId: string, env: WorkerEnv): Promise<Uint8Array> {
  const salt = env.SEGMENT_ENCRYPTION_SALT || 'chigua-hls-segment';
  const input = `${salt}:${segmentId}`;
  const hash = await crypto.subtle.digest('SHA-256', textEncoder.encode(input));
  return new Uint8Array(hash).slice(0, 16); // AES-CTR counter 16 bytes
}

export async function encryptSegmentPayload(
  payload: ArrayBuffer,
  segmentId: string,
  env: WorkerEnv
): Promise<Uint8Array> {
  if (!isSegmentEncryptionEnabled(env)) {
    return new Uint8Array(payload);
  }

  const cryptoKey = await importSegmentKey(env);
  const counter = await deriveCounter(segmentId, env);
  const encrypted = await crypto.subtle.encrypt(
    {
      name: 'AES-CTR',
      counter,
      length: 64,
    },
    cryptoKey,
    payload
  );
  return new Uint8Array(encrypted);
}

export function buildSegmentEncryptionMetadata(segmentId: string, env: WorkerEnv) {
  return {
    algorithm: env.SEGMENT_ENCRYPTION_ALGORITHM || 'AES-CTR',
    salt: env.SEGMENT_ENCRYPTION_SALT || 'chigua-hls-segment',
    segmentId,
  };
}

export function isSegmentEncryptionActive(env: WorkerEnv): boolean {
  return isSegmentEncryptionEnabled(env) && Boolean(env.SEGMENT_ENCRYPTION_KEY);
}
