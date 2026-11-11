import { useEffect, useMemo, useRef, useState } from 'preact/hooks';
import { IconAdd, IconBack, IconClose, IconSettings, IconShare } from './icons';
import { archiveFileName, todayPrefix } from './naming';
import { createZip } from './zip';

const NAME_KEY = 'jfzip.name';

type ImageItem = {
  id: string;
  file: File;
  url: string;
};

type BuildState =
  | { kind: 'idle' }
  | { kind: 'building'; done: number; total: number }
  | { kind: 'ready'; file: File; name: string; size: number; count: number };

type ShareNavigator = Navigator & {
  canShare?: (data: ShareData) => boolean;
};

function newId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

function readDefaultName(): string {
  try {
    return localStorage.getItem(NAME_KEY) ?? '';
  } catch {
    return '';
  }
}

function saveDefaultName(value: string) {
  try {
    localStorage.setItem(NAME_KEY, value);
  } catch {
    // Storage can be unavailable in private mode.
  }
}

function fileKey(file: File): string {
  return `${file.name}|${file.size}|${file.lastModified}`;
}

function isImageFile(file: File): boolean {
  return file.type.startsWith('image/') || /\.(jpe?g|png|webp|heic|heif|gif|bmp|avif)$/i.test(file.name);
}

function pad2(value: number): string {
  return String(value).padStart(2, '0');
}

function formatSize(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes <= 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  let value = bytes;
  let index = 0;
  while (value >= 1024 && index < units.length - 1) {
    value /= 1024;
    index += 1;
  }
  const digits = index === 0 || value >= 100 ? 0 : 1;
  return `${value.toFixed(digits)} ${units[index]}`;
}

function downloadFile(file: File) {
  const url = URL.createObjectURL(file);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = file.name;
  anchor.rel = 'noopener';
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 10_000);
}

