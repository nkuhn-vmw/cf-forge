import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import {
  ArrowLeft, Store, Database, Shield, Search, MessageSquare, Gauge,
  Sparkles, Plus, Loader2, Tag, Filter,
} from 'lucide-react'
import { useMarketplace, useServiceRecommendations, useProvisionService } from '../../api/queries.ts'

const SERVICE_ICONS: Record<string, typeof Database> = {
  postgres: Database,
  mysql: Database,
  redis: Database,
  rabbitmq: MessageSquare,
  'p-identity': Shield,
  'p.mysql': Database,
  'p-redis': Database,
  'p-rabbitmq': MessageSquare,
}

const CATEGORIES = ['All', 'Database', 'Messaging', 'Cache', 'Identity', 'Other'] as const

function categorize(name: string): string {
  const n = name.toLowerCase()
  if (n.includes('sql') || n.includes('postgres') || n.includes('mongo') || n.includes('db')) return 'Database'
  if (n.includes('rabbit') || n.includes('kafka') || n.includes('mq')) return 'Messaging'
  if (n.includes('redis') || n.includes('cache') || n.includes('memcache')) return 'Cache'
  if (n.includes('identity') || n.includes('auth') || n.includes('sso') || n.includes('uaa')) return 'Identity'
  return 'Other'
}

