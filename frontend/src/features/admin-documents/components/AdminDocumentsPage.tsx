import { FileUp, FolderSync, KeyRound, RefreshCcw, ShieldAlert, Trash2 } from 'lucide-react'
import { useRef, useState, type ChangeEvent, type FormEvent } from 'react'
import { useAdminDocuments } from '../hooks/useAdminDocuments'

function formatCountLabel(count: number) {
  return `${count} document${count > 1 ? 's' : ''} indexé${count > 1 ? 's' : ''}`
}

export function AdminDocumentsPage() {
  const [draftApiKey, setDraftApiKey] = useState('')
  const [pendingSourceDelete, setPendingSourceDelete] = useState<string | null>(null)
  const fileInputRef = useRef<HTMLInputElement | null>(null)
  const {
    adminApiKey,
    sources,
    count,
    isLoading,
    isMutating,
    statusMessage,
    errorMessage,
    authenticate,
    refreshDocuments,
    uploadDocument,
    scanDocuments,
    deleteDocument,
    clearAccess,
  } = useAdminDocuments()

  const handleAuthenticate = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const sanitizedKey = draftApiKey.trim()
    if (!sanitizedKey) {
      return
    }

    const authenticated = await authenticate(sanitizedKey)
    if (authenticated) {
      setDraftApiKey('')
    }
  }

  const handleUploadChange = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) {
      return
    }

    await uploadDocument(file)
    event.target.value = ''
  }

  const handleDelete = async (source: string) => {
    const confirmed = globalThis.confirm(`Supprimer les documents indexés pour "${source}" ?`)
    if (!confirmed) {
      return
    }

    setPendingSourceDelete(source)
    await deleteDocument(source)
    setPendingSourceDelete(null)
  }

  if (!adminApiKey) {
    return (
      <section className="min-h-[calc(100vh-140px)] flex items-center justify-center">
        <div className="theme-panel max-w-xl w-full rounded-3xl border p-8">
          <div className="theme-badge inline-flex items-center gap-2 rounded-full border px-4 py-1.5 text-sm mb-4">
            <KeyRound className="w-4 h-4" />
            Interface privée admin
          </div>
          <h1 className="theme-text-primary text-2xl font-semibold mb-3">Gestion documentaire</h1>
          <p className="theme-text-muted leading-relaxed mb-6">
            Cette interface permet de lister, importer, rescanner et supprimer les documents indexés. L accès est protégé par <code>X-API-Key</code> et la clé n est conservée qu en mémoire dans l onglet courant.
          </p>

          <form className="space-y-4" onSubmit={handleAuthenticate}>
            <label className="block">
              <span className="theme-text-secondary text-sm font-medium">Clé admin</span>
              <input
                type="password"
                value={draftApiKey}
                onChange={(event) => setDraftApiKey(event.target.value)}
                placeholder="ADMIN_API_KEY"
                className="mt-2 w-full rounded-2xl border bg-transparent px-4 py-3 outline-none"
                autoComplete="off"
              />
            </label>
            <button
              type="submit"
              disabled={isLoading || !draftApiKey.trim()}
              className="btn-theme-primary inline-flex items-center justify-center gap-2 rounded-2xl px-5 py-3 font-semibold disabled:opacity-60"
            >
              <KeyRound className="w-4 h-4" />
              {isLoading ? 'Validation...' : 'Ouvrir l interface'}
            </button>
          </form>

          {errorMessage && (
            <div className="mt-5 rounded-2xl border border-amber-500/30 bg-amber-500/10 px-4 py-3 text-sm text-amber-100">
              {errorMessage}
            </div>
          )}
        </div>
      </section>
    )
  }

  return (
    <section className="space-y-6">
      <div className="theme-panel rounded-3xl border p-6">
        <div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <div className="theme-badge inline-flex items-center gap-2 rounded-full border px-4 py-1.5 text-sm mb-4">
              <ShieldAlert className="w-4 h-4" />
              Admin documents
            </div>
            <h1 className="theme-text-primary text-3xl font-semibold mb-2">Base documentaire</h1>
            <p className="theme-text-muted max-w-2xl leading-relaxed">
              Gère les sources indexées dans la base RAG sans passer par le filesystem du serveur.
            </p>
            <p className="theme-text-secondary text-sm mt-3">{formatCountLabel(count)}</p>
          </div>

          <div className="flex flex-wrap gap-3">
            <button
              type="button"
              onClick={() => void refreshDocuments()}
              disabled={isLoading || isMutating}
              className="btn-theme-secondary inline-flex items-center justify-center gap-2 rounded-2xl border px-4 py-3 disabled:opacity-60"
            >
              <RefreshCcw className="w-4 h-4" />
              Actualiser
            </button>
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              disabled={isLoading || isMutating}
              className="btn-theme-secondary inline-flex items-center justify-center gap-2 rounded-2xl border px-4 py-3 disabled:opacity-60"
            >
              <FileUp className="w-4 h-4" />
              Importer
            </button>
            <button
              type="button"
              onClick={() => void scanDocuments()}
              disabled={isLoading || isMutating}
              className="btn-theme-primary inline-flex items-center justify-center gap-2 rounded-2xl px-4 py-3 font-semibold disabled:opacity-60"
            >
              <FolderSync className="w-4 h-4" />
              Lancer un scan
            </button>
            <button
              type="button"
              onClick={() => clearAccess('Clé admin effacée localement.')}
              className="theme-icon-button rounded-2xl border px-4 py-3"
            >
              Effacer la clé
            </button>
            <input
              ref={fileInputRef}
              type="file"
              className="hidden"
              onChange={(event) => void handleUploadChange(event)}
            />
          </div>
        </div>

        {statusMessage && (
          <div className="mt-5 rounded-2xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-sm text-emerald-100">
            {statusMessage}
          </div>
        )}
        {errorMessage && (
          <div className="mt-5 rounded-2xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-100">
            {errorMessage}
          </div>
        )}
      </div>

      <div className="theme-panel rounded-3xl border p-6">
        <div className="flex items-center justify-between gap-3 mb-5">
          <h2 className="theme-text-primary text-xl font-semibold">Sources indexées</h2>
          {isLoading && <span className="theme-text-muted text-sm">Chargement...</span>}
        </div>

        {sources.length === 0 ? (
          <div className="rounded-2xl border border-dashed p-8 text-center theme-text-muted">
            Aucun document indexé.
          </div>
        ) : (
          <div className="space-y-3">
            {sources.map((source) => (
              <div key={source} className="theme-surface flex flex-col gap-3 rounded-2xl border p-4 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <div className="theme-text-primary font-medium break-all">{source}</div>
                  <div className="theme-text-muted text-sm">Source indexée dans Qdrant</div>
                </div>
                <button
                  type="button"
                  onClick={() => void handleDelete(source)}
                  disabled={isMutating && pendingSourceDelete === source}
                  className="inline-flex items-center justify-center gap-2 rounded-2xl border border-rose-500/30 px-4 py-2 text-sm text-rose-200 disabled:opacity-60"
                >
                  <Trash2 className="w-4 h-4" />
                  {isMutating && pendingSourceDelete === source ? 'Suppression...' : 'Supprimer'}
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </section>
  )
}