export function App() {
  const [items, setItems] = useState<ImageItem[]>([]);
  const [defaultName, setDefaultName] = useState<string>(() => readDefaultName().slice(0, 24));
  const [batchName, setBatchName] = useState(defaultName);
  const [nameEdited, setNameEdited] = useState(false);
  const [prefix, setPrefix] = useState<string>(() => todayPrefix());
  const [build, setBuild] = useState<BuildState>({ kind: 'idle' });
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  const fileInput = useRef<HTMLInputElement>(null);
  const buildToken = useRef(0);
  const generating = useRef(false);
  const itemsRef = useRef(items);
  const buildRef = useRef(build);

  itemsRef.current = items;
  buildRef.current = build;

  useEffect(() => {
    return () => {
      itemsRef.current.forEach((item) => URL.revokeObjectURL(item.url));
    };
  }, []);

  useEffect(() => {
    const update = () => {
      if (buildRef.current.kind !== 'ready') setPrefix(todayPrefix());
    };
    const timer = window.setInterval(update, 60_000);
    document.addEventListener('visibilitychange', update);
    return () => {
      window.clearInterval(timer);
      document.removeEventListener('visibilitychange', update);
    };
  }, []);

  useEffect(() => {
    if (!message) return;
    const timer = window.setTimeout(() => setMessage(null), 2400);
    return () => window.clearTimeout(timer);
  }, [message]);

  const settingsPreview = useMemo(
    () => archiveFileName(prefix, defaultName),
    [prefix, defaultName],
  );

  function resetBuild() {
    buildToken.current += 1;
    generating.current = false;
    setBuild({ kind: 'idle' });
    setPrefix(todayPrefix());
  }

  function addFiles(fileList: FileList | File[]) {
    const incoming = Array.from(fileList).filter(isImageFile);
    if (incoming.length === 0) return;

    resetBuild();
    setItems((current) => {
      const seen = new Set(current.map((item) => fileKey(item.file)));
      const next = [...current];
      for (const file of incoming) {
        const key = fileKey(file);
        if (seen.has(key)) continue;
        seen.add(key);
        next.push({ id: newId(), file, url: URL.createObjectURL(file) });
      }
      return next;
    });
  }

  function removeItem(id: string) {
    resetBuild();
    setItems((current) => {
      const target = current.find((item) => item.id === id);
      if (target) URL.revokeObjectURL(target.url);
      return current.filter((item) => item.id !== id);
    });
  }

  function clearItems() {
    resetBuild();
    setItems((current) => {
      current.forEach((item) => URL.revokeObjectURL(item.url));
      return [];
    });
  }

  function changeBatchName(value: string) {
    setBatchName(value.slice(0, 24));
    setNameEdited(true);
    resetBuild();
  }

  function changeDefaultName(value: string) {
    const next = value.slice(0, 24);
    setDefaultName(next);
    saveDefaultName(next);
    if (!nameEdited) {
      setBatchName(next);
      resetBuild();
    }
  }

  function openPicker() {
    fileInput.current?.click();
  }

  async function generate() {
    if (items.length === 0 || generating.current) return;

    const token = buildToken.current + 1;
    buildToken.current = token;
    generating.current = true;

    const currentPrefix = todayPrefix();
    const files = items.map((item) => item.file);
    const name = archiveFileName(currentPrefix, batchName);

    setPrefix(currentPrefix);
    setBuild({ kind: 'building', done: 0, total: files.length });

    try {
      const blob = await createZip(
        files,
        currentPrefix,
        (done) => {
          if (buildToken.current !== token) return;
          setBuild((current) =>
            current.kind === 'building' ? { ...current, done } : current,
          );
        },
        () => buildToken.current !== token,
      );

      if (buildToken.current !== token) return;

      const file = new File([blob], name, {
        type: 'application/zip',
        lastModified: Date.now(),
      });
      setBuild({
        kind: 'ready',
        file,
        name,
        size: file.size,
        count: files.length,
      });
    } catch (error) {
      if (buildToken.current !== token) return;
      if (error instanceof DOMException && error.name === 'AbortError') return;
      setBuild({ kind: 'idle' });
      setMessage('生成失败');
    } finally {
      if (buildToken.current === token) generating.current = false;
    }
  }

  async function share() {
    if (build.kind !== 'ready') return;

    const file = build.file;
    const data: ShareData = { files: [file], title: file.name };
    const shareNavigator = navigator as ShareNavigator;

    try {
      if (
        typeof shareNavigator.share === 'function' &&
        (!shareNavigator.canShare || shareNavigator.canShare(data))
      ) {
        await shareNavigator.share(data);
        return;
      }
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') return;
    }

    downloadFile(file);
  }

  if (settingsOpen) {
    return (
      <Settings
        name={defaultName}
        preview={settingsPreview}
        onNameChange={changeDefaultName}
        onBack={() => setSettingsOpen(false)}
      />
    );
  }

  return (
    <div
      className="app"
      onDragOver={(event) => event.preventDefault()}
      onDrop={(event) => {
        event.preventDefault();
        const files = event.dataTransfer?.files;
        if (files) addFiles(files);
      }}
    >
      <header className="topbar">
        <div className="brand">JFZip</div>
        <button
          type="button"
          className="icon-button"
          aria-label="设置"
          onClick={() => setSettingsOpen(true)}
        >
          <IconSettings />
        </button>
      </header>

      <main className="main">
        <section className="archive-card" aria-label="压缩包">
          <div className="card-label">压缩包</div>
          <div className="archive-row">
            <span className="date-stamp">{prefix}</span>
            <span className="dash">-</span>
            <input
              className="name-input"
              value={batchName}
              maxlength={24}
              placeholder="姓名"
              aria-label="姓名"
              autocomplete="off"
              spellcheck={false}
              onInput={(event) => changeBatchName(event.currentTarget.value)}
            />
            <span className="zip-suffix">.zip</span>
          </div>
        </section>

        <div className="section-head">
          <h2>图片</h2>
          {items.length > 0 && (
            <div className="section-meta">
              <span className="count">{items.length} 张</span>
              <button type="button" className="text-button" onClick={clearItems}>
                清空
              </button>
            </div>
          )}
        </div>

        {items.length === 0 ? (
          <button type="button" className="empty-pick" onClick={openPicker}>
            <IconAdd size={26} />
            <span>选择图片</span>
          </button>
        ) : (
          <div className="grid">
            {items.map((item, index) => (
              <div className="image-cell" key={item.id}>
                <img src={item.url} alt="" loading="lazy" />
                <span className="index">{pad2(index + 1)}</span>
                <button
                  type="button"
                  className="remove"
                  aria-label={`移除第 ${index + 1} 张`}
                  onClick={() => removeItem(item.id)}
                >
                  <IconClose />
                </button>
              </div>
            ))}
            <button type="button" className="add-cell" aria-label="添加图片" onClick={openPicker}>
              <IconAdd />
            </button>
          </div>
        )}
      </main>

      <BottomBar build={build} count={items.length} onGenerate={generate} onShare={share} />

      <input
        ref={fileInput}
        className="file-input"
        type="file"
        accept="image/*"
        multiple
        onChange={(event) => {
          const input = event.currentTarget;
          if (input.files) addFiles(input.files);
          input.value = '';
        }}
      />

      {message && (
        <div className="toast" role="status" aria-live="polite">
          {message}
        </div>
      )}
    </div>
  );
}