export function Marketplace() {
  const [searchParams] = useSearchParams()
  const projectId = searchParams.get('projectId') ?? ''
  const { data: services, isLoading } = useMarketplace()
  const { data: recommendations } = useServiceRecommendations(projectId)
  const provision = useProvisionService(projectId)
  const [searchTerm, setSearchTerm] = useState('')
  const [activeCategory, setActiveCategory] = useState<string>('All')
  const [provisioningName, setProvisioningName] = useState<string | null>(null)

  const filtered = services?.filter((svc) => {
    const matchesSearch = !searchTerm ||
      svc.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      svc.description.toLowerCase().includes(searchTerm.toLowerCase())
    const matchesCategory = activeCategory === 'All' || categorize(svc.name) === activeCategory
    return matchesSearch && matchesCategory
  })

  const handleProvision = async (serviceName: string, plan: string) => {
    const instanceName = `${serviceName}-${Date.now().toString(36)}`
    setProvisioningName(serviceName)
    try {
      await provision.mutateAsync({ serviceName, plan, instanceName })
    } finally {
      setProvisioningName(null)
    }
  }

  return (
    <div style={{ height: '100%', overflow: 'auto', backgroundColor: 'var(--bg-primary)' }}>
      <header
        style={{
          padding: '12px 24px',
          borderBottom: '1px solid var(--border)',
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          backgroundColor: 'var(--bg-secondary)',
        }}
      >
        <Link to="/dashboard" style={{ color: 'var(--text-muted)', display: 'flex' }}>
          <ArrowLeft size={18} />
        </Link>
        <Store size={20} color="var(--accent)" />
        <h1 style={{ fontSize: '16px', fontWeight: 600 }}>Service Marketplace</h1>
      </header>

      <div style={{ maxWidth: '960px', margin: '0 auto', padding: '24px' }}>
        {/* AI Recommendations */}
        {recommendations && recommendations.length > 0 && (
          <div style={{ marginBottom: '24px', padding: '16px', backgroundColor: 'var(--bg-secondary)', border: '1px solid var(--accent)', borderRadius: '8px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '12px' }}>
              <Sparkles size={16} color="var(--accent)" />
              <span style={{ fontSize: '13px', fontWeight: 600 }}>AI Recommended Services</span>
            </div>
            <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
              {recommendations.map((rec) => (
                <div
                  key={rec.serviceName}
                  style={{
                    padding: '10px 14px', backgroundColor: 'var(--bg-primary)',
                    border: '1px solid var(--border)', borderRadius: '6px',
                    display: 'flex', alignItems: 'center', gap: '10px', fontSize: '13px',
                  }}
                >
                  <div>
                    <div style={{ fontWeight: 500 }}>{rec.serviceName} <span style={{ color: 'var(--text-muted)', fontSize: '11px' }}>({rec.plan})</span></div>
                    <div style={{ color: 'var(--text-muted)', fontSize: '11px', marginTop: '2px' }}>{rec.reason}</div>
                  </div>
                  <button
                    onClick={() => handleProvision(rec.serviceName, rec.plan)}
                    disabled={provisioningName === rec.serviceName}
                    style={{
                      padding: '4px 10px', backgroundColor: 'var(--accent)', border: 'none',
                      borderRadius: '4px', color: 'white', fontSize: '11px', display: 'flex',
                      alignItems: 'center', gap: '4px', whiteSpace: 'nowrap',
                    }}
                  >
                    {provisioningName === rec.serviceName ? <Loader2 size={11} /> : <Plus size={11} />}
                    Bind
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Search + Filter */}
        <div style={{ marginBottom: '20px', display: 'flex', gap: '12px', alignItems: 'center' }}>
          <div
            style={{
              flex: 1, display: 'flex', alignItems: 'center', gap: '8px', padding: '10px 16px',
              backgroundColor: 'var(--bg-secondary)', border: '1px solid var(--border)',
              borderRadius: '8px',
            }}
          >
            <Search size={16} color="var(--text-muted)" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Search services..."
              style={{
                flex: 1, background: 'none', border: 'none', color: 'var(--text-primary)',
                fontSize: '14px', outline: 'none',
              }}
            />
          </div>
        </div>

        {/* Category chips */}
        <div style={{ display: 'flex', gap: '6px', marginBottom: '20px', flexWrap: 'wrap' }}>
          {CATEGORIES.map((cat) => (
            <button
              key={cat}
              onClick={() => setActiveCategory(cat)}
              style={{
                padding: '4px 12px', fontSize: '12px', borderRadius: '12px', border: 'none',
                backgroundColor: activeCategory === cat ? 'var(--accent)' : 'var(--bg-tertiary)',
                color: activeCategory === cat ? 'white' : 'var(--text-secondary)',
                display: 'flex', alignItems: 'center', gap: '4px',
              }}
            >
              {cat === 'All' ? <Filter size={11} /> : <Tag size={11} />}
              {cat}
            </button>
          ))}
        </div>

        {isLoading ? (
          <div style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>Loading marketplace...</div>
        ) : !filtered?.length ? (
          <div style={{ textAlign: 'center', padding: '60px', color: 'var(--text-muted)' }}>
            <Store size={48} style={{ marginBottom: '16px', opacity: 0.3 }} />
            <p style={{ fontSize: '16px', marginBottom: '8px' }}>
              {searchTerm || activeCategory !== 'All' ? 'No matching services' : 'No services available'}
            </p>
            <p style={{ fontSize: '13px' }}>
              {searchTerm || activeCategory !== 'All' ? 'Try adjusting your filters' : 'Connect a CF target to browse the marketplace'}
            </p>
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '16px' }}>
            {filtered.map((service) => {
              const Icon = SERVICE_ICONS[service.name] ?? Gauge
              return (
                <div
                  key={service.name}
                  style={{
                    padding: '20px',
                    backgroundColor: 'var(--bg-secondary)',
                    border: '1px solid var(--border)',
                    borderRadius: '8px',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '10px' }}>
                    <Icon size={20} color="var(--accent)" />
                    <h3 style={{ fontSize: '15px', fontWeight: 600 }}>{service.name}</h3>
                    <span style={{ fontSize: '10px', padding: '1px 6px', backgroundColor: 'var(--bg-tertiary)', borderRadius: '3px', color: 'var(--text-muted)', marginLeft: 'auto' }}>
                      {categorize(service.name)}
                    </span>
                  </div>
                  <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginBottom: '12px', lineHeight: '1.5' }}>
                    {service.description}
                  </p>
                  {service.plans?.length > 0 && (
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px', marginBottom: '12px' }}>
                      {service.plans.map((plan) => (
                        <span
                          key={plan}
                          style={{
                            padding: '2px 8px', fontSize: '11px', backgroundColor: 'var(--bg-tertiary)',
                            borderRadius: '4px', color: 'var(--text-muted)',
                          }}
                        >
                          {plan}
                        </span>
                      ))}
                    </div>
                  )}
                  {projectId && service.plans?.length > 0 && (
                    <button
                      onClick={() => handleProvision(service.name, service.plans[0])}
                      disabled={provisioningName === service.name || provision.isSuccess}
                      style={{
                        width: '100%', padding: '6px 12px', backgroundColor: 'var(--bg-tertiary)',
                        border: '1px solid var(--border)', borderRadius: '4px',
                        color: 'var(--text-primary)', fontSize: '12px', display: 'flex',
                        alignItems: 'center', justifyContent: 'center', gap: '6px',
                      }}
                    >
                      {provisioningName === service.name ? (
                        <><Loader2 size={12} /> Provisioning...</>
                      ) : (
                        <><Plus size={12} /> Provision & Bind</>
                      )}
                    </button>
                  )}
                </div>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )
}
