import { useState } from 'react'
import { CheckCircle, AlertCircle, XCircle, FileCode } from 'lucide-react'
import '../../ui.css'

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
    <div className="content-container-sm">
      <div className="row mb-16">
        <FileCode size={18} color="var(--accent)" />
        <h3 className="text-lg font-semibold">Manifest Validator</h3>
      </div>

      <textarea
        value={manifest}
        onChange={(e) => setManifest(e.target.value)}
        className="form-textarea"
        style={{ width: '100%', height: '250px' }}
      />

      <button
        onClick={handleValidate}
        className="btn-primary"
        style={{ marginTop: '12px' }}
      >
        Validate
      </button>

      {result && (
        <div className="validation-result">
          <div className={`validation-status ${result.valid ? 'validation-status-valid' : 'validation-status-invalid'}`}>
            {result.valid ? <CheckCircle size={18} /> : <XCircle size={18} />}
            {result.valid ? 'Manifest is valid' : 'Manifest has errors'}
          </div>

          {result.errors.map((err, i) => (
            <div key={`e${i}`} className="validation-msg validation-error">
              <XCircle size={14} /> {err}
            </div>
          ))}
          {result.warnings.map((warn, i) => (
            <div key={`w${i}`} className="validation-msg validation-warning">
              <AlertCircle size={14} /> {warn}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