type BottomBarProps = {
  build: BuildState;
  count: number;
  onGenerate: () => void;
  onShare: () => void;
};

function BottomBar({ build, count, onGenerate, onShare }: BottomBarProps) {
  if (build.kind === 'building') {
    const progress = build.total === 0 ? 0 : build.done / build.total;
    return (
      <div className="bottom-bar">
        <div className="progress-meta">
          <span>
            {pad2(build.done)} / {pad2(build.total)}
          </span>
        </div>
        <div className="progress-track">
          <div className="progress-fill" style={{ transform: `scaleX(${progress})` }} />
        </div>
        <button type="button" className="button primary full" disabled>
          生成中
        </button>
      </div>
    );
  }

  if (build.kind === 'ready') {
    return (
      <div className="bottom-bar">
        <div className="ready-summary">
          <div className="ready-name">{build.name}</div>
          <div className="ready-meta">
            {build.count} 张 · {formatSize(build.size)}
          </div>
        </div>
        <div className="action-row">
          <button type="button" className="button secondary" onClick={onGenerate}>
            重新生成
          </button>
          <button type="button" className="button primary" onClick={onShare}>
            <IconShare />
            分享
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="bottom-bar">
      <button
        type="button"
        className="button primary full"
        disabled={count === 0}
        onClick={onGenerate}
      >
        生成压缩包
      </button>
    </div>
  );
}

type SettingsProps = {
  name: string;
  preview: string;
  onNameChange: (value: string) => void;
  onBack: () => void;
};

function Settings({ name, preview, onNameChange, onBack }: SettingsProps) {
  return (
    <div className="app">
      <header className="topbar">
        <button type="button" className="icon-button" aria-label="返回" onClick={onBack}>
          <IconBack />
        </button>
        <h1 className="topbar-title">设置</h1>
      </header>

      <main className="settings-main">
        <label className="field-label" for="default-name">
          姓名
        </label>
        <input
          id="default-name"
          className="settings-input"
          value={name}
          maxlength={24}
          placeholder="未命名"
          autocomplete="off"
          spellcheck={false}
          onInput={(event) => onNameChange(event.currentTarget.value)}
        />

        <div className="preview-block">
          <div className="field-label">压缩包</div>
          <div className="preview-name">{preview}</div>
        </div>
      </main>
    </div>
  );
}
