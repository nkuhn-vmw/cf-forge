import { useState } from 'react'
import { FileCode, Loader2, CheckCircle, AlertTriangle } from 'lucide-react'

interface MigrationPlan {
  sourceStack: string
  targetStack: string
  complexityScore: string
  steps: { order: number; title: string; description: string; category: string; effort: string }[]
  recommendedServices: string[]
  risks: string[]
  effortEstimate: Record<string, string>
}

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
      const res = await fetch('/api/v1/migration/analyze', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ code, description, sourceStack }),
      })
      if (!res.ok) throw new Error('Analysis failed')
      const result = await res.json()
      setPlan(result)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Unknown error')
    } finally {
      setLoading(false)
    }
  }

  const complexityColor = (score: string) => {
    switch (score) {
      case 'LOW': return '#22c55e'
      case 'MEDIUM': return '#eab308'
      case 'HIGH': return '#f97316'
      case 'CRITICAL': return '#ef4444'
      default: return '#94a3b8'
    }
  }

  return (
    <div style={{ padding: 32, maxWidth: 1000, margin: '0 auto' }}>
      <h1 style={{ fontSize: 28, marginBottom: 8 }}>AI Migration Assistant</h1>
      <p style={{ color: '#94a3b8', marginBottom: 24 }}>
        Analyze legacy applications and generate migration plans to Cloud Foundry + Spring Boot
      </p>

      {!plan ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div>
            <label style={{ display: 'block', marginBottom: 6, color: '#cbd5e1', fontSize: 14 }}>
              Source Technology Stack
            </label>
            <select
              value={sourceStack}
              onChange={(e) => setSourceStack(e.target.value)}
              style={{ background: '#1e293b', color: '#e2e8f0', border: '1px solid #334155',
                       padding: '8px 12px', borderRadius: 6, width: '100%' }}
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
            <label style={{ display: 'block', marginBottom: 6, color: '#cbd5e1', fontSize: 14 }}>
              Application Description
            </label>
            <input
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="e.g., Customer portal with user auth, order management, payment integration..."
              style={{ background: '#1e293b', color: '#e2e8f0', border: '1px solid #334155',
                       padding: '8px 12px', borderRadius: 6, width: '100%' }}
            />
          </div>

          <div>
            <label style={{ display: 'block', marginBottom: 6, color: '#cbd5e1', fontSize: 14 }}>
              Code Samples (paste key files — web.xml, pom.xml, controllers, configs)
            </label>
            <textarea
              value={code}
              onChange={(e) => setCode(e.target.value)}
              rows={16}
              placeholder="Paste representative code snippets here..."
              style={{ background: '#1e293b', color: '#e2e8f0', border: '1px solid #334155',
                       padding: 12, borderRadius: 6, width: '100%', fontFamily: 'monospace',
                       fontSize: 13, resize: 'vertical' }}
            />
          </div>

          {error && (
            <div style={{ color: '#ef4444', display: 'flex', gap: 8, alignItems: 'center' }}>
              <AlertTriangle size={16} /> {error}
            </div>
          )}

          <button
            onClick={analyze}
            disabled={loading || !code.trim()}
            style={{ background: '#2563eb', color: 'white', border: 'none', padding: '12px 24px',
                     borderRadius: 8, fontSize: 16, cursor: 'pointer', display: 'flex',
                     alignItems: 'center', gap: 8, justifyContent: 'center', opacity: loading ? 0.7 : 1 }}
          >
            {loading ? <Loader2 size={18} className="spin" /> : <FileCode size={18} />}
            {loading ? 'Analyzing...' : 'Analyze & Generate Migration Plan'}
          </button>
        </div>
      ) : (
        <div>
          <button onClick={() => setPlan(null)}
            style={{ background: '#334155', color: '#e2e8f0', border: 'none', padding: '6px 14px',
                     borderRadius: 6, cursor: 'pointer', marginBottom: 20 }}>
            New Analysis
          </button>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                        gap: 12, marginBottom: 24 }}>
            <div style={{ background: '#1e293b', padding: 16, borderRadius: 8, border: '1px solid #334155' }}>
              <div style={{ color: '#94a3b8', fontSize: 12, marginBottom: 4 }}>Complexity</div>
              <div style={{ fontSize: 24, fontWeight: 700, color: complexityColor(plan.complexityScore) }}>
                {plan.complexityScore}
              </div>
            </div>
            <div style={{ background: '#1e293b', padding: 16, borderRadius: 8, border: '1px solid #334155' }}>
              <div style={{ color: '#94a3b8', fontSize: 12, marginBottom: 4 }}>Source</div>
              <div style={{ fontSize: 18, fontWeight: 600 }}>{plan.sourceStack}</div>
            </div>
            <div style={{ background: '#1e293b', padding: 16, borderRadius: 8, border: '1px solid #334155' }}>
              <div style={{ color: '#94a3b8', fontSize: 12, marginBottom: 4 }}>Target</div>
              <div style={{ fontSize: 18, fontWeight: 600 }}>{plan.targetStack}</div>
            </div>
            <div style={{ background: '#1e293b', padding: 16, borderRadius: 8, border: '1px solid #334155' }}>
              <div style={{ color: '#94a3b8', fontSize: 12, marginBottom: 4 }}>Steps</div>
              <div style={{ fontSize: 24, fontWeight: 700, color: '#38bdf8' }}>{plan.steps?.length ?? 0}</div>
            </div>
          </div>

          <h2 style={{ fontSize: 20, marginBottom: 12 }}>Migration Steps</h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginBottom: 24 }}>
            {plan.steps?.map((step) => (
              <div key={step.order} style={{ background: '#1e293b', padding: 16, borderRadius: 8,
                          border: '1px solid #334155', display: 'flex', gap: 12 }}>
                <div style={{ background: '#1e3a5f', color: '#38bdf8', borderRadius: '50%',
                              width: 32, height: 32, display: 'flex', alignItems: 'center',
                              justifyContent: 'center', flexShrink: 0, fontWeight: 700 }}>
                  {step.order}
                </div>
                <div>
                  <div style={{ fontWeight: 600, marginBottom: 4 }}>{step.title}</div>
                  <div style={{ color: '#94a3b8', fontSize: 14 }}>{step.description}</div>
                  <div style={{ marginTop: 6, display: 'flex', gap: 8 }}>
                    <span style={{ background: '#334155', padding: '2px 8px', borderRadius: 4,
                                   fontSize: 12 }}>{step.category}</span>
                    <span style={{ background: '#1e3a5f', color: '#7dd3fc', padding: '2px 8px',
                                   borderRadius: 4, fontSize: 12 }}>{step.effort}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {plan.recommendedServices && plan.recommendedServices.length > 0 && (
            <>
              <h2 style={{ fontSize: 20, marginBottom: 12 }}>Recommended CF Services</h2>
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 24 }}>
                {plan.recommendedServices.map((svc) => (
                  <span key={svc} style={{ background: '#065f46', color: '#6ee7b7', padding: '4px 12px',
                                          borderRadius: 6, fontSize: 14 }}>
                    <CheckCircle size={14} style={{ marginRight: 4, verticalAlign: 'middle' }} />
                    {svc}
                  </span>
                ))}
              </div>
            </>
          )}

          {plan.risks && plan.risks.length > 0 && (
            <>
              <h2 style={{ fontSize: 20, marginBottom: 12 }}>Risks</h2>
              <ul style={{ color: '#fbbf24', paddingLeft: 20, marginBottom: 24 }}>
                {plan.risks.map((risk, i) => (
                  <li key={i} style={{ marginBottom: 6 }}>{risk}</li>
                ))}
              </ul>
            </>
          )}
        </div>
      )}
    </div>
  )
}
