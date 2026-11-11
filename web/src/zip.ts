import { Zip, ZipPassThrough } from 'fflate';
import { extensionFor, imageEntryName } from './naming';

export function createZip(
  files: File[],
  prefix: string,
  onProgress: (done: number) => void,
  isCancelled: () => boolean,
): Promise<Blob> {
  return new Promise((resolve, reject) => {
    const chunks: Uint8Array[] = [];
    const zip = new Zip((error, chunk, final) => {
      if (error) {
        reject(error);
        return;
      }
      if (chunk.length > 0) chunks.push(chunk);
      if (final) {
        resolve(new Blob(chunks as BlobPart[], { type: 'application/zip' }));
      }
    });

    const run = async () => {
      for (let index = 0; index < files.length; index += 1) {
        if (isCancelled()) {
          reject(new DOMException('Cancelled', 'AbortError'));
          return;
        }

        const file = files[index];
        const entry = new ZipPassThrough(imageEntryName(prefix, index + 1, extensionFor(file)));
        zip.add(entry);

        const bytes = new Uint8Array(await file.arrayBuffer());
        if (isCancelled()) {
          reject(new DOMException('Cancelled', 'AbortError'));
          return;
        }

        entry.push(bytes, true);
        onProgress(index + 1);

        await new Promise<void>((next) => window.setTimeout(next, 0));
      }

      zip.end();
    };

    run().catch(reject);
  });
}
