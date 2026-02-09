import { useState } from 'react'
import { CheckCircle, AlertCircle, XCircle, FileCode } from 'lucide-react'

interface ValidationResult {
  valid: boolean
  errors: string[]
  warnings: string[]
}

function validateManifest(yaml: string): ValidationResult {
  const errors: string[] = []
  const warnings: string[] = []

  if (!yaml.trim()) {
    return { valid: false, errors: ['Manifest is empty'], warnings: [] }
  }

  if (!yaml.includes('applications:')) {
    errors.push('Missing required "applications:" section')
  }

  if (!yaml.includes('name:')) {
    errors.push('Missing required "name" field')
  }

  if (!yaml.includes('memory:')) {
    warnings.push('No memory specified - will default to 1G')
  }

  if (!yaml.includes('buildpacks:') && !yaml.includes('buildpack:')) {
    warnings.push('No buildpack specified - CF will auto-detect')
  }

  if (yaml.includes('instances:')) {
    const match = yaml.match(/instances:\s*(\d+)/)
    if (match && parseInt(match[1]) > 10) {
      warnings.push(`High instance count (${match[1]}) - verify this is intended`)
    }
  }

  if (yaml.includes('disk_quota:')) {
    const match = yaml.match(/disk_quota:\s*(\d+)([GM])/)
    if (match && match[2] === 'G' && parseInt(match[1]) > 4) {
      warnings.push(`Large disk quota (${match[1]}G) - verify this is needed`)
    }
  }

  if (yaml.includes('no-route: true') && yaml.includes('routes:')) {
    errors.push('"no-route: true" conflicts with "routes:" section')
  }

  return { valid: errors.length === 0, errors, warnings }
}

export function ManifestValidator() {
  const [manifest, setManifest] = useState(`---
applications:
  - name: my-app
    memory: 1G
    instances: 1
    buildpacks:
      - java_buildpack
    services:
      - my-db
`)
  const [result, setResult] = useState<ValidationResult | null>(null)

  const handleValidate = () => {
    setResult(validateManifest(manifest))
  }

  return (
    <div style={{ padding: '20px', maxWidth: '700px' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px' }}>
        <FileCode size={18} color="var(--accent)" />
        <h3 style={{ fontSize: '15px', fontWeight: 600 }}>Manifest Validator</h3>
      </div>

      <textarea
        value={manifest}
        onChange={(e) => setManifest(e.target.value)}
        style={{
          width: '100%',
          height: '250px',
          padding: '12px',
          backgroundColor: 'var(--bg-primary)',
          border: '1px solid var(--border)',
          borderRadius: '6px',
          color: 'var(--text-primary)',
          fontFamily: "'JetBrains Mono', monospace",
          fontSize: '13px',
          resize: 'vertical',
          outline: 'none',
        }}
      />

      <button
        onClick={handleValidate}
        style={{
          marginTop: '12px',
          padding: '8px 20px',
          backgroundColor: 'var(--accent)',
          color: 'white',
          border: 'none',
          borderRadius: '6px',
          fontSize: '13px',
          fontWeight: 500,
        }}
      >
        Validate
      </button>

      {result && (
        <div style={{ marginTop: '16px' }}>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              marginBottom: '12px',
              fontSize: '14px',
              fontWeight: 600,
              color: result.valid ? 'var(--success)' : 'var(--danger)',
            }}
          >
            {result.valid ? <CheckCircle size={18} /> : <XCircle size={18} />}
            {result.valid ? 'Manifest is valid' : 'Manifest has errors'}
          </div>

          {result.errors.map((err, i) => (
            <div
              key={`e${i}`}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                padding: '8px 12px',
                marginBottom: '6px',
                backgroundColor: 'rgba(248, 81, 73, 0.1)',
                borderRadius: '4px',
                fontSize: '13px',
                color: 'var(--danger)',
              }}
            >
              <XCircle size={14} /> {err}
            </div>
          ))}
          {result.warnings.map((warn, i) => (
            <div
              key={`w${i}`}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                padding: '8px 12px',
                marginBottom: '6px',
                backgroundColor: 'rgba(210, 153, 34, 0.1)',
                borderRadius: '4px',
                fontSize: '13px',
                color: 'var(--warning)',
              }}
            >
              <AlertCircle size={14} /> {warn}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
