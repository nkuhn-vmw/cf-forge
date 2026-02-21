import { useState } from 'react'
import { Link } from 'react-router-dom'
import { FileCode, Loader2, CheckCircle, AlertTriangle, ArrowLeft } from 'lucide-react'
import { api } from '../../api/client.ts'
import type { MigrationPlan } from '../../api/client.ts'

export function MigrationAssistant() {
  const [code, setCode] = useState('')
  const [description, setDescription] = useState('')
  const [sourceStack, setSourceStack] = useState('j2ee')
  const [plan, setPlan] = useState<MigrationPlan | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const analyze = async () => {
    setLoading(true)
    setError('')
    try {
      const result = await api.migration.analyze({ code, description, sourceStack })
      setPlan(result)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Unknown error')
    } finally {
      setLoading(false)
    }
  }

  const complexityClass = (score: string) => {
    switch (score) {
      case 'LOW': return 'complexity-low'
      case 'MEDIUM': return 'complexity-medium'
      case 'HIGH': return 'complexity-high'
      case 'CRITICAL': return 'complexity-critical'
      default: return 'text-secondary'
    }
  }

  return (
    <div className="page">
      <header className="page-header">
        <Link to="/dashboard" className="btn-icon">
          <ArrowLeft size={18} />
        </Link>
        <FileCode size={20} color="var(--accent)" />
        <h1 className="page-header-title">AI Migration Assistant</h1>
      </header>

      <div className="content-container">
      <p className="text-secondary mb-24">
        Analyze legacy applications and generate migration plans to Cloud Foundry + Spring Boot
      </p>

      {!plan ? (
        <div className="col-layout gap-16">
          <div>
            <label className="migration-form-label">
              Source Technology Stack
            </label>
            <select
              value={sourceStack}
              onChange={(e) => setSourceStack(e.target.value)}
              className="migration-form-input"
            >
              <option value="j2ee">J2EE / Java EE</option>
              <option value="spring-legacy">Legacy Spring (pre-Boot)</option>
              <option value="dotnet-framework">.NET Framework</option>
              <option value="php">PHP</option>
              <option value="nodejs-legacy">Legacy Node.js</option>
              <option value="ruby-rails">Ruby on Rails</option>
              <option value="python-django">Python Django</option>
              <option value="other">Other</option>
            </select>
          </div>

          <div>
            <label className="migration-form-label">
              Application Description
            </label>
            <input
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="e.g., Customer portal with user auth, order management, payment integration..."
              className="migration-form-input"
            />
          </div>

          <div>
            <label className="migration-form-label">
              Code Samples (paste key files — web.xml, pom.xml, controllers, configs)
            </label>
            <textarea
              value={code}
              onChange={(e) => setCode(e.target.value)}
              rows={16}
              placeholder="Paste representative code snippets here..."
              className="migration-form-textarea"
            />
          </div>

          {error && (
            <div className="row text-danger">
              <AlertTriangle size={16} /> {error}
            </div>
          )}

          <button
            onClick={analyze}
            disabled={loading || !code.trim()}
            className={`migration-btn-primary${loading ? ' btn-disabled' : ''}`}
          >
            {loading ? <Loader2 size={18} className="spin" /> : <FileCode size={18} />}
            {loading ? 'Analyzing...' : 'Analyze & Generate Migration Plan'}
          </button>
        </div>
      ) : (
        <div>
          <button onClick={() => setPlan(null)} className="migration-btn-secondary">
            New Analysis
          </button>

          <div className="grid-auto-200 mb-24">
            <div className="migration-card">
              <div className="migration-card-label">Complexity</div>
              <div className={`text-4xl font-bold ${complexityClass(plan.complexityScore)}`}>
                {plan.complexityScore}
              </div>
            </div>
            <div className="migration-card">
              <div className="migration-card-label">Source</div>
              <div className="text-2xl font-semibold">{plan.sourceStack}</div>
            </div>
            <div className="migration-card">
              <div className="migration-card-label">Target</div>
              <div className="text-2xl font-semibold">{plan.targetStack}</div>
            </div>
            <div className="migration-card">
              <div className="migration-card-label">Steps</div>
              <div className="text-4xl font-bold text-accent">{plan.steps?.length ?? 0}</div>
            </div>
          </div>

          <h2 className="text-3xl mb-12">Migration Steps</h2>
          <div className="col-layout gap-8 mb-24">
            {plan.steps?.map((step) => (
              <div key={step.order} className="migration-step">
                <div className="migration-step-number">
                  {step.order}
                </div>
                <div>
                  <div className="font-semibold mb-4">{step.title}</div>
                  <div className="text-secondary text-md mb-6">{step.description}</div>
                  <div className="row gap-8">
                    <span className="migration-badge-step">{step.category}</span>
                    <span className="badge-info">{step.effort}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {plan.recommendedServices && plan.recommendedServices.length > 0 && (
            <>
              <h2 className="text-3xl mb-12">Recommended CF Services</h2>
              <div className="grid-auto-200 mb-24">
                {plan.recommendedServices.map((svc) => (
                  <span key={svc} className="migration-service-badge row gap-4">
                    <CheckCircle size={14} />
                    {svc}
                  </span>
                ))}
              </div>
            </>
          )}

          {plan.risks && plan.risks.length > 0 && (
            <>
              <h2 className="text-3xl mb-12">Risks</h2>
              <ul className="migration-risk-list">
                {plan.risks.map((risk, i) => (
                  <li key={i}>{risk}</li>
                ))}
              </ul>
            </>
          )}
        </div>
      )}
      </div>
    </div>
  )
}
