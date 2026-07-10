import { formatMoney } from '@/api'

export function ServiceAddonCard({
  service,
  inCart = false,
  onAddToCart,
  onRemoveFromCart,
  compact = false,
}) {
  const priceLabel = service.free ? 'Free' : formatMoney(service.price, 'PHP')

  return (
    <article className={`service-addon-card ${compact ? 'service-addon-card--compact' : ''}`}>
      <div className="service-addon-card-media">
        {service.imageUrl ? (
          <img src={service.imageUrl} alt="" className="service-addon-card-image" />
        ) : (
          <div className="service-addon-card-image service-addon-card-image--placeholder" aria-hidden />
        )}
      </div>
      <div className="service-addon-card-body">
        <div className="service-addon-card-head">
          <h3 className="service-addon-card-title">{service.title}</h3>
          {service.subtitle ? (
            <p className="service-addon-card-subtitle">{service.subtitle}</p>
          ) : null}
        </div>
        {service.details ? <p className="service-addon-card-details">{service.details}</p> : null}
        <div className="service-addon-card-footer">
          <span className={`service-addon-card-price ${service.free ? 'is-free' : ''}`}>
            {priceLabel}
          </span>
          {onAddToCart || onRemoveFromCart ? (
            <button
              type="button"
              className={`service-addon-card-action ${inCart ? 'is-added' : ''}`}
              onClick={inCart ? onRemoveFromCart : onAddToCart}
            >
              {inCart ? 'Remove' : 'Add to cart'}
            </button>
          ) : null}
        </div>
      </div>
    </article>
  )
}
