import { Link } from 'react-router-dom'
import { ArrowLeft, Store, Database, Shield, Search, MessageSquare, Gauge } from 'lucide-react'
import { useMarketplace } from '../../api/queries.ts'

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

export function Marketplace() {
  const { data: services, isLoading } = useMarketplace()

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
        <div style={{ marginBottom: '24px' }}>
          <div
            style={{
              display: 'flex', alignItems: 'center', gap: '8px', padding: '10px 16px',
              backgroundColor: 'var(--bg-secondary)', border: '1px solid var(--border)',
              borderRadius: '8px',
            }}
          >
            <Search size={16} color="var(--text-muted)" />
            <input
              type="text"
              placeholder="Search services..."
              style={{
                flex: 1, background: 'none', border: 'none', color: 'var(--text-primary)',
                fontSize: '14px', outline: 'none',
              }}
            />
          </div>
        </div>

        {isLoading ? (
          <div style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>Loading marketplace...</div>
        ) : !services?.length ? (
          <div style={{ textAlign: 'center', padding: '60px', color: 'var(--text-muted)' }}>
            <Store size={48} style={{ marginBottom: '16px', opacity: 0.3 }} />
            <p style={{ fontSize: '16px', marginBottom: '8px' }}>No services available</p>
            <p style={{ fontSize: '13px' }}>Connect a CF target to browse the marketplace</p>
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '16px' }}>
            {services.map((service) => {
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
                  </div>
                  <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginBottom: '12px', lineHeight: '1.5' }}>
                    {service.description}
                  </p>
                  {service.plans?.length > 0 && (
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px' }}>
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
                </div>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )
}
