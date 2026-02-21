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
  const [provisionedServices, setProvisionedServices] = useState<Set<string>>(new Set())

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
      setProvisionedServices((prev) => new Set(prev).add(serviceName))
    } finally {
      setProvisioningName(null)
    }
  }

  return (
    <div className="page">
      <header className="page-header">
        <Link to="/dashboard" className="btn-icon">
          <ArrowLeft size={18} />
        </Link>
        <Store size={20} color="var(--accent)" />
        <h1 className="page-header-title">Service Marketplace</h1>
      </header>

      <div className="content-container">
        {/* AI Recommendations */}
        {recommendations && recommendations.length > 0 && (
          <div className="recommendation-card">
            <div className="row mb-12">
              <Sparkles size={16} color="var(--accent)" />
              <span className="text-base font-semibold">AI Recommended Services</span>
            </div>
            <div className="row gap-12" style={{ flexWrap: 'wrap' }}>
              {recommendations.map((rec) => (
                <div key={rec.serviceName} className="card row gap-10 text-base">
                  <div>
                    <div className="font-medium">{rec.serviceName} <span className="text-muted text-sm">({rec.plan})</span></div>
                    <div className="text-muted text-sm" style={{ marginTop: '2px' }}>{rec.reason}</div>
                  </div>
                  <button
                    onClick={() => handleProvision(rec.serviceName, rec.plan)}
                    disabled={provisioningName === rec.serviceName}
                    className="btn-primary btn-sm nowrap"
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
        <div className="row mb-20 gap-12">
          <div className="search-bar">
            <Search size={16} color="var(--text-muted)" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Search services..."
            />
          </div>
        </div>

        {/* Category chips */}
        <div className="row gap-6 mb-20" style={{ flexWrap: 'wrap' }}>
          {CATEGORIES.map((cat) => (
            <button
              key={cat}
              onClick={() => setActiveCategory(cat)}
              className={`chip-toggle ${activeCategory === cat ? 'active' : ''}`}
            >
              {cat === 'All' ? <Filter size={11} /> : <Tag size={11} />}
              {cat}
            </button>
          ))}
        </div>

        {isLoading ? (
          <div className="empty-state-sm">Loading marketplace...</div>
        ) : !filtered?.length ? (
          <div className="empty-state">
            <Store size={48} className="empty-state-icon" />
            <p className="empty-state-title">
              {searchTerm || activeCategory !== 'All' ? 'No matching services' : 'No services available'}
            </p>
            <p className="empty-state-text">
              {searchTerm || activeCategory !== 'All' ? 'Try adjusting your filters' : 'Connect a CF target to browse the marketplace'}
            </p>
          </div>
        ) : (
          <div className="grid-auto-280">
            {filtered.map((service) => {
              const Icon = SERVICE_ICONS[service.name] ?? Gauge
              return (
                <div key={service.name} className="card-lg">
                  <div className="row gap-10 mb-10">
                    <Icon size={20} color="var(--accent)" />
                    <h3 className="text-lg font-semibold">{service.name}</h3>
                    <span className="badge-category">
                      {categorize(service.name)}
                    </span>
                  </div>
                  <p className="text-base text-secondary mb-12" style={{ lineHeight: '1.5' }}>
                    {service.description}
                  </p>
                  {service.plans?.length > 0 && (
                    <div className="row gap-4 mb-12" style={{ flexWrap: 'wrap' }}>
                      {service.plans.map((plan) => (
                        <span key={plan} className="badge">
                          {plan}
                        </span>
                      ))}
                    </div>
                  )}
                  {projectId && service.plans?.length > 0 && (
                    <button
                      onClick={() => handleProvision(service.name, service.plans[0])}
                      disabled={provisioningName === service.name || provisionedServices.has(service.name)}
                      className="btn-provision"
                    >
                      {provisioningName === service.name ? (
                        <><Loader2 size={12} /> Provisioning...</>
                      ) : provisionedServices.has(service.name) ? (
                        <><Database size={12} /> Provisioned</>
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
