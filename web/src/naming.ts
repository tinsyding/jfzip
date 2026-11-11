const MIME_EXTENSIONS: Record<string, string> = {
  'image/jpeg': 'jpg',
  'image/jpg': 'jpg',
  'image/png': 'png',
  'image/webp': 'webp',
  'image/heic': 'heic',
  'image/heif': 'heif',
  'image/gif': 'gif',
  'image/bmp': 'bmp',
  'image/avif': 'avif',
};

const ILLEGAL = /[\\/:*?"<>|\p{Cc}]/gu;

function trimEdges(value: string): string {
  return value.replace(/^[\s.]+|[\s.]+$/g, '');
}

export function todayPrefix(date = new Date()): string {
  const year = String(date.getFullYear() % 100).padStart(2, '0');
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}.${month}.${day}`;
}

export function sanitizeName(raw: string): string {
  const cleaned = trimEdges(
    trimEdges(raw.trim().replace(ILLEGAL, '_')).slice(0, 24),
  );
  return cleaned || '未命名';
}

export function archiveFileName(prefix: string, rawName: string): string {
  return `${prefix}-${sanitizeName(rawName)}.zip`;
}

export function imageEntryName(prefix: string, index: number, extension: string): string {
  return `${prefix}-${String(index).padStart(2, '0')}.${extension}`;
}

export function extensionFor(file: File): string {
  const fromMime = MIME_EXTENSIONS[file.type.toLowerCase()];
  if (fromMime) return fromMime;

  const fromName = file.name.split('.').pop()?.toLowerCase() ?? '';
  if (/^[a-z0-9]{2,5}$/.test(fromName)) return fromName;

  return 'jpg';
}
